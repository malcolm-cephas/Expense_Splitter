import { NextApiResponse } from 'next';
import connectDB from '../_db.js';
import Group from '../_models/Group.js';
import Expense from '../_models/Expense.js';
import User from '../_models/User.js';
import { withAuth, AuthenticatedRequest } from '../_middleware.js';
import { Decimal } from 'decimal.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const auth0Id = req.user!.sub;

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const data = req.body;
    const groupData = data.group || data;
    const expensesData = data.expenses || [];
    
    // 1. Identify the current user in our system
    const currentUser = await User.findOne({ auth0Id });
    if (!currentUser) {
      return res.status(404).json({ error: 'User not found in system' });
    }

    // 2. Map all users found in the import
    const nameToUserMap = new Map<string, any>();
    
    // We prioritize the current user's name from the token/DB
    nameToUserMap.set(currentUser.name, currentUser);
    
    const findOrCreateUser = async (name: string) => {
      if (!name) return currentUser;
      if (nameToUserMap.has(name)) return nameToUserMap.get(name);
      
      // Try to find an existing user by name in the database
      let targetUser = await User.findOne({ name });
      
      if (!targetUser) {
        // Create a placeholder user for members not in the system
        const slug = name.toLowerCase().replace(/\s+/g, '.');
        const random = Math.random().toString(36).substring(2, 6);
        const placeholderEmail = `${slug}.${random}@import.splitpro`;
        
        targetUser = await User.create({
          auth0Id: `import|${name.replace(/\s+/g, '_')}|${Date.now()}`,
          name: name,
          email: placeholderEmail,
          currencyPreference: groupData.budgetCurrency || 'USD'
        });
      }
      
      nameToUserMap.set(name, targetUser);
      return targetUser;
    };

    // Pre-extract all names from group members if available
    if (groupData.members && Array.isArray(groupData.members)) {
      for (const m of groupData.members) {
        const name = m.name || (m.user && m.user.name);
        if (name) await findOrCreateUser(name);
      }
    }

    // Also scan expenses for any additional names (fallback)
    for (const exp of expensesData) {
      if (exp.payments && Array.isArray(exp.payments)) {
        for (const p of exp.payments) {
          const name = p.userName || (p.user && p.user.name);
          if (name) await findOrCreateUser(name);
        }
      }
      if (exp.shares && Array.isArray(exp.shares)) {
        for (const s of exp.shares) {
          const name = s.userName || (s.user && s.user.name);
          if (name) await findOrCreateUser(name);
        }
      }
    }

    // 3. Create the Group with all mapped members
    const groupMembers = Array.from(nameToUserMap.values()).map(u => ({
      userId: u._id,
      role: u._id.equals(currentUser._id) ? 'admin' : 'member'
    }));

    const newGroup = await Group.create({
      name: groupData.name || 'Imported Group',
      description: groupData.description || 'Imported from SplitPro JSON',
      budget: (groupData.budget || '0').toString(),
      budgetCurrency: groupData.budgetCurrency || 'INR',
      familyGroupingEnabled: groupData.familyGroupingEnabled || false,
      members: groupMembers,
      createdBy: currentUser._id,
    });

    // 4. Create Expenses
    if (expensesData && Array.isArray(expensesData)) {
      const expensesToCreate = [];
      
      for (const exp of expensesData) {
        const amountStr = (exp.amount || '0').toString();
        const totalAmount = new Decimal(amountStr);
        
        // Handle date array format [2026, 3, 21] vs ISO string
        let expenseDate = new Date();
        if (Array.isArray(exp.expenseDate)) {
          const [year, month, day] = exp.expenseDate;
          expenseDate = new Date(year, month - 1, day);
        } else if (exp.expenseDate) {
          expenseDate = new Date(exp.expenseDate);
        }

        // Map Payers (Multiple Payers Support)
        let payers = [];
        if (exp.payments && Array.isArray(exp.payments)) {
          for (const p of exp.payments) {
            const name = p.userName || (p.user && p.user.name);
            const u = await findOrCreateUser(name);
            payers.push({ 
              userId: u._id, 
              amount: new Decimal(p.amount || 0).toString() 
            });
          }
        } else {
          // Fallback to current user if no multi-payer data exists
          payers = [{ userId: currentUser._id, amount: amountStr }];
        }

        // Map Splits/Shares
        let splits = [];
        if (exp.shares && Array.isArray(exp.shares)) {
          for (const s of exp.shares) {
            const name = s.userName || (s.user && s.user.name);
            const u = await findOrCreateUser(name);
            splits.push({ 
              userId: u._id, 
              owedAmount: new Decimal(s.amount || s.owedAmount || 0).toString(),
              paidAmount: new Decimal(s.paidAmount || 0).toString(),
              isPaid: !!s.isPaid
            });
          }
        } else if (exp.splits && Array.isArray(exp.splits)) {
          // Alternative field name
          for (const s of exp.splits) {
             const name = s.userName || (s.user && s.user.name) || (s.userId && s.userId.name);
             const u = await findOrCreateUser(name);
             splits.push({
               userId: u._id,
               owedAmount: new Decimal(s.owedAmount || s.amount || 0).toString(),
               paidAmount: new Decimal(s.paidAmount || 0).toString(),
               isPaid: !!s.isPaid
             });
          }
        } else {
          // Default to equal split among all group members
          const shareAmount = totalAmount.dividedBy(groupMembers.length || 1).toDecimalPlaces(2);
          splits = groupMembers.map(m => ({
            userId: m.userId,
            owedAmount: shareAmount.toString(),
            paidAmount: '0',
            isPaid: false
          }));
        }

        expensesToCreate.push({
          groupId: newGroup._id,
          description: exp.description || 'Imported Expense',
          amount: amountStr,
          currency: exp.currency || groupData.budgetCurrency || 'INR',
          splitType: (exp.splitType || 'equal').toLowerCase(),
          category: exp.category || 'General',
          expenseDate: expenseDate,
          payers,
          splits,
          createdBy: currentUser._id,
        });
      }
      
      if (expensesToCreate.length > 0) {
        await Expense.insertMany(expensesToCreate);
      }
    }

    return res.status(201).json({ data: newGroup });
  } catch (error) {
    console.error('Import Error:', error);
    return res.status(500).json({ error: 'Failed to import group: ' + (error as any).message });
  }
}

export default withAuth(handler);

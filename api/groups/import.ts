import { NextApiResponse } from 'next';
import connectDB from '../_db.js';
import Group from '../_models/Group.js';
import Expense from '../_models/Expense.js';
import User from '../_models/User.js';
import { withAuth, AuthenticatedRequest } from '../_middleware.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const auth0Id = req.user!.sub;

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const { group, expenses } = req.body;
    
    // Find the internal user ID
    const user = await User.findOne({ auth0Id });
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    // 1. Create the Group
    const newGroup = await Group.create({
      name: group?.name || 'Imported Group',
      description: group?.description || 'Imported from JSON',
      budget: group?.budget || '0',
      budgetCurrency: group?.budgetCurrency || 'USD',
      familyGroupingEnabled: group?.familyGroupingEnabled || false,
      members: [{ userId: user._id, role: 'admin' }],
      createdBy: user._id,
    });

    // 2. Create Expenses
    if (expenses && Array.isArray(expenses)) {
      const expensesToCreate = expenses.map((exp: any) => {
        const amount = (exp.amount || '0').toString();
        
        // Handle date array format [2026, 3, 21]
        let expenseDate = new Date();
        if (Array.isArray(exp.expenseDate)) {
          const [year, month, day] = exp.expenseDate;
          expenseDate = new Date(year, month - 1, day);
        } else if (exp.expenseDate) {
          expenseDate = new Date(exp.expenseDate);
        }

        return {
          groupId: newGroup._id,
          description: exp.description || 'Imported Expense',
          amount: amount,
          currency: exp.currency || group?.budgetCurrency || 'INR',
          splitType: (exp.splitType || 'equal').toLowerCase(),
          category: exp.category || 'General',
          expenseDate: expenseDate,
          payers: [{ userId: user._id, amount: amount }],
          splits: [{ userId: user._id, owedAmount: amount, paidAmount: amount, isPaid: true }],
          createdBy: user._id,
        };
      });
      
      await Expense.insertMany(expensesToCreate);
    }

    return res.status(201).json({ data: newGroup });
  } catch (error) {
    console.error('Import Error:', error);
    return res.status(500).json({ error: 'Failed to import group' });
  }
}

export default withAuth(handler);

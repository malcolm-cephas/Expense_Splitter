import { NextApiResponse } from 'next';
import connectDB from '../_db.js';
import Group from '../_models/Group.js';
import Expense from '../_models/Expense.js';
import User from '../_models/User.js';
import PendingInvite from '../_models/PendingInvite.js';
import { withAuth, AuthenticatedRequest } from '../_middleware.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const auth0Id = req.user!.sub;

  const user = await User.findOne({ auth0Id });
  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }

  if (req.method === 'GET') {
    try {
      const groups = await Group.find({
        'members.userId': user._id,
      }).lean();

      const groupIds = groups.map(g => g._id);
      
      // Bulk fetch all expenses for these groups to avoid N+1 problem
      const allExpenses = await Expense.find({ groupId: { $in: groupIds } }).lean();
      
      // Group expenses by groupId
      const expensesByGroup = allExpenses.reduce((acc: any, exp: any) => {
        const gid = exp.groupId.toString();
        if (!acc[gid]) acc[gid] = [];
        acc[gid].push(exp);
        return acc;
      }, {});

      const groupsWithStats = groups.map((group: any) => {
        const expenses = expensesByGroup[group._id.toString()] || [];
        
        let totalExpenses = 0;
        let userBalance = 0;

        expenses.forEach((exp: any) => {
          totalExpenses += parseFloat(exp.amount);
          
          const userOwes = exp.splits.find((s: any) => s.userId.toString() === user._id.toString())?.owedAmount || '0';
          const userPaid = exp.payers.find((p: any) => p.userId.toString() === user._id.toString())?.amount || '0';
          
          userBalance += (parseFloat(userPaid) - parseFloat(userOwes));
        });

        return {
          ...group,
          memberCount: group.members.length,
          totalExpenses: totalExpenses.toString(),
          userBalance: userBalance.toString(),
        };
      });

      return res.status(200).json({ data: groupsWithStats });
    } catch (error) {
      console.error('Error fetching groups:', error);
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  } else if (req.method === 'POST') {
    const { name, description, budget, budgetCurrency, familyGroupingEnabled, initialMembers } = req.body;

    try {
      const newGroup = await Group.create({
        name,
        description,
        budget,
        budgetCurrency: budgetCurrency || 'USD',
        familyGroupingEnabled: familyGroupingEnabled || false,
        members: [{ userId: user._id, role: 'admin' }],
        createdBy: user._id,
      });

      // Handle initial members if provided
      if (initialMembers && Array.isArray(initialMembers)) {
        for (const m of initialMembers) {
          const { name: mName, email: mEmail } = typeof m === 'string' ? { name: '', email: m } : m;
          
          if (mEmail) {
            if (mEmail === user.email) continue;
            const targetUser = await User.findOne({ email: mEmail });
            if (targetUser) {
              await Group.findByIdAndUpdate(newGroup._id, {
                $addToSet: { members: { userId: targetUser._id, role: 'member' } },
              });
            } else {
              await PendingInvite.findOneAndUpdate(
                { email: mEmail, groupId: newGroup._id },
                { email: mEmail, groupId: newGroup._id, invitedBy: user._id },
                { upsert: true }
              );
            }
          } else if (mName) {
            // Create ghost user with unique placeholder email
            const ghost = await User.create({
              name: mName,
              isGhost: true,
              email: `ghost_${Date.now()}_${Math.random().toString(36).substring(7)}@managed.local`,
              currencyPreference: user.currencyPreference || 'USD'
            });
            await Group.findByIdAndUpdate(newGroup._id, {
              $addToSet: { members: { userId: ghost._id, role: 'member' } },
            });
          }
        }
      }

      // Re-fetch to get updated members
      const finalGroup = await Group.findById(newGroup._id).lean();
      return res.status(201).json({ data: finalGroup });
    } catch (error) {
      console.error('Error creating group:', error);
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  } else {
    res.setHeader('Allow', ['GET', 'POST']);
    return res.status(405).end(`Method ${req.method} Not Allowed`);
  }
}

export default withAuth(handler);

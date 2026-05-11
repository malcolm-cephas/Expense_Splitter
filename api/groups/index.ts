import { NextApiResponse } from 'next';
import connectDB from '../_db.js';
import Group from '../_models/Group.js';
import Expense from '../_models/Expense.js';
import { withAuth, AuthenticatedRequest } from '../_middleware.js';
import User from '../_models/User.js';
import mongoose from 'mongoose';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const auth0Id = req.user!.sub;

  // Find the internal user ID first
  const user = await User.findOne({ auth0Id });
  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }

  if (req.method === 'GET') {
    try {
      const groups = await Group.find({
        'members.userId': user._id,
      }).lean();

      // For each group, we need to calculate total expenses and user balance
      const groupsWithStats = await Promise.all(
        groups.map(async (group: any) => {
          const expenses = await Expense.find({ groupId: group._id });
          
          let totalExpenses = 0;
          let userBalance = 0;

          expenses.forEach((exp: any) => {
            totalExpenses += parseFloat(exp.amount);
            
            // Calculate user's net balance in this expense
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
        })
      );

      return res.status(200).json({ data: groupsWithStats });
    } catch (error) {
      console.error('Error fetching groups:', error);
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  } else if (req.method === 'POST') {
    const { name, description, budget, budgetCurrency, familyGroupingEnabled } = req.body;

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

      return res.status(201).json({ data: newGroup });
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

import { NextApiResponse } from 'next';
import connectDB from '../../_db.js';
import Group from '../../_models/Group.js';
import Expense from '../../_models/Expense.js';
import User from '../../_models/User.js';
import { withAuth, AuthenticatedRequest } from '../../_middleware.js';
import mongoose from 'mongoose';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const { id } = req.query;
  const auth0Id = req.user!.sub;

  const currentUser = await User.findOne({ auth0Id });
  if (!currentUser) return res.status(404).json({ error: 'User not found' });

  if (req.method === 'GET') {
    try {
      const group = await Group.findById(id).populate('members.userId', 'name email picture familyName').lean();
      if (!group) return res.status(404).json({ error: 'Group not found' });

      // Check if user is member
      const isMember = group.members.some((m: any) => m.userId._id.toString() === currentUser._id.toString());
      if (!isMember) return res.status(403).json({ error: 'Forbidden' });

      // Calculate total expenses and member balances
      const expenses = await Expense.find({ groupId: id });
      let totalExpenses = 0;
      const memberBalances: Record<string, number> = {};

      group.members.forEach((m: any) => {
        memberBalances[m.userId._id.toString()] = 0;
      });

      expenses.forEach((exp: any) => {
        totalExpenses += parseFloat(exp.amount);
        
        // Subtract what they owe
        exp.splits.forEach((s: any) => {
          if (memberBalances[s.userId.toString()] !== undefined) {
            memberBalances[s.userId.toString()] -= parseFloat(s.owedAmount);
          }
        });

        // Add what they paid
        exp.payers.forEach((p: any) => {
          if (memberBalances[p.userId.toString()] !== undefined) {
            memberBalances[p.userId.toString()] += parseFloat(p.amount);
          }
        });
      });

      const groupWithStats = {
        ...group,
        totalExpenses: totalExpenses.toString(),
        members: group.members.map((m: any) => ({
          ...m,
          balance: memberBalances[m.userId._id.toString()].toString(),
        })),
      };

      return res.status(200).json({ data: groupWithStats });
    } catch (error) {
      console.error(error);
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  }

  if (req.method === 'PUT') {
    try {
      const updatedGroup = await Group.findByIdAndUpdate(id, req.body, { new: true });
      return res.status(200).json({ data: updatedGroup });
    } catch (error) {
      return res.status(500).json({ error: 'Failed to update group' });
    }
  }

  if (req.method === 'DELETE') {
    try {
      await Group.findByIdAndDelete(id);
      // Also delete all expenses for this group
      await Expense.deleteMany({ groupId: id });
      return res.status(200).json({ message: 'Group and associated expenses deleted' });
    } catch (error) {
      return res.status(500).json({ error: 'Failed to delete group' });
    }
  }

  return res.status(405).end(`Method ${req.method} Not Allowed`);
}

export default withAuth(handler);

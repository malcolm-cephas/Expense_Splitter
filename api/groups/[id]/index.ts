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

      // High-performance aggregation to calculate total expenses and balances
      const stats = await Expense.aggregate([
        { $match: { groupId: new mongoose.Types.ObjectId(id as string) } },
        {
          $facet: {
            totalAmount: [
              { $group: { _id: null, sum: { $sum: { $toDouble: "$amount" } } } }
            ],
            memberBalances: [
              { $project: { payers: 1, splits: 1 } },
              { $facet: {
                paid: [
                  { $unwind: "$payers" },
                  { $group: { _id: "$payers.userId", totalPaid: { $sum: { $toDouble: "$payers.amount" } } } }
                ],
                owed: [
                  { $unwind: "$splits" },
                  { $group: { _id: "$splits.userId", totalOwed: { $sum: { $toDouble: "$splits.owedAmount" } } } }
                ]
              }}
            ]
          }
        }
      ]);

      const totalExpenses = stats[0].totalAmount[0]?.sum || 0;
      const paidBalances = stats[0].memberBalances[0].paid;
      const owedBalances = stats[0].memberBalances[0].owed;

      const balanceMap: Record<string, number> = {};
      group.members.forEach((m: any) => {
        balanceMap[m.userId._id.toString()] = 0;
      });

      paidBalances.forEach((p: any) => {
        if (balanceMap[p._id.toString()] !== undefined) {
          balanceMap[p._id.toString()] += p.totalPaid;
        }
      });

      owedBalances.forEach((o: any) => {
        if (balanceMap[o._id.toString()] !== undefined) {
          balanceMap[o._id.toString()] -= o.totalOwed;
        }
      });

      const groupWithStats = {
        ...group,
        totalExpenses: totalExpenses.toString(),
        members: group.members.map((m: any) => ({
          ...m,
          balance: (balanceMap[m.userId._id.toString()] || 0).toString(),
        })),
      };

      return res.status(200).json({ data: groupWithStats });
    } catch (error) {
      console.error('Aggregation Error:', error);
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
      await Expense.deleteMany({ groupId: id });
      return res.status(200).json({ message: 'Group and associated expenses deleted' });
    } catch (error) {
      return res.status(500).json({ error: 'Failed to delete group' });
    }
  }

  return res.status(405).end(`Method ${req.method} Not Allowed`);
}

export default withAuth(handler);

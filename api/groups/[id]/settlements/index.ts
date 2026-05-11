import { NextApiResponse } from 'next';
import connectDB from '../../../_db.js';
import Group from '../../../_models/Group.js';
import Expense from '../../../_models/Expense.js';
import User from '../../../_models/User.js';
import { withAuth, AuthenticatedRequest } from '../../../_middleware.js';
import { calculateSettlements, MemberBalance } from '../../../_logic/settlements.js';
import { Decimal } from 'decimal.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const { id: groupId } = req.query;
  const auth0Id = req.user!.sub;

  const currentUser = await User.findOne({ auth0Id });
  if (!currentUser) return res.status(404).json({ error: 'User not found' });

  if (req.method === 'GET') {
    try {
      const group = await Group.findById(groupId).populate('members.userId', 'name familyName').lean();
      if (!group) return res.status(404).json({ error: 'Group not found' });

      const expenses = await Expense.find({ groupId });
      const balances: Record<string, MemberBalance> = {};

      group.members.forEach((m: any) => {
        balances[m.userId._id.toString()] = {
          userId: m.userId._id.toString(),
          familyName: m.userId.familyName,
          balance: new Decimal(0),
        };
      });

      expenses.forEach((exp: any) => {
        exp.splits.forEach((s: any) => {
          if (balances[s.userId.toString()]) {
            balances[s.userId.toString()].balance = balances[s.userId.toString()].balance.minus(s.owedAmount);
          }
        });
        exp.payers.forEach((p: any) => {
          if (balances[p.userId.toString()]) {
            balances[p.userId.toString()].balance = balances[p.userId.toString()].balance.plus(p.amount);
          }
        });
      });

      const settlements = calculateSettlements(
        Object.values(balances),
        group.budgetCurrency,
        group.familyGroupingEnabled
      );

      return res.status(200).json({ data: settlements });
    } catch (error) {
      console.error(error);
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  } else if (req.method === 'POST') {
    const { from, to, amount, currency } = req.body;
    
    try {
      const expense = await Expense.create({
        groupId,
        description: `Settlement: ${from} to ${to}`,
        amount,
        currency,
        splitType: 'exact',
        category: 'Settlement',
        expenseDate: new Date(),
        payers: [{ userId: from, amount }],
        splits: [{ userId: to, owedAmount: amount, paidAmount: amount, isPaid: true }],
        createdBy: currentUser._id,
      });
      
      return res.status(201).json({ data: expense });
    } catch (error) {
      console.error(error);
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  } else {
    return res.status(405).end(`Method ${req.method} Not Allowed`);
  }
}

export default withAuth(handler);

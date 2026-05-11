import { NextApiResponse } from 'next';
import connectDB from '../../../_db.js';
import Group from '../../../_models/Group.js';
import Expense from '../../../_models/Expense.js';
import User from '../../../_models/User.js';
import { withAuth, AuthenticatedRequest } from '../../../_middleware.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const { id: groupId, format } = req.query;

  try {
    const group = await Group.findById(groupId).populate('members.userId', 'name email').lean();
    if (!group) return res.status(404).json({ error: 'Group not found' });

    const expenses = await Expense.find({ groupId }).sort({ expenseDate: 1 }).lean();

    if (format === 'json') {
      res.setHeader('Content-Type', 'application/json');
      res.setHeader('Content-Disposition', `attachment; filename=expense_splitter_${groupId}.json`);
      return res.status(200).json({ group, expenses });
    }

    if (format === 'csv') {
      const headers = [
        'Date',
        'Description',
        'Category',
        'Currency',
        'Total Amount',
        'Payers',
        'Split Type',
        ...group.members.map((m: any) => `Owed by ${m.userId.name}`)
      ];

      const rows = expenses.map((exp: any) => {
        const payersInfo = exp.payers.map((p: any) => {
          const m = group.members.find((gm: any) => gm.userId._id.toString() === p.userId.toString());
          return `${m?.userId.name || 'Unknown'}: ${p.amount}`;
        }).join('; ');

        const memberOwed = group.members.map((m: any) => {
          const split = exp.splits.find((s: any) => s.userId.toString() === m.userId._id.toString());
          return split ? split.owedAmount : '0';
        });

        return [
          new Date(exp.expenseDate).toLocaleDateString(),
          exp.description,
          exp.category,
          exp.currency,
          exp.amount,
          `"${payersInfo}"`,
          exp.splitType,
          ...memberOwed
        ];
      });

      const csvContent = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n');

      res.setHeader('Content-Type', 'text/csv');
      res.setHeader('Content-Disposition', `attachment; filename=expense_splitter_${groupId}.csv`);
      return res.status(200).send(csvContent);
    }

    return res.status(400).json({ error: 'Invalid format' });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
}

export default withAuth(handler);

import { NextApiResponse } from 'next';
import connectDB from '../../../_db.js';
import Expense from '../../../_models/Expense.js';
import User from '../../../_models/User.js';
import Group from '../../../_models/Group.js';
import { withAuth, AuthenticatedRequest } from '../../../_middleware.js';
import { Decimal } from 'decimal.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const { id: groupId } = req.query;
  
  try {
    const expenses = await Expense.find({ groupId }).populate('splits.userId', 'name').lean();
    
    const byCategory: Record<string, Decimal> = {};
    const byMember: Record<string, Decimal> = {};
    const byTime: Record<string, Decimal> = {};

    expenses.forEach((exp: any) => {
      // By Category
      byCategory[exp.category] = (byCategory[exp.category] || new Decimal(0)).plus(exp.amount);

      // By Member (using splits to see who actually spent what)
      exp.splits.forEach((s: any) => {
        const name = s.userId?.name || 'Unknown';
        byMember[name] = (byMember[name] || new Decimal(0)).plus(s.owedAmount);
      });

      // By Time (grouped by month)
      const date = new Date(exp.expenseDate);
      const monthYear = `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}`;
      byTime[monthYear] = (byTime[monthYear] || new Decimal(0)).plus(exp.amount);
    });

    const data = {
      byCategory: Object.entries(byCategory).map(([category, amount]) => ({ category, amount: amount.toString() })),
      byMember: Object.entries(byMember).map(([name, amount]) => ({ name, amount: amount.toString() })),
      byTime: Object.entries(byTime).map(([date, amount]) => ({ date, amount: amount.toString() })).sort((a, b) => a.date.localeCompare(b.date)),
    };

    return res.status(200).json({ data });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
}

export default withAuth(handler);

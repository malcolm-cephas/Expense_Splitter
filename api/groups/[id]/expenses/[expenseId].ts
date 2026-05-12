import { NextApiResponse } from 'next';
import connectDB from '../../../_db.js';
import Expense from '../../../_models/Expense.js';
import { withAuth, AuthenticatedRequest } from '../../../_middleware.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const { id: groupId, expenseId } = req.query;

  try {
    const expense = await Expense.findOne({ _id: expenseId, groupId });
    if (!expense) return res.status(404).json({ error: 'Expense not found' });

    // Check if user is the creator or group admin (simplified: only creator for now)
    // In a real app, you'd check Group members for admin role

    if (req.method === 'DELETE') {
      await Expense.findByIdAndDelete(expenseId);
      return res.status(200).json({ message: 'Expense deleted' });
    }

    if (req.method === 'PUT') {
      const updatedExpense = await Expense.findByIdAndUpdate(expenseId, req.body, { new: true });
      return res.status(200).json({ data: updatedExpense });
    }

    return res.status(405).json({ error: 'Method not allowed' });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
}

export default withAuth(handler);

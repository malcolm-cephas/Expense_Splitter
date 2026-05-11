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
    // We modify the group data to ensure the current user is the admin and owner
    const newGroup = await Group.create({
      name: `${group.name} (Imported)`,
      description: group.description,
      budget: group.budget,
      budgetCurrency: group.budgetCurrency || 'USD',
      familyGroupingEnabled: group.familyGroupingEnabled || false,
      members: [{ userId: user._id, role: 'admin' }],
      createdBy: user._id,
    });

    // 2. Create Expenses
    if (expenses && Array.isArray(expenses)) {
      const expensesToCreate = expenses.map((exp: any) => ({
        groupId: newGroup._id,
        description: exp.description,
        amount: exp.amount,
        currency: exp.currency,
        splitType: exp.splitType,
        category: exp.category,
        expenseDate: exp.expenseDate,
        payers: [{ userId: user._id, amount: exp.amount }], // Defaulting to current user paid
        splits: [{ userId: user._id, owedAmount: exp.amount, paidAmount: exp.amount, isPaid: true }], // Defaulting to simple split
        createdBy: user._id,
      }));
      
      await Expense.insertMany(expensesToCreate);
    }

    return res.status(201).json({ data: newGroup });
  } catch (error) {
    console.error('Import Error:', error);
    return res.status(500).json({ error: 'Failed to import group' });
  }
}

export default withAuth(handler);

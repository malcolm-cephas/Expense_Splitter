import connectDB from '../../../_db';
import Expense from '../../../_models/Expense';
import User from '../../../_models/User';
import Group from '../../../_models/Group';
import { withAuth } from '../../../_middleware';
async function handler(req, res) {
    await connectDB();
    const { id: groupId } = req.query;
    const auth0Id = req.user.sub;
    const currentUser = await User.findOne({ auth0Id });
    if (!currentUser)
        return res.status(404).json({ error: 'User not found' });
    // Verify membership
    const group = await Group.findById(groupId);
    if (!group)
        return res.status(404).json({ error: 'Group not found' });
    const isMember = group.members.some((m) => m.userId.toString() === currentUser._id.toString());
    if (!isMember)
        return res.status(403).json({ error: 'Forbidden' });
    if (req.method === 'GET') {
        try {
            const expenses = await Expense.find({ groupId }).sort({ expenseDate: -1 });
            return res.status(200).json({ data: expenses });
        }
        catch (error) {
            return res.status(500).json({ error: 'Internal Server Error' });
        }
    }
    else if (req.method === 'POST') {
        try {
            const expenseData = {
                ...req.body,
                groupId,
                createdBy: currentUser._id,
            };
            const expense = await Expense.create(expenseData);
            return res.status(201).json({ data: expense });
        }
        catch (error) {
            console.error('Error creating expense:', error);
            return res.status(500).json({ error: 'Internal Server Error' });
        }
    }
    else {
        return res.status(405).end(`Method ${req.method} Not Allowed`);
    }
}
export default withAuth(handler);

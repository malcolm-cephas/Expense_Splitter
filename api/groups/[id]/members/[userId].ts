import { NextApiResponse } from 'next';
import connectDB from '../../../_db.js';
import Group from '../../../_models/Group.js';
import { withAuth, AuthenticatedRequest } from '../../../_middleware.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const { id: groupId, userId } = req.query;

  try {
    const group = await Group.findById(groupId);
    if (!group) return res.status(404).json({ error: 'Group not found' });

    if (req.method === 'DELETE') {
      // Remove the member from the members array
      await Group.findByIdAndUpdate(groupId, {
        $pull: { members: { userId: userId } }
      });
      return res.status(200).json({ message: 'Member removed' });
    }

    return res.status(405).json({ error: 'Method not allowed' });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
}

export default withAuth(handler);

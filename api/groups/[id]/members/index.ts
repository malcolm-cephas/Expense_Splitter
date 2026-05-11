import { NextApiResponse } from 'next';
import connectDB from '../../../_db';
import Group from '../../../_models/Group';
import User from '../../../_models/User';
import PendingInvite from '../../../_models/PendingInvite';
import { withAuth, AuthenticatedRequest } from '../../../_middleware';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const { id: groupId } = req.query;
  const auth0Id = req.user!.sub;

  const currentUser = await User.findOne({ auth0Id });
  if (!currentUser) return res.status(404).json({ error: 'User not found' });

  if (req.method === 'POST') {
    const { email } = req.body;
    if (!email) return res.status(400).json({ error: 'Email is required' });

    try {
      // 1. Check if user already exists
      const targetUser = await User.findOne({ email });

      if (targetUser) {
        // 2. Add to group members if not already there
        await Group.findByIdAndUpdate(groupId, {
          $addToSet: { members: { userId: targetUser._id, role: 'member' } },
        });
        return res.status(200).json({ data: { status: 'joined', user: targetUser } });
      } else {
        // 3. Create pending invite
        await PendingInvite.findOneAndUpdate(
          { email, groupId },
          { email, groupId, invitedBy: currentUser._id },
          { upsert: true }
        );
        return res.status(200).json({ data: { status: 'pending', email } });
      }
    } catch (error) {
      console.error(error);
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  } else {
    return res.status(405).end(`Method ${req.method} Not Allowed`);
  }
}

export default withAuth(handler);

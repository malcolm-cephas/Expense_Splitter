import { NextApiResponse } from 'next';
import connectDB from '../../../_db.js';
import Group from '../../../_models/Group.js';
import User from '../../../_models/User.js';
import PendingInvite from '../../../_models/PendingInvite.js';
import { withAuth, AuthenticatedRequest } from '../../../_middleware.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();
  const { id: groupId } = req.query;
  const auth0Id = req.user!.sub;

  const currentUser = await User.findOne({ auth0Id });
  if (!currentUser) return res.status(404).json({ error: 'User not found' });

  if (req.method === 'POST') {
    const { name, email } = req.body;
    if (!name) return res.status(400).json({ error: 'Name is required' });

    try {
      let targetUserId;

      if (email) {
        // existing logic for email-based invite
        const targetUser = await User.findOne({ email });
        if (targetUser) {
          targetUserId = targetUser._id;
        } else {
          // Create pending invite
          await PendingInvite.findOneAndUpdate(
            { email, groupId },
            { email, groupId, invitedBy: currentUser._id },
            { upsert: true }
          );
          // If user doesn't exist, we don't add them to group yet, 
          // they join via /users/me when they register.
          return res.status(200).json({ data: { status: 'pending', email } });
        }
      } else {
        // Create a Ghost User
        const ghostUser = await User.create({
          name,
          isGhost: true,
          currencyPreference: currentUser.currencyPreference || 'USD'
        });
        targetUserId = ghostUser._id;
      }

      if (targetUserId) {
        await Group.findByIdAndUpdate(groupId, {
          $addToSet: { members: { userId: targetUserId, role: 'member' } },
        });
        return res.status(200).json({ data: { status: 'joined', userId: targetUserId } });
      }

      return res.status(500).json({ error: 'Failed to add member' });
    } catch (error) {
      console.error(error);
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  } else {
    return res.status(405).end(`Method ${req.method} Not Allowed`);
  }
}

export default withAuth(handler);

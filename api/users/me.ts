import { NextApiResponse } from 'next';
import connectDB from '../_db.js';
import User from '../_models/User.js';
import Group from '../_models/Group.js';
import PendingInvite from '../_models/PendingInvite.js';
import { withAuth, AuthenticatedRequest } from '../_middleware.js';

async function handler(req: AuthenticatedRequest, res: NextApiResponse) {
  await connectDB();

  if (req.method === 'POST') {
    const { sub, email, name, family_name } = req.user!;
    const finalName = name || (email ? email.split('@')[0] : 'User');

    try {
      // 1. Upsert the user
      const user = await User.findOneAndUpdate(
        { auth0Id: sub },
        {
          auth0Id: sub,
          email,
          name: finalName,
          familyName: family_name,
        },
        { upsert: true, new: true }
      );

      // 2. Check for pending invites and auto-join
      const invites = await PendingInvite.find({ email });
      
      if (invites.length > 0) {
        for (const invite of invites) {
          await Group.findByIdAndUpdate(invite.groupId, {
            $addToSet: { members: { userId: user._id, role: 'member' } },
          });
        }
        // Delete processed invites
        await PendingInvite.deleteMany({ email });
      }

      return res.status(200).json({ data: user });
    } catch (error) {
      console.error('Error in /api/users/me:', error);
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  } else if (req.method === 'GET') {
    try {
      const user = await User.findOne({ auth0Id: req.user!.sub });
      if (!user) {
        return res.status(404).json({ error: 'User not found' });
      }
      return res.status(200).json({ data: user });
    } catch (error) {
      return res.status(500).json({ error: 'Internal Server Error' });
    }
  } else {
    res.setHeader('Allow', ['GET', 'POST']);
    return res.status(405).end(`Method ${req.method} Not Allowed`);
  }
}

export default withAuth(handler);

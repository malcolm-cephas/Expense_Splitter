import mongoose, { Schema, Document } from 'mongoose';

export interface IPendingInvite extends Document {
  email: string;
  groupId: mongoose.Types.ObjectId;
  invitedBy: mongoose.Types.ObjectId;
  createdAt: Date;
}

const PendingInviteSchema: Schema = new Schema({
  email: { type: String, required: true, index: true },
  groupId: { type: Schema.Types.ObjectId, ref: 'Group', required: true },
  invitedBy: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  createdAt: { type: Date, default: Date.now },
});

export default mongoose.models.PendingInvite || mongoose.model<IPendingInvite>('PendingInvite', PendingInviteSchema);

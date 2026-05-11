import mongoose, { Schema, Document } from 'mongoose';

export interface IGroup extends Document {
  name: string;
  description?: string;
  budget?: string; // Stored as string for Decimal.js
  budgetCurrency: string;
  familyGroupingEnabled: boolean;
  members: {
    userId: mongoose.Types.ObjectId;
    role: 'admin' | 'member';
  }[];
  createdBy: mongoose.Types.ObjectId;
  createdAt: Date;
}

const GroupSchema: Schema = new Schema({
  name: { type: String, required: true },
  description: { type: String },
  budget: { type: String },
  budgetCurrency: { type: String, required: true, default: 'USD' },
  familyGroupingEnabled: { type: Boolean, default: false },
  members: [
    {
      userId: { type: Schema.Types.ObjectId, ref: 'User', required: true },
      role: { type: String, enum: ['admin', 'member'], default: 'member' },
    },
  ],
  createdBy: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  createdAt: { type: Date, default: Date.now },
});

export default mongoose.models.Group || mongoose.model<IGroup>('Group', GroupSchema);

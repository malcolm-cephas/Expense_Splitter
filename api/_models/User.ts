import mongoose, { Schema, Document } from 'mongoose';

export interface IUser extends Document {
  auth0Id?: string;
  name: string;
  email?: string;
  currencyPreference: string;
  familyName?: string;
  isGhost: boolean;
  createdAt: Date;
}

const UserSchema: Schema = new Schema({
  auth0Id: { type: String, unique: true, sparse: true },
  name: { type: String, required: true },
  email: { type: String, unique: true, sparse: true },
  currencyPreference: { type: String, default: 'USD' },
  familyName: { type: String },
  isGhost: { type: Boolean, default: false },
  createdAt: { type: Date, default: Date.now },
});

export default mongoose.models.User || mongoose.model<IUser>('User', UserSchema);

import mongoose, { Schema, Document } from 'mongoose';

export interface IUser extends Document {
  auth0Id: string;
  name: string;
  email: string;
  currencyPreference: string;
  familyName?: string;
  createdAt: Date;
}

const UserSchema: Schema = new Schema({
  auth0Id: { type: String, required: true, unique: true },
  name: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  currencyPreference: { type: String, default: 'USD' },
  familyName: { type: String },
  createdAt: { type: Date, default: Date.now },
});

export default mongoose.models.User || mongoose.model<IUser>('User', UserSchema);

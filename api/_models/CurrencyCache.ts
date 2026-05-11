import mongoose, { Schema, Document } from 'mongoose';

export interface ICurrencyCache extends Document {
  data: any;
  updatedAt: Date;
}

const CurrencyCacheSchema: Schema = new Schema({
  data: { type: Object, required: true },
  updatedAt: { type: Date, default: Date.now, expires: 86400 }, // 24h TTL
});

export default mongoose.models.CurrencyCache || mongoose.model<ICurrencyCache>('CurrencyCache', CurrencyCacheSchema);

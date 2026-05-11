import mongoose, { Schema, Document } from 'mongoose';

export interface IExpense extends Document {
  groupId: mongoose.Types.ObjectId;
  description: string;
  amount: string; // Stored as string for Decimal.js
  currency: string;
  splitType: 'equal' | 'exact' | 'percentage' | 'shares';
  category: string;
  expenseDate: Date;
  payers: {
    userId: mongoose.Types.ObjectId;
    amount: string; // Stored as string for Decimal.js
  }[];
  splits: {
    userId: mongoose.Types.ObjectId;
    owedAmount: string; // Stored as string for Decimal.js
    paidAmount: string; // Stored as string for Decimal.js
    isPaid: boolean;
  }[];
  createdBy: mongoose.Types.ObjectId;
  createdAt: Date;
}

const ExpenseSchema: Schema = new Schema({
  groupId: { type: Schema.Types.ObjectId, ref: 'Group', required: true },
  description: { type: String, required: true },
  amount: { type: String, required: true },
  currency: { type: String, required: true },
  splitType: {
    type: String,
    enum: ['equal', 'exact', 'percentage', 'shares'],
    required: true,
  },
  category: { type: String, required: true },
  expenseDate: { type: Date, default: Date.now },
  payers: [
    {
      userId: { type: Schema.Types.ObjectId, ref: 'User', required: true },
      amount: { type: String, required: true },
    },
  ],
  splits: [
    {
      userId: { type: Schema.Types.ObjectId, ref: 'User', required: true },
      owedAmount: { type: String, required: true },
      paidAmount: { type: String, default: '0' },
      isPaid: { type: Boolean, default: false },
    },
  ],
  createdBy: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  createdAt: { type: Date, default: Date.now },
});

export default mongoose.models.Expense || mongoose.model<IExpense>('Expense', ExpenseSchema);

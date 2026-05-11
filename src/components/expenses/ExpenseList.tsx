import React from 'react';
import { ExpenseCard } from './ExpenseCard';
import type { Expense } from '@/hooks/useExpenses';
import { Skeleton } from '@/components/ui/skeleton';
import { Receipt } from 'lucide-react';

interface ExpenseListProps {
  expenses: Expense[];
  isLoading: boolean;
}

export const ExpenseList: React.FC<ExpenseListProps> = ({ expenses, isLoading }) => {
  if (isLoading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map(i => (
          <Skeleton key={i} className="h-20 w-full bg-white/5 rounded-xl" />
        ))}
      </div>
    );
  }

  if (expenses.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 glass-card rounded-2xl border-dashed">
        <div className="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mb-4">
          <Receipt className="w-8 h-8 text-gray-600" />
        </div>
        <h3 className="text-xl font-medium text-white">No expenses yet</h3>
        <p className="text-gray-500 mt-2">Start adding expenses to track your shared spending.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {expenses.map(expense => (
        <ExpenseCard key={expense._id} expense={expense} />
      ))}
    </div>
  );
};

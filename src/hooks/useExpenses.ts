import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export interface Payer {
  userId: string;
  amount: string;
}

export interface Split {
  userId: string;
  owedAmount: string;
  paidAmount: string;
  isPaid: boolean;
}

export interface Expense {
  _id: string;
  groupId: string;
  description: string;
  amount: string;
  currency: string;
  splitType: 'equal' | 'exact' | 'percentage' | 'shares';
  category: string;
  expenseDate: string;
  payers: Payer[];
  splits: Split[];
  createdBy: string;
  createdAt: string;
}

export const useExpenses = (groupId: string) => {
  const queryClient = useQueryClient();

  const expensesQuery = useQuery({
    queryKey: ['expenses', groupId],
    queryFn: async () => {
      const response = await api.get(`/groups/${groupId}/expenses`);
      return response.data.data as Expense[];
    },
    enabled: !!groupId,
  });

  const addExpenseMutation = useMutation({
    mutationFn: async (newExpense: Partial<Expense>) => {
      const response = await api.post(`/groups/${groupId}/expenses`, newExpense);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses', groupId] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });

  return {
    expenses: expensesQuery.data || [],
    isLoading: expensesQuery.isLoading,
    addExpense: addExpenseMutation.mutateAsync,
  };
};

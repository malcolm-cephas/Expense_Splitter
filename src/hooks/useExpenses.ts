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

  const deleteExpenseMutation = useMutation({
    mutationFn: async (expenseId: string) => {
      await api.delete(`/groups/${groupId}/expenses/${expenseId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses', groupId] });
      queryClient.invalidateQueries({ queryKey: ['group', groupId] });
      queryClient.invalidateQueries({ queryKey: ['statistics', groupId] });
      queryClient.invalidateQueries({ queryKey: ['settlements', groupId] });
    },
  });

  const updateExpenseMutation = useMutation({
    mutationFn: async ({ expenseId, data }: { expenseId: string; data: Partial<Expense> }) => {
      const response = await api.put(`/groups/${groupId}/expenses/${expenseId}`, data);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenses', groupId] });
      queryClient.invalidateQueries({ queryKey: ['group', groupId] });
      queryClient.invalidateQueries({ queryKey: ['statistics', groupId] });
      queryClient.invalidateQueries({ queryKey: ['settlements', groupId] });
    },
  });

  return {
    expenses: expensesQuery.data || [],
    isLoading: expensesQuery.isLoading,
    addExpense: addExpenseMutation.mutateAsync,
    deleteExpense: deleteExpenseMutation.mutateAsync,
    updateExpense: updateExpenseMutation.mutateAsync,
  };
};

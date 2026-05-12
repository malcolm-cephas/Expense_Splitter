import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export interface Group {
  _id: string;
  name: string;
  description?: string;
  budget?: string;
  budgetCurrency: string;
  memberCount: number;
  totalExpenses: string;
  userBalance: string;
  familyGroupingEnabled: boolean;
  initialMembers?: string[];
}

export const useGroups = () => {
  const queryClient = useQueryClient();

  const groupsQuery = useQuery({
    queryKey: ['groups'],
    queryFn: async () => {
      const response = await api.get('/groups');
      return response.data.data as Group[];
    },
  });

  const createGroupMutation = useMutation({
    mutationFn: async (newGroup: Partial<Group>) => {
      const response = await api.post('/groups', newGroup);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });

  return {
    groups: groupsQuery.data || [],
    isLoading: groupsQuery.isLoading,
    error: groupsQuery.error,
    createGroup: createGroupMutation.mutateAsync,
  };
};

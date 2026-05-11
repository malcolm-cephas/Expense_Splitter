import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { SettlementTransaction } from '@/lib/settlements';

export const useSettlements = (groupId: string) => {
  const queryClient = useQueryClient();
  const settlementsQuery = useQuery({
    queryKey: ['settlements', groupId],
    queryFn: async () => {
      const response = await api.get(`/groups/${groupId}/settlements`);
      return response.data.data as SettlementTransaction[];
    },
    enabled: !!groupId,
  });

  const settleUpMutation = useMutation({
    mutationFn: async (transaction: SettlementTransaction) => {
      const response = await api.post(`/groups/${groupId}/settlements`, transaction);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['settlements', groupId] });
      queryClient.invalidateQueries({ queryKey: ['expenses', groupId] });
      queryClient.invalidateQueries({ queryKey: ['group', groupId] });
    },
  });

  return {
    settlements: settlementsQuery.data || [],
    isLoading: settlementsQuery.isLoading,
    settleUp: settleUpMutation.mutateAsync,
  };
};

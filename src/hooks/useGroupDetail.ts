import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export interface GroupMember {
  userId: {
    _id: string;
    name: string;
    email: string;
    picture?: string;
    familyName?: string;
  };
  role: 'admin' | 'member';
  balance: string; // Calculated on the fly
}

export interface GroupDetail extends Group {
  members: GroupMember[];
}

import type { Group } from './useGroups';

export const useGroupDetail = (groupId: string) => {
  const queryClient = useQueryClient();

  const groupQuery = useQuery({
    queryKey: ['group', groupId],
    queryFn: async () => {
      const response = await api.get(`/groups/${groupId}`);
      return response.data.data as GroupDetail;
    },
    enabled: !!groupId,
  });

  const addMemberMutation = useMutation({
    mutationFn: async (email: string) => {
      const response = await api.post(`/groups/${groupId}/members`, { email });
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['group', groupId] });
    },
  });

  return {
    group: groupQuery.data,
    isLoading: groupQuery.isLoading,
    addMember: addMemberMutation.mutateAsync,
  };
};

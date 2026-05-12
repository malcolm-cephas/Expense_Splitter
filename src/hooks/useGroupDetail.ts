import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';

export interface GroupMember {
  userId: {
    _id: string;
    name: string;
    email?: string;
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
    mutationFn: async (data: { name: string; email?: string }) => {
      const response = await api.post(`/groups/${groupId}/members`, data);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['group', groupId] });
    },
  });

  const removeMemberMutation = useMutation({
    mutationFn: async (userId: string) => {
      await api.delete(`/groups/${groupId}/members/${userId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['group', groupId] });
    },
  });

  const deleteGroupMutation = useMutation({
    mutationFn: async () => {
      await api.delete(`/groups/${groupId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });

  const updateGroupMutation = useMutation({
    mutationFn: async (data: Partial<Group>) => {
      const response = await api.put(`/groups/${groupId}`, data);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['group', groupId] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });

  return {
    group: groupQuery.data,
    isLoading: groupQuery.isLoading,
    addMember: addMemberMutation.mutateAsync,
    removeMember: removeMemberMutation.mutateAsync,
    deleteGroup: deleteGroupMutation.mutateAsync,
    updateGroup: updateGroupMutation.mutateAsync,
  };
};

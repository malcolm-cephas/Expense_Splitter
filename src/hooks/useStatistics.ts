import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';

export interface CategoryStat {
  category: string;
  amount: string;
}

export interface MemberStat {
  name: string;
  amount: string;
}

export interface TimeStat {
  date: string;
  amount: string;
}

export interface Statistics {
  byCategory: CategoryStat[];
  byMember: MemberStat[];
  byTime: TimeStat[];
}

export const useStatistics = (groupId: string) => {
  const statisticsQuery = useQuery({
    queryKey: ['statistics', groupId],
    queryFn: async () => {
      const response = await api.get(`/groups/${groupId}/statistics`);
      return response.data.data as Statistics;
    },
    enabled: !!groupId,
  });

  return {
    statistics: statisticsQuery.data,
    isLoading: statisticsQuery.isLoading,
  };
};

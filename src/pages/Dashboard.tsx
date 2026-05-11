import React from 'react';
import { Plus, LayoutGrid, ListFilter, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useGroups } from '@/hooks/useGroups';
import { GroupCard } from '@/components/groups/GroupCard';
import { Skeleton } from '@/components/ui/skeleton';
import { CreateGroupModal } from '@/components/groups/CreateGroupModal';

const Dashboard: React.FC = () => {
  const { groups, isLoading } = useGroups();
  const [search, setSearch] = React.useState('');

  const filteredGroups = groups.filter(g => 
    g.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white tracking-tight">Dashboard</h1>
          <p className="text-gray-400 mt-1">Manage your shared expenses and groups.</p>
        </div>
        <CreateGroupModal />
      </div>

      <div className="flex flex-col md:flex-row gap-4 items-center justify-between glass-card p-4 rounded-xl">
        <div className="relative w-full md:w-96">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
          <Input 
            placeholder="Search groups..." 
            className="pl-10 bg-white/5 border-white/10 focus:border-primary/50"
            value={search}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearch(e.target.value)}
          />
        </div>
        <div className="flex items-center gap-2 w-full md:w-auto">
          <Button variant="outline" size="sm" className="border-white/10 text-gray-400">
            <ListFilter className="w-4 h-4 mr-2" />
            Filter
          </Button>
          <Button variant="outline" size="sm" className="border-white/10 text-primary bg-primary/5">
            <LayoutGrid className="w-4 h-4 mr-2" />
            Grid
          </Button>
        </div>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map(i => (
            <Skeleton key={i} className="h-64 rounded-xl bg-white/5" />
          ))}
        </div>
      ) : filteredGroups.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredGroups.map(group => (
            <GroupCard key={group._id} group={group} />
          ))}
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center py-20 glass-card rounded-2xl border-dashed">
          <div className="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mb-4">
            <Search className="w-8 h-8 text-gray-600" />
          </div>
          <h3 className="text-xl font-medium text-white">No groups found</h3>
          <p className="text-gray-500 mt-2">Try adjusting your search or create a new group.</p>
          <Button variant="link" className="text-primary mt-4">
            Clear search
          </Button>
        </div>
      )}
    </div>
  );
};

export default Dashboard;

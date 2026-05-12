import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, 
  Plus, 
  Users, 
  Receipt, 
  BarChart3, 
  CheckCircle2, 
  Settings,
  Download,
  Share2
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuTrigger,
  DropdownMenuSeparator
} from '@/components/ui/dropdown-menu';
import { toast } from 'sonner';
import { InviteMemberModal } from '@/components/groups/InviteMemberModal';
import { EditGroupModal } from '@/components/groups/EditGroupModal';
import { useGroupDetail } from '@/hooks/useGroupDetail';
import { useExpenses } from '@/hooks/useExpenses';
import { Skeleton } from '@/components/ui/skeleton';

import { ExpenseList } from '@/components/expenses/ExpenseList';
import { AddExpenseModal } from '@/components/expenses/AddExpenseModal';
import { GroupMemberList } from '@/components/groups/GroupMemberList';
import { SettlementList } from '@/components/settlements/SettlementList';
import { StatisticsView } from '@/components/statistics/StatisticsView';
import { useSettlements } from '@/hooks/useSettlements';
import { useStatistics } from '@/hooks/useStatistics';

const GroupDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { group, isLoading: isGroupLoading, addMember, removeMember, deleteGroup, updateGroup } = useGroupDetail(id!);
  const { expenses, isLoading: isExpensesLoading, addExpense, deleteExpense } = useExpenses(id!);
  const { settlements, isLoading: isSettlementsLoading, settleUp } = useSettlements(id!);
  const { statistics, isLoading: isStatisticsLoading } = useStatistics(id!);
  const [isEditModalOpen, setIsEditModalOpen] = React.useState(false);

  if (isGroupLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-32 bg-white/5" />
        <Skeleton className="h-20 w-full bg-white/5" />
        <Skeleton className="h-[400px] w-full bg-white/5" />
      </div>
    );
  }

  if (!group) return <div>Group not found</div>;

  const exportData = (format: 'csv' | 'json') => {
    window.open(`/api/groups/${id}/export/${format}`, '_blank');
  };

  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex items-center justify-between">
        <Button 
          variant="ghost" 
          size="sm" 
          className="text-gray-400 hover:text-white"
          onClick={() => navigate('/')}
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Dashboard
        </Button>
        <div className="flex items-center gap-2">
          <InviteMemberModal 
            onInvite={addMember}
            trigger={
              <Button variant="outline" size="sm" className="border-white/10 text-gray-400">
                <Share2 className="w-4 h-4 mr-2" />
                Invite
              </Button>
            }
          />
          <DropdownMenu>
            <DropdownMenuTrigger 
              render={
                <Button variant="outline" size="sm" className="border-white/10 text-gray-400">
                  <Download className="w-4 h-4 mr-2" />
                  Export
                </Button>
              }
            />
            <DropdownMenuContent className="glass-card bg-slate-900 border-white/10 text-white">
              <DropdownMenuItem className="focus:bg-white/10" onClick={() => exportData('csv')}>
                Download CSV
              </DropdownMenuItem>
              <DropdownMenuItem className="focus:bg-white/10" onClick={() => exportData('json')}>
                Download JSON Backup
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          <DropdownMenu>
            <DropdownMenuTrigger 
              render={
                <Button variant="outline" size="icon" className="border-white/10 text-gray-400">
                  <Settings className="w-4 h-4" />
                </Button>
              }
            />
            <DropdownMenuContent align="end" className="glass-card bg-slate-900 border-white/10 text-white">
              <DropdownMenuItem className="focus:bg-white/10" onClick={() => setIsEditModalOpen(true)}>
                Edit Group Details
              </DropdownMenuItem>
              <DropdownMenuSeparator className="bg-white/10" />
              <DropdownMenuItem 
                className="focus:bg-white/10 text-red-400 focus:text-red-400"
                onClick={async () => {
                  if (window.confirm('Are you sure you want to delete this group? This cannot be undone.')) {
                    try {
                      await deleteGroup();
                      toast.success('Group deleted');
                      navigate('/');
                    } catch (err) {
                      toast.error('Failed to delete group');
                    }
                  }
                }}
              >
                Delete Group
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      <div className="glass-card p-8 rounded-2xl border-b-4 border-b-primary/30">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-4xl font-bold text-white tracking-tight">{group.name}</h1>
              {group.familyGroupingEnabled && (
                <span className="px-2 py-0.5 rounded-full bg-primary/10 text-primary text-[10px] font-bold uppercase tracking-wider border border-primary/20">
                  Family Grouping
                </span>
              )}
            </div>
            <p className="text-gray-400 max-w-2xl">{group.description || 'No description provided for this group.'}</p>
          </div>
          <div className="text-right">
            <p className="text-xs text-gray-500 uppercase tracking-widest mb-1">Total Expenses</p>
            <p className="text-3xl font-mono font-bold text-white">
              {new Intl.NumberFormat('en-US', { style: 'currency', currency: group.budgetCurrency }).format(parseFloat(group.totalExpenses || '0'))}
            </p>
          </div>
        </div>
      </div>

      <Tabs defaultValue="expenses" className="w-full">
        <TabsList className="grid w-full grid-cols-4 bg-white/5 border border-white/10 p-1 h-14 rounded-xl">
          <TabsTrigger value="expenses" className="rounded-lg data-active:bg-primary data-active:text-background-dark flex items-center gap-2 h-full">
            <Receipt className="w-4 h-4" />
            <span className="hidden sm:inline">Expenses</span>
          </TabsTrigger>
          <TabsTrigger value="members" className="rounded-lg data-active:bg-primary data-active:text-background-dark flex items-center gap-2 h-full">
            <Users className="w-4 h-4" />
            <span className="hidden sm:inline">Members</span>
          </TabsTrigger>
          <TabsTrigger value="statistics" className="rounded-lg data-active:bg-primary data-active:text-background-dark flex items-center gap-2 h-full">
            <BarChart3 className="w-4 h-4" />
            <span className="hidden sm:inline">Statistics</span>
          </TabsTrigger>
          <TabsTrigger value="settle" className="rounded-lg data-active:bg-primary data-active:text-background-dark flex items-center gap-2 h-full">
            <CheckCircle2 className="w-4 h-4" />
            <span className="hidden sm:inline">Settle Up</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="expenses" className="space-y-4 outline-none mt-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-semibold text-white">Recent Expenses</h2>
            <AddExpenseModal 
              members={group.members} 
              baseCurrency={group.budgetCurrency}
              onAdd={addExpense}
              trigger={
                <Button className="btn-primary shadow-lg shadow-primary/20">
                  <Plus className="w-4 h-4 mr-2" />
                  Add Expense
                </Button>
              }
            />
          </div>
          <ExpenseList expenses={expenses} isLoading={isExpensesLoading} onRemove={deleteExpense} />
        </TabsContent>
        
        <TabsContent value="members" className="outline-none mt-6">
            <GroupMemberList 
              members={group.members} 
              currency={group.budgetCurrency} 
              onInvite={addMember}
              onRemove={removeMember}
            />
        </TabsContent>
        
        <TabsContent value="statistics" className="outline-none mt-6">
            <StatisticsView 
              statistics={statistics} 
              isLoading={isStatisticsLoading} 
              currency={group.budgetCurrency} 
            />
        </TabsContent>
        
        <TabsContent value="settle" className="outline-none mt-6">
            <SettlementList 
              settlements={settlements} 
              members={group.members} 
              isLoading={isSettlementsLoading} 
              onSettle={settleUp}
            />
        </TabsContent>
      </Tabs>

      <EditGroupModal 
        group={group}
        open={isEditModalOpen}
        onOpenChange={setIsEditModalOpen}
        onUpdate={updateGroup}
      />
    </div>
  );
};

export default GroupDetail;

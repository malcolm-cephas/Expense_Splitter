import React from 'react';
import { ArrowRight, CheckCircle2, User as UserIcon, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import type { SettlementTransaction } from '@/lib/settlements';
import { formatCurrency } from '@/lib/currency';
import type { GroupMember } from '@/hooks/useGroupDetail';

export const SettlementCard: React.FC<{ 
  transaction: SettlementTransaction;
  members: GroupMember[];
  onSettle: (transaction: SettlementTransaction) => Promise<void>;
}> = ({ transaction, members, onSettle }) => {
  const [isSettling, setIsSettling] = React.useState(false);
  const fromMember = members.find(m => m.userId._id === transaction.from || `family:${m.userId.familyName}` === transaction.from);
  const toMember = members.find(m => m.userId._id === transaction.to || `family:${m.userId.familyName}` === transaction.to);

  const fromName = fromMember?.userId.name || transaction.from;
  const toName = toMember?.userId.name || transaction.to;

  const handleSettle = async () => {
    setIsSettling(true);
    try {
      await onSettle(transaction);
      toast.success(`Settlement of ${formatCurrency(transaction.amount, transaction.currency)} recorded!`);
    } catch (error) {
      toast.error('Failed to record settlement.');
    } finally {
      setIsSettling(false);
    }
  };

  return (
    <Card className="glass-card border-l-4 border-l-amber-500 overflow-hidden">
      <CardContent className="p-6 flex items-center justify-between">
        <div className="flex items-center gap-6 flex-1">
          <div className="flex flex-col items-center">
            <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center">
              <UserIcon className="w-5 h-5 text-gray-400" />
            </div>
            <span className="text-xs text-gray-500 mt-1 max-w-[80px] truncate">{fromName}</span>
          </div>
          
          <div className="flex-1 flex flex-col items-center gap-1">
            <div className="text-lg font-mono font-bold text-white">
              {formatCurrency(transaction.amount, transaction.currency)}
            </div>
            <div className="w-full flex items-center">
              <div className="h-px bg-white/10 flex-1" />
              <ArrowRight className="w-4 h-4 text-primary mx-2" />
              <div className="h-px bg-white/10 flex-1" />
            </div>
            <span className="text-[10px] text-gray-600 uppercase tracking-widest">Minimal Transfer</span>
          </div>

          <div className="flex flex-col items-center">
            <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
              <UserIcon className="w-5 h-5 text-primary" />
            </div>
            <span className="text-xs text-white mt-1 max-w-[80px] truncate">{toName}</span>
          </div>
        </div>

        <div className="ml-8">
          <Button 
            variant="outline" 
            size="sm" 
            className="border-primary/20 text-primary hover:bg-primary/10"
            onClick={handleSettle}
            disabled={isSettling}
          >
            {isSettling ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : <CheckCircle2 className="w-4 h-4 mr-2" />}
            Mark Settled
          </Button>
        </div>
      </CardContent>
    </Card>
  );
};

export const SettlementList: React.FC<{ 
  settlements: SettlementTransaction[];
  members: GroupMember[];
  isLoading: boolean;
  onSettle: (transaction: SettlementTransaction) => Promise<any>;
}> = ({ settlements, members, isLoading, onSettle }) => {
  if (isLoading) {
    return <div className="text-gray-400 py-10 text-center">Calculating best ways to settle...</div>;
  }

  if (settlements.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 glass-card rounded-2xl border-dashed">
        <div className="w-16 h-16 rounded-full bg-green-500/10 flex items-center justify-center mb-4">
          <CheckCircle2 className="w-8 h-8 text-green-500" />
        </div>
        <h3 className="text-xl font-medium text-white">All settled up!</h3>
        <p className="text-gray-500 mt-2">Everyone in this group is squared away.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {settlements.map((s, idx) => (
        <SettlementCard key={idx} transaction={s} members={members} onSettle={onSettle} />
      ))}
    </div>
  );
};

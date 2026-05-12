import React, { useState } from 'react';
import { 
  Shield, 
  MoreVertical,
  UserPlus,
  Loader2,
  Mail
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu';
import type { GroupMember } from '@/hooks/useGroupDetail';
import { formatCurrency } from '@/lib/currency';
import { toast } from 'sonner';
import Decimal from 'decimal.js';

interface GroupMemberListProps {
  members: GroupMember[];
  currency: string;
  onInvite: (email: string) => Promise<any>;
  onRemove?: (userId: string) => Promise<any>;
}

export const GroupMemberList: React.FC<GroupMemberListProps> = ({ members, currency, onInvite, onRemove }) => {
  const [quickEmail, setQuickEmail] = useState('');
  const [isInviting, setIsInviting] = useState(false);

  const handleQuickInvite = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!quickEmail || !quickEmail.includes('@')) {
      toast.error('Please enter a valid email');
      return;
    }

    setIsInviting(true);
    try {
      const result = await onInvite(quickEmail);
      if (result.status === 'pending') {
        toast.success(`Invite sent to ${quickEmail}`);
      } else {
        toast.success(`${quickEmail} joined the group!`);
      }
      setQuickEmail('');
    } catch (error) {
      toast.error('Failed to invite member');
    } finally {
      setIsInviting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="glass-card p-6 rounded-2xl border-white/5 bg-white/5">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <UserPlus className="w-5 h-5 text-primary" />
              Quick Add Member
            </h2>
            <p className="text-xs text-gray-500 mt-1">Invite collaborators by their email address</p>
          </div>
          
          <form onSubmit={handleQuickInvite} className="flex gap-2 w-full md:w-auto">
            <div className="relative flex-1 md:w-64">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
              <Input
                placeholder="friend@example.com"
                value={quickEmail}
                onChange={(e) => setQuickEmail(e.target.value)}
                className="pl-10 bg-white/5 border-white/10 focus:border-primary/50"
              />
            </div>
            <Button 
              type="submit" 
              className="btn-primary"
              disabled={isInviting || !quickEmail}
            >
              {isInviting ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Invite'}
            </Button>
          </form>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {members.map((member) => {
          const balance = new Decimal(member.balance || '0');
          const isOwed = balance.gt(0);
          const isNeutral = balance.isZero();

          return (
            <Card key={member.userId._id} className="glass-card hover:border-white/20 transition-all border-white/10">
              <CardContent className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <Avatar className="w-12 h-12 border border-white/10 shadow-inner">
                    <AvatarImage src={member.userId.picture} />
                    <AvatarFallback className="bg-primary/10 text-primary">{member.userId.name[0]}</AvatarFallback>
                  </Avatar>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-white">{member.userId.name}</h3>
                      {member.role === 'admin' && (
                        <Shield className="w-3 h-3 text-primary" />
                      )}
                    </div>
                    <p className="text-[10px] text-gray-500 font-mono">{member.userId.email}</p>
                    {member.userId.familyName && (
                      <span className="text-[10px] text-primary/70 uppercase tracking-widest mt-1 block font-bold">
                        {member.userId.familyName} Family
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <p className={`text-sm font-mono font-bold ${isOwed ? 'text-primary' : isNeutral ? 'text-gray-500' : 'text-red-400'}`}>
                      {isOwed ? '+' : ''}{formatCurrency(balance.toString(), currency)}
                    </p>
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest">
                      {isOwed ? 'is owed' : isNeutral ? 'settled' : 'owes'}
                    </p>
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger 
                      render={
                        <Button variant="ghost" size="icon" className="text-gray-500 hover:text-white rounded-full">
                          <MoreVertical className="w-4 h-4" />
                        </Button>
                      }
                    />
                    <DropdownMenuContent align="end" className="glass-card bg-slate-900 border-white/10 text-white">
                      <DropdownMenuItem className="focus:bg-white/10 cursor-pointer">View Statistics</DropdownMenuItem>
                      <DropdownMenuItem 
                        className="focus:bg-white/10 text-red-400 focus:text-red-400 cursor-pointer"
                        onClick={() => onRemove?.(member.userId._id)}
                      >
                        Remove from Group
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
};

import React from 'react';
import { 
  Shield, 
  MoreVertical,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu';
import type { GroupMember } from '@/hooks/useGroupDetail';
import { formatCurrency } from '@/lib/currency';
import { InviteMemberModal } from './InviteMemberModal';
import Decimal from 'decimal.js';

interface GroupMemberListProps {
  members: GroupMember[];
  currency: string;
  onInvite: (email: string) => Promise<any>;
}

export const GroupMemberList: React.FC<GroupMemberListProps> = ({ members, currency, onInvite }) => {
  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold text-white">Group Members</h2>
        <InviteMemberModal onInvite={onInvite} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {members.map((member) => {
          const balance = new Decimal(member.balance || '0');
          const isOwed = balance.gt(0);
          const isNeutral = balance.isZero();

          return (
            <Card key={member.userId._id} className="glass-card hover:border-white/20 transition-all">
              <CardContent className="p-4 flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <Avatar className="w-12 h-12 border border-white/10">
                    <AvatarImage src={member.userId.picture} />
                    <AvatarFallback>{member.userId.name[0]}</AvatarFallback>
                  </Avatar>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-white">{member.userId.name}</h3>
                      {member.role === 'admin' && (
                        <Shield className="w-3 h-3 text-primary" />
                      )}
                    </div>
                    <p className="text-xs text-gray-500">{member.userId.email}</p>
                    {member.userId.familyName && (
                      <span className="text-[10px] text-gray-600 uppercase tracking-widest mt-1 block">
                        Family: {member.userId.familyName}
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
                        <Button variant="ghost" size="icon" className="text-gray-500 hover:text-white">
                          <MoreVertical className="w-4 h-4" />
                        </Button>
                      }
                    />
                    <DropdownMenuContent align="end" className="glass-card bg-slate-900 border-white/10">
                      <DropdownMenuItem className="focus:bg-white/10">View Statistics</DropdownMenuItem>
                      <DropdownMenuItem className="focus:bg-white/10 text-red-400 focus:text-red-400">Remove from Group</DropdownMenuItem>
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

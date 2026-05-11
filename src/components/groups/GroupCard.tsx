import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, TrendingUp, TrendingDown, ArrowRight } from 'lucide-react';
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import type { Group } from '@/hooks/useGroups';
import { formatCurrency } from '@/lib/currency';
import Decimal from 'decimal.js';

export const GroupCard: React.FC<{ group: Group }> = ({ group }) => {
  const navigate = useNavigate();
  const balance = new Decimal(group.userBalance || '0');
  const isOwed = balance.gt(0);
  const isNeutral = balance.isZero();

  return (
    <Card className="glass-card hover:border-primary/50 cursor-pointer group" onClick={() => navigate(`/groups/${group._id}`)}>
      <CardHeader className="pb-2">
        <div className="flex justify-between items-start">
          <CardTitle className="text-xl font-bold text-white group-hover:text-primary transition-colors">
            {group.name}
          </CardTitle>
          <Badge variant="outline" className="bg-white/5 border-white/10 text-gray-400">
            <Users className="w-3 h-3 mr-1" />
            {group.memberCount}
          </Badge>
        </div>
        <p className="text-sm text-gray-400 line-clamp-1">{group.description || 'No description'}</p>
      </CardHeader>
      
      <CardContent className="py-4">
        <div className="flex flex-col gap-4">
          <div className="flex justify-between items-center text-sm">
            <span className="text-gray-500">Total Expenses</span>
            <span className="font-mono font-medium">{formatCurrency(group.totalExpenses, group.budgetCurrency)}</span>
          </div>
          
          <div className="pt-2 border-t border-white/5">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-500">Your Balance</span>
              <div className={`flex items-center gap-1 font-bold ${isOwed ? 'text-primary' : isNeutral ? 'text-gray-400' : 'text-red-400'}`}>
                {isOwed ? <TrendingUp className="w-4 h-4" /> : !isNeutral && <TrendingDown className="w-4 h-4" />}
                <span className="font-mono">{formatCurrency(balance.abs().toString(), group.budgetCurrency)}</span>
              </div>
            </div>
            <p className="text-[10px] text-right text-gray-500 mt-1 uppercase tracking-wider">
              {isOwed ? 'You are owed' : isNeutral ? 'Settled up' : 'You owe'}
            </p>
          </div>
        </div>
      </CardContent>

      <CardFooter className="pt-0">
        <Button variant="ghost" size="sm" className="w-full text-xs text-gray-500 group-hover:text-white group-hover:bg-white/5">
          View Details
          <ArrowRight className="w-3 h-3 ml-2 group-hover:translate-x-1 transition-transform" />
        </Button>
      </CardFooter>
    </Card>
  );
};

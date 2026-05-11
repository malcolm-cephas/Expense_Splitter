import { 
  Calendar, 
  ChevronRight, 
  Users,
  CreditCard
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import type { Expense } from '@/hooks/useExpenses';
import { formatCurrency } from '@/lib/currency';
import { format } from 'date-fns';

export const ExpenseCard: React.FC<{ expense: Expense }> = ({ expense }) => {
  const [isExpanded, setIsExpanded] = React.useState(false);

  const getCategoryIcon = (category: string) => {
    switch (category.toLowerCase()) {
      case 'food': return '🍔';
      case 'transport': return '🚗';
      case 'accommodation': return '🏠';
      case 'entertainment': return '🎬';
      case 'shopping': return '🛍️';
      default: return '📦';
    }
  };

  return (
    <Card className="glass-card hover:border-white/20 transition-all overflow-hidden border-l-4 border-l-primary">
      <CardContent className="p-0">
        <div 
          className="p-4 flex items-center justify-between cursor-pointer group"
          onClick={() => setIsExpanded(!isExpanded)}
        >
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-white/5 flex items-center justify-center text-2xl">
              {getCategoryIcon(expense.category)}
            </div>
            <div>
              <h3 className="font-bold text-white group-hover:text-primary transition-colors">
                {expense.description}
              </h3>
              <div className="flex items-center gap-3 mt-1">
                <span className="flex items-center text-[10px] text-gray-500 uppercase tracking-wider">
                  <Calendar className="w-3 h-3 mr-1" />
                  {format(new Date(expense.expenseDate), 'MMM dd, yyyy')}
                </span>
                <Badge variant="outline" className="text-[9px] h-4 px-1.5 border-white/10 bg-white/5 text-gray-400 capitalize">
                  {expense.splitType}
                </Badge>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-6">
            <div className="text-right">
              <p className="text-lg font-mono font-bold text-white">
                {formatCurrency(expense.amount, expense.currency)}
              </p>
              <p className="text-[10px] text-gray-500 uppercase tracking-widest">
                Paid by {expense.payers.length > 1 ? `${expense.payers.length} members` : '1 member'}
              </p>
            </div>
            <ChevronRight className={`w-5 h-5 text-gray-600 transition-transform duration-300 ${isExpanded ? 'rotate-90' : ''}`} />
          </div>
        </div>

        {isExpanded && (
          <div className="px-4 pb-4 pt-2 border-t border-white/5 bg-white/[0.02] animate-in slide-in-from-top-2 duration-300">
             <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-2">
                <div>
                  <h4 className="text-[10px] font-bold text-gray-500 uppercase tracking-widest mb-3 flex items-center gap-2">
                    <CreditCard className="w-3 h-3" />
                    Paid By
                  </h4>
                  <div className="space-y-2">
                    {expense.payers.map((payer, idx) => (
                      <div key={idx} className="flex justify-between items-center bg-white/5 p-2 rounded-lg border border-white/5">
                        <span className="text-sm text-gray-300">Member {idx + 1}</span>
                        <span className="text-sm font-mono font-medium text-white">{formatCurrency(payer.amount, expense.currency)}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <div>
                  <h4 className="text-[10px] font-bold text-gray-500 uppercase tracking-widest mb-3 flex items-center gap-2">
                    <Users className="w-3 h-3" />
                    Split Details
                  </h4>
                  <div className="space-y-2">
                    {expense.splits.map((split, idx) => (
                      <div key={idx} className="flex justify-between items-center p-2">
                        <span className="text-sm text-gray-400">Member {idx + 1}</span>
                        <span className="text-sm font-mono text-gray-300">{formatCurrency(split.owedAmount, expense.currency)}</span>
                      </div>
                    ))}
                  </div>
                </div>
             </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

import React from 'react';
import { 
  PieChart, 
  Pie, 
  Cell, 
  ResponsiveContainer, 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  Tooltip, 
  LineChart, 
  Line,
  CartesianGrid
} from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { Statistics } from '@/hooks/useStatistics';
import { formatCurrency } from '@/lib/currency';

const COLORS = ['#00d4aa', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

export const StatisticsView: React.FC<{ 
  statistics?: Statistics;
  isLoading: boolean;
  currency: string;
}> = ({ statistics, isLoading, currency }) => {
  if (isLoading || !statistics) {
    return <div className="text-gray-400 py-10 text-center">Crunching the numbers...</div>;
  }

  const categoryData = statistics.byCategory.map(c => ({
    name: c.category,
    value: parseFloat(c.amount)
  }));

  const memberData = statistics.byMember.map(m => ({
    name: m.name,
    amount: parseFloat(m.amount)
  }));

  const timeData = statistics.byTime.map(t => ({
    date: t.date,
    amount: parseFloat(t.amount)
  }));

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Spending by Category */}
        <Card className="glass-card">
          <CardHeader>
            <CardTitle className="text-lg font-bold text-white">Spending by Category</CardTitle>
          </CardHeader>
          <CardContent className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={categoryData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {categoryData.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ backgroundColor: '#0a0f1e', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px' }}
                  itemStyle={{ color: '#fff' }}
                  formatter={(value: any) => formatCurrency(value.toString(), currency)}
                />
              </PieChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Per-Member Spending */}
        <Card className="glass-card">
          <CardHeader>
            <CardTitle className="text-lg font-bold text-white">Spending per Member</CardTitle>
          </CardHeader>
          <CardContent className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={memberData}>
                <XAxis dataKey="name" stroke="#6b7280" fontSize={12} />
                <YAxis stroke="#6b7280" fontSize={12} tickFormatter={(value) => `${currency} ${value}`} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#0a0f1e', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px' }}
                  cursor={{ fill: 'rgba(255,255,255,0.05)' }}
                />
                <Bar dataKey="amount" fill="#00d4aa" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      {/* Spending Over Time */}
      <Card className="glass-card">
        <CardHeader>
          <CardTitle className="text-lg font-bold text-white">Spending Over Time</CardTitle>
        </CardHeader>
        <CardContent className="h-[300px]">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={timeData}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="date" stroke="#6b7280" fontSize={12} />
              <YAxis stroke="#6b7280" fontSize={12} tickFormatter={(value) => `${currency} ${value}`} />
              <Tooltip 
                contentStyle={{ backgroundColor: '#0a0f1e', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px' }}
              />
              <Line type="monotone" dataKey="amount" stroke="#00d4aa" strokeWidth={2} dot={{ fill: '#00d4aa' }} />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>
    </div>
  );
};

import React, { useState, useEffect } from 'react';
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle, 
  DialogFooter,
  DialogTrigger 
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Plus, Minus, Receipt, Users, CreditCard } from 'lucide-react';
import { calculateSplits, type SplitType } from '@/lib/splits';
import type { GroupMember } from '@/hooks/useGroupDetail';
import Decimal from 'decimal.js';

interface AddExpenseModalProps {
  members: GroupMember[];
  baseCurrency: string;
  onAdd: (expense: any) => Promise<void>;
  trigger?: React.ReactNode;
}

export const AddExpenseModal: React.FC<AddExpenseModalProps> = ({ 
  members, 
  baseCurrency, 
  onAdd,
  trigger
}) => {
  const [open, setOpen] = useState(false);
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('Other');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  
  const [splitType, setSplitType] = useState<SplitType>('equal');
  const [selectedMembers, setSelectedMembers] = useState<string[]>(members.map(m => m.userId._id));
  const [customValues, setCustomValues] = useState<Record<string, string>>({});
  
  const [payers, setPayers] = useState<{ userId: string; amount: string }[]>([
    { userId: members[0]?.userId._id || '', amount: '' }
  ]);

  const [previewSplits, setPreviewSplits] = useState<{ userId: string; owedAmount: string }[]>([]);

  // Update preview whenever inputs change
  useEffect(() => {
    if (!amount || isNaN(parseFloat(amount))) {
      setPreviewSplits([]);
      return;
    }

    const splitMembers = selectedMembers.map(userId => ({
      userId,
      value: customValues[userId] || '0'
    }));

    const results = calculateSplits(amount, splitType, splitMembers);
    setPreviewSplits(results);
  }, [amount, splitType, selectedMembers, customValues]);

  const handleSubmit = async () => {
    // Basic validation
    if (!description || !amount || payers.some(p => !p.amount)) return;
    
    // Ensure payers sum to total amount
    const totalPaid = payers.reduce((acc, p) => acc.plus(new Decimal(p.amount || '0')), new Decimal(0));
    if (!totalPaid.equals(new Decimal(amount))) {
      alert(`Payer total (${totalPaid}) must equal expense amount (${amount})`);
      return;
    }

    const expense = {
      description,
      amount,
      currency: baseCurrency,
      category,
      expenseDate: new Date(date),
      splitType,
      payers,
      splits: previewSplits.map(s => ({
        userId: s.userId,
        owedAmount: s.owedAmount,
        paidAmount: payers.find(p => p.userId === s.userId)?.amount || '0',
        isPaid: false
      }))
    };

    try {
      await onAdd(expense);
      setOpen(false);
      resetForm();
    } catch (error) {
      console.error(error);
    }
  };

  const resetForm = () => {
    setDescription('');
    setAmount('');
    setPayers([{ userId: members[0]?.userId._id || '', amount: '' }]);
    setSplitType('equal');
    setSelectedMembers(members.map(m => m.userId._id));
    setCustomValues({});
  };

  const addPayer = () => {
    setPayers([...payers, { userId: members[0]?.userId._id || '', amount: '' }]);
  };

  const removePayer = (index: number) => {
    setPayers(payers.filter((_, i) => i !== index));
  };

  const updatePayer = (index: number, field: string, value: string) => {
    const newPayers = [...payers];
    (newPayers[index] as any)[field] = value;
    setPayers(newPayers);
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {trigger || (
          <Button className="btn-primary">
            <Plus className="w-4 h-4 mr-2" />
            Add Expense
          </Button>
        )}
      </DialogTrigger>
      <DialogContent className="max-w-2xl glass-card bg-slate-900/95 border-white/10 text-white overflow-hidden flex flex-col max-h-[90vh]">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold flex items-center gap-2">
            <Receipt className="w-6 h-6 text-primary" />
            New Expense
          </DialogTitle>
        </DialogHeader>

        <ScrollArea className="flex-1 pr-4">
          <div className="space-y-6 py-4">
            {/* Basic Info */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Input 
                  id="description" 
                  placeholder="e.g. Dinner at Joe's" 
                  value={description}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setDescription(e.target.value)}
                  className="bg-white/5 border-white/10"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="amount">Amount</Label>
                <div className="relative">
                  <Input 
                    id="amount" 
                    type="number" 
                    placeholder="0.00" 
                    value={amount}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setAmount(e.target.value)}
                    className="bg-white/5 border-white/10 pl-12 font-mono text-lg"
                  />
                  <div className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 font-mono">
                    {baseCurrency}
                  </div>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
               <div className="space-y-2">
                <Label>Category</Label>
                <Select value={category} onValueChange={setCategory}>
                  <SelectTrigger className="bg-white/5 border-white/10">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent className="bg-slate-900 border-white/10">
                    <SelectItem value="Food">Food</SelectItem>
                    <SelectItem value="Transport">Transport</SelectItem>
                    <SelectItem value="Accommodation">Accommodation</SelectItem>
                    <SelectItem value="Entertainment">Entertainment</SelectItem>
                    <SelectItem value="Shopping">Shopping</SelectItem>
                    <SelectItem value="Other">Other</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label>Date</Label>
                <Input 
                  type="date" 
                  value={date}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setDate(e.target.value)}
                  className="bg-white/5 border-white/10"
                />
              </div>
            </div>

            {/* Payers */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <Label className="flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-primary" />
                  Paid By
                </Label>
                <Button variant="ghost" size="sm" onClick={addPayer} className="text-primary hover:bg-primary/10 h-7 text-xs">
                  <Plus className="w-3 h-3 mr-1" /> Add Payer
                </Button>
              </div>
              <div className="space-y-2">
                {payers.map((payer, idx) => (
                  <div key={idx} className="flex gap-2 items-center">
                    <Select value={payer.userId} onValueChange={(v: string) => updatePayer(idx, 'userId', v)}>
                      <SelectTrigger className="flex-1 bg-white/5 border-white/10">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="bg-slate-900 border-white/10">
                        {members.map(m => (
                          <SelectItem key={m.userId._id} value={m.userId._id}>{m.userId.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <Input 
                      type="number" 
                      placeholder="0.00" 
                      value={payer.amount}
                      onChange={(e: React.ChangeEvent<HTMLInputElement>) => updatePayer(idx, 'amount', e.target.value)}
                      className="w-32 bg-white/5 border-white/10 font-mono"
                    />
                    {payers.length > 1 && (
                      <Button variant="ghost" size="icon" onClick={() => removePayer(idx)} className="text-red-400 hover:bg-red-400/10">
                        <Minus className="w-4 h-4" />
                      </Button>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Split Type */}
            <div className="space-y-4 pt-2">
              <Label className="flex items-center gap-2">
                <Users className="w-4 h-4 text-primary" />
                Split Options
              </Label>
              <Tabs value={splitType} onValueChange={(v: string) => setSplitType(v as SplitType)} className="w-full">
                <TabsList className="grid w-full grid-cols-4 bg-white/5 border border-white/10">
                  <TabsTrigger value="equal">Equal</TabsTrigger>
                  <TabsTrigger value="exact">Exact</TabsTrigger>
                  <TabsTrigger value="percentage">%</TabsTrigger>
                  <TabsTrigger value="shares">Shares</TabsTrigger>
                </TabsList>
              </Tabs>

              <div className="glass-card p-4 rounded-xl space-y-4">
                {members.map(member => {
                  const isSelected = selectedMembers.includes(member.userId._id);
                  const preview = previewSplits.find(s => s.userId === member.userId._id);

                  return (
                    <div key={member.userId._id} className="flex items-center justify-between gap-4">
                      <div className="flex items-center gap-3">
                        <Checkbox 
                          checked={isSelected}
                          onCheckedChange={(checked: boolean) => {
                            if (checked) setSelectedMembers([...selectedMembers, member.userId._id]);
                            else setSelectedMembers(selectedMembers.filter(id => id !== member.userId._id));
                          }}
                        />
                        <span className={`text-sm ${isSelected ? 'text-white' : 'text-gray-500'}`}>
                          {member.userId.name}
                        </span>
                      </div>
                      
                      <div className="flex items-center gap-4">
                        {splitType !== 'equal' && isSelected && (
                          <Input 
                            type="number"
                            placeholder={splitType === 'percentage' ? '%' : splitType === 'shares' ? 'shares' : '0.00'}
                            value={customValues[member.userId._id] || ''}
                            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setCustomValues({ ...customValues, [member.userId._id]: e.target.value })}
                            className="w-20 h-8 bg-white/5 border-white/10 text-xs font-mono"
                          />
                        )}
                        <div className="w-24 text-right">
                          <span className={`text-sm font-mono ${isSelected ? 'text-primary' : 'text-gray-700'}`}>
                            {preview ? `${baseCurrency} ${preview.owedAmount}` : '--'}
                          </span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </ScrollArea>

        <DialogFooter className="pt-6 border-t border-white/10">
          <Button variant="ghost" onClick={() => setOpen(false)} className="text-gray-400">Cancel</Button>
          <Button onClick={handleSubmit} className="btn-primary px-8">
            Save Expense
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

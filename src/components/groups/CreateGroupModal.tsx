import React, { useState } from 'react';
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle, 
  DialogTrigger,
  DialogFooter
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Plus } from 'lucide-react';
import { useGroups } from '@/hooks/useGroups';
import { toast } from 'sonner';

export const CreateGroupModal: React.FC = () => {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [budget, setBudget] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [familyGrouping, setFamilyGrouping] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { createGroup } = useGroups();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name) return;

    setIsSubmitting(true);
    try {
      await createGroup({
        name,
        description,
        budget,
        budgetCurrency: currency,
        familyGroupingEnabled: familyGrouping
      });
      toast.success('Group created successfully!');
      setOpen(false);
      resetForm();
    } catch (error) {
      console.error(error);
      toast.error('Failed to create group');
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setName('');
    setDescription('');
    setBudget('');
    setCurrency('USD');
    setFamilyGrouping(false);
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger 
        render={
          <Button className="btn-primary shadow-lg shadow-primary/20">
            <Plus className="w-5 h-5 mr-2" />
            Create New Group
          </Button>
        }
      />
      <DialogContent className="sm:max-w-[425px] glass-card bg-slate-900 border-white/10 text-white">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold">Create New Group</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">Group Name</Label>
              <Input
                id="name"
                placeholder="Trip to Japan, Home Expenses..."
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="bg-white/5 border-white/10 focus:border-primary/50"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Description (Optional)</Label>
              <Textarea
                id="description"
                placeholder="What's this group for?"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="bg-white/5 border-white/10 focus:border-primary/50 min-h-[100px]"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="currency">Base Currency</Label>
                <Input
                  id="currency"
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                  className="bg-white/5 border-white/10 focus:border-primary/50"
                  maxLength={3}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="budget">Budget (Optional)</Label>
                <Input
                  id="budget"
                  type="number"
                  placeholder="0.00"
                  value={budget}
                  onChange={(e) => setBudget(e.target.value)}
                  className="bg-white/5 border-white/10 focus:border-primary/50"
                />
              </div>
            </div>
            <div className="flex items-center gap-2 mt-2">
              <input
                type="checkbox"
                id="familyGrouping"
                checked={familyGrouping}
                onChange={(e) => setFamilyGrouping(e.target.checked)}
                className="rounded border-white/10 bg-white/5 text-primary focus:ring-primary/50"
              />
              <Label htmlFor="familyGrouping" className="text-sm text-gray-300">Enable Family Grouping</Label>
            </div>
          </div>
          <DialogFooter>
            <Button 
              type="submit" 
              className="w-full btn-primary"
              disabled={isSubmitting || !name}
            >
              {isSubmitting ? 'Creating...' : 'Create Group'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

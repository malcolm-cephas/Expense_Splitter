import React, { useState, useEffect } from 'react';
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle, 
  DialogFooter
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { toast } from 'sonner';

interface EditGroupModalProps {
  group: any;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUpdate: (data: any) => Promise<any>;
}

export const EditGroupModal: React.FC<EditGroupModalProps> = ({ group, open, onOpenChange, onUpdate }) => {
  const [name, setName] = useState(group?.name || '');
  const [description, setDescription] = useState(group?.description || '');
  const [budget, setBudget] = useState(group?.budget || '');
  const [currency, setCurrency] = useState(group?.budgetCurrency || 'USD');
  const [familyGrouping, setFamilyGrouping] = useState(group?.familyGroupingEnabled || false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (group) {
      setName(group.name);
      setDescription(group.description || '');
      setBudget(group.budget || '');
      setCurrency(group.budgetCurrency || 'USD');
      setFamilyGrouping(group.familyGroupingEnabled || false);
    }
  }, [group]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name) return;

    setIsSubmitting(true);
    try {
      await onUpdate({
        name,
        description,
        budget,
        budgetCurrency: currency,
        familyGroupingEnabled: familyGrouping
      });
      toast.success('Group updated successfully!');
      onOpenChange(false);
    } catch (error) {
      console.error(error);
      toast.error('Failed to update group');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px] glass-card bg-slate-900 border-white/10 text-white">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold">Edit Group Details</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="edit-name">Group Name</Label>
              <Input
                id="edit-name"
                placeholder="Trip to Japan, Home Expenses..."
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="bg-white/5 border-white/10 focus:border-primary/50"
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-description">Description (Optional)</Label>
              <Textarea
                id="edit-description"
                placeholder="What's this group for?"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="bg-white/5 border-white/10 focus:border-primary/50 min-h-[100px]"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="edit-currency">Base Currency</Label>
                <Input
                  id="edit-currency"
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                  className="bg-white/5 border-white/10 focus:border-primary/50"
                  maxLength={3}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-budget">Budget (Optional)</Label>
                <Input
                  id="edit-budget"
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
                id="edit-familyGrouping"
                checked={familyGrouping}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFamilyGrouping(e.target.checked)}
                className="rounded border-white/10 bg-white/5 text-primary focus:ring-primary/50"
              />
              <Label htmlFor="edit-familyGrouping" className="text-sm text-gray-300">Enable Family Grouping</Label>
            </div>
          </div>
          <DialogFooter>
            <Button 
              type="submit" 
              className="w-full btn-primary"
              disabled={isSubmitting || !name}
            >
              {isSubmitting ? 'Updating...' : 'Save Changes'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

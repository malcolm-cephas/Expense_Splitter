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
import { Plus, X, UserPlus, Users } from 'lucide-react';
import { useGroups } from '@/hooks/useGroups';
import { toast } from 'sonner';

export const CreateGroupModal: React.FC = () => {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [budget, setBudget] = useState('');
  const [currency, setCurrency] = useState('INR');
  const [familyGrouping, setFamilyGrouping] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const [memberName, setMemberName] = useState('');
  const [memberEmail, setMemberEmail] = useState('');
  const [initialMembers, setInitialMembers] = useState<{ name: string; email?: string }[]>([]);

  const { createGroup } = useGroups();

  const handleAddMember = () => {
    if (!memberName) {
      toast.error('Member name is required');
      return;
    }
    if (memberEmail && !memberEmail.includes('@')) {
      toast.error('Invalid email address');
      return;
    }
    if (memberEmail && initialMembers.some(m => m.email === memberEmail)) {
      toast.error('Member with this email already added');
      return;
    }
    setInitialMembers([...initialMembers, { name: memberName, email: memberEmail }]);
    setMemberName('');
    setMemberEmail('');
  };

  const removeMember = (index: number) => {
    setInitialMembers(initialMembers.filter((_, i) => i !== index));
  };

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
        familyGroupingEnabled: familyGrouping,
        initialMembers
      });
      toast.success('Group created and invites sent!');
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
    setCurrency('INR');
    setFamilyGrouping(false);
    setInitialMembers([]);
    setMemberName('');
    setMemberEmail('');
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
      <DialogContent className="sm:max-w-[500px] glass-card bg-slate-900 border-white/10 text-white overflow-hidden flex flex-col max-h-[90vh]">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold flex items-center gap-2">
            <Users className="w-6 h-6 text-primary" />
            Create New Group
          </DialogTitle>
        </DialogHeader>
        
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto pr-2 custom-scrollbar">
          <div className="grid gap-6 py-4">
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name" className="text-xs uppercase tracking-widest text-gray-400">Group Name</Label>
                <Input
                  id="name"
                  placeholder="Trip to Japan, Home Expenses..."
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="bg-white/5 border-white/10 focus:border-primary/50 h-12 text-lg"
                  required
                />
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="description" className="text-xs uppercase tracking-widest text-gray-400">Description (Optional)</Label>
                <Textarea
                  id="description"
                  placeholder="What's this group for?"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="bg-white/5 border-white/10 focus:border-primary/50 min-h-[80px] resize-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="currency" className="text-xs uppercase tracking-widest text-gray-400">Currency</Label>
                <Input
                  id="currency"
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                  className="bg-white/5 border-white/10 focus:border-primary/50"
                  maxLength={3}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="budget" className="text-xs uppercase tracking-widest text-gray-400">Budget</Label>
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

            <div className="space-y-3 bg-white/5 p-4 rounded-xl border border-white/5">
              <Label className="text-xs uppercase tracking-widest text-gray-400">Add Members (Optional)</Label>
              <div className="flex flex-col gap-2">
                <div className="flex gap-2">
                  <Input
                    placeholder="Name (e.g. John)"
                    value={memberName}
                    onChange={(e) => setMemberName(e.target.value)}
                    className="bg-white/10 border-white/5 focus:border-primary/30 flex-1"
                  />
                  <Input
                    placeholder="Email (Optional)"
                    value={memberEmail}
                    onChange={(e) => setMemberEmail(e.target.value)}
                    className="bg-white/10 border-white/5 focus:border-primary/30 flex-1"
                  />
                  <Button type="button" size="icon" onClick={handleAddMember} className="bg-primary/20 text-primary hover:bg-primary hover:text-background-dark shrink-0">
                    <UserPlus className="w-4 h-4" />
                  </Button>
                </div>
                <p className="text-[10px] text-gray-500 italic">Adding a name only creates a managed local member.</p>
              </div>
              
              {initialMembers.length > 0 && (
                <div className="flex flex-wrap gap-2 pt-2">
                  {initialMembers.map((member, index) => (
                    <div key={index} className="flex items-center gap-1.5 px-3 py-1 bg-white/10 rounded-full border border-white/10 text-xs">
                      <span className="font-bold">{member.name}</span>
                      {member.email && <span className="text-gray-500 ml-1 truncate max-w-[100px]">({member.email})</span>}
                      <button type="button" onClick={() => removeMember(index)} className="text-gray-500 hover:text-red-400 transition-colors ml-1">
                        <X className="w-3 h-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="flex items-center gap-2 px-2">
              <input
                type="checkbox"
                id="familyGrouping"
                checked={familyGrouping}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFamilyGrouping(e.target.checked)}
                className="rounded border-white/10 bg-white/5 text-primary focus:ring-primary/50 w-4 h-4"
              />
              <Label htmlFor="familyGrouping" className="text-sm text-gray-300">Enable Family Grouping</Label>
            </div>
          </div>
          
          <DialogFooter className="pt-4 border-t border-white/10">
            <Button 
              type="submit" 
              className="w-full btn-primary h-12 text-lg shadow-xl shadow-primary/20"
              disabled={isSubmitting || !name}
            >
              {isSubmitting ? 'Creating Group...' : 'Create Group'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

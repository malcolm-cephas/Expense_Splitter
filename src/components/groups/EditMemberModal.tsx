import React, { useState } from 'react';
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle, 
  DialogFooter,
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
import { User, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import type { GroupMember } from '@/hooks/useGroupDetail';

interface EditMemberModalProps {
  member: GroupMember;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUpdate: (userId: string, data: { name?: string; role?: string }) => Promise<any>;
}

export const EditMemberModal: React.FC<EditMemberModalProps> = ({ 
  member, 
  open, 
  onOpenChange, 
  onUpdate 
}) => {
  const [name, setName] = useState(member.userId.name);
  const [role, setRole] = useState(member.role);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Check if user is a ghost (managed locally)
  // We can detect this by checking if the email contains '@managed.local' or similar logic
  // but for now let's assume if they have a name we can try to update it.
  const isGhost = !member.userId.email || member.userId.email.includes('@managed.local');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await onUpdate(member.userId._id, { name: isGhost ? name : undefined, role });
      toast.success('Member updated successfully');
      onOpenChange(false);
    } catch (error) {
      toast.error('Failed to update member');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px] glass-card bg-slate-900 border-white/10 text-white">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle className="text-xl font-bold flex items-center gap-2">
              <User className="w-5 h-5 text-primary" />
              Edit Member
            </DialogTitle>
          </DialogHeader>
          
          <div className="py-6 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="edit-name">Display Name</Label>
              <Input 
                id="edit-name" 
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="bg-white/5 border-white/10 focus:border-primary/50"
                disabled={!isGhost}
                placeholder={isGhost ? "Enter name" : "Only local members can be renamed"}
              />
              {!isGhost && (
                <p className="text-[10px] text-gray-500 italic">
                  Registered users manage their own names in their profile.
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit-role">Group Role</Label>
              <Select value={role} onValueChange={(val: any) => setRole(val)}>
                <SelectTrigger className="bg-white/5 border-white/10 focus:ring-primary/50">
                  <SelectValue placeholder="Select role" />
                </SelectTrigger>
                <SelectContent className="glass-card bg-slate-900 border-white/10 text-white">
                  <SelectItem value="member" className="focus:bg-white/10">Member</SelectItem>
                  <SelectItem value="admin" className="focus:bg-white/10">Admin</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <DialogFooter>
            <Button 
              type="button" 
              variant="ghost" 
              onClick={() => onOpenChange(false)}
              className="text-gray-400"
            >
              Cancel
            </Button>
            <Button 
              type="submit" 
              className="btn-primary"
              disabled={isSubmitting || (isGhost && !name)}
            >
              {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : null}
              Save Changes
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

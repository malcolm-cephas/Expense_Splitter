import React, { useState } from 'react';
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
import { UserPlus, Mail, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

interface InviteMemberModalProps {
  onInvite: (email: string) => Promise<any>;
  trigger?: React.ReactNode;
}

export const InviteMemberModal: React.FC<InviteMemberModalProps> = ({ 
  onInvite,
  trigger
}) => {
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;

    setIsSubmitting(true);
    try {
      const result = await onInvite(email);
      if (result.status === 'joined') {
        toast.success(`${email} has been added to the group!`);
      } else {
        toast.info(`Invite sent! ${email} will be added once they sign up.`);
      }
      setOpen(false);
      setEmail('');
    } catch (error) {
      toast.error('Failed to send invitation. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger 
        render={(trigger as React.ReactElement) || (
          <Button variant="outline" size="sm" className="border-white/10 text-primary bg-primary/5">
            <UserPlus className="w-4 h-4 mr-2" />
            Invite Member
          </Button>
        )}
      />
      <DialogContent className="sm:max-w-[425px] glass-card bg-slate-900 border-white/10 text-white">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle className="text-xl font-bold flex items-center gap-2">
              <UserPlus className="w-5 h-5 text-primary" />
              Invite to Group
            </DialogTitle>
          </DialogHeader>
          
          <div className="py-6 space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">Email Address</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                <Input 
                  id="email" 
                  type="email" 
                  placeholder="friend@example.com" 
                  value={email}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEmail(e.target.value)}
                  className="bg-white/5 border-white/10 pl-10 focus:border-primary/50"
                  required
                />
              </div>
            </div>
            <p className="text-xs text-gray-500">
              If the user already has an account, they'll be added immediately. Otherwise, we'll send them a pending invite.
            </p>
          </div>

          <DialogFooter>
            <Button 
              type="button" 
              variant="ghost" 
              onClick={() => setOpen(false)}
              className="text-gray-400"
            >
              Cancel
            </Button>
            <Button 
              type="submit" 
              className="btn-primary"
              disabled={isSubmitting || !email}
            >
              {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : null}
              Send Invitation
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

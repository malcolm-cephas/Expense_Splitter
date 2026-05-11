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
import { FileUp, Loader2, FileJson } from 'lucide-react';
import { toast } from 'sonner';
import api from '@/lib/api';

export const ImportModal: React.FC = () => {
  const [open, setOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (event) => {
      try {
        const json = JSON.parse(event.target?.result as string);
        await importData(json);
      } catch (error) {
        toast.error('Invalid JSON file format');
      }
    };
    reader.readAsText(file);
  };

  const importData = async (data: any) => {
    setIsSubmitting(true);
    try {
      const response = await api.post('/groups/import', data);
      toast.success(`Group "${response.data.data.name}" imported successfully!`);
      setOpen(false);
      // Invalidate queries to refresh list
      window.location.reload(); // Simple way to refresh everything
    } catch (error) {
      console.error(error);
      toast.error('Failed to import group');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger 
        render={
          <Button variant="outline" className="border-white/10 text-gray-400 hover:text-white hover:bg-white/5">
            <FileUp className="w-5 h-5 mr-2" />
            Import from JSON
          </Button>
        }
      />
      <DialogContent className="sm:max-w-[425px] glass-card bg-slate-900 border-white/10 text-white">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold flex items-center gap-2">
            <FileJson className="w-6 h-6 text-primary" />
            Import Group
          </DialogTitle>
        </DialogHeader>
        
        <div className="py-10 flex flex-col items-center justify-center border-2 border-dashed border-white/10 rounded-xl bg-white/5 hover:bg-white/10 transition-all group relative">
          <input
            type="file"
            accept=".json"
            onChange={handleFileUpload}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
            disabled={isSubmitting}
          />
          <div className="flex flex-col items-center gap-3">
            <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center group-hover:scale-110 transition-transform">
              {isSubmitting ? <Loader2 className="w-6 h-6 text-primary animate-spin" /> : <FileUp className="w-6 h-6 text-primary" />}
            </div>
            <div className="text-center">
              <p className="text-sm font-medium text-white">Click or drag JSON file to import</p>
              <p className="text-xs text-gray-500 mt-1">Supports SplitPro exported JSON files</p>
            </div>
          </div>
        </div>

        <DialogFooter className="sm:justify-start">
          <p className="text-[10px] text-gray-600 uppercase tracking-widest text-center w-full">
            All imported expenses will be assigned to you as the payer
          </p>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

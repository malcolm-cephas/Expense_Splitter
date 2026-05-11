import { Routes, Route } from 'react-router-dom';
import { useAuth0 } from '@auth0/auth0-react';
import React, { useEffect } from 'react';
import { AppShell } from './components/layout/AppShell';
import Dashboard from './pages/Dashboard';
import GroupDetail from './pages/GroupDetail';
import { Loader2 } from 'lucide-react';
import { Toaster } from '@/components/ui/sonner';
import api from './lib/api';

function App() {
  const { isAuthenticated, isLoading, loginWithRedirect, user } = useAuth0();

  useEffect(() => {
    if (isAuthenticated && user) {
      // Upsert user in our DB
      api.post('/users/me').catch(console.error);
    }
  }, [isAuthenticated, user]);

  if (isLoading) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-background-dark">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="flex h-screen w-screen flex-col items-center justify-center bg-background-dark p-6 text-center">
        <div className="mb-8 h-20 w-20 rounded-3xl bg-gradient-to-br from-primary to-teal-500 p-0.5">
          <div className="flex h-full w-full items-center justify-center rounded-[22px] bg-slate-900">
             <span className="text-3xl font-black text-white">S</span>
          </div>
        </div>
        <h1 className="mb-2 text-4xl font-bold text-white tracking-tight">Welcome to SplitPro</h1>
        <p className="mb-8 max-w-sm text-gray-400">
          The ultimate platform for managing shared expenses with elegance and precision.
        </p>
        <button
          onClick={() => loginWithRedirect()}
          className="rounded-xl bg-primary px-10 py-4 font-bold text-background-dark shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all"
        >
          Get Started
        </button>
      </div>
    );
  }

  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/groups" element={<Dashboard />} />
        <Route path="/groups/:id" element={<GroupDetail />} />
        {/* More routes will be added here */}
        <Route path="*" element={<Dashboard />} />
      </Routes>
      <Toaster position="top-right" richColors />
    </AppShell>
  );
}

export default App;

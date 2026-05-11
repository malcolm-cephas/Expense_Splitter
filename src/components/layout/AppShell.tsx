import React from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { 
  LayoutDashboard, 
  Users, 
  Settings, 
  LogOut, 
  Menu, 
  X,
  CreditCard,
  History
} from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuLabel, 
  DropdownMenuSeparator, 
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu';

const Sidebar = ({ isOpen, toggle }: { isOpen: boolean; toggle: () => void }) => {
  const { user, logout } = useAuth0();

  const links = [
    { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/groups', icon: Users, label: 'Groups' },
    { to: '/statistics', icon: CreditCard, label: 'Statistics' },
    { to: '/activity', icon: History, label: 'Recent Activity' },
    { to: '/settings', icon: Settings, label: 'Settings' },
  ];

  return (
    <aside className={`fixed inset-y-0 left-0 z-50 w-64 glass-card rounded-none border-r border-white/10 transform transition-transform duration-300 lg:relative lg:translate-x-0 ${isOpen ? 'translate-x-0' : '-translate-x-full'}`}>
      <div className="flex items-center justify-between h-16 px-6 border-b border-white/10">
        <span className="text-xl font-bold bg-gradient-to-r from-primary to-teal-400 bg-clip-text text-transparent">
          SplitPro
        </span>
        <Button variant="ghost" size="icon" className="lg:hidden" onClick={toggle}>
          <X className="w-5 h-5" />
        </Button>
      </div>

      <ScrollArea className="flex-1 px-4 py-6">
        <nav className="space-y-2">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                  isActive 
                    ? 'bg-primary/10 text-primary' 
                    : 'text-gray-400 hover:bg-white/5 hover:text-white'
                }`
              }
            >
              <link.icon className="w-5 h-5" />
              <span className="font-medium">{link.label}</span>
            </NavLink>
          ))}
        </nav>
      </ScrollArea>

      <div className="p-4 border-t border-white/10">
        <DropdownMenu>
          <DropdownMenuTrigger 
            render={
              <Button variant="ghost" className="w-full flex items-center justify-start gap-3 p-2 hover:bg-white/5 h-auto">
                <Avatar className="w-9 h-9 border border-white/20">
                  <AvatarImage src={user?.picture} />
                  <AvatarFallback>{user?.name?.[0]}</AvatarFallback>
                </Avatar>
                <div className="flex-1 text-left">
                  <p className="text-sm font-medium text-white truncate">{user?.name}</p>
                  <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                </div>
              </Button>
            }
          />
          <DropdownMenuContent align="end" className="w-56 glass-card bg-slate-900/95 border-white/10">
            <DropdownMenuLabel>My Account</DropdownMenuLabel>
            <DropdownMenuSeparator className="bg-white/10" />
            <DropdownMenuItem className="focus:bg-white/10 focus:text-white">Profile</DropdownMenuItem>
            <DropdownMenuItem className="focus:bg-white/10 focus:text-white">Settings</DropdownMenuItem>
            <DropdownMenuSeparator className="bg-white/10" />
            <DropdownMenuItem className="text-red-400 focus:bg-red-400/10 focus:text-red-400" onClick={() => logout()}>
              <LogOut className="w-4 h-4 mr-2" />
              Log Out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </aside>
  );
};

const TopBar = ({ toggleSidebar }: { toggleSidebar: () => void }) => {
  return (
    <header className="h-16 border-b border-white/10 flex items-center justify-between px-6 bg-background-dark/50 backdrop-blur-sm sticky top-0 z-40">
      <Button variant="ghost" size="icon" className="lg:hidden" onClick={toggleSidebar}>
        <Menu className="w-6 h-6" />
      </Button>
      <div className="flex-1" />
      <div className="flex items-center gap-4">
        {/* Notifications or Search could go here */}
      </div>
    </header>
  );
};

export const AppShell: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isSidebarOpen, setIsSidebarOpen] = React.useState(false);

  return (
    <div className="flex min-h-screen bg-background-dark text-gray-100">
      <Sidebar isOpen={isSidebarOpen} toggle={() => setIsSidebarOpen(false)} />
      
      <div className="flex-1 flex flex-col min-w-0">
        <TopBar toggleSidebar={() => setIsSidebarOpen(true)} />
        <main className="flex-1 overflow-y-auto p-6 lg:p-8">
          {children}
        </main>
      </div>

      {/* Overlay for mobile sidebar */}
      {isSidebarOpen && (
        <div 
          className="fixed inset-0 z-40 bg-black/60 lg:hidden"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}
    </div>
  );
};

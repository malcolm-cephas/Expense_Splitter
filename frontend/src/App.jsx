import { useAuth0 } from "@auth0/auth0-react";
import { motion, AnimatePresence } from "framer-motion";
import { Wallet, Users, Receipt, PieChart, LogIn, LogOut, Plus, ChevronRight } from "lucide-react";
import "./App.css";

function App() {
  const { loginWithRedirect, logout, isAuthenticated, user, isLoading } = useAuth0();

  if (isLoading) {
    return (
      <div className="auth-container">
        <div className="loading-spinner" />
      </div>
    );
  }

  return (
    <div className="app-shell">
      <AnimatePresence mode="wait">
        {!isAuthenticated ? (
          <motion.div
            key="hero"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="auth-container"
          >
            <div className="glass-panel auth-card fade-in">
              <div className="logo-icon">
                <Wallet size={32} />
              </div>
              <div className="welcome-text">
                <h1>Expense Splitter Pro</h1>
                <p>Collaborative group finances made elegant.</p>
              </div>
              <button 
                className="btn-primary w-full" 
                onClick={() => loginWithRedirect()}
              >
                <LogIn size={20} />
                Get Started
              </button>
              <div className="hero-features">
                <div className="feature-pill">
                  <Users size={14} /> Multi-user
                </div>
                <div className="feature-pill">
                  <Receipt size={14} /> Smart Settle
                </div>
              </div>
            </div>
          </motion.div>
        ) : (
          <motion.div
            key="dashboard"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="dashboard-container"
          >
            {/* Header */}
            <header className="dashboard-header glass-panel">
              <div className="brand">
                <Wallet className="text-primary" />
                <span>Splitter Pro</span>
              </div>
              <div className="user-profile">
                <span className="user-name">{user.name}</span>
                <img src={user.picture} alt={user.name} className="avatar" />
                <button onClick={() => logout({ returnTo: window.location.origin })} className="logout-btn">
                  <LogOut size={18} />
                </button>
              </div>
            </header>

            {/* Main Grid */}
            <main className="dashboard-main">
              <section className="dashboard-hero">
                <h1>Welcome back, {user.given_name || user.name.split(' ')[0]}!</h1>
                <p>You have 3 active collaborative groups.</p>
              </section>

              <div className="stats-row">
                <div className="stat-card glass-panel">
                  <div className="stat-label">Total Balance</div>
                  <div className="stat-value text-positive">₹1,250.00</div>
                  <div className="stat-delta">+12% from last week</div>
                </div>
                <div className="stat-card glass-panel">
                  <div className="stat-label">Pending Settlements</div>
                  <div className="stat-value">4</div>
                  <div className="stat-delta text-negative">2 overdue</div>
                </div>
              </div>

              <section className="groups-section">
                <div className="section-header">
                  <h2>Your Groups</h2>
                  <button className="btn-icon-plus">
                    <Plus size={20} />
                  </button>
                </div>
                <div className="groups-list">
                  {[
                    { id: 1, name: "Euro Trip 2024", members: 4, balance: -450 },
                    { id: 2, name: "Flat Mates", members: 3, balance: 1200 },
                    { id: 3, name: "Office Lunch", members: 12, balance: 0 },
                  ].map((group) => (
                    <motion.div 
                      key={group.id} 
                      whileHover={{ scale: 1.02 }}
                      className="group-card glass-panel"
                    >
                      <div className="group-info">
                        <h3>{group.name}</h3>
                        <p>{group.members} collaborators</p>
                      </div>
                      <div className="group-action">
                        <span className={`balance ${group.balance < 0 ? 'negative' : 'positive'}`}>
                          {group.balance < 0 ? `- ₹${Math.abs(group.balance)}` : `+ ₹${group.balance}`}
                        </span>
                        <ChevronRight size={20} />
                      </div>
                    </motion.div>
                  ))}
                </div>
              </section>
            </main>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export default App;

import { useAuth0 } from "@auth0/auth0-react";
import { motion, AnimatePresence } from "framer-motion";
import { Wallet, Users, Receipt, PieChart, LogIn, LogOut, Plus, ChevronRight } from "lucide-react";
import { useState, useEffect } from "react";
import axios from "axios";
import "./App.css";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function App() {
  const { loginWithRedirect, logout, isAuthenticated, user, isLoading, getAccessTokenSilently } = useAuth0();
  const [groups, setGroups] = useState([]);
  const [loadingData, setLoadingData] = useState(false);

  useEffect(() => {
    const fetchGroups = async () => {
      if (!isAuthenticated) return;
      try {
        setLoadingData(true);
        const token = await getAccessTokenSilently();
        const response = await axios.get(`${API_BASE}/groups`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        setGroups(response.data);
      } catch (e) {
        console.error("Error fetching groups:", e);
      } finally {
        setLoadingData(false);
      }
    };
    fetchGroups();
  }, [isAuthenticated, getAccessTokenSilently]);

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
                  {loadingData ? (
                     <div className="loading-spinner" style={{ margin: '20px auto' }} />
                  ) : groups.length === 0 ? (
                     <p style={{ textAlign: "center", color: "var(--text-muted)", padding: "20px" }}>
                       You don't have any groups yet. Click the + button to create one!
                     </p>
                  ) : groups.map((group) => (
                    <motion.div 
                      key={group.id} 
                      whileHover={{ scale: 1.02 }}
                      className="group-card glass-panel"
                      onClick={() => alert(`Clicked on ${group.name}! Full feature opening soon.`)}
                    >
                      <div className="group-info">
                        <h3>{group.name}</h3>
                        <p>{group.members?.length || 1} collaborators</p>
                      </div>
                      <div className="group-action">
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

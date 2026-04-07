import { useAuth0 } from "@auth0/auth0-react";
import { useState, useEffect } from "react";
import axios from "axios";
import "./App.css";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function App() {
  const { loginWithRedirect, logout, isAuthenticated, getAccessTokenSilently } = useAuth0();
  const [groups, setGroups] = useState([]);
  const [loadingData, setLoadingData] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);

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

  return (
    <div className="app-border-pane">
      {/* Top Header */}
      <div className="header-bar">
        <div className="brand-title">
          💸 Expense Splitter Pro
        </div>
        <div className="header-tools">
          <button className="fx-button" style={{ width: '90px' }} onClick={() => alert('Theme toggle disabled for classic styling.')}>
            🌙 Dark
          </button>
          <span className="text-muted">Default Currency: </span>
          <input type="text" className="currency-input" placeholder="Search currency (e.g. INR)" />
          {isAuthenticated ? (
             <button className="fx-button" style={{ width: '80px', marginLeft: '10px' }} onClick={() => logout({ returnTo: window.location.origin })}>
               Logout
             </button>
          ) : (
             <button className="fx-button accent" style={{ width: '80px', marginLeft: '10px' }} onClick={() => loginWithRedirect()}>
               Login
             </button>
          )}
        </div>
      </div>

      {/* Main Body */}
      <div className="main-body">
        {/* Left Sidebar */}
        <div className="sidebar">
          <h2>Groups</h2>
          
          <div className="list-view">
            {loadingData ? (
               <div style={{ padding: '10px', color: '#666' }}>Loading...</div>
            ) : groups.length === 0 ? (
               <div style={{ padding: '10px', color: '#666' }}>{isAuthenticated ? 'No groups found.' : 'Log in to sync groups.'}</div>
            ) : (
               groups.map(group => (
                 <div 
                   key={group.id} 
                   className="list-item" 
                   onClick={() => setSelectedGroup(group)}
                   style={{ backgroundColor: selectedGroup?.id === group.id ? 'var(--list-selected)' : 'transparent' }}
                 >
                   📁 {group.name}
                 </div>
               ))
            )}
          </div>

          <h4>Create New Group</h4>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
            <input type="text" className="fx-input" placeholder="Group Name" />
            <input type="text" className="fx-input" placeholder="Initial Budget (Optional)" />
            <button className="fx-button accent" onClick={() => alert('Create button clicked - functionality soon!')}>Create</button>
          </div>

          <h4>Tools</h4>
          <button className="fx-button" onClick={() => alert('Import features coming soon.')}>📂 Import Group Backup</button>
        </div>

        {/* Center Main Content Area */}
        <div className="content-area">
          {selectedGroup ? (
            <div style={{ padding: '20px' }}>
              <h1 style={{ marginTop: 0 }}>{selectedGroup.name}</h1>
              <p>ID: {selectedGroup.id}</p>
              <p>Budget: {selectedGroup.budgetCurrency || 'INR'} {selectedGroup.budget || 0}</p>
              <p>Members: {selectedGroup.members?.length || 1}</p>
              <div style={{ marginTop: '20px', padding: '20px', backgroundColor: '#f9f9f9', border: '1px solid #ddd' }}>
                 Expense components will load here.
              </div>
            </div>
          ) : (
            <div className="center-placeholder">
              Select a group from the sidebar
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;

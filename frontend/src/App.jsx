import { useState, useEffect, useRef } from "react";
import axios from "axios";
import jsPDF from "jspdf";
import "jspdf-autotable";
import "./App.css";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function App() {
  const { loginWithRedirect, logout, isAuthenticated, getAccessTokenSilently, user } = useAuth0();
  const fileInputRef = useRef(null);
  const [groups, setGroups] = useState([]);
  const [loadingData, setLoadingData] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [me, setMe] = useState(null);

  // New Group State
  const [newGroupName, setNewGroupName] = useState("");
  const [newGroupBudget, setNewGroupBudget] = useState("");
  
  // Theme & Currency
  const [currency, setCurrency] = useState("INR");
  const [isDark, setIsDark] = useState(false);

  // Modals Controller
  const [activeModal, setActiveModal] = useState(null); // 'ADD_MEMBER', 'ADD_EXPENSE', 'SETTLE_UP', 'STATS'
  
  // Modal states
  const [memberEmail, setMemberEmail] = useState("");
  const [expenseDesc, setExpenseDesc] = useState("");
  const [expenseAmt, setExpenseAmt] = useState("");
  const [expenseCat, setExpenseCat] = useState("Other");
  const [settlementGraph, setSettlementGraph] = useState([]);

  useEffect(() => {
    if (isDark) {
      document.documentElement.classList.add("dark-theme");
    } else {
      document.documentElement.classList.remove("dark-theme");
    }
  }, [isDark]);

  useEffect(() => {
    const fetchGroups = async () => {
      if (!isAuthenticated) return;
      try {
        setLoadingData(true);
        const token = await getAccessTokenSilently();
        
        if (user && !me) setMe(user);

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
  }, [isAuthenticated, getAccessTokenSilently, user, me]);

  const handleCreateGroup = async () => {
    if (!newGroupName) return alert("Please enter a group name");
    try {
      const token = await getAccessTokenSilently();
      const budgetVal = newGroupBudget ? parseFloat(newGroupBudget) : 0;
      const response = await axios.post(`${API_BASE}/groups`, {
        name: newGroupName,
        budget: budgetVal,
        budgetCurrency: currency
      }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setGroups([...groups, response.data]);
      setNewGroupName("");
      setNewGroupBudget("");
    } catch(e) {
      console.error(e);
      alert("Failed to create group");
    }
  };

  // --- CRUD Modals Handlers ---

  const submitAddMember = async () => {
    if (!memberEmail) return;
    try {
      const token = await getAccessTokenSilently();
      const response = await axios.post(`${API_BASE}/groups/${selectedGroup.id}/members?email=${encodeURIComponent(memberEmail)}`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setSelectedGroup(response.data);
      setGroups(groups.map(g => g.id === response.data.id ? response.data : g));
      setActiveModal(null);
      setMemberEmail("");
    } catch (e) {
      console.error(e); alert("Failed to add member.");
    }
  };

  const submitAddExpense = async () => {
    const amount = parseFloat(expenseAmt);
    if (isNaN(amount) || !expenseDesc) return alert("Invalid amount or description.");
    try {
      const token = await getAccessTokenSilently();
      const myMember = selectedGroup.members.find(m => m.email === user.email) || selectedGroup.members[0];
      const paidById = myMember ? myMember.id : selectedGroup.members[0].id;
      
      const response = await axios.post(`${API_BASE}/expenses`, null, {
        params: {
          groupId: selectedGroup.id,
          paidById: paidById,
          amount: amount,
          description: expenseDesc,
          splitType: 'EQUAL',
          category: expenseCat,
          currency: selectedGroup.budgetCurrency || 'INR',
          ignoreDuplicate: true
        },
        headers: { Authorization: `Bearer ${token}` }
      });
      const updatedGroup = {...selectedGroup, expenses: [...(selectedGroup.expenses || []), response.data]};
      setSelectedGroup(updatedGroup);
      setGroups(groups.map(g => g.id === updatedGroup.id ? updatedGroup : g));
      setActiveModal(null);
      setExpenseAmt(""); setExpenseDesc("");
    } catch (e) {
      console.error(e); alert("Failed to add expense.");
    }
  };

  const loadSettlementGraph = async () => {
    setActiveModal('SETTLE_UP');
    try {
      const token = await getAccessTokenSilently();
      const response = await axios.get(`${API_BASE}/groups/${selectedGroup.id}/debt-graph`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setSettlementGraph(response.data.edges || []);
    } catch (e) {
      console.error(e); alert("Failed to calculate settlement graph.");
    }
  };

  // --- Inline Edits ---
  
  const handleEditExpense = async (exp) => {
    const input = window.prompt("Edit expense description and amount (e.g. 'Lunch 500'):", `${exp.description} ${exp.amount}`);
    if (!input) return;
    const parts = input.trim().split(" ");
    const amountStr = parts.pop();
    const description = parts.join(" ");
    const amount = parseFloat(amountStr);
    if (isNaN(amount) || !description) return alert("Invalid format.");

    try {
      const token = await getAccessTokenSilently();
      const response = await axios.put(`${API_BASE}/expenses/${exp.id}`, null, {
        params: { amount, description },
        headers: { Authorization: `Bearer ${token}` }
      });
      const updatedGroup = {...selectedGroup, expenses: selectedGroup.expenses.map(e => e.id === exp.id ? response.data : e)};
      setSelectedGroup(updatedGroup);
      setGroups(groups.map(g => g.id === updatedGroup.id ? updatedGroup : g));
    } catch(e) {
      console.error(e); alert("Failed to edit expense.");
    }
  };

  const handleDeleteExpense = async (exp) => {
    if (!window.confirm("Are you sure you want to delete this expense?")) return;
    try {
      const token = await getAccessTokenSilently();
      await axios.delete(`${API_BASE}/expenses/${exp.id}`, { headers: { Authorization: `Bearer ${token}` } });
      const updatedGroup = {...selectedGroup, expenses: selectedGroup.expenses.filter(e => e.id !== exp.id)};
      setSelectedGroup(updatedGroup);
      setGroups(groups.map(g => g.id === updatedGroup.id ? updatedGroup : g));
    } catch(e) {
      console.error(e); alert("Failed to delete expense.");
    }
  };

  const handleRemoveMember = async (member) => {
    if (!window.confirm("Remove member from group?")) return;
    try {
      const token = await getAccessTokenSilently();
      const response = await axios.delete(`${API_BASE}/groups/${selectedGroup.id}/members/${member.id}`, { headers: { Authorization: `Bearer ${token}` } });
      setSelectedGroup(response.data);
      setGroups(groups.map(g => g.id === response.data.id ? response.data : g));
    } catch(e) {
      console.error(e); alert("Failed to remove member. You might not be the creator.");
    }
  };

  const handleExportCSV = () => {
    if (!selectedGroup || !selectedGroup.expenses) return;
    const header = "Date,Description,Amount,Category\n";
    const rows = selectedGroup.expenses.map(e => `${e.expenseDate || ''},"${e.description}",${e.amount},${e.category || ''}`).join("\n");
    const blob = new Blob([header + rows], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Group_${selectedGroup.name}_Export.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const handleExportPDF = () => {
    if (!selectedGroup) return;
    const doc = new jsPDF();
    doc.text(`Expense Report: ${selectedGroup.name}`, 14, 15);
    doc.text(`Budget: ${selectedGroup.budgetCurrency || 'INR'} ${selectedGroup.budget || 0}`, 14, 25);
    
    const tableData = (selectedGroup.expenses || []).map(e => [
      e.expenseDate || '',
      e.description,
      e.category || 'Other',
      `${selectedGroup.budgetCurrency || 'INR'} ${e.amount}`
    ]);

    doc.autoTable({
      startY: 35,
      head: [['Date', 'Description', 'Category', 'Amount']],
      body: tableData,
    });

    doc.save(`Group_${selectedGroup.name}_Report.pdf`);
  };

  const handleExportJSON = () => {
    if (!selectedGroup) return;
    const dataStr = JSON.stringify(selectedGroup, null, 2);
    const blob = new Blob([dataStr], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Backup_${selectedGroup.name}.json`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const handleImportJSON = (event) => {
    const file = event.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const importedData = JSON.parse(e.target.result);
        if (!importedData.name) throw new Error("Invalid format");
        
        const token = await getAccessTokenSilently();
        // Since the ID from backup might conflict or be irrelevant, 
        // we'll treat it as a new group creation
        const { id, expenses, members, createdBy, ...rest } = importedData;
        const response = await axios.post(`${API_BASE}/groups`, rest, {
          headers: { Authorization: `Bearer ${token}` }
        });
        
        setGroups([...groups, response.data]);
        alert("Backup imported successfully as a new group!");
      } catch (err) {
        console.error(err);
        alert("Failed to import JSON. Ensure it is a valid backup file.");
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="app-border-pane">
      {/* Top Header */}
      <div className="header-bar">
        <div className="brand-title">
          💸 Expense Splitter Pro
        </div>
        <div className="header-tools">
          <button className="fx-button" style={{ width: '90px' }} onClick={() => setIsDark(!isDark)}>
            {isDark ? '☀️ Light' : '🌙 Dark'}
          </button>
          <span className="text-muted">Default Currency: </span>
          <input type="text" className="currency-input" value={currency} onChange={e => setCurrency(e.target.value)} placeholder="Search currency (e.g. INR)" />
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
            <input type="text" className="fx-input" value={newGroupName} onChange={e => setNewGroupName(e.target.value)} placeholder="Group Name" />
            <input type="text" className="fx-input" value={newGroupBudget} onChange={e => setNewGroupBudget(e.target.value)} placeholder="Initial Budget (Optional)" />
            <button className="fx-button accent" onClick={handleCreateGroup}>Create</button>
          </div>

          <h4>Tools</h4>
          <input type="file" ref={fileInputRef} onChange={handleImportJSON} style={{ display: 'none' }} accept=".json" />
          <button className="fx-button" onClick={() => fileInputRef.current.click()}>📂 Import JSON Backup</button>
        </div>

        {/* Center Main Content Area */}
        <div className="content-area">
          {selectedGroup ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', height: '100%' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ flex: 1 }}>
                  <h1 style={{ marginTop: 0, marginBottom: '5px' }}>{selectedGroup.name}</h1>
                  <span className="text-muted" style={{ fontWeight: 'bold' }}>Budget: {selectedGroup.budgetCurrency || 'INR'} {selectedGroup.budget || 0}</span>
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button className="fx-button accent" style={{ width: 'auto' }} onClick={() => setActiveModal('ADD_MEMBER')}>Add Member</button>
                  <button className="fx-button accent" style={{ width: 'auto' }} onClick={() => setActiveModal('ADD_EXPENSE')}>Add Expense</button>
                  <button className="fx-button" style={{ width: 'auto', backgroundColor: '#e2f0d9', borderColor: '#7fbf7f', color: '#333' }} onClick={loadSettlementGraph}>Settle Up</button>
                  <div style={{ display: 'flex', gap: '5px' }}>
                    <button className="fx-button" style={{ width: 'auto', padding: '6px' }} onClick={handleExportCSV}>CSV</button>
                    <button className="fx-button" style={{ width: 'auto', padding: '6px' }} onClick={handleExportPDF}>PDF</button>
                    <button className="fx-button" style={{ width: 'auto', padding: '6px' }} onClick={handleExportJSON}>JSON</button>
                  </div>
                  <button className="fx-button" style={{ width: 'auto' }} onClick={() => setActiveModal('STATS')}>Stats</button>
                </div>
              </div>
              
              <div style={{ display: 'flex', gap: '20px', flex: 1, minHeight: 0 }}>
                <div style={{ flex: 2, display: 'flex', flexDirection: 'column' }}>
                  <h3 style={{ margin: '0 0 10px 0' }}>Expenses List</h3>
                  <div className="list-view" style={{ flex: 1, marginBottom: 0 }}>
                    {selectedGroup.expenses && selectedGroup.expenses.length > 0 ? (
                      selectedGroup.expenses.map((exp, idx) => (
                        <div key={idx} className="list-item">
                          <span>{exp.description} <small style={{color: 'gray'}}>({exp.category || 'Other'})</small></span>
                          <span style={{ marginLeft: 'auto', fontWeight: 'bold', marginRight: '10px' }}>{exp.amount}</span>
                          <button onClick={() => handleEditExpense(exp)} style={{ cursor: 'pointer', padding: '2px 5px', fontSize: '12px' }}>Edit</button>
                          <button onClick={() => handleDeleteExpense(exp)} style={{ cursor: 'pointer', padding: '2px 5px', fontSize: '12px', color: 'red' }}>Del</button>
                        </div>
                      ))
                    ) : (
                      <div style={{ padding: '10px', color: '#666' }}>No expenses recorded yet.</div>
                    )}
                  </div>
                </div>
                <div style={{ flex: 1, padding: '15px', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--surface-color)' }}>
                  <h3 style={{ margin: '0 0 10px 0' }}>Members</h3>
                  <div className="list-view" style={{ flex: 1, marginBottom: 0, border: 'none' }}>
                    {selectedGroup.members?.map(m => (
                      <div key={m.id} className="list-item" style={{ cursor: 'default', borderBottom: '1px solid var(--border-color)' }}>
                        👤 {m.name}
                        {selectedGroup.createdBy?.id !== m.id && (
                             <button onClick={() => handleRemoveMember(m)} style={{ marginLeft: 'auto', cursor: 'pointer', padding: '2px 5px', fontSize: '12px', color: 'red' }}>Remove</button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="center-placeholder">
              Select a group from the sidebar
            </div>
          )}
        </div>
      </div>

      {/* MODALS OVERLAYS */}
      {activeModal === 'ADD_MEMBER' && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <span>Add New Member</span>
              <button style={{border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px', color: 'var(--text-muted)'}} onClick={() => setActiveModal(null)}>✖</button>
            </div>
            <div className="modal-body">
              <label>User Email Address:</label>
              <input type="email" className="fx-input" value={memberEmail} onChange={e=>setMemberEmail(e.target.value)} placeholder="friend@example.com" style={{marginTop: '10px'}}/>
            </div>
            <div className="modal-footer">
              <button className="fx-button" onClick={() => setActiveModal(null)} style={{width: '100px'}}>Cancel</button>
              <button className="fx-button accent" onClick={submitAddMember} style={{width: '100px'}}>Add</button>
            </div>
          </div>
        </div>
      )}

      {activeModal === 'ADD_EXPENSE' && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <span>Record New Expense</span>
              <button style={{border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px', color: 'var(--text-muted)'}} onClick={() => setActiveModal(null)}>✖</button>
            </div>
            <div className="modal-body" style={{display: 'flex', flexDirection: 'column', gap: '15px'}}>
              <div>
                <label>Description (What was it for?)</label>
                <input type="text" className="fx-input" value={expenseDesc} onChange={e=>setExpenseDesc(e.target.value)} placeholder="Dinner at Joe's" style={{marginTop: '5px'}}/>
              </div>
              <div>
                <label>Amount</label>
                <input type="number" className="fx-input" value={expenseAmt} onChange={e=>setExpenseAmt(e.target.value)} placeholder="0.00" style={{marginTop: '5px'}}/>
              </div>
              <div>
                <label>Category</label>
                <select className="fx-input" value={expenseCat} onChange={e=>setExpenseCat(e.target.value)} style={{marginTop: '5px'}}>
                  <option>Food</option>
                  <option>Travel</option>
                  <option>Utilities</option>
                  <option>Entertainment</option>
                  <option>Other</option>
                </select>
              </div>
              <div>
                 <span className="text-muted" style={{fontSize: '12px'}}>Note: Advanced 'Split By Percentage' requires backend module upgrade. Defaulting to EQUAL split amongst all group members.</span>
              </div>
            </div>
            <div className="modal-footer">
              <button className="fx-button" onClick={() => setActiveModal(null)} style={{width: '100px'}}>Cancel</button>
              <button className="fx-button accent" onClick={submitAddExpense} style={{width: '100px'}}>Save</button>
            </div>
          </div>
        </div>
      )}

      {activeModal === 'SETTLE_UP' && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <span>Settle Up Debts</span>
              <button style={{border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px', color: 'var(--text-muted)'}} onClick={() => setActiveModal(null)}>✖</button>
            </div>
            <div className="modal-body">
              {settlementGraph.length === 0 ? (
                 <div style={{textAlign: 'center', padding: '20px', color: 'var(--text-muted)'}}>🎉 All settled up! No one owes anything.</div>
              ) : (
                 <div style={{display: 'flex', flexDirection: 'column', gap: '10px'}}>
                   {settlementGraph.map((edge, i) => (
                     <div key={i} style={{padding: '10px', border: '1px solid var(--border-color)', borderRadius: '4px', display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                       <span><strong>{edge.from}</strong> owes <strong>{edge.to}</strong></span>
                       <span style={{fontSize: '16px', fontWeight: 'bold'}}>{selectedGroup.budgetCurrency || 'INR'} {parseFloat(edge.amount).toFixed(2)}</span>
                     </div>
                   ))}
                   <p className="text-muted" style={{fontSize: '12px', marginTop: '10px'}}>To truly settle these debts out-of-band, log an expense from the debtor to the creditor.</p>
                 </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="fx-button accent" onClick={() => setActiveModal(null)} style={{width: '100px'}}>Close</button>
            </div>
          </div>
        </div>
      )}

      {activeModal === 'STATS' && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <span>Group Statistics</span>
              <button style={{border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px', color: 'var(--text-muted)'}} onClick={() => setActiveModal(null)}>✖</button>
            </div>
            <div className="modal-body">
               <h3 style={{marginTop: 0}}>Total Expenses: {selectedGroup.expenses?.length || 0}</h3>
               <h3>Budget Spent: {selectedGroup.expenses?.reduce((acc, curr) => acc + curr.amount, 0) || 0} / {selectedGroup.budget || 0}</h3>
               <p className="text-muted">Interactive Charts (Pie/Bar) module is currently initializing in cloud container. Statistics preview above is text-only.</p>
            </div>
            <div className="modal-footer">
              <button className="fx-button accent" onClick={() => setActiveModal(null)} style={{width: '100px'}}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;

import { useAuth0 } from "@auth0/auth0-react";
import { useState, useEffect, useRef } from "react";
import axios from "axios";
import jsPDF from "jspdf";
import "jspdf-autotable";
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import "./App.css";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d'];

interface Member {
  id: string;
  name: string;
  email: string;
  familyName?: string;
}

interface Expense {
  id: string;
  description: string;
  amount: number;
  category: string;
  expenseDate: string;
  paidBy: Member;
  splitMembers: Member[];
}

interface Group {
  id: string;
  name: string;
  description?: string;
  budget: number;
  budgetCurrency: string;
  familyGroupingEnabled: boolean;
  members: Member[];
  expenses: Expense[];
}

interface SettlementEdge {
  from: string;
  to: string;
  amount: number;
}

interface ContextMenu {
  x: number;
  y: number;
  type: 'GROUP' | 'EXPENSE' | 'MEMBER';
  data: any;
}

function App() {
  const { loginWithRedirect, logout, isAuthenticated, getAccessTokenSilently } = useAuth0();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [groups, setGroups] = useState<Group[]>([]);
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);

  // Modal/Form States
  const [activeModal, setActiveModal] = useState<string | null>(null); // 'ADD_MEMBER', 'ADD_EXPENSE', 'STATS', 'SETTLE_UP'
  const [isDark, setIsDark] = useState(false);
  const [menu, setMenu] = useState<ContextMenu | null>(null);

  // Expense Modal state
  const [expDesc, setExpDesc] = useState("");
  const [expAmt, setExpAmt] = useState("");
  const [expCat, setExpCat] = useState("Food");
  const [expDate, setExpDate] = useState(new Date().toISOString().split('T')[0]);
  const [expPayerId, setExpPayerId] = useState("");
  const [involvedMembers, setInvolvedMembers] = useState<string[]>([]);

  // Settlement graph
  const [settlementGraph, setSettlementGraph] = useState<SettlementEdge[]>([]);

  // New Group/Member state
  const [newGroupName, setNewGroupName] = useState("");
  const [newGroupBudget, setNewGroupBudget] = useState("");
  const [memberEmail, setMemberEmail] = useState("");

  useEffect(() => {
    if (isDark) document.documentElement.classList.add("dark-theme");
    else document.documentElement.classList.remove("dark-theme");
  }, [isDark]);

  useEffect(() => {
    const handleClick = () => setMenu(null);
    window.addEventListener("click", handleClick);
    return () => window.removeEventListener("click", handleClick);
  }, []);

  useEffect(() => {
    const fetchGroups = async () => {
      if (!isAuthenticated) return;
      try {
        const token = await getAccessTokenSilently();
        const response = await axios.get(`${API_BASE}/groups`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        setGroups(response.data);
      } catch (e) { console.error(e); }
    };
    fetchGroups();
  }, [isAuthenticated, getAccessTokenSilently]);

  const handleCreateGroup = async () => {
    if (!isAuthenticated) return alert("Please login first");
    if (!newGroupName) return alert("Enter group name");
    try {
      const token = await getAccessTokenSilently();
      const response = await axios.post(`${API_BASE}/groups`, {
        name: newGroupName, budget: parseFloat(newGroupBudget || "0"), budgetCurrency: "INR"
      }, { headers: { Authorization: `Bearer ${token}` } });
      setGroups([...groups, response.data]);
      setNewGroupName(""); setNewGroupBudget("");
      alert("Group created successfully!");
    } catch (e: any) {
      console.error(e);
      alert(`Failed to create group: ${e.response?.data?.error || e.message}`);
    }
  };

  const handleContextMenu = (e: React.MouseEvent, type: 'GROUP' | 'EXPENSE' | 'MEMBER', data: any) => {
    e.preventDefault();
    setMenu({ x: e.clientX, y: e.clientY, type, data });
  };

  const refreshSelectedGroup = async () => {
    if (!selectedGroup) return;
    try {
      const token = await getAccessTokenSilently();
      const res = await axios.get(`${API_BASE}/groups/${selectedGroup.id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setSelectedGroup(res.data);
      setGroups(groups.map(g => g.id === res.data.id ? res.data : g));
    } catch (e: any) {
      console.error(e);
      alert(`Failed to refresh group: ${e.response?.data?.error || e.message}`);
    }
  };

  // --- Actions ---
  const loadSettlements = async () => {
    if (!selectedGroup) return;
    try {
      const token = await getAccessTokenSilently();
      const res = await axios.get(`${API_BASE}/groups/${selectedGroup.id}/debt-graph`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setSettlementGraph(res.data.edges || []);
      setActiveModal('SETTLE_UP');
    } catch (e: any) {
      console.error(e);
      alert(`Failed to calculate debts: ${e.response?.data?.error || e.message}`);
    }
  };

  const toggleFamilyGrouping = async () => {
    if (!selectedGroup) return;
    try {
      const token = await getAccessTokenSilently();
      const res = await axios.patch(`${API_BASE}/groups/${selectedGroup.id}/family-grouping`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setSelectedGroup(res.data);
      alert(`Family Grouping ${res.data.familyGroupingEnabled ? "ENABLED" : "DISABLED"}`);
    } catch (e: any) {
      console.error(e);
      alert(`Failed to toggle family grouping: ${e.response?.data?.error || e.message}`);
    }
  };

  const handleSetFamilyName = async (member: Member) => {
    const fam = window.prompt(`Enter family name for ${member.name}:`, member.familyName || "");
    if (fam === null) return;
    try {
      const token = await getAccessTokenSilently();
      await axios.patch(`${API_BASE}/groups/members/${member.id}/family?familyName=${encodeURIComponent(fam)}`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      refreshSelectedGroup();
    } catch (e: any) {
      console.error(e);
      alert(`Failed to set family name: ${e.response?.data?.error || e.message}`);
    }
  };

  const handleImportJSON = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (e) => {
      try {
        const importedData = JSON.parse(e.target?.result as string);
        const token = await getAccessTokenSilently();
        const response = await axios.post(`${API_BASE}/groups/import`, importedData, {
          headers: { Authorization: `Bearer ${token}` }
        });
        setGroups([...groups, response.data]);
        alert("Backup imported successfully!");
      } catch (err: any) {
        console.error(err);
        alert(`Import failed: ${err.response?.data?.error || err.message}`);
      }
    };
    reader.readAsText(file!);
  };

  const handleExportJSON = () => {
    if (!selectedGroup) return;
    const blob = new Blob([JSON.stringify(selectedGroup, null, 2)], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Backup_${selectedGroup.name}.json`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  const handleExportPDF = () => {
    if (!selectedGroup) return;
    try {
      const doc = new jsPDF();
      doc.text(`${selectedGroup.name} Report`, 14, 20);
      const data = (selectedGroup.expenses || []).map(e => [e.expenseDate, e.description, e.category, e.amount]);
      (doc as any).autoTable({ startY: 25, head: [['Date', 'Description', 'Category', 'Amount']], body: data });
      doc.save(`${selectedGroup.name}_Report.pdf`);
    } catch (e: any) {
      console.error(e);
      alert("Error generating PDF");
    }
  };

  const submitAddExpense = async () => {
    const amt = parseFloat(expAmt);
    if (!expDesc || isNaN(amt) || involvedMembers.length === 0) return alert("Fill all fields and select members");
    try {
      const token = await getAccessTokenSilently();
      await axios.post(`${API_BASE}/expenses`, {
        groupId: selectedGroup?.id,
        paidById: expPayerId,
        amount: amt,
        description: expDesc,
        category: expCat,
        expenseDate: expDate,
        splitType: 'EQUAL',
        splitMemberIds: involvedMembers
      }, {
        headers: { Authorization: `Bearer ${token}` }
      });
      refreshSelectedGroup();
      setActiveModal(null);
    } catch (e: any) {
      console.error(e);
      alert(`Failed to add expense: ${e.response?.data?.error || e.message}`);
    }
  };

  const totalSpent = selectedGroup?.expenses?.reduce((sum, e) => sum + e.amount, 0) || 0;
  const budget = selectedGroup?.budget || 0;
  const progress = budget > 0 ? (totalSpent / budget) * 100 : 0;
  const progressClass = progress >= 100 ? "danger" : progress >= 80 ? "warning" : "success";

  return (
    <div className="app-border-pane" onContextMenu={(e) => e.preventDefault()}>
      <div className="header-bar">
        <div className="brand-title">💸 Expense Splitter Pro</div>
        <div className="header-tools">
          <button className="fx-button" onClick={() => setIsDark(!isDark)}>{isDark ? "☀️" : "🌙"}</button>
          {isAuthenticated ? <button className="fx-button" onClick={() => logout()}>Logout</button> : <button className="fx-button accent" onClick={() => loginWithRedirect()}>Login</button>}
        </div>
      </div>

      <div className="main-body">
        <div className="sidebar" onContextMenu={(e) => e.preventDefault()}>
          <h2>Groups</h2>
          <div className="list-view">
            {groups.map(g => (
              <div key={g.id} className="list-item" onClick={() => setSelectedGroup(g)} onContextMenu={(ex) => handleContextMenu(ex, "GROUP", g)} style={{ background: selectedGroup?.id === g.id ? "var(--list-selected)" : "" }}>
                📁 {g.name}
              </div>
            ))}
          </div>
          <h4>New Group</h4>
          <input className="fx-input" placeholder="Name" value={newGroupName} onChange={e => setNewGroupName((e.target as HTMLInputElement).value)} />
          <input className="fx-input" placeholder="Budget" value={newGroupBudget} onChange={e => setNewGroupBudget((e.target as HTMLInputElement).value)} />
          <button className="fx-button accent" onClick={handleCreateGroup}>Create</button>
          <h4>Tools</h4>
          <input type="file" ref={fileInputRef} onChange={handleImportJSON} style={{ display: 'none' }} accept=".json" />
          <button className="fx-button" onClick={() => fileInputRef.current?.click()}>📂 Import Backup</button>
        </div>

        <div className="content-area">
          {selectedGroup ? (
            <div className="flex-col h-full gap-20">
              <div className="flex-row items-start justify-between">
                <div className="flex-1">
                  <h1 className="m-0">{selectedGroup.name}</h1>
                  <p className="text-muted m-0">{selectedGroup.description || `${selectedGroup.members.length} members`}</p>
                </div>
                <div className="flex-row gap-10">
                  <button className="fx-button" onClick={toggleFamilyGrouping}>{selectedGroup.familyGroupingEnabled ? "👨‍👩‍👧 Family ON" : "🏠 Family OFF"}</button>
                  <button className="fx-button accent" onClick={() => setActiveModal("ADD_MEMBER")}>Add Member</button>
                  <button className="fx-button accent" onClick={() => {
                    setExpDesc(""); setExpAmt(""); setExpDate(new Date().toISOString().split("T")[0]);
                    setExpPayerId(selectedGroup.members[0].id); setInvolvedMembers(selectedGroup.members.map((m: Member) => m.id));
                    setActiveModal("ADD_EXPENSE");
                  }}>Add Expense</button>
                  <button className="fx-button" onClick={loadSettlements}>Settle Up</button>
                  <button className="fx-button" onClick={handleExportPDF}>PDF</button>
                  <button className="fx-button" onClick={() => setActiveModal("STATS")}>Stats</button>
                </div>
              </div>

              {budget > 0 && (
                <div className="budget-box">
                  <div className="flex-row justify-between">
                    <span className="bold">Budget: INR {budget} | Remaining: INR {budget - totalSpent}</span>
                    <span className="text-muted">{progress.toFixed(1)}%</span>
                  </div>
                  <div className="progress-container">
                    <div className={`progress-bar ${progressClass}`} style={{ width: `${Math.min(progress, 100)}%` }}></div>
                  </div>
                </div>
              )}

              <div className="flex-row gap-20 flex-1 min-h-0">
                <div className="flex-2 flex-col">
                  <h3>Expenses (Right-click to Edit)</h3>
                  <div className="list-view flex-1">
                    {selectedGroup.expenses?.map((e, i) => (
                      <div key={i} className="list-item" onContextMenu={(ex) => handleContextMenu(ex, "EXPENSE", e)}>
                        <span>{e.description} <small className="text-muted">({e.category})</small></span>
                        <span className="ml-auto bold">INR {e.amount}</span>
                      </div>
                    ))}
                  </div>
                </div>
                <div className="flex-1 flex-col">
                  <h3>Members (Right-click)</h3>
                  <div className="list-view flex-1">
                    {selectedGroup.members?.map(m => (
                      <div key={m.id} className="list-item" onContextMenu={(ex) => handleContextMenu(ex, "MEMBER", m)}>
                        👤 {m.name} {m.familyName && <span className="family-badge">{m.familyName}</span>}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          ) : <div className="center-placeholder">Select a group to start</div>}
        </div>
      </div>

      {menu && (
        <div className="context-menu" style={{ top: menu.y, left: menu.x }}>
          {menu.type === "EXPENSE" && (
            <>
              <div className="context-menu-item" onClick={() => { setExpDesc(menu.data.description); setExpAmt(menu.data.amount); setExpCat(menu.data.category); setExpDate(menu.data.expenseDate); setActiveModal("ADD_EXPENSE"); }}>Edit</div>
              <div className="context-menu-item danger" onClick={async () => {
                const token = await getAccessTokenSilently();
                await axios.delete(`${API_BASE}/expenses/${menu.data.id}`, { headers: { Authorization: `Bearer ${token}` } });
                refreshSelectedGroup();
              }}>Delete</div>
            </>
          )}
          {menu.type === "MEMBER" && <div className="context-menu-item" onClick={() => handleSetFamilyName(menu.data)}>Set Family</div>}
          {menu.type === "GROUP" && (
            <>
              <div className="context-menu-item" onClick={() => handleExportJSON()}>Export Backup</div>
              <div className="context-menu-item danger" onClick={async () => {
                const token = await getAccessTokenSilently();
                await axios.delete(`${API_BASE}/groups/${menu.data.id}`, { headers: { Authorization: `Bearer ${token}` } });
                setGroups(groups.filter(g => g.id !== menu.data.id));
                if (selectedGroup?.id === menu.data.id) setSelectedGroup(null);
              }}>Delete Group</div>
            </>
          )}
        </div>
      )}

      {/* Modals */}
      {activeModal === "ADD_MEMBER" && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header"><span>Invite</span><button onClick={() => setActiveModal(null)}>✖</button></div>
            <div className="modal-body">
              <input className="fx-input" placeholder="Email" value={memberEmail} onChange={e => setMemberEmail((e.target as HTMLInputElement).value)} />
            </div>
            <div className="modal-footer"><button className="fx-button accent" onClick={async () => {
              const token = await getAccessTokenSilently();
              await axios.post(`${API_BASE}/groups/${selectedGroup?.id}/members?email=${encodeURIComponent(memberEmail)}`, {}, { headers: { Authorization: `Bearer ${token}` } });
              refreshSelectedGroup(); setActiveModal(null); setMemberEmail("");
            }}>Add</button></div>
          </div>
        </div>
      )}

      {activeModal === "ADD_EXPENSE" && (
        <div className="modal-overlay">
          <div className="modal-content w-450">
            <div className="modal-header"><span>Expense</span><button onClick={() => setActiveModal(null)}>✖</button></div>
            <div className="modal-body flex-col gap-10">
              <input className="fx-input" placeholder="Desc" value={expDesc} onChange={e => setExpDesc((e.target as HTMLInputElement).value)} />
              <input className="fx-input" type="number" placeholder="Amt" value={expAmt} onChange={e => setExpAmt((e.target as HTMLInputElement).value)} />
              <select className="fx-input" value={expCat} onChange={e => setExpCat((e.target as HTMLSelectElement).value)}>
                <option>Food</option><option>Travel</option><option>Utilities</option><option>Entertainment</option><option>Other</option>
              </select>
              <input className="fx-input" type="date" value={expDate} onChange={e => setExpDate((e.target as HTMLInputElement).value)} />
              <label>Paid By:</label>
              <select className="fx-input" value={expPayerId} onChange={e => setExpPayerId((e.target as HTMLSelectElement).value)}>
                {selectedGroup?.members.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
              </select>
              <label>Split:</label>
              <div className="split-checklist">
                {selectedGroup?.members.map(m => (
                  <div key={m.id} className="flex-row gap-10 items-center">
                    <input type="checkbox" checked={involvedMembers.includes(m.id)} onChange={() => setInvolvedMembers(p => p.includes(m.id) ? p.filter(x => x !== m.id) : [...p, m.id])} />
                    <span>{m.name}</span>
                  </div>
                ))}
              </div>
            </div>
            <div className="modal-footer"><button className="fx-button accent" onClick={submitAddExpense}>Save</button></div>
          </div>
        </div>
      )}

      {activeModal === "SETTLE_UP" && (
        <div className="modal-overlay">
          <div className="modal-content w-450">
            <div className="modal-header"><span>Settle Up Suggestions</span><button onClick={() => setActiveModal(null)}>✖</button></div>
            <div className="modal-body">
              {settlementGraph.length === 0 ? <p className="text-center">No debts!</p> : settlementGraph.map((ed, i) => (
                <div key={i} className="list-item">
                  <span>{ed.from} owes {ed.to}</span>
                  <span className="ml-auto bold">INR {ed.amount.toFixed(2)}</span>
                </div>
              ))}
            </div>
            <div className="modal-footer"><button className="fx-button accent" onClick={() => setActiveModal(null)}>Close</button></div>
          </div>
        </div>
      )}

      {activeModal === "STATS" && selectedGroup && (
        <div className="modal-overlay">
          <div className="modal-content w-600">
            <div className="modal-header"><span>Stats</span><button onClick={() => setActiveModal(null)}>✖</button></div>
            <div className="modal-body h-400">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={(() => {
                    const m: Record<string, number> = {}; selectedGroup.expenses?.forEach(e => m[e.category] = (m[e.category] || 0) + e.amount);
                    return Object.keys(m).map(n => ({ name: n, value: m[n] }));
                  })()} outerRadius={120} label={({ name, percent }) => `${name} ${((percent || 0) * 100).toFixed(0)}%`} dataKey="value">
                    {COLORS.map((c, i) => <Cell key={i} fill={c} />)}
                  </Pie>
                  <Tooltip /><Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="modal-footer"><button className="fx-button accent" onClick={() => setActiveModal(null)}>Close</button></div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;

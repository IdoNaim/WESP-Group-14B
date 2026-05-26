import { useState, useEffect, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import {
  ManagerPermission,
  ALL_PERMISSIONS,
  PERMISSION_LABELS,
  RolesTreeDTO,
  HistoryOrderItem,
  createProductionCompany,
  assignOwner,
  appointManager,
  removeManager,
  getRolesTree,
  getCompanyPurchaseHistory,
} from '../api/productionCompanyApi';

type Tab = 'team' | 'history' | 'actions';

export default function ProductionCompanyPage() {
  const token = localStorage.getItem('authToken') ?? '';

  // companyId is lazily initialised from localStorage so it persists across reloads
  const [companyId, setCompanyId] = useState<number | null>(() => {
    const stored = localStorage.getItem('companyId');
    return stored ? Number(stored) : null;
  });
  const [rolesTree, setRolesTree] = useState<RolesTreeDTO | null>(null);
  const [history, setHistory] = useState<HistoryOrderItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<Tab>('team');

  // Create-company form
  const [createName, setCreateName] = useState('');
  const [createDesc, setCreateDesc] = useState('');
  const [createEmail, setCreateEmail] = useState('');
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Actions form
  const [ownerUserId, setOwnerUserId] = useState('');
  const [managerUserId, setManagerUserId] = useState('');
  const [managerPerms, setManagerPerms] = useState<ManagerPermission[]>([]);
  const [removeManagerId, setRemoveManagerId] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMsg, setActionMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Fetch roles tree + purchase history whenever the companyId changes
  useEffect(() => {
    if (!companyId || !token) return;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const [tree, hist] = await Promise.all([
          getRolesTree(token, companyId),
          getCompanyPurchaseHistory(token, companyId),
        ]);
        setRolesTree(tree);
        setHistory(hist);
      } catch (e: unknown) {
        setError(e instanceof Error ? e.message : 'Failed to load company data');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [companyId, token]);

  const refreshData = async () => {
    if (!companyId || !token) return;
    try {
      const [tree, hist] = await Promise.all([
        getRolesTree(token, companyId),
        getCompanyPurchaseHistory(token, companyId),
      ]);
      setRolesTree(tree);
      setHistory(hist);
    } catch {}
  };

  const handleCreateCompany = async (e: FormEvent) => {
    e.preventDefault();
    if (!token) { setCreateError('You must be logged in first.'); return; }
    setCreateLoading(true);
    setCreateError(null);
    try {
      const res = await createProductionCompany(token, createName, createDesc, createEmail);
      const id = Number(res.companyId);
      localStorage.setItem('companyId', String(id));
      setCompanyId(id);
    } catch (e: unknown) {
      setCreateError(e instanceof Error ? e.message : 'Unknown error');
    } finally {
      setCreateLoading(false);
    }
  };

  const handleAssignOwner = async (e: FormEvent) => {
    e.preventDefault();
    if (!companyId || !token) return;
    setActionLoading(true);
    setActionMsg(null);
    try {
      await assignOwner(token, companyId, ownerUserId);
      setActionMsg({ type: 'success', text: `Owner "${ownerUserId}" assigned.` });
      setOwnerUserId('');
      await refreshData();
    } catch (e: unknown) {
      setActionMsg({ type: 'error', text: e instanceof Error ? e.message : 'Failed' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleAppointManager = async (e: FormEvent) => {
    e.preventDefault();
    if (!companyId || !token) return;
    setActionLoading(true);
    setActionMsg(null);
    try {
      await appointManager(token, companyId, managerUserId, managerPerms);
      setActionMsg({ type: 'success', text: `Manager "${managerUserId}" appointed.` });
      setManagerUserId('');
      setManagerPerms([]);
      await refreshData();
    } catch (e: unknown) {
      setActionMsg({ type: 'error', text: e instanceof Error ? e.message : 'Failed' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemoveManager = async (e: FormEvent) => {
    e.preventDefault();
    if (!companyId || !token) return;
    setActionLoading(true);
    setActionMsg(null);
    try {
      await removeManager(token, companyId, removeManagerId);
      setActionMsg({ type: 'success', text: `Manager "${removeManagerId}" removed.` });
      setRemoveManagerId('');
      await refreshData();
    } catch (e: unknown) {
      setActionMsg({ type: 'error', text: e instanceof Error ? e.message : 'Failed' });
    } finally {
      setActionLoading(false);
    }
  };

  const togglePerm = (p: ManagerPermission) =>
    setManagerPerms(prev => prev.includes(p) ? prev.filter(x => x !== p) : [...prev, p]);

  // ── NOT LOGGED IN ──────────────────────────────────────────────────────────
  if (!token) {
    return (
      <div className="bg-[#0b1326] min-h-screen flex items-center justify-center p-6">
        <div className="bg-[#171f33] border border-yellow-500/20 p-10 rounded-2xl text-center max-w-md shadow-[0_0_40px_rgba(234,179,8,0.05)]">
          <span className="material-symbols-outlined text-6xl text-yellow-400 block mb-4">key_off</span>
          <h1 className="text-2xl font-black text-white mb-2">SESSION REQUIRED</h1>
          <p className="text-sm text-gray-400 mb-6">Sign in to access your production company portal.</p>
          <Link to="/login" className="inline-flex items-center gap-2 bg-gradient-to-b from-[#2563eb] to-[#0053db] text-white px-6 py-3 rounded-xl font-bold hover:opacity-90 transition-opacity">
            <span className="material-symbols-outlined text-[18px]">login</span>
            SIGN IN
          </Link>
        </div>
      </div>
    );
  }

  // ── LOADING ────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="bg-[#0b1326] min-h-screen flex flex-col items-center justify-center gap-4">
        <span className="material-symbols-outlined text-5xl text-[#2563eb] animate-spin">refresh</span>
        <p className="text-[#b4c5ff] font-mono text-sm tracking-widest">LOADING COMPANY DATA...</p>
      </div>
    );
  }

  // ── ERROR ──────────────────────────────────────────────────────────────────
  if (error) {
    return (
      <div className="bg-[#0b1326] min-h-screen flex items-center justify-center p-6">
        <div className="bg-[#171f33] border border-red-500/20 p-10 rounded-2xl text-center max-w-md">
          <span className="material-symbols-outlined text-6xl text-red-400 block mb-4">error</span>
          <h1 className="text-xl font-black text-white mb-2">COULD NOT LOAD</h1>
          <p className="text-sm text-gray-400 mb-6">{error}</p>
          <button
            onClick={() => { setError(null); setCompanyId(null); localStorage.removeItem('companyId'); }}
            className="bg-[#2563eb] text-white px-6 py-3 rounded-xl font-bold hover:bg-[#0053db] transition-colors"
          >
            TRY AGAIN
          </button>
        </div>
      </div>
    );
  }

  // ── CREATE COMPANY ─────────────────────────────────────────────────────────
  if (!companyId) {
    return (
      <div className="bg-[#0b1326] min-h-screen flex items-center justify-center p-6 relative overflow-hidden">
        <div className="fixed inset-0 z-0">
          <img
            src="https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?q=80&w=1200"
            alt=""
            className="w-full h-full object-cover opacity-20 blur-sm"
          />
          <div className="absolute inset-0 bg-gradient-to-b from-[#0b1326]/70 to-[#0b1326]" />
        </div>

        <div className="w-full max-w-md z-10">
          <div className="text-center mb-8">
            <span className="material-symbols-outlined text-5xl text-[#2563eb] block mb-3">rocket_launch</span>
            <h1 className="text-3xl font-black text-white tracking-tight">LAUNCH YOUR COMPANY</h1>
            <p className="text-xs text-[#2563eb] font-mono mt-1 tracking-widest">PRODUCTION COMPANY PORTAL</p>
          </div>

          <div className="bg-[#eeefff] rounded-2xl shadow-2xl pt-8 pb-10 px-8 text-[#171f33]">
            <div className="relative mb-6">
              <div className="border-t-2 border-dashed border-[#cbd5e1]" />
              <div className="absolute -left-[46px] -top-[11px] w-6 h-6 rounded-full bg-[#0b1326]" />
              <div className="absolute -right-[46px] -top-[11px] w-6 h-6 rounded-full bg-[#0b1326]" />
            </div>

            <form onSubmit={handleCreateCompany} className="space-y-4">
              <div className="space-y-1">
                <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#2d3449] ml-1">Company Name</label>
                <div className="relative group">
                  <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8d90a0] group-focus-within:text-[#2563eb] transition-colors">domain</span>
                  <input
                    className="w-full bg-[#060e20]/5 border border-[#434655]/20 rounded-xl pl-10 pr-4 py-3 text-[#0b1326] placeholder-[#8d90a0] focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/10 outline-none transition-all"
                    placeholder="Sunset Productions"
                    value={createName}
                    onChange={e => setCreateName(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#2d3449] ml-1">Company Email</label>
                <div className="relative group">
                  <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8d90a0] group-focus-within:text-[#2563eb] transition-colors">alternate_email</span>
                  <input
                    type="email"
                    className="w-full bg-[#060e20]/5 border border-[#434655]/20 rounded-xl pl-10 pr-4 py-3 text-[#0b1326] placeholder-[#8d90a0] focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/10 outline-none transition-all"
                    placeholder="contact@company.com"
                    value={createEmail}
                    onChange={e => setCreateEmail(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#2d3449] ml-1">Description</label>
                <textarea
                  className="w-full bg-[#060e20]/5 border border-[#434655]/20 rounded-xl px-4 py-3 text-[#0b1326] placeholder-[#8d90a0] focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/10 outline-none transition-all resize-none"
                  placeholder="What kind of events does your company produce?"
                  rows={3}
                  value={createDesc}
                  onChange={e => setCreateDesc(e.target.value)}
                />
              </div>

              {createError && (
                <div className="bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg px-4 py-2 flex items-center gap-2">
                  <span className="material-symbols-outlined text-[16px]">error</span>
                  {createError}
                </div>
              )}

              <button
                type="submit"
                disabled={createLoading}
                className="w-full py-4 rounded-xl bg-gradient-to-b from-[#2563eb] to-[#0053db] text-white font-bold tracking-wide shadow-lg hover:shadow-[#2563eb]/30 active:scale-[0.98] transition-all flex items-center justify-center gap-2 disabled:opacity-60"
              >
                {createLoading
                  ? <span className="material-symbols-outlined animate-spin">refresh</span>
                  : <><span>CREATE COMPANY</span><span className="material-symbols-outlined">rocket_launch</span></>
                }
              </button>
            </form>
          </div>
        </div>
      </div>
    );
  }

  // ── DASHBOARD ──────────────────────────────────────────────────────────────
  const ownerCount = Object.keys(rolesTree?.ownershipTree ?? {}).length;
  const managerCount = Object.keys(rolesTree?.managerTree ?? {}).length;

  return (
    <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen p-6 md:p-12 font-sans">
      <div className="fixed inset-0 z-0 bg-gradient-to-br from-[#060e20] to-[#0b1326] pointer-events-none" />

      <div className="max-w-5xl mx-auto relative z-10 space-y-6">

        {/* ── Hero Header ── */}
        <div className="bg-[#eeefff] rounded-2xl shadow-xl p-8 text-[#171f33] flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <span className="material-symbols-outlined text-[#2563eb] text-3xl">domain</span>
              <h1 className="text-3xl font-black text-[#0b1326] tracking-tight">
                Company #{rolesTree?.companyId}
              </h1>
            </div>
            <div className="flex items-center gap-2 text-sm font-mono text-gray-500">
              <span className="material-symbols-outlined text-[16px]">person_crown</span>
              Founder: <span className="font-bold text-[#0b1326]">{rolesTree?.founderId}</span>
            </div>
          </div>

          <div className="flex gap-3 flex-wrap">
            <div className="bg-[#0b1326] px-5 py-3 rounded-xl text-center min-w-[80px]">
              <div className="text-2xl font-black text-[#2563eb]">{ownerCount}</div>
              <div className="text-[10px] font-mono uppercase text-[#b4c5ff] tracking-wider">Owners</div>
            </div>
            <div className="bg-[#0b1326] px-5 py-3 rounded-xl text-center min-w-[80px]">
              <div className="text-2xl font-black text-[#00dbe7]">{managerCount}</div>
              <div className="text-[10px] font-mono uppercase text-[#b4c5ff] tracking-wider">Managers</div>
            </div>
            <div className="bg-[#0b1326] px-5 py-3 rounded-xl text-center min-w-[80px]">
              <div className="text-2xl font-black text-green-400">{history.length}</div>
              <div className="text-[10px] font-mono uppercase text-[#b4c5ff] tracking-wider">Orders</div>
            </div>
          </div>
        </div>

        {/* ── Tab Navigation ── */}
        <div className="flex gap-1.5 bg-[#171f33] p-1.5 rounded-xl border border-gray-800">
          {(['team', 'history', 'actions'] as Tab[]).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex-1 py-2.5 rounded-lg text-xs font-bold tracking-widest uppercase transition-all flex items-center justify-center gap-1.5 ${
                activeTab === tab
                  ? 'bg-[#2563eb] text-white shadow-lg'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              <span className="material-symbols-outlined text-[16px]">
                {tab === 'team' ? 'groups' : tab === 'history' ? 'receipt_long' : 'bolt'}
              </span>
              {tab.toUpperCase()}
            </button>
          ))}
        </div>

        {/* ── TEAM TAB ── */}
        {activeTab === 'team' && (
          <div className="space-y-4">
            {/* Founder */}
            <div className="bg-[#171f33] rounded-2xl p-6 border border-gray-800">
              <h2 className="text-xs font-bold uppercase tracking-widest text-[#b4c5ff] mb-4 flex items-center gap-2">
                <span className="material-symbols-outlined text-yellow-400 text-[18px]">person_crown</span>
                Founder
              </h2>
              <div className="bg-[#0b1326] rounded-xl p-4 flex items-center gap-4 border border-yellow-500/20">
                <div className="w-11 h-11 bg-yellow-500/10 rounded-full flex items-center justify-center flex-shrink-0">
                  <span className="material-symbols-outlined text-yellow-400">star</span>
                </div>
                <div>
                  <div className="font-bold text-white">{rolesTree?.founderId}</div>
                  <div className="text-xs text-yellow-400/70 font-mono tracking-wider mt-0.5">FOUNDER · FULL ACCESS</div>
                </div>
              </div>
            </div>

            {/* Owners */}
            <div className="bg-[#171f33] rounded-2xl p-6 border border-gray-800">
              <h2 className="text-xs font-bold uppercase tracking-widest text-[#b4c5ff] mb-4 flex items-center gap-2">
                <span className="material-symbols-outlined text-[#2563eb] text-[18px]">group</span>
                Owners ({ownerCount})
              </h2>
              {ownerCount === 0 ? (
                <p className="text-gray-500 text-sm text-center py-6">No owners assigned yet</p>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2">
                  {Object.entries(rolesTree?.ownershipTree ?? {}).map(([userId, dto]) => (
                    <div key={userId} className="bg-[#0b1326] rounded-xl p-4 flex items-center gap-3 border border-gray-700/50 hover:border-[#2563eb]/40 transition-colors">
                      <div className="w-10 h-10 bg-[#2563eb]/10 rounded-full flex items-center justify-center flex-shrink-0">
                        <span className="material-symbols-outlined text-[#2563eb]">person</span>
                      </div>
                      <div className="min-w-0">
                        <div className="font-bold text-white text-sm truncate">{userId}</div>
                        <div className="text-xs text-gray-400 font-mono">
                          by: {dto.appointerId ?? 'Founder'}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Managers */}
            <div className="bg-[#171f33] rounded-2xl p-6 border border-gray-800">
              <h2 className="text-xs font-bold uppercase tracking-widest text-[#b4c5ff] mb-4 flex items-center gap-2">
                <span className="material-symbols-outlined text-[#00dbe7] text-[18px]">manage_accounts</span>
                Managers ({managerCount})
              </h2>
              {managerCount === 0 ? (
                <p className="text-gray-500 text-sm text-center py-6">No managers appointed yet</p>
              ) : (
                <div className="space-y-3">
                  {Object.entries(rolesTree?.managerTree ?? {}).map(([userId]) => {
                    const perms = (rolesTree?.managerPermissions[userId] ?? []) as ManagerPermission[];
                    return (
                      <div key={userId} className="bg-[#0b1326] rounded-xl p-4 border border-gray-700/50 hover:border-[#00dbe7]/30 transition-colors">
                        <div className="flex items-center gap-3 mb-3">
                          <div className="w-9 h-9 bg-[#00dbe7]/10 rounded-full flex items-center justify-center flex-shrink-0">
                            <span className="material-symbols-outlined text-[#00dbe7] text-[18px]">badge</span>
                          </div>
                          <div>
                            <div className="font-bold text-white text-sm">{userId}</div>
                            <div className="text-xs text-gray-400 font-mono">Manager · {perms.length} permissions</div>
                          </div>
                        </div>
                        {perms.length > 0 && (
                          <div className="flex flex-wrap gap-1.5 ml-12">
                            {perms.map(p => (
                              <span key={p} className="bg-[#00dbe7]/10 text-[#00dbe7] text-[10px] font-mono px-2 py-0.5 rounded-full border border-[#00dbe7]/20">
                                {PERMISSION_LABELS[p]}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}

        {/* ── HISTORY TAB ── */}
        {activeTab === 'history' && (
          <div className="bg-[#171f33] rounded-2xl p-6 border border-gray-800">
            <h2 className="text-xs font-bold uppercase tracking-widest text-[#b4c5ff] mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-green-400 text-[18px]">receipt_long</span>
              Purchase History ({history.length} orders)
            </h2>
            {history.length === 0 ? (
              <div className="text-center py-16">
                <span className="material-symbols-outlined text-5xl text-gray-700 block mb-3">receipt_long</span>
                <p className="text-gray-500 text-sm">No purchase history yet</p>
              </div>
            ) : (
              <div className="space-y-3">
                {history.map(order => (
                  <div key={order.orderId} className="bg-[#0b1326] rounded-xl p-4 border border-gray-700/50 hover:border-green-500/20 transition-colors">
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
                      <div className="space-y-1">
                        <div className="font-mono text-xs text-[#2563eb]">{order.orderId}</div>
                        <div className="text-sm text-white">
                          User: <span className="font-bold">{order.userId}</span>
                        </div>
                        <div className="text-xs text-gray-400">Event: {order.eventId}</div>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <div className="text-xl font-black text-green-400">${order.price.toFixed(2)}</div>
                        <div className="text-xs text-gray-500 font-mono">{order.seatIds.length} seats</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* ── ACTIONS TAB ── */}
        {activeTab === 'actions' && (
          <div className="space-y-4">
            {actionMsg && (
              <div className={`rounded-xl px-5 py-3 text-sm font-bold flex items-center gap-2 ${
                actionMsg.type === 'success'
                  ? 'bg-green-900/30 text-green-400 border border-green-500/20'
                  : 'bg-red-900/30 text-red-400 border border-red-500/20'
              }`}>
                <span className="material-symbols-outlined text-[18px]">
                  {actionMsg.type === 'success' ? 'check_circle' : 'error'}
                </span>
                {actionMsg.text}
              </div>
            )}

            {/* Assign Owner */}
            <div className="bg-[#171f33] rounded-2xl p-6 border border-gray-800">
              <h2 className="text-xs font-bold uppercase tracking-widest text-[#b4c5ff] mb-4 flex items-center gap-2">
                <span className="material-symbols-outlined text-[#2563eb] text-[18px]">person_add</span>
                Assign Owner
              </h2>
              <form onSubmit={handleAssignOwner} className="flex gap-3">
                <input
                  className="flex-1 bg-[#0b1326] border border-gray-700 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:border-[#2563eb] outline-none transition-colors text-sm"
                  placeholder="User ID to assign as owner"
                  value={ownerUserId}
                  onChange={e => setOwnerUserId(e.target.value)}
                  required
                />
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="bg-[#2563eb] text-white px-5 py-3 rounded-xl font-bold text-sm hover:bg-[#0053db] transition-colors disabled:opacity-50 flex-shrink-0"
                >
                  ASSIGN
                </button>
              </form>
            </div>

            {/* Appoint Manager */}
            <div className="bg-[#171f33] rounded-2xl p-6 border border-gray-800">
              <h2 className="text-xs font-bold uppercase tracking-widest text-[#b4c5ff] mb-4 flex items-center gap-2">
                <span className="material-symbols-outlined text-[#00dbe7] text-[18px]">manage_accounts</span>
                Appoint Manager
              </h2>
              <form onSubmit={handleAppointManager} className="space-y-4">
                <input
                  className="w-full bg-[#0b1326] border border-gray-700 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:border-[#2563eb] outline-none transition-colors text-sm"
                  placeholder="User ID to appoint as manager"
                  value={managerUserId}
                  onChange={e => setManagerUserId(e.target.value)}
                  required
                />
                <div>
                  <p className="text-[10px] text-gray-400 mb-2 font-mono uppercase tracking-widest">Grant Permissions</p>
                  <div className="grid grid-cols-2 gap-2">
                    {ALL_PERMISSIONS.map(p => (
                      <button
                        key={p}
                        type="button"
                        onClick={() => togglePerm(p)}
                        className={`text-[11px] font-mono px-3 py-2 rounded-lg border transition-all text-left flex items-center gap-1.5 ${
                          managerPerms.includes(p)
                            ? 'bg-[#00dbe7]/15 border-[#00dbe7]/40 text-[#00dbe7]'
                            : 'bg-[#0b1326] border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-300'
                        }`}
                      >
                        <span className="material-symbols-outlined text-[14px]">
                          {managerPerms.includes(p) ? 'check_box' : 'check_box_outline_blank'}
                        </span>
                        {PERMISSION_LABELS[p]}
                      </button>
                    ))}
                  </div>
                </div>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="w-full py-3 bg-[#00dbe7] text-[#0b1326] rounded-xl font-black text-sm hover:opacity-90 transition-opacity disabled:opacity-50"
                >
                  {actionLoading ? 'APPOINTING...' : 'APPOINT MANAGER'}
                </button>
              </form>
            </div>

            {/* Remove Manager */}
            <div className="bg-[#171f33] rounded-2xl p-6 border border-gray-800">
              <h2 className="text-xs font-bold uppercase tracking-widest text-[#b4c5ff] mb-4 flex items-center gap-2">
                <span className="material-symbols-outlined text-red-400 text-[18px]">person_remove</span>
                Remove Manager
              </h2>
              <form onSubmit={handleRemoveManager} className="flex gap-3">
                <input
                  className="flex-1 bg-[#0b1326] border border-gray-700 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:border-red-500/40 outline-none transition-colors text-sm"
                  placeholder="Manager user ID to remove"
                  value={removeManagerId}
                  onChange={e => setRemoveManagerId(e.target.value)}
                  required
                />
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="bg-red-900/40 text-red-400 border border-red-500/20 px-5 py-3 rounded-xl font-bold text-sm hover:bg-red-900/60 transition-colors disabled:opacity-50 flex-shrink-0"
                >
                  REMOVE
                </button>
              </form>
            </div>

            {/* Purchase Policy link */}
            <Link
              to="/company/policies"
              className="flex items-center justify-between bg-[#171f33] rounded-2xl p-6 border border-gray-800 hover:border-[#2563eb]/40 transition-colors group"
            >
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-[#2563eb] text-2xl">policy</span>
                <div>
                  <div className="font-bold text-white">Purchase Policies</div>
                  <div className="text-xs text-gray-400 mt-0.5">Configure pricing rules and discounts</div>
                </div>
              </div>
              <span className="material-symbols-outlined text-gray-600 group-hover:text-[#2563eb] group-hover:translate-x-1 transition-all">
                arrow_forward
              </span>
            </Link>
          </div>
        )}

      </div>
    </div>
  );
}

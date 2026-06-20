import { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import * as api from '../../api/productionCompanyApi';
import { authApi } from '../../api/authApi';

type UserRole = 'FOUNDER' | 'OWNER' | 'MANAGER';

function resolveRole(userId: string, rolesTree: api.RolesTreeDTO): UserRole {
    if (rolesTree.founderId === userId) return 'FOUNDER';
    if (rolesTree.ownershipTree[userId]) return 'OWNER';
    return 'MANAGER';
}

function myPermissions(userId: string, rolesTree: api.RolesTreeDTO): Set<api.ManagerPermission> {
    return new Set((rolesTree.managerPermissions[userId] ?? []) as api.ManagerPermission[]);
}


type Tab = 'TEAM' | 'HISTORY' | 'ACTIONS';
type ModalType = 'assignOwner' | 'appointManager' | { type: 'editPerms'; managerId: string } | null;

const PERM_LABELS: Record<api.ManagerPermission, string> = {
    INVENTORY_MANAGEMENT: 'Inventory Management',
    VENUE_CONFIGURATION_AND_EVENT_MAPPING: 'Venue & Event Mapping',
    COMPANY_POLICY_MANAGEMENT: 'Company Policy',
    PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT: 'Purchasing & Discount Policy',
    CUSTOMER_INQUIRY_AND_RESPONSE_MANAGEMENT: 'Customer Inquiry',
    PURCHASE_AND_ORDER_HISTORY_ACCESS: 'Order History Access',
    SALES_REPORT_GENERATION: 'Sales Reports',
};

// ─── Small reusable pieces ───────────────────────────────────────────────────

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
    return (
        <div className="bg-[#1a2340] rounded-xl px-4 py-2 flex flex-col items-center min-w-[72px]">
            <span className={`text-xl font-black ${color}`}>{value}</span>
            <span className="text-[9px] font-mono text-gray-400 tracking-widest mt-0.5">{label}</span>
        </div>
    );
}

function SectionHeader({ icon, label, count, color }: { icon: string; label: string; count?: number; color: string }) {
    return (
        <div className="flex items-center gap-2 mb-3">
            <span className={`material-symbols-outlined text-[20px] ${color}`}>{icon}</span>
            <span className={`text-xs font-black tracking-widest ${color}`}>
                {label}{count !== undefined ? ` (${count})` : ''}
            </span>
        </div>
    );
}

function EmptyState({ msg }: { msg: string }) {
    return (
        <p className="text-center text-sm text-gray-500 py-5 font-mono">{msg}</p>
    );
}

function UserIdInput({
    label, value, onChange,
}: { label: string; value: string; onChange: (v: string) => void }) {
    return (
        <div className="space-y-1">
            <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400">{label}</label>
            <input
                className="w-full bg-[#0b1326] border border-gray-600 rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[#2563eb] transition-colors"
                placeholder="Enter user ID..."
                value={value}
                onChange={e => onChange(e.target.value)}
                required
            />
        </div>
    );
}

function PermissionsChecklist({
    selected, onToggle,
}: { selected: Set<api.ManagerPermission>; onToggle: (p: api.ManagerPermission) => void }) {
    return (
        <div className="space-y-1.5">
            <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400">Permissions</label>
            <div className="space-y-1 max-h-48 overflow-y-auto pr-1">
                {api.ALL_PERMISSIONS.map(p => (
                    <label key={p} className="flex items-center gap-2.5 cursor-pointer group">
                        <input
                            type="checkbox"
                            checked={selected.has(p)}
                            onChange={() => onToggle(p)}
                            className="w-4 h-4 rounded border-gray-500 text-[#2563eb] bg-[#0b1326] focus:ring-[#2563eb]"
                        />
                        <span className="text-sm text-gray-300 group-hover:text-white transition-colors">{PERM_LABELS[p]}</span>
                    </label>
                ))}
            </div>
        </div>
    );
}

// ─── Modal shell ─────────────────────────────────────────────────────────────

function Modal({ title, onClose, onSubmit, loading, error, children }: {
    title: string;
    onClose: () => void;
    onSubmit: (e: React.FormEvent) => void;
    loading: boolean;
    error: string | null;
    children: React.ReactNode;
}) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
            <div className="bg-[#171f33] border border-gray-700 rounded-2xl w-full max-w-md shadow-2xl">
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-700">
                    <h3 className="font-black text-white tracking-wide text-sm">{title}</h3>
                    <button onClick={onClose} className="text-gray-400 hover:text-white">
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>
                <form onSubmit={onSubmit} className="p-6 space-y-4">
                    {children}
                    {error && (
                        <p className="text-red-400 text-xs bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">{error}</p>
                    )}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 bg-[#2563eb] hover:bg-[#0053db] text-white font-bold text-sm rounded-xl transition-colors flex items-center justify-center gap-2 disabled:opacity-60"
                    >
                        {loading && <span className="material-symbols-outlined animate-spin text-[18px]">refresh</span>}
                        CONFIRM
                    </button>
                </form>
            </div>
        </div>
    );
}

// ─── Tab: TEAM ────────────────────────────────────────────────────────────────

function TeamTab({
    rolesTree,
    myRole,
    onAssignOwner,
    onAppointManager,
    onEditPerms,
    onRemoveManager,
    onRemoveOwner,
}: {
    rolesTree: api.RolesTreeDTO;
    myRole: UserRole;
    onAssignOwner: () => void;
    onAppointManager: () => void;
    onEditPerms: (managerId: string) => void;
    onRemoveManager: (managerId: string) => void;
    onRemoveOwner: (ownerId: string) => void;
}) {
    const owners = Object.values(rolesTree.ownershipTree);
    const managers = Object.values(rolesTree.managerTree);
    const canManage = myRole === 'FOUNDER' || myRole === 'OWNER';

    return (
        <div className="space-y-4">
            {/* FOUNDER */}
            <div className="bg-[#171f33] rounded-2xl p-5 border border-gray-800">
                <SectionHeader icon="person" label="FOUNDER" color="text-[#00dbe7]" />
                <div className="bg-[#0f1627] rounded-xl px-4 py-3 flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-[#1a2340] flex items-center justify-center flex-shrink-0">
                        <span className="material-symbols-outlined text-[20px] text-amber-400">star</span>
                    </div>
                    <div>
                        <span className="text-white font-bold text-sm">{rolesTree.founderId}</span>
                        <p className="text-[10px] font-mono text-amber-400 tracking-widest mt-0.5">FOUNDER · FULL ACCESS</p>
                    </div>
                </div>
            </div>

            {/* OWNERS */}
            <div className="bg-[#171f33] rounded-2xl p-5 border border-gray-800">
                <div className="flex items-center justify-between mb-3">
                    <SectionHeader icon="group" label="OWNERS" count={owners.length} color="text-[#60a5fa]" />
                    {canManage && (
                        <button
                            onClick={onAssignOwner}
                            className="flex items-center gap-1 text-xs bg-[#2563eb] hover:bg-[#0053db] text-white px-3 py-1.5 rounded-lg font-bold tracking-wider transition-colors"
                        >
                            <span className="material-symbols-outlined text-[14px]">person_add</span>
                            ASSIGN
                        </button>
                    )}
                </div>
                {owners.length === 0 ? (
                    <EmptyState msg="No owners assigned yet" />
                ) : (
                    <div className="space-y-2">
                        {owners.map(o => {
                            const isFounder = o.userId === rolesTree.founderId;
                            return (
                                <div key={o.userId} className="bg-[#0f1627] rounded-xl px-4 py-3 flex items-center gap-3">
                                    <div className="w-8 h-8 rounded-full bg-[#2563eb]/20 flex items-center justify-center flex-shrink-0">
                                        <span className="material-symbols-outlined text-[18px] text-[#60a5fa]">manage_accounts</span>
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <span className="text-white font-bold text-sm truncate block">{o.userId}</span>
                                        {o.appointerId && (
                                            <p className="text-[10px] font-mono text-gray-500 mt-0.5">Appointed by {o.appointerId}</p>
                                        )}
                                    </div>
                                    {isFounder ? (
                                        <span className="text-[10px] font-mono text-amber-400 bg-amber-400/10 px-2 py-0.5 rounded-full">FOUNDER</span>
                                    ) : (
                                        <>
                                            <span className="text-[10px] font-mono text-[#60a5fa] bg-[#2563eb]/10 px-2 py-0.5 rounded-full">OWNER</span>
                                            {canManage && (
                                                <button
                                                    onClick={() => onRemoveOwner(o.userId)}
                                                    className="p-1.5 rounded-lg bg-red-900/40 hover:bg-red-900/70 text-red-400 hover:text-red-300 transition-colors"
                                                    title="Remove owner"
                                                >
                                                    <span className="material-symbols-outlined text-[16px]">person_remove</span>
                                                </button>
                                            )}
                                        </>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* MANAGERS */}
            <div className="bg-[#171f33] rounded-2xl p-5 border border-gray-800">
                <div className="flex items-center justify-between mb-3">
                    <SectionHeader icon="supervisor_account" label="MANAGERS" count={managers.length} color="text-[#34d399]" />
                    {canManage && (
                        <button
                            onClick={onAppointManager}
                            className="flex items-center gap-1 text-xs bg-emerald-700 hover:bg-emerald-600 text-white px-3 py-1.5 rounded-lg font-bold tracking-wider transition-colors"
                        >
                            <span className="material-symbols-outlined text-[14px]">person_add</span>
                            APPOINT
                        </button>
                    )}
                </div>
                {managers.length === 0 ? (
                    <EmptyState msg="No managers appointed yet" />
                ) : (
                    <div className="space-y-2">
                        {managers.map(m => {
                            const perms = rolesTree.managerPermissions[m.userId] ?? [];
                            return (
                                <div key={m.userId} className="bg-[#0f1627] rounded-xl px-4 py-3">
                                    <div className="flex items-center gap-3">
                                        <div className="w-8 h-8 rounded-full bg-emerald-500/20 flex items-center justify-center flex-shrink-0">
                                            <span className="material-symbols-outlined text-[18px] text-[#34d399]">badge</span>
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <span className="text-white font-bold text-sm truncate block">{m.userId}</span>
                                            <p className="text-[10px] font-mono text-gray-500 mt-0.5">Appointed by {m.appointerId}</p>
                                        </div>
                                        {canManage && (
                                            <div className="flex gap-1.5">
                                                <button
                                                    onClick={() => onEditPerms(m.userId)}
                                                    className="p-1.5 rounded-lg bg-gray-700 hover:bg-gray-600 text-gray-300 hover:text-white transition-colors"
                                                    title="Edit permissions"
                                                >
                                                    <span className="material-symbols-outlined text-[16px]">edit</span>
                                                </button>
                                                <button
                                                    onClick={() => onRemoveManager(m.userId)}
                                                    className="p-1.5 rounded-lg bg-red-900/40 hover:bg-red-900/70 text-red-400 hover:text-red-300 transition-colors"
                                                    title="Remove manager"
                                                >
                                                    <span className="material-symbols-outlined text-[16px]">person_remove</span>
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                    {perms.length > 0 && (
                                        <div className="flex flex-wrap gap-1.5 mt-2.5 ml-11">
                                            {perms.map(p => (
                                                <span key={p} className="text-[9px] font-mono bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2 py-0.5 rounded-full">
                                                    {PERM_LABELS[p]}
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
    );
}

// ─── Tab: HISTORY ─────────────────────────────────────────────────────────────

function HistoryTab({ history, loading }: { history: api.HistoryOrderItem[]; loading: boolean }) {
    if (loading) {
        return (
            <div className="flex justify-center py-16">
                <span className="material-symbols-outlined animate-spin text-3xl text-[#2563eb]">refresh</span>
            </div>
        );
    }
    if (history.length === 0) {
        return (
            <div className="bg-[#171f33] rounded-2xl p-10 border border-gray-800 text-center">
                <span className="material-symbols-outlined text-5xl text-gray-600 mb-3">receipt_long</span>
                <p className="text-gray-500 font-mono text-sm">No purchase history yet</p>
            </div>
        );
    }
    return (
        <div className="space-y-3">
            {history.map(order => (
                <div key={order.orderId} className="bg-[#171f33] border border-gray-800 rounded-xl px-5 py-4 flex flex-col md:flex-row md:items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-[#2563eb]/10 flex items-center justify-center flex-shrink-0">
                        <span className="material-symbols-outlined text-[20px] text-[#60a5fa]">receipt</span>
                    </div>
                    <div className="flex-1 min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                            <span className="text-white font-bold text-sm font-mono">{order.orderId}</span>
                            <span className="text-[10px] bg-[#2563eb]/20 text-[#60a5fa] px-2 py-0.5 rounded-full font-mono">
                                Event #{order.eventId}
                            </span>
                        </div>
                        <div className="flex flex-wrap gap-3 mt-1 text-[11px] font-mono text-gray-400">
                            <span>User: {order.userId}</span>
                            {order.purchaseDate && <span>{new Date(order.purchaseDate).toLocaleDateString()}</span>}
                            {order.seatIds?.length > 0 && <span>{order.seatIds.length} seat(s)</span>}
                        </div>
                    </div>
                    <div className="text-right flex-shrink-0">
                        <span className="text-lg font-black text-[#00dbe7]">${order.price?.toFixed(2) ?? '—'}</span>
                    </div>
                </div>
            ))}
        </div>
    );
}

// ─── Tab: ACTIONS ─────────────────────────────────────────────────────────────

function ActionsTab({
    companyId,
    myRole,
    myPerms,
    onAssignOwner,
    onAppointManager,
    onManagePolicies,
    onSwitchToHistory,
}: {
    companyId: number;
    myRole: UserRole;
    myPerms: Set<api.ManagerPermission>;
    onAssignOwner: () => void;
    onAppointManager: () => void;
    onManagePolicies: () => void;
    onSwitchToHistory: () => void;
}) {
    const isManager = myRole === 'MANAGER';

    type ActionDef = { icon: string; label: string; desc: string; color: string; bg: string; onClick?: () => void; to?: string; requiredPerm?: api.ManagerPermission | api.ManagerPermission[]; ownerOnly?: boolean };
    const allActions: ActionDef[] = [
        {
            icon: 'event',
            label: 'Manage Events',
            desc: 'Create and manage events for this company',
            color: 'text-[#00dbe7]',
            bg: 'bg-[#00dbe7]/10 border-[#00dbe7]/20 hover:border-[#00dbe7]/50',
            to: `/company/${companyId}/events`,
            requiredPerm: ['INVENTORY_MANAGEMENT', 'VENUE_CONFIGURATION_AND_EVENT_MAPPING', 'PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT'],
        },
        {
            icon: 'person_add',
            label: 'Assign Owner',
            desc: 'Grant ownership rights to a registered user',
            color: 'text-[#60a5fa]',
            bg: 'bg-[#2563eb]/10 border-[#2563eb]/20 hover:border-[#2563eb]/50',
            onClick: onAssignOwner,
            ownerOnly: true,
        },
        {
            icon: 'manage_accounts',
            label: 'Appoint Manager',
            desc: 'Appoint a user as manager with specific permissions',
            color: 'text-[#34d399]',
            bg: 'bg-emerald-500/10 border-emerald-500/20 hover:border-emerald-500/50',
            onClick: onAppointManager,
            ownerOnly: true,
        },
        {
            icon: 'policy',
            label: 'Manage Policies',
            desc: 'View and edit purchase policies for this company',
            color: 'text-[#f59e0b]',
            bg: 'bg-amber-500/10 border-amber-500/20 hover:border-amber-500/50',
            onClick: onManagePolicies,
            requiredPerm: 'COMPANY_POLICY_MANAGEMENT',
        },
        {
            icon: 'history',
            label: 'Purchase History',
            desc: 'View all ticket orders made through this company',
            color: 'text-[#a78bfa]',
            bg: 'bg-violet-500/10 border-violet-500/20 hover:border-violet-500/50',
            onClick: onSwitchToHistory,
            requiredPerm: 'PURCHASE_AND_ORDER_HISTORY_ACCESS',
        },
    ];

    const actions = allActions.filter(a => {
        if (!isManager) return true;               // founders/owners see everything
        if (a.ownerOnly) return false;             // managers never see owner-only actions
        if (a.requiredPerm) {
            const perms = Array.isArray(a.requiredPerm) ? a.requiredPerm : [a.requiredPerm];
            if (!perms.some(perm => myPerms.has(perm))) return false;
        }
        return true;
    });

    const rowClass = (i: number) =>
        `w-full text-left flex items-center gap-6 px-8 py-7 cursor-pointer transition-colors hover:bg-[#1e2a45] ${i !== actions.length - 1 ? 'border-b border-gray-800' : ''}`;

    return (
        <div className="bg-[#171f33] rounded-2xl border border-gray-800 overflow-hidden">
            {actions.map((a, i) => {
                const content = <>
                    <div className={`w-16 h-16 rounded-2xl ${a.bg.split(' ')[0]} flex items-center justify-center flex-shrink-0`}>
                        <span className={`material-symbols-outlined text-[34px] ${a.color}`}>{a.icon}</span>
                    </div>
                    <span className="text-gray-600 font-black text-lg select-none w-7 flex-shrink-0">{i + 1}.</span>
                    <div className="flex-1 min-w-0">
                        <p className={`font-black text-lg tracking-wide ${a.color}`}>{a.label}</p>
                        <p className="text-base text-gray-500 mt-1">{a.desc}</p>
                    </div>
                    <span className="material-symbols-outlined text-gray-600 text-[28px] flex-shrink-0">chevron_right</span>
                </>;

                if (a.to) {
                    return (
                        <Link key={a.label} to={a.to} className={rowClass(i)}>
                            {content}
                        </Link>
                    );
                }
                return (
                    <button key={a.label} onClick={a.onClick} className={rowClass(i)}>
                        {content}
                    </button>
                );
            })}
        </div>
    );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function ProductionCompanyPage() {
    const { companyId } = useParams<{ companyId: string }>();
    const numericId = Number(companyId);
    const navigate = useNavigate();

    const handleLogout = async () => {
        const token = localStorage.getItem('token') ?? '';
        const userId = localStorage.getItem('userId') ?? '';
        try { await authApi.logout(token, userId); } catch { /* ignore */ }
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        navigate('/login');
    };

    const [activeTab, setActiveTab] = useState<Tab>('TEAM');
    const [rolesTree, setRolesTree] = useState<api.RolesTreeDTO | null>(null);
    const [memberInfo, setMemberInfo] = useState<api.MemberInfo | null>(null);
    const [history, setHistory] = useState<api.HistoryOrderItem[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [pageError, setPageError] = useState<string | null>(null);

    const [modal, setModal] = useState<ModalType>(null);
    const [formUserId, setFormUserId] = useState('');
    const [formPerms, setFormPerms] = useState<Set<api.ManagerPermission>>(new Set());
    const [formLoading, setFormLoading] = useState(false);
    const [formError, setFormError] = useState<string | null>(null);

    const [toast, setToast] = useState<{ msg: string; ok: boolean } | null>(null);

    const showToast = (msg: string, ok = true) => {
        setToast({ msg, ok });
        setTimeout(() => setToast(null), 3000);
    };

    const fetchRoles = useCallback(async () => {
        try {
            const tree = await api.getRolesTree(numericId);
            setRolesTree(tree);
            setPageError(null);
        } catch {
            // Founder/owner-only endpoint — try the member-level fallback for managers
            try {
                const info = await api.getMyMemberInfo(numericId);
                setMemberInfo(info);
                setPageError(null);
            } catch (e) {
                setPageError(e instanceof Error ? e.message : 'Failed to load company data');
            }
        }
    }, [numericId]);

    const fetchHistory = useCallback(async () => {
        setHistoryLoading(true);
        try {
            const hist = await api.getPurchaseHistory(numericId);
            setHistory(hist);
        } catch {
            // history may be restricted for non-owners
        }
        setHistoryLoading(false);
    }, [numericId]);

    useEffect(() => {
        (async () => {
            setIsLoading(true);
            await fetchRoles();
            setIsLoading(false);
        })();
    }, [fetchRoles]);

    useEffect(() => {
        if (activeTab === 'HISTORY') fetchHistory();
    }, [activeTab, fetchHistory]);

    const openModal = (type: ModalType) => {
        setModal(type);
        setFormUserId('');
        setFormError(null);
        if (type && typeof type === 'object' && type.type === 'editPerms' && rolesTree) {
            const existing = (rolesTree.managerPermissions[type.managerId] ?? []) as api.ManagerPermission[];
            setFormPerms(new Set(existing));
        } else {
            setFormPerms(new Set());
        }
    };

    const closeModal = () => { setModal(null); setFormError(null); };

    const togglePerm = (p: api.ManagerPermission) =>
        setFormPerms(prev => { const s = new Set(prev); s.has(p) ? s.delete(p) : s.add(p); return s; });

    const handleAssignOwner = async (e: React.FormEvent) => {
        e.preventDefault();
        setFormLoading(true); setFormError(null);
        try {
            await api.assignOwner(numericId, formUserId);
            showToast('Owner assigned successfully!');
            closeModal();
            await fetchRoles();
        } catch (e) {
            setFormError(e instanceof Error ? e.message : 'Failed to assign owner');
        }
        setFormLoading(false);
    };

    const handleAppointManager = async (e: React.FormEvent) => {
        e.preventDefault();
        setFormLoading(true); setFormError(null);
        try {
            await api.appointManager(numericId, formUserId, [...formPerms]);
            showToast('Manager appointed successfully!');
            closeModal();
            await fetchRoles();
        } catch (e) {
            setFormError(e instanceof Error ? e.message : 'Failed to appoint manager');
        }
        setFormLoading(false);
    };

    const handleEditPerms = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!modal || typeof modal !== 'object') return;
        setFormLoading(true); setFormError(null);
        try {
            await api.modifyManagerPermissions(numericId, modal.managerId, [...formPerms]);
            showToast('Permissions updated!');
            closeModal();
            await fetchRoles();
        } catch (e) {
            setFormError(e instanceof Error ? e.message : 'Failed to update permissions');
        }
        setFormLoading(false);
    };

    const handleRemoveManager = async (managerId: string) => {
        if (!window.confirm(`Remove manager "${managerId}"?`)) return;
        try {
            await api.removeManager(numericId, managerId);
            showToast('Manager removed!');
            await fetchRoles();
        } catch (e) {
            showToast(e instanceof Error ? e.message : 'Failed to remove manager', false);
        }
    };

    const handleRemoveOwner = async (ownerId: string) => {
        if (!window.confirm(`Remove owner "${ownerId}"?`)) return;
        try {
            await api.removeOwner(numericId, ownerId);
            showToast('Owner removed!');
            await fetchRoles();
        } catch (e) {
            showToast(e instanceof Error ? e.message : 'Failed to remove owner', false);
        }
    };

    const currentUserId = localStorage.getItem('userId') ?? '';

    // Build an effective roles-tree from memberInfo when the manager-restricted endpoint is unavailable
    const effectiveRolesTree: api.RolesTreeDTO | null = rolesTree ?? (memberInfo ? {
        companyId: numericId,
        companyName: memberInfo.companyName,
        founderId: memberInfo.founderId,
        ownershipTree: memberInfo.ownershipTree,
        managerTree: memberInfo.managerTree,
        managerPermissions: memberInfo.managerPermissions,
    } : null);

    const myRole: UserRole = effectiveRolesTree
        ? resolveRole(currentUserId, effectiveRolesTree)
        : (memberInfo?.role ?? 'MANAGER');
    const myPerms: Set<api.ManagerPermission> = effectiveRolesTree
        ? myPermissions(currentUserId, effectiveRolesTree)
        : new Set((memberInfo?.permissions ?? []) as api.ManagerPermission[]);
    const companyDisplayName = effectiveRolesTree?.companyName ?? `Company #${numericId}`;

    const ownerCount = effectiveRolesTree ? Object.keys(effectiveRolesTree.ownershipTree).length : 0;
    const managerCount = effectiveRolesTree ? Object.keys(effectiveRolesTree.managerTree).length : 0;

    if (isLoading) {
        return (
            <div className="bg-[#0b1326] min-h-screen flex items-center justify-center">
                <div className="flex items-center gap-3 text-[#dae2fd]">
                    <span className="material-symbols-outlined animate-spin text-3xl text-[#2563eb]">refresh</span>
                    <span className="font-mono text-sm tracking-widest">LOADING COMPANY DATA...</span>
                </div>
            </div>
        );
    }

    if (pageError) {
        return (
            <div className="bg-[#0b1326] min-h-screen flex items-center justify-center p-6">
                <div className="bg-[#171f33] border border-red-500/30 p-10 rounded-2xl text-center max-w-md shadow-xl">
                    <span className="material-symbols-outlined text-6xl text-red-500 mb-4 block">gpp_bad</span>
                    <h1 className="text-2xl font-black text-white mb-2">ACCESS DENIED</h1>
                    <p className="text-sm text-gray-400 mb-6">{pageError}</p>
                    <Link to="/dashboard" className="bg-[#2563eb] text-white px-6 py-3 rounded-lg font-bold text-sm">
                        RETURN TO DASHBOARD
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen font-sans">

            {/* Toast */}
            {toast && (
                <div className={`fixed top-4 right-4 z-50 px-5 py-3 rounded-xl shadow-xl text-sm font-bold tracking-wide flex items-center gap-2 transition-all ${toast.ok ? 'bg-green-600' : 'bg-red-600'} text-white`}>
                    <span className="material-symbols-outlined text-[18px]">{toast.ok ? 'check_circle' : 'error'}</span>
                    {toast.msg}
                </div>
            )}

            {/* Header */}
            <div className="bg-[#eeefff] text-[#171f33] px-6 md:px-10 py-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="flex items-center gap-1.5 text-xs font-bold text-gray-500 hover:text-[#2563eb] transition-colors"
                        title="Back to My Companies"
                    >
                        <span className="material-symbols-outlined text-[20px]">arrow_back</span>
                        <span className="hidden sm:inline tracking-wider">MY COMPANIES</span>
                    </button>
                    <div className="w-px h-8 bg-gray-300" />
                    <div>
                        <div className="flex items-center gap-2.5 mb-1">
                            <span className="material-symbols-outlined text-[#2563eb] text-3xl">domain</span>
                            <h1 className="text-2xl font-black tracking-tight text-[#0b1326]">
                                {companyDisplayName}
                            </h1>
                        </div>
                        <div className="flex items-center gap-1.5 text-sm text-gray-500 font-mono ml-0.5">
                            <span className="material-symbols-outlined text-[15px]">person</span>
                            <span className="material-symbols-outlined text-[13px] text-amber-500">workspace_premium</span>
                            <span>Founder: <strong className="text-[#0b1326]">{effectiveRolesTree?.founderId ?? '—'}</strong></span>
                        </div>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    <div className="flex gap-2">
                        <StatCard label="OWNERS" value={ownerCount} color="text-[#2563eb]" />
                        <StatCard label="MANAGERS" value={managerCount} color="text-[#00c896]" />
                        <StatCard label="ORDERS" value={history.length} color="text-[#00c896]" />
                    </div>
                    <button
                        onClick={() => navigate('/profile')} //TODO: CONNECT TO THE PROFILE PAGE
                        className="flex items-center gap-2 border border-gray-400 hover:border-[#2563eb] text-gray-500 hover:text-[#2563eb] px-4 py-2.5 rounded-xl font-bold text-sm tracking-wider transition-colors"
                    >
                        <span className="material-symbols-outlined text-[18px]">account_circle</span>
                        MY PROFILE
                    </button>
                    <button
                        onClick={handleLogout}
                        className="flex items-center gap-2 border border-gray-400 hover:border-red-400 text-gray-500 hover:text-red-500 px-4 py-2.5 rounded-xl font-bold text-sm tracking-wider transition-colors"
                    >
                        <span className="material-symbols-outlined text-[18px]">logout</span>
                        LOGOUT
                    </button>
                </div>
            </div>

            {/* Tabs */}
            <div className="px-4 md:px-6 py-3 bg-[#0d1525] border-b border-gray-800">
                <div className="flex gap-2 max-w-4xl mx-auto">
                    {(['TEAM', 'HISTORY', 'ACTIONS'] as Tab[]).map(tab => (
                        <button
                            key={tab}
                            onClick={() => setActiveTab(tab)}
                            className={`flex-1 py-3 text-xs font-black tracking-widest flex items-center justify-center gap-2 rounded-xl border transition-all ${activeTab === tab
                                ? 'bg-[#2563eb] border-[#2563eb] text-white shadow-lg shadow-[#2563eb]/20'
                                : 'bg-[#2a4a82] border-[#3a5fa0] text-[#b8d0f5] hover:bg-[#3356a0] hover:border-[#4a70b8] hover:text-white'
                                }`}
                        >
                            <span className="material-symbols-outlined text-[18px]">
                                {tab === 'TEAM' ? 'group' : tab === 'HISTORY' ? 'history' : 'bolt'}
                            </span>
                            {tab}
                        </button>
                    ))}
                </div>
            </div>

            {/* Tab Content */}
            <div className="p-4 md:p-6 max-w-4xl mx-auto">
                {activeTab === 'TEAM' && effectiveRolesTree && (
                    <TeamTab
                        rolesTree={effectiveRolesTree}
                        myRole={myRole}
                        onAssignOwner={() => openModal('assignOwner')}
                        onAppointManager={() => openModal('appointManager')}
                        onEditPerms={id => openModal({ type: 'editPerms', managerId: id })}
                        onRemoveManager={handleRemoveManager}
                        onRemoveOwner={handleRemoveOwner}
                    />
                )}
                {activeTab === 'HISTORY' && (
                    <HistoryTab history={history} loading={historyLoading} />
                )}
                {activeTab === 'ACTIONS' && (
                    <ActionsTab
                        companyId={numericId}
                        myRole={myRole}
                        myPerms={myPerms}
                        onAssignOwner={() => { setActiveTab('TEAM'); openModal('assignOwner'); }}
                        onAppointManager={() => { setActiveTab('TEAM'); openModal('appointManager'); }}
                        onManagePolicies={() => navigate(`/company/${numericId}/purchase-policy`)}
                        onSwitchToHistory={() => setActiveTab('HISTORY')}
                    />
                )}
            </div>

            {/* Modal: Assign Owner */}
            {modal === 'assignOwner' && (
                <Modal title="ASSIGN OWNER" onClose={closeModal} onSubmit={handleAssignOwner} loading={formLoading} error={formError}>
                    <UserIdInput label="User ID to assign as owner" value={formUserId} onChange={setFormUserId} />
                </Modal>
            )}

            {/* Modal: Appoint Manager */}
            {modal === 'appointManager' && (
                <Modal title="APPOINT MANAGER" onClose={closeModal} onSubmit={handleAppointManager} loading={formLoading} error={formError}>
                    <UserIdInput label="User ID to appoint as manager" value={formUserId} onChange={setFormUserId} />
                    <PermissionsChecklist selected={formPerms} onToggle={togglePerm} />
                </Modal>
            )}

            {/* Modal: Edit Permissions */}
            {modal && typeof modal === 'object' && modal.type === 'editPerms' && (
                <Modal title={`EDIT PERMISSIONS — ${modal.managerId}`} onClose={closeModal} onSubmit={handleEditPerms} loading={formLoading} error={formError}>
                    <PermissionsChecklist selected={formPerms} onToggle={togglePerm} />
                </Modal>
            )}
        </div>
    );
}

import { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getCompanyPolicyDTO, setCompanyPolicyDTO } from '../../api/purchasePoliciesApi';
import type { PurchasePolicyDTO } from '../../api/eventsApi';

// ─── Shared styles ─────────────────────────────────────────────────────────────

const inputCls = 'w-full bg-[#0b1326] border border-gray-600 rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[#2563eb] transition-colors';
const labelCls = 'text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400';
const sectionHdr = 'flex items-center gap-1.5 text-[10px] font-mono font-bold uppercase tracking-widest text-[#2563eb] mb-3';

// ─── Policy Builder (same as CompanyEventsPage) ───────────────────────────────

type PolicyKey = 'minTickets' | 'maxTickets' | 'minAge' | 'maxAge';
interface PolicyGroup { id: string; type: 'AND' | 'OR'; keys: PolicyKey[]; }

const POLICY_META: { key: PolicyKey; label: string }[] = [
    { key: 'minTickets', label: 'Min Tickets / User' },
    { key: 'maxTickets', label: 'Max Tickets / User' },
    { key: 'minAge',     label: 'Min Age'            },
    { key: 'maxAge',     label: 'Max Age'            },
];

function dtoToFields(dto: PurchasePolicyDTO | null) {
    return {
        minTickets: dto?.minTickets != null ? String(dto.minTickets) : '',
        maxTickets: dto?.maxTickets != null ? String(dto.maxTickets) : '',
        minAge:     dto?.minAge     != null ? String(dto.minAge)     : '',
        maxAge:     dto?.maxAge     != null ? String(dto.maxAge)     : '',
    };
}

function dtoToGroups(dto: PurchasePolicyDTO | null): { groups: PolicyGroup[]; combine: 'AND' | 'OR' } {
    if (!dto) return { groups: [], combine: 'AND' };
    const hasAge = dto.minAge != null || dto.maxAge != null;
    const hasQty = dto.minTickets != null || dto.maxTickets != null;
    const groups: PolicyGroup[] = [];

    const ageKeys: PolicyKey[] = [];
    if (dto.minAge != null) ageKeys.push('minAge');
    if (dto.maxAge != null) ageKeys.push('maxAge');
    const qtyKeys: PolicyKey[] = [];
    if (dto.minTickets != null) qtyKeys.push('minTickets');
    if (dto.maxTickets != null) qtyKeys.push('maxTickets');

    if (hasAge && ageKeys.length > 0) {
        groups.push({ id: 'age', type: dto.isAgeOr ? 'OR' : 'AND', keys: ageKeys });
    }
    if (hasQty && qtyKeys.length > 0) {
        groups.push({ id: 'qty', type: dto.isQuantityOr ? 'OR' : 'AND', keys: qtyKeys });
    }

    const combine = (hasAge && hasQty && dto.isAgeAndQuantityOr) ? 'OR' : 'AND';
    return { groups, combine };
}

function PolicyBuilder({
    initialDTO,
    onChange,
}: {
    initialDTO: PurchasePolicyDTO | null;
    onChange: (dto: PurchasePolicyDTO, hasError: boolean) => void;
}) {
    const init = dtoToFields(initialDTO);
    const initGroups = dtoToGroups(initialDTO);

    const [minTickets, setMinTickets] = useState(init.minTickets);
    const [maxTickets, setMaxTickets] = useState(init.maxTickets);
    const [minAge,     setMinAge]     = useState(init.minAge);
    const [maxAge,     setMaxAge]     = useState(init.maxAge);
    const [groups,     setGroups]     = useState<PolicyGroup[]>(initGroups.groups);
    const [groupCombine, setGroupCombine] = useState<'AND' | 'OR'>(initGroups.combine);

    const vals: Record<PolicyKey, string> = { minTickets, maxTickets, minAge, maxAge };

    const available = useMemo(
        () => POLICY_META.filter(m => vals[m.key] !== ''),
        [minTickets, maxTickets, minAge, maxAge],
    );

    const assignedKeys = useMemo(
        () => new Set(groups.flatMap(g => g.keys)),
        [groups],
    );

    useEffect(() => {
        const orKeys  = new Set(groups.filter(g => g.type === 'OR').flatMap(g => g.keys));
        const usedKeys = new Set(groups.flatMap(g => g.keys));

        const filledKeys: PolicyKey[] = [];
        if (minTickets !== '') filledKeys.push('minTickets');
        if (maxTickets !== '') filledKeys.push('maxTickets');
        if (minAge !== '') filledKeys.push('minAge');
        if (maxAge !== '') filledKeys.push('maxAge');
        const effectiveKeys = usedKeys.size === 0 && filledKeys.length === 1
            ? new Set<PolicyKey>([filledKeys[0]])
            : usedKeys;

        const minT = effectiveKeys.has('minTickets') && minTickets !== '' ? Number(minTickets) : null;
        const maxT = effectiveKeys.has('maxTickets') && maxTickets !== '' ? Number(maxTickets) : null;
        const minA = effectiveKeys.has('minAge')     && minAge     !== '' ? Number(minAge)     : null;
        const maxA = effectiveKeys.has('maxAge')     && maxAge     !== '' ? Number(maxAge)     : null;

        const hasTickets = minT !== null || maxT !== null;
        const hasAge     = minA !== null || maxA !== null;

        const hasAndError = groups.some(g => {
            if (g.type !== 'AND') return false;
            if (g.keys.includes('minTickets') && g.keys.includes('maxTickets')
                && minTickets !== '' && maxTickets !== '' && Number(minTickets) > Number(maxTickets)) return true;
            if (g.keys.includes('minAge') && g.keys.includes('maxAge')
                && minAge !== '' && maxAge !== '' && Number(minAge) > Number(maxAge)) return true;
            return false;
        });

        onChange({
            minTickets: minT,
            maxTickets: maxT,
            isQuantityOr: orKeys.has('minTickets') || orKeys.has('maxTickets'),
            minAge: minA,
            maxAge: maxA,
            isAgeOr: orKeys.has('minAge') || orKeys.has('maxAge'),
            isAgeAndQuantityOr: hasTickets && hasAge && groups.length === 2 && groupCombine === 'OR',
        }, hasAndError);
    }, [minTickets, maxTickets, minAge, maxAge, groups, groupCombine]);

    const addGroup = (type: 'AND' | 'OR') => {
        if (groups.some(g => g.type === type)) return;
        setGroups(prev => [...prev, { id: `${type}-${Date.now()}`, type, keys: [] }]);
    };
    const removeGroup = (id: string) => setGroups(prev => prev.filter(g => g.id !== id));
    const toggleKey = (groupId: string, key: PolicyKey) => {
        const inOther = assignedKeys.has(key) && !groups.find(g => g.id === groupId)?.keys.includes(key);
        if (inOther) return;
        setGroups(prev => prev.map(g => {
            if (g.id !== groupId) return g;
            const has = g.keys.includes(key);
            return { ...g, keys: has ? g.keys.filter(k => k !== key) : [...g.keys, key] };
        }));
    };

    function groupWarning(group: PolicyGroup): string | null {
        if (group.type !== 'AND') return null;
        if (group.keys.includes('minTickets') && group.keys.includes('maxTickets')
            && minTickets !== '' && maxTickets !== '' && Number(minTickets) > Number(maxTickets))
            return `Min tickets (${minTickets}) > Max tickets (${maxTickets}) — AND would always fail.`;
        if (group.keys.includes('minAge') && group.keys.includes('maxAge')
            && minAge !== '' && maxAge !== '' && Number(minAge) > Number(maxAge))
            return `Min age (${minAge}) > Max age (${maxAge}) — AND would always fail.`;
        return null;
    }

    const hasAnd = groups.some(g => g.type === 'AND');
    const hasOr  = groups.some(g => g.type === 'OR');

    return (
        <>
            {/* Step 1 – values */}
            <div className="space-y-3">
                <p className={sectionHdr}>
                    <span className="material-symbols-outlined text-[14px]">tune</span>
                    Step 1 — Set Policy Values
                </p>
                <div className="grid grid-cols-2 gap-3">
                    {(['minTickets','maxTickets','minAge','maxAge'] as const).map(key => {
                        const meta = POLICY_META.find(m => m.key === key)!;
                        const setter = key === 'minTickets' ? setMinTickets
                            : key === 'maxTickets' ? setMaxTickets
                            : key === 'minAge' ? setMinAge : setMaxAge;
                        return (
                            <div key={key} className="space-y-1">
                                <label className={labelCls}>{meta.label}</label>
                                <input className={inputCls} type="number" min={0}
                                    placeholder="no limit" value={vals[key]}
                                    onChange={e => setter(e.target.value)} />
                            </div>
                        );
                    })}
                </div>
            </div>

            {/* Step 2 – groups */}
            <div className="border-t border-gray-800 pt-4 space-y-3">
                <p className={sectionHdr}>
                    <span className="material-symbols-outlined text-[14px]">account_tree</span>
                    Step 2 — Assign Values to AND / OR Groups
                </p>

                {groups.length === 0 && (
                    available.length === 1
                        ? <p className="text-[11px] text-green-500 font-mono">
                            One policy — no AND/OR group needed.
                          </p>
                        : available.length >= 2
                            ? <p className="text-[11px] text-red-400 font-mono">
                                {available.length} policies set — you must assign them to AND/OR groups below.
                              </p>
                            : <p className="text-[11px] text-gray-600 font-mono">
                                Add a group below, then pick which values belong to it.
                                A rule is only active when assigned to a group.
                              </p>
                )}

                {groups.map(group => {
                    const warn = groupWarning(group);
                    return (
                        <div key={group.id} className={`rounded-xl border p-3.5 space-y-2.5 ${
                            group.type === 'AND' ? 'border-[#2563eb]/25' : 'border-amber-400/25'}`}>
                            <div className="flex items-center justify-between">
                                <span className={`text-xs font-black tracking-widest px-2.5 py-1 rounded-md ${
                                    group.type === 'AND'
                                        ? 'bg-[#2563eb]/15 text-[#60a5fa]'
                                        : 'bg-amber-400/15 text-amber-300'}`}>{group.type} GROUP</span>
                                <button type="button" onClick={() => removeGroup(group.id)}
                                    className="text-gray-600 hover:text-red-400 transition-colors">
                                    <span className="material-symbols-outlined text-[18px]">delete</span>
                                </button>
                            </div>
                            <p className="text-[10px] font-mono text-gray-600">
                                {group.type === 'AND' ? 'All checked rules must pass.' : 'At least one checked rule must pass.'}
                            </p>
                            <div className="space-y-2">
                                {available.length === 0 && <p className="text-[11px] text-gray-500">Fill in values in Step 1 first.</p>}
                                {available.map(({ key, label }) => {
                                    const inThis  = group.keys.includes(key);
                                    const inOther = !inThis && assignedKeys.has(key);
                                    return (
                                        <label key={key} className={`flex items-center gap-2.5 select-none ${
                                            inOther ? 'opacity-35 cursor-not-allowed' : 'cursor-pointer'}`}>
                                            <input type="checkbox"
                                                className="w-3.5 h-3.5 rounded"
                                                style={{ accentColor: group.type === 'AND' ? '#2563eb' : '#fbbf24' }}
                                                checked={inThis} disabled={inOther}
                                                onChange={() => toggleKey(group.id, key)} />
                                            <span className="text-xs text-gray-300">
                                                {label}
                                                <span className="text-gray-500 ml-1.5 font-mono">({vals[key]})</span>
                                            </span>
                                        </label>
                                    );
                                })}
                            </div>
                            {warn && (
                                <p className="text-amber-300 text-xs bg-amber-400/10 border border-amber-400/20 rounded-lg px-3 py-2 flex items-start gap-1.5">
                                    <span className="material-symbols-outlined text-[13px] mt-0.5 flex-shrink-0">warning</span>
                                    {warn}
                                </p>
                            )}
                        </div>
                    );
                })}

                {available.length > 0 && (
                    <div className="flex gap-2">
                        <button type="button" disabled={hasAnd} onClick={() => addGroup('AND')}
                            className={`flex-1 py-2 text-xs font-bold rounded-lg border transition-colors flex items-center justify-center gap-1.5 ${
                                hasAnd ? 'opacity-30 cursor-not-allowed border-gray-800 text-gray-700'
                                       : 'bg-[#2563eb]/10 border-[#2563eb]/30 text-[#60a5fa] hover:bg-[#2563eb]/20'}`}>
                            <span className="material-symbols-outlined text-[15px]">add</span>Add AND Group
                        </button>
                        <button type="button" disabled={hasOr} onClick={() => addGroup('OR')}
                            className={`flex-1 py-2 text-xs font-bold rounded-lg border transition-colors flex items-center justify-center gap-1.5 ${
                                hasOr ? 'opacity-30 cursor-not-allowed border-gray-800 text-gray-700'
                                      : 'bg-amber-400/10 border-amber-400/30 text-amber-300 hover:bg-amber-400/20'}`}>
                            <span className="material-symbols-outlined text-[15px]">add</span>Add OR Group
                        </button>
                    </div>
                )}

                {groups.length === 2 && (
                    <div className="space-y-2 pt-1">
                        <p className={labelCls}>Combine both groups with</p>
                        <div className="flex gap-2">
                            <button type="button" onClick={() => setGroupCombine('AND')}
                                className={`flex-1 py-2 text-xs font-bold rounded-lg border transition-colors ${
                                    groupCombine === 'AND'
                                        ? 'bg-[#2563eb]/15 border-[#2563eb]/40 text-[#60a5fa]'
                                        : 'bg-[#0b1326] border-gray-700 text-gray-500 hover:border-gray-500'}`}>
                                AND — must pass both groups
                            </button>
                            <button type="button" onClick={() => setGroupCombine('OR')}
                                className={`flex-1 py-2 text-xs font-bold rounded-lg border transition-colors ${
                                    groupCombine === 'OR'
                                        ? 'bg-amber-400/15 border-amber-400/40 text-amber-300'
                                        : 'bg-[#0b1326] border-gray-700 text-gray-500 hover:border-gray-500'}`}>
                                OR — passing either group is enough
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </>
    );
}

// ─── Page ──────────────────────────────────────────────────────────────────────

export default function CompanyPurchasePolicyPage() {
    const { companyId } = useParams<{ companyId: string }>();
    const numericId = Number(companyId);
    const navigate = useNavigate();

    const [currentDTO, setCurrentDTO] = useState<PurchasePolicyDTO | null>(null);
    const [policyDTO, setPolicyDTO] = useState<PurchasePolicyDTO>({ isQuantityOr: false, isAgeOr: false, isAgeAndQuantityOr: false });
    const [policyHasError, setPolicyHasError] = useState(false);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [toast, setToast] = useState<{ msg: string; ok: boolean } | null>(null);

    const showToast = (msg: string, ok = true) => {
        setToast({ msg, ok });
        setTimeout(() => setToast(null), 3000);
    };

    const fetchPolicy = useCallback(async () => {
        setLoading(true);
        try {
            const dto = await getCompanyPolicyDTO(numericId);
            setCurrentDTO(dto);
        } catch {
            // no policy yet is fine
        }
        setLoading(false);
    }, [numericId]);

    useEffect(() => { fetchPolicy(); }, [fetchPolicy]);

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true); setError(null);
        try {
            await setCompanyPolicyDTO(numericId, policyDTO);
            showToast('Company purchase policy saved!');
            await fetchPolicy();
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to save policy');
        }
        setSaving(false);
    };

    const hasCurrentPolicy = currentDTO && (
        currentDTO.minAge != null || currentDTO.maxAge != null ||
        currentDTO.minTickets != null || currentDTO.maxTickets != null
    );

    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen font-sans">

            {/* Toast */}
            {toast && (
                <div className={`fixed top-4 right-4 z-50 px-5 py-3 rounded-xl shadow-xl text-sm font-bold tracking-wide flex items-center gap-2 ${toast.ok ? 'bg-green-600' : 'bg-red-600'} text-white`}>
                    <span className="material-symbols-outlined text-[18px]">{toast.ok ? 'check_circle' : 'error'}</span>
                    {toast.msg}
                </div>
            )}

            {/* Header */}
            <div className="bg-[#eeefff] text-[#171f33] px-6 md:px-10 py-5 flex items-center gap-4">
                <button onClick={() => navigate(`/company/${companyId}`)}
                    className="flex items-center gap-1.5 text-xs font-bold text-gray-500 hover:text-[#2563eb] transition-colors">
                    <span className="material-symbols-outlined text-[20px]">arrow_back</span>
                    <span className="hidden sm:inline tracking-wider">BACK TO COMPANY</span>
                </button>
                <div className="w-px h-8 bg-gray-300" />
                <div className="flex items-center gap-2.5">
                    <span className="material-symbols-outlined text-[#2563eb] text-3xl">policy</span>
                    <div>
                        <h1 className="text-2xl font-black tracking-tight text-[#0b1326]">COMPANY PURCHASE POLICY</h1>
                        <p className="text-xs text-gray-500 font-mono mt-0.5">Applies to all events of this company</p>
                    </div>
                </div>
            </div>

            {/* Content */}
            <div className="p-4 md:p-8 max-w-2xl mx-auto space-y-6">

                {/* Current policy banner */}
                {!loading && hasCurrentPolicy && (
                    <div className="bg-[#171f33] border border-[#2563eb]/30 rounded-2xl p-5">
                        <p className="text-[10px] font-mono font-bold text-[#2563eb] uppercase tracking-widest mb-3 flex items-center gap-1.5">
                            <span className="material-symbols-outlined text-[14px]">shield_check</span>
                            Active Policy
                        </p>
                        <div className="grid grid-cols-2 gap-2 text-xs font-mono">
                            {currentDTO!.minAge     != null && <span className="text-gray-300">Min age: <strong className="text-white">{currentDTO!.minAge}</strong></span>}
                            {currentDTO!.maxAge     != null && <span className="text-gray-300">Max age: <strong className="text-white">{currentDTO!.maxAge}</strong></span>}
                            {currentDTO!.minTickets != null && <span className="text-gray-300">Min tickets: <strong className="text-white">{currentDTO!.minTickets}</strong></span>}
                            {currentDTO!.maxTickets != null && <span className="text-gray-300">Max tickets: <strong className="text-white">{currentDTO!.maxTickets}</strong></span>}
                        </div>
                        {(currentDTO!.isAgeOr || currentDTO!.isQuantityOr || currentDTO!.isAgeAndQuantityOr) && (
                            <p className="text-[10px] text-gray-500 font-mono mt-2">
                                Composition: {currentDTO!.isAgeAndQuantityOr ? 'Age OR Quantity' : currentDTO!.isAgeOr ? 'Age rules combined with OR' : 'Quantity rules combined with OR'}
                            </p>
                        )}
                    </div>
                )}

                {!loading && !hasCurrentPolicy && (
                    <div className="bg-amber-500/5 border border-amber-500/20 rounded-xl px-4 py-3">
                        <p className="text-xs text-amber-300 font-mono">No company purchase policy set yet. Define one below.</p>
                    </div>
                )}

                {/* Edit form */}
                {loading ? (
                    <div className="flex justify-center py-16">
                        <span className="material-symbols-outlined animate-spin text-3xl text-[#2563eb]">refresh</span>
                    </div>
                ) : (
                    <form onSubmit={handleSave} className="bg-[#171f33] border border-gray-800 rounded-2xl p-6 space-y-5">
                        <p className="text-[10px] font-mono font-bold text-[#2563eb] uppercase tracking-widest flex items-center gap-1.5 mb-1">
                            <span className="material-symbols-outlined text-[14px]">edit</span>
                            {hasCurrentPolicy ? 'Replace Policy' : 'Set Policy'}
                        </p>

                        <PolicyBuilder initialDTO={currentDTO} onChange={(dto, hasError) => { setPolicyDTO(dto); setPolicyHasError(hasError); }} />

                        <p className="text-[10px] text-gray-600 border-t border-gray-800 pt-3">
                            Saving replaces the existing company policy. Leave all fields empty to clear the policy.
                            This policy will apply to all events as a baseline — each event can add its own rules on top.
                        </p>

                        {error && (
                            <p className="text-red-400 text-xs bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">{error}</p>
                        )}

                        {policyHasError && (
                            <p className="text-red-400 text-xs bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">Fix the AND group errors above before saving.</p>
                        )}
                        <button type="submit" disabled={saving || policyHasError}
                            className="w-full py-3 bg-[#2563eb] hover:bg-[#0053db] text-white font-bold text-sm rounded-xl transition-colors flex items-center justify-center gap-2 disabled:opacity-60">
                            {saving && <span className="material-symbols-outlined animate-spin text-[18px]">refresh</span>}
                            SAVE POLICY
                        </button>
                    </form>
                )}
            </div>
        </div>
    );
}

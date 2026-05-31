import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { eventApi, EventDTO, PurchasePolicyDTO } from '../../api/eventsApi';
import { getRolesTree, getPurchaseHistory, HistoryOrderItem } from '../../api/productionCompanyApi';
import { getCompanyPolicy } from '../../api/purchasePoliciesApi';

function formatDate(iso: string) {
    try {
        return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    } catch { return iso; }
}

function toDatetimeLocal(iso: string) {
    try { return new Date(iso).toISOString().slice(0, 16); } catch { return ''; }
}

// ─── Per-event stats ──────────────────────────────────────────────────────────

interface EventStats {
    ticketsSold: number;
    historyRevenue: number;
}

function buildStatsMap(history: HistoryOrderItem[]): Record<string, EventStats> {
    const map: Record<string, EventStats> = {};
    for (const order of history) {
        const id = order.eventId;
        if (!id) continue;
        if (!map[id]) map[id] = { ticketsSold: 0, historyRevenue: 0 };
        const seats = order.seatIds?.length ?? 0;
        const standing = Object.values(order.standingAreaQuantities ?? {}).reduce((a, b) => a + b, 0);
        map[id].ticketsSold += seats + standing;
        map[id].historyRevenue += order.price ?? 0;
    }
    return map;
}

// ─── Shared styles ────────────────────────────────────────────────────────────

const inputCls = 'w-full bg-[#0b1326] border border-gray-600 rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[#00dbe7] transition-colors';
const labelCls = 'text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400';
const sectionHdr = 'flex items-center gap-1.5 text-[10px] font-mono font-bold uppercase tracking-widest text-[#00dbe7] mb-3';

// ─── Modal shell ──────────────────────────────────────────────────────────────

function Modal({ title, icon, onClose, onSubmit, submitLabel, loading, error, children }: {
    title: string; icon: string; onClose: () => void;
    onSubmit: (e: React.FormEvent) => void;
    submitLabel?: string;
    loading: boolean; error: string | null; children: React.ReactNode;
}) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
            <div className="bg-[#171f33] border border-gray-700 rounded-2xl w-full max-w-lg shadow-2xl max-h-[90vh] flex flex-col">
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-700 flex-shrink-0">
                    <h3 className="font-black text-white tracking-wide text-sm flex items-center gap-2">
                        <span className="material-symbols-outlined text-[#00dbe7] text-[20px]">{icon}</span>
                        {title}
                    </h3>
                    <button onClick={onClose} className="text-gray-400 hover:text-white">
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>
                <form onSubmit={onSubmit} className="p-6 space-y-4 overflow-y-auto flex-1">
                    {children}
                    {error && <p className="text-red-400 text-xs bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">{error}</p>}
                    <button type="submit" disabled={loading}
                        className="w-full py-3 bg-[#00dbe7] hover:bg-[#00bfc9] text-[#0b1326] font-bold text-sm rounded-xl transition-colors flex items-center justify-center gap-2 disabled:opacity-60">
                        {loading && <span className="material-symbols-outlined animate-spin text-[18px]">refresh</span>}
                        {submitLabel ?? 'CONFIRM'}
                    </button>
                </form>
            </div>
        </div>
    );
}

// ─── Toggle row ───────────────────────────────────────────────────────────────

function ToggleRow({ label, checked, onChange, hint }: { label: string; checked: boolean; onChange: (v: boolean) => void; hint?: string }) {
    return (
        <label className="flex items-start gap-2.5 cursor-pointer group">
            <div className="relative mt-0.5 flex-shrink-0">
                <input type="checkbox" className="sr-only" checked={checked} onChange={e => onChange(e.target.checked)} />
                <div className={`w-8 h-4 rounded-full transition-colors ${checked ? 'bg-[#00dbe7]' : 'bg-gray-700'}`} />
                <div className={`absolute top-0.5 left-0.5 w-3 h-3 rounded-full bg-white shadow transition-transform ${checked ? 'translate-x-4' : ''}`} />
            </div>
            <div>
                <span className="text-xs text-gray-300 group-hover:text-white transition-colors">{label}</span>
                {hint && <p className="text-[10px] text-gray-600 mt-0.5">{hint}</p>}
            </div>
        </label>
    );
}

// ─── Policy section (reused in create + edit) ─────────────────────────────────

function PolicySection({
    minTickets, setMinTickets,
    maxTickets, setMaxTickets,
    isQuantityOr, setIsQuantityOr,
    minAge, setMinAge,
    maxAge, setMaxAge,
    isAgeOr, setIsAgeOr,
    isAgeAndQuantityOr, setIsAgeAndQuantityOr,
}: {
    minTickets: string; setMinTickets: (v: string) => void;
    maxTickets: string; setMaxTickets: (v: string) => void;
    isQuantityOr: boolean; setIsQuantityOr: (v: boolean) => void;
    minAge: string; setMinAge: (v: string) => void;
    maxAge: string; setMaxAge: (v: string) => void;
    isAgeOr: boolean; setIsAgeOr: (v: boolean) => void;
    isAgeAndQuantityOr: boolean; setIsAgeAndQuantityOr: (v: boolean) => void;
}) {
    const hasAge = minAge !== '' || maxAge !== '';
    return (
        <>
            <div className="border-t border-gray-800 pt-4">
                <p className={sectionHdr}>
                    <span className="material-symbols-outlined text-[14px]">confirmation_number</span>
                    Ticket Quantity Policy
                </p>
                <div className="grid grid-cols-2 gap-3 mb-3">
                    <div className="space-y-1">
                        <label className={labelCls}>Min Tickets / User</label>
                        <input className={inputCls} type="number" min={0} placeholder="0 = no limit"
                            value={minTickets} onChange={e => setMinTickets(e.target.value)} />
                    </div>
                    <div className="space-y-1">
                        <label className={labelCls}>Max Tickets / User</label>
                        <input className={inputCls} type="number" min={1} placeholder="e.g. 10"
                            value={maxTickets} onChange={e => setMaxTickets(e.target.value)} />
                    </div>
                </div>
                <ToggleRow label="Quantity uses OR logic" checked={isQuantityOr} onChange={setIsQuantityOr}
                    hint="OFF = buyer must satisfy BOTH min AND max. ON = satisfying either is enough." />
            </div>

            <div className="border-t border-gray-800 pt-4">
                <p className={sectionHdr}>
                    <span className="material-symbols-outlined text-[14px]">person</span>
                    Age Policy <span className="text-gray-600 normal-case font-normal ml-1">(optional)</span>
                </p>
                <div className="grid grid-cols-2 gap-3 mb-3">
                    <div className="space-y-1">
                        <label className={labelCls}>Min Age</label>
                        <input className={inputCls} type="number" min={0} max={120} placeholder="e.g. 18"
                            value={minAge} onChange={e => setMinAge(e.target.value)} />
                    </div>
                    <div className="space-y-1">
                        <label className={labelCls}>Max Age</label>
                        <input className={inputCls} type="number" min={0} max={120} placeholder="e.g. 65"
                            value={maxAge} onChange={e => setMaxAge(e.target.value)} />
                    </div>
                </div>
                <ToggleRow label="Age uses OR logic" checked={isAgeOr} onChange={setIsAgeOr}
                    hint="OFF = buyer must be within BOTH min AND max age. ON = either condition is enough." />
            </div>

            {hasAge && (
                <div className="border-t border-gray-800 pt-4">
                    <p className={sectionHdr}>
                        <span className="material-symbols-outlined text-[14px]">merge</span>
                        Policy Composition
                    </p>
                    <ToggleRow label="Age OR Quantity (outer)" checked={isAgeAndQuantityOr} onChange={setIsAgeAndQuantityOr}
                        hint="OFF = buyer must pass BOTH age AND quantity rules. ON = passing either block is enough." />
                </div>
            )}
        </>
    );
}

// ─── Create modal ─────────────────────────────────────────────────────────────

function CreateEventModal({ companyId, onClose, onCreated }: {
    companyId: number; onClose: () => void; onCreated: () => void;
}) {
    const token = localStorage.getItem('token') || '';

    const [eventName, setEventName] = useState('');
    const [capacity, setCapacity] = useState('');
    const [dateTime, setDateTime] = useState('');
    const [location, setLocation] = useState('');
    const [ticketPrice, setTicketPrice] = useState('');

    const [minTickets, setMinTickets] = useState('1');
    const [maxTickets, setMaxTickets] = useState('10');
    const [isQuantityOr, setIsQuantityOr] = useState(false);
    const [minAge, setMinAge] = useState('');
    const [maxAge, setMaxAge] = useState('');
    const [isAgeOr, setIsAgeOr] = useState(false);
    const [isAgeAndQuantityOr, setIsAgeAndQuantityOr] = useState(false);

    const [companyPolicyDesc, setCompanyPolicyDesc] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        getCompanyPolicy(companyId)
            .then(p => setCompanyPolicyDesc(p?.description ?? null))
            .catch(() => {});
    }, [companyId]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true); setError(null);
        try {
            const ok = await eventApi.createEvent(token, {
                event: {
                    companyId,
                    eventName,
                    eventCapacity: Number(capacity),
                    eventDateTime: new Date(dateTime).toISOString().slice(0, 19),
                    eventLocation: location || null,
                    ticketPrice: ticketPrice !== '' ? Number(ticketPrice) : null,
                },
                purchasePolicy: {
                    minTickets: Number(minTickets),
                    maxTickets: Number(maxTickets),
                    isQuantityOr,
                    minAge: minAge !== '' ? Number(minAge) : null,
                    maxAge: maxAge !== '' ? Number(maxAge) : null,
                    isAgeOr,
                    isAgeAndQuantityOr: (minAge !== '' || maxAge !== '') ? isAgeAndQuantityOr : false,
                },
            });
            if (!ok) throw new Error('Failed to create event');
            onCreated();
        } catch (err) { setError(err instanceof Error ? err.message : 'Failed to create event'); }
        setLoading(false);
    };

    return (
        <Modal title="CREATE NEW EVENT" icon="event" onClose={onClose} onSubmit={handleSubmit}
            submitLabel="CREATE EVENT" loading={loading} error={error}>

            <div>
                <p className={sectionHdr}>
                    <span className="material-symbols-outlined text-[14px]">info</span>
                    Event Details
                </p>
                <div className="space-y-3">
                    <div className="space-y-1">
                        <label className={labelCls}>Event Name</label>
                        <input className={inputCls} placeholder="e.g. Summer Music Festival"
                            value={eventName} onChange={e => setEventName(e.target.value)} required />
                    </div>
                    <div className="space-y-1">
                        <label className={labelCls}>Location</label>
                        <input className={inputCls} placeholder="e.g. Tel Aviv, Yarkon Park"
                            value={location} onChange={e => setLocation(e.target.value)} />
                    </div>
                    <div className="space-y-1">
                        <label className={labelCls}>Date & Time</label>
                        <input className={inputCls} type="datetime-local"
                            value={dateTime} onChange={e => setDateTime(e.target.value)} required />
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1">
                            <label className={labelCls}>Total Capacity</label>
                            <input className={inputCls} type="number" min={1} placeholder="e.g. 500"
                                value={capacity} onChange={e => setCapacity(e.target.value)} required />
                        </div>
                        <div className="space-y-1">
                            <label className={labelCls}>Ticket Price ($)</label>
                            <input className={inputCls} type="number" min={0} step="0.01" placeholder="e.g. 49.99"
                                value={ticketPrice} onChange={e => setTicketPrice(e.target.value)} />
                        </div>
                    </div>
                </div>
            </div>

            <PolicySection
                minTickets={minTickets} setMinTickets={setMinTickets}
                maxTickets={maxTickets} setMaxTickets={setMaxTickets}
                isQuantityOr={isQuantityOr} setIsQuantityOr={setIsQuantityOr}
                minAge={minAge} setMinAge={setMinAge}
                maxAge={maxAge} setMaxAge={setMaxAge}
                isAgeOr={isAgeOr} setIsAgeOr={setIsAgeOr}
                isAgeAndQuantityOr={isAgeAndQuantityOr} setIsAgeAndQuantityOr={setIsAgeAndQuantityOr}
            />

            {companyPolicyDesc && (
                <div className="border-t border-gray-800 pt-4">
                    <p className={sectionHdr}>
                        <span className="material-symbols-outlined text-[14px]">domain</span>
                        Company Policy (applies to all events)
                    </p>
                    <div className="bg-amber-500/5 border border-amber-500/20 rounded-lg px-3 py-2.5">
                        <p className="text-xs text-amber-300 font-mono">{companyPolicyDesc}</p>
                        <p className="text-[10px] text-gray-600 mt-1">Event policy is applied on top of this.</p>
                    </div>
                </div>
            )}
        </Modal>
    );
}

// ─── Edit modal ───────────────────────────────────────────────────────────────

function EditEventModal({ event, onClose, onSaved }: {
    event: EventDTO; onClose: () => void; onSaved: () => void;
}) {
    const token = localStorage.getItem('token') || '';

    const [dateTime, setDateTime] = useState(event.eventDateTime ? toDatetimeLocal(event.eventDateTime) : '');
    const [capacity, setCapacity] = useState(String(event.eventCapacity));
    const [location, setLocation] = useState(event.eventLocation ?? '');
    const [ticketPrice, setTicketPrice] = useState(event.ticketPrice != null ? String(event.ticketPrice) : '');

    const [minTickets, setMinTickets] = useState('1');
    const [maxTickets, setMaxTickets] = useState('10');
    const [isQuantityOr, setIsQuantityOr] = useState(false);
    const [minAge, setMinAge] = useState('');
    const [maxAge, setMaxAge] = useState('');
    const [isAgeOr, setIsAgeOr] = useState(false);
    const [isAgeAndQuantityOr, setIsAgeAndQuantityOr] = useState(false);
    const [updatePolicy, setUpdatePolicy] = useState(false);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true); setError(null);
        try {
            const eventId = event.eventId!;
            const ops: Promise<boolean>[] = [];

            if (dateTime !== toDatetimeLocal(event.eventDateTime ?? ''))
                ops.push(eventApi.editEventDate(token, eventId, { newDateTime: new Date(dateTime).toISOString().slice(0, 19) }));

            if (Number(capacity) !== event.eventCapacity)
                ops.push(eventApi.editEventCapacity(token, eventId, { newCapacity: Number(capacity) }));

            const newLoc = location || null;
            if (newLoc !== (event.eventLocation ?? null))
                ops.push(eventApi.editEventLocation(token, eventId, newLoc));

            const newPrice = ticketPrice !== '' ? Number(ticketPrice) : null;
            if (newPrice !== (event.ticketPrice ?? null))
                ops.push(eventApi.editEventPrice(token, eventId, newPrice));

            if (updatePolicy) {
                const policy: PurchasePolicyDTO = {
                    minTickets: Number(minTickets),
                    maxTickets: Number(maxTickets),
                    isQuantityOr,
                    minAge: minAge !== '' ? Number(minAge) : null,
                    maxAge: maxAge !== '' ? Number(maxAge) : null,
                    isAgeOr,
                    isAgeAndQuantityOr: (minAge !== '' || maxAge !== '') ? isAgeAndQuantityOr : false,
                };
                ops.push(eventApi.editEventPolicy(token, eventId, policy));
            }

            if (ops.length === 0) { onClose(); return; }

            const results = await Promise.all(ops);
            if (results.some(r => !r)) throw new Error('One or more updates failed');
            onSaved();
        } catch (err) { setError(err instanceof Error ? err.message : 'Failed to update event'); }
        setLoading(false);
    };

    return (
        <Modal title={`EDIT — ${event.eventName}`} icon="edit_calendar"
            onClose={onClose} onSubmit={handleSubmit} submitLabel="SAVE CHANGES" loading={loading} error={error}>

            {/* Basic info */}
            <div>
                <p className={sectionHdr}>
                    <span className="material-symbols-outlined text-[14px]">info</span>
                    Event Details
                </p>
                <div className="space-y-3">
                    <div className="space-y-1">
                        <label className={labelCls}>Location</label>
                        <input className={inputCls} placeholder="e.g. Tel Aviv, Yarkon Park"
                            value={location} onChange={e => setLocation(e.target.value)} />
                    </div>
                    <div className="space-y-1">
                        <label className={labelCls}>Date & Time</label>
                        <input className={inputCls} type="datetime-local"
                            value={dateTime} onChange={e => setDateTime(e.target.value)} required />
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                        <div className="space-y-1">
                            <label className={labelCls}>Total Capacity</label>
                            <input className={inputCls} type="number" min={1}
                                value={capacity} onChange={e => setCapacity(e.target.value)} required />
                        </div>
                        <div className="space-y-1">
                            <label className={labelCls}>Ticket Price ($)</label>
                            <input className={inputCls} type="number" min={0} step="0.01" placeholder="e.g. 49.99"
                                value={ticketPrice} onChange={e => setTicketPrice(e.target.value)} />
                        </div>
                    </div>
                </div>
            </div>

            {/* Purchase policy toggle */}
            <div className="border-t border-gray-800 pt-4">
                <button type="button" onClick={() => setUpdatePolicy(v => !v)}
                    className={`w-full flex items-center justify-between px-4 py-3 rounded-xl border text-sm font-bold transition-colors ${updatePolicy ? 'bg-[#00dbe7]/10 border-[#00dbe7]/30 text-[#00dbe7]' : 'bg-[#0b1326] border-gray-700 text-gray-400 hover:border-gray-500'}`}>
                    <span className="flex items-center gap-2">
                        <span className="material-symbols-outlined text-[18px]">policy</span>
                        Update Purchase Policy
                    </span>
                    <span className="material-symbols-outlined text-[18px]">{updatePolicy ? 'expand_less' : 'expand_more'}</span>
                </button>

                {updatePolicy && (
                    <div className="mt-3">
                        <PolicySection
                            minTickets={minTickets} setMinTickets={setMinTickets}
                            maxTickets={maxTickets} setMaxTickets={setMaxTickets}
                            isQuantityOr={isQuantityOr} setIsQuantityOr={setIsQuantityOr}
                            minAge={minAge} setMinAge={setMinAge}
                            maxAge={maxAge} setMaxAge={setMaxAge}
                            isAgeOr={isAgeOr} setIsAgeOr={setIsAgeOr}
                            isAgeAndQuantityOr={isAgeAndQuantityOr} setIsAgeAndQuantityOr={setIsAgeAndQuantityOr}
                        />
                    </div>
                )}
            </div>
        </Modal>
    );
}

// ─── Event card ───────────────────────────────────────────────────────────────

function EventCard({ event, stats, onEdit, onDelete }: {
    event: EventDTO;
    stats?: EventStats;
    onEdit: () => void;
    onDelete: () => void;
}) {
    const ticketsSold = stats?.ticketsSold ?? 0;
    const revenue = event.ticketPrice != null
        ? ticketsSold * event.ticketPrice
        : (stats?.historyRevenue ?? 0);
    const available = event.eventCapacity - ticketsSold;
    const soldPct = event.eventCapacity > 0 ? Math.min(100, Math.round((ticketsSold / event.eventCapacity) * 100)) : 0;

    return (
        <div className="bg-[#171f33] border border-gray-800 rounded-2xl p-5 flex flex-col gap-3">
            {/* Header row */}
            <div className="flex items-start justify-between">
                <div className="w-10 h-10 rounded-xl bg-[#00dbe7]/10 flex items-center justify-center flex-shrink-0">
                    <span className="material-symbols-outlined text-[22px] text-[#00dbe7]">event</span>
                </div>
                <span className={`text-[10px] font-mono font-bold px-2.5 py-1 rounded-full border ${event.isActive ? 'text-[#34d399] bg-emerald-500/10 border-emerald-500/20' : 'text-gray-500 bg-gray-500/10 border-gray-500/20'}`}>
                    {event.isActive ? 'ACTIVE' : 'INACTIVE'}
                </span>
            </div>

            {/* Name / date / location */}
            <div>
                <h3 className="text-white font-black text-base tracking-tight">{event.eventName}</h3>
                {event.eventDateTime && (
                    <p className="text-xs text-gray-500 font-mono mt-1 flex items-center gap-1">
                        <span className="material-symbols-outlined text-[13px]">schedule</span>
                        {formatDate(event.eventDateTime)}
                    </p>
                )}
                {event.eventLocation && (
                    <p className="text-xs text-gray-500 font-mono mt-0.5 flex items-center gap-1">
                        <span className="material-symbols-outlined text-[13px]">location_on</span>
                        {event.eventLocation}
                    </p>
                )}
            </div>

            {/* Ticket price */}
            {event.ticketPrice != null && (
                <div className="flex items-center gap-1.5 text-[11px] font-mono bg-[#00dbe7]/5 border border-[#00dbe7]/15 rounded-lg px-3 py-1.5">
                    <span className="material-symbols-outlined text-[13px] text-[#00dbe7]">local_activity</span>
                    <span className="text-gray-400">Price per ticket:</span>
                    <span className="text-white font-bold">${event.ticketPrice.toFixed(2)}</span>
                </div>
            )}

            {/* Capacity progress bar */}
            <div className="space-y-1.5">
                <div className="flex items-center justify-between text-[10px] font-mono text-gray-500">
                    <span>{ticketsSold} sold</span>
                    <span>{available} available</span>
                    <span>{event.eventCapacity} total</span>
                </div>
                <div className="w-full h-1.5 bg-gray-800 rounded-full overflow-hidden">
                    <div
                        className={`h-full rounded-full transition-all ${soldPct >= 90 ? 'bg-red-500' : soldPct >= 60 ? 'bg-amber-400' : 'bg-[#00dbe7]'}`}
                        style={{ width: `${soldPct}%` }}
                    />
                </div>
                <div className="text-[10px] font-mono text-gray-600 text-right">{soldPct}% sold</div>
            </div>

            {/* Revenue + ID */}
            <div className="flex items-center justify-between pt-1 border-t border-gray-800">
                <div className="flex items-center gap-1 text-[11px] font-mono text-gray-500">
                    <span className="material-symbols-outlined text-[13px]">payments</span>
                    <span className="text-[#34d399] font-bold">${revenue.toFixed(2)}</span>
                    <span>revenue</span>
                </div>
                {event.eventId && (
                    <span className="text-[10px] font-mono text-gray-600">ID: {event.eventId}</span>
                )}
            </div>

            {/* Actions */}
            <div className="flex gap-2 pt-1">
                <button onClick={onEdit}
                    className="flex-1 flex items-center justify-center gap-1.5 py-2 bg-[#00dbe7]/10 hover:bg-[#00dbe7]/20 text-[#00dbe7] text-xs font-bold rounded-lg transition-colors border border-[#00dbe7]/20">
                    <span className="material-symbols-outlined text-[15px]">edit</span>
                    EDIT
                </button>
                <button onClick={onDelete}
                    className="flex-1 flex items-center justify-center gap-1.5 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 text-xs font-bold rounded-lg transition-colors border border-red-500/20">
                    <span className="material-symbols-outlined text-[15px]">delete</span>
                    DELETE
                </button>
            </div>
        </div>
    );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function CompanyEventsPage() {
    const { companyId } = useParams<{ companyId: string }>();
    const numericId = Number(companyId);
    const navigate = useNavigate();
    const token = localStorage.getItem('token') || '';

    const [companyName, setCompanyName] = useState('');
    const [events, setEvents] = useState<EventDTO[]>([]);
    const [statsMap, setStatsMap] = useState<Record<string, EventStats>>({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showCreate, setShowCreate] = useState(false);
    const [editingEvent, setEditingEvent] = useState<EventDTO | null>(null);

    const fetchEvents = useCallback(async () => {
        setLoading(true);
        try {
            const [list, rolesTree, history] = await Promise.all([
                eventApi.getEventsByCompany(token, numericId),
                getRolesTree(numericId).catch(() => null),
                getPurchaseHistory(numericId).catch(() => [] as HistoryOrderItem[]),
            ]);
            setEvents(list);
            if (rolesTree?.companyName) setCompanyName(rolesTree.companyName);
            setStatsMap(buildStatsMap(history));
            setError(null);
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to load events');
        }
        setLoading(false);
    }, [token, numericId]);

    useEffect(() => { fetchEvents(); }, [fetchEvents]);

    const handleDelete = async (event: EventDTO) => {
        if (!window.confirm(`Delete event "${event.eventName}"?`)) return;
        const ok = await eventApi.removeEvent(token, event.eventId!);
        if (ok) fetchEvents();
        else alert('Failed to delete event');
    };

    const totalRevenue = events.reduce((sum, ev) => {
        const stats = ev.eventId ? statsMap[ev.eventId] : undefined;
        const sold = stats?.ticketsSold ?? 0;
        return sum + (ev.ticketPrice != null ? sold * ev.ticketPrice : (stats?.historyRevenue ?? 0));
    }, 0);
    const totalTicketsSold = Object.values(statsMap).reduce((s, st) => s + st.ticketsSold, 0);

    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen font-sans">
            {/* Header */}
            <div className="bg-[#eeefff] text-[#171f33] px-6 md:px-10 py-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                    <button onClick={() => navigate(`/company/${companyId}`)}
                        className="flex items-center gap-1.5 text-xs font-bold text-gray-500 hover:text-[#2563eb] transition-colors">
                        <span className="material-symbols-outlined text-[20px]">arrow_back</span>
                        <span className="hidden sm:inline tracking-wider">BACK TO COMPANY</span>
                    </button>
                    <div className="w-px h-8 bg-gray-300" />
                    <div>
                        <div className="flex items-center gap-2.5 mb-1">
                            <span className="material-symbols-outlined text-[#00dbe7] text-3xl">event</span>
                            <h1 className="text-2xl font-black tracking-tight text-[#0b1326]">EVENTS</h1>
                        </div>
                        <p className="text-sm text-gray-500 font-mono">
                            {companyName || `Company #${companyId}`} · {events.length} event{events.length !== 1 ? 's' : ''}
                        </p>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    {events.length > 0 && (
                        <div className="hidden md:flex gap-3">
                            <div className="bg-[#1a2340] rounded-xl px-4 py-2 min-w-[80px] text-center">
                                <p className="text-lg font-black text-[#00dbe7]">{totalTicketsSold}</p>
                                <p className="text-[9px] font-mono text-gray-400 tracking-widest">TICKETS SOLD</p>
                            </div>
                            <div className="bg-[#1a2340] rounded-xl px-4 py-2 min-w-[80px] text-center">
                                <p className="text-lg font-black text-[#34d399]">${totalRevenue.toFixed(0)}</p>
                                <p className="text-[9px] font-mono text-gray-400 tracking-widest">TOTAL REVENUE</p>
                            </div>
                        </div>
                    )}
                    <button onClick={() => setShowCreate(true)}
                        className="flex items-center gap-2 bg-[#00dbe7] hover:bg-[#00bfc9] text-[#0b1326] px-5 py-2.5 rounded-xl font-bold text-sm tracking-wider transition-colors">
                        <span className="material-symbols-outlined text-[18px]">add</span>
                        NEW EVENT
                    </button>
                </div>
            </div>

            {/* Content */}
            <div className="p-4 md:p-8 max-w-5xl mx-auto">
                {loading ? (
                    <div className="flex justify-center py-20">
                        <span className="material-symbols-outlined animate-spin text-3xl text-[#00dbe7]">refresh</span>
                    </div>
                ) : error ? (
                    <div className="bg-[#171f33] border border-red-500/30 p-10 rounded-2xl text-center">
                        <span className="material-symbols-outlined text-5xl text-red-500 mb-3 block">error</span>
                        <p className="text-sm text-gray-400">{error}</p>
                    </div>
                ) : events.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-24 text-center">
                        <span className="material-symbols-outlined text-7xl text-gray-700 mb-4">event_busy</span>
                        <h2 className="text-xl font-black text-gray-400 mb-2">No events yet</h2>
                        <p className="text-sm text-gray-600 font-mono mb-8">Create your first event for this company.</p>
                        <button onClick={() => setShowCreate(true)}
                            className="flex items-center gap-2 bg-[#00dbe7] hover:bg-[#00bfc9] text-[#0b1326] px-6 py-3 rounded-xl font-bold text-sm tracking-wider transition-colors">
                            <span className="material-symbols-outlined text-[18px]">add</span>
                            CREATE EVENT
                        </button>
                    </div>
                ) : (
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {events.map((event, i) => (
                            <EventCard
                                key={event.eventId ?? i}
                                event={event}
                                stats={event.eventId ? statsMap[event.eventId] : undefined}
                                onEdit={() => setEditingEvent(event)}
                                onDelete={() => handleDelete(event)}
                            />
                        ))}
                    </div>
                )}
            </div>

            {showCreate && (
                <CreateEventModal
                    companyId={numericId}
                    onClose={() => setShowCreate(false)}
                    onCreated={() => { setShowCreate(false); fetchEvents(); }}
                />
            )}

            {editingEvent && (
                <EditEventModal
                    event={editingEvent}
                    onClose={() => setEditingEvent(null)}
                    onSaved={() => { setEditingEvent(null); fetchEvents(); }}
                />
            )}
        </div>
    );
}

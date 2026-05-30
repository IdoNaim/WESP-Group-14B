import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { eventApi, EventDTO } from '../../api/eventsApi';
import { getRolesTree } from '../../api/productionCompanyApi';
import { getCompanyPolicy } from '../../api/purchasePoliciesApi';

function formatDate(iso: string) {
    try {
        return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    } catch { return iso; }
}

function toDatetimeLocal(iso: string) {
    try { return new Date(iso).toISOString().slice(0, 16); } catch { return ''; }
}

// ─── Modal shell ──────────────────────────────────────────────────────────────

function Modal({ title, icon, onClose, onSubmit, loading, error, children, wide }: {
    title: string; icon: string; onClose: () => void;
    onSubmit: (e: React.FormEvent) => void;
    loading: boolean; error: string | null; children: React.ReactNode;
    wide?: boolean;
}) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
            <div className={`bg-[#171f33] border border-gray-700 rounded-2xl w-full ${wide ? 'max-w-lg' : 'max-w-md'} shadow-2xl max-h-[90vh] flex flex-col`}>
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
                        CONFIRM
                    </button>
                </form>
            </div>
        </div>
    );
}

// ─── Shared field styles ──────────────────────────────────────────────────────

const inputCls = 'w-full bg-[#0b1326] border border-gray-600 rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[#00dbe7] transition-colors';
const labelCls = 'text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400';
const sectionHeaderCls = 'flex items-center gap-1.5 text-[10px] font-mono font-bold uppercase tracking-widest text-[#00dbe7] mb-3';

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

// ─── Create modal ─────────────────────────────────────────────────────────────

function CreateEventModal({ companyId, onClose, onCreated }: {
    companyId: number; onClose: () => void; onCreated: () => void;
}) {
    const token = localStorage.getItem('token') || '';

    // Event details
    const [eventName, setEventName] = useState('');
    const [capacity, setCapacity] = useState('');
    const [dateTime, setDateTime] = useState('');

    // Quantity policy
    const [minTickets, setMinTickets] = useState('1');
    const [maxTickets, setMaxTickets] = useState('10');
    const [isQuantityOr, setIsQuantityOr] = useState(false);

    // Age policy
    const [minAge, setMinAge] = useState('');
    const [maxAge, setMaxAge] = useState('');
    const [isAgeOr, setIsAgeOr] = useState(false);

    // Outer composition (age block vs quantity block)
    const [isAgeAndQuantityOr, setIsAgeAndQuantityOr] = useState(false);

    // Company policy reference
    const [companyPolicyDesc, setCompanyPolicyDesc] = useState<string | null>(null);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        getCompanyPolicy(companyId)
            .then(p => setCompanyPolicyDesc(p?.description ?? null))
            .catch(() => {/* non-critical */ });
    }, [companyId]);

    const hasAgePolicy = minAge !== '' || maxAge !== '';

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
                },
                purchasePolicy: {
                    minTickets: Number(minTickets),
                    maxTickets: Number(maxTickets),
                    isQuantityOr,
                    minAge: minAge !== '' ? Number(minAge) : null,
                    maxAge: maxAge !== '' ? Number(maxAge) : null,
                    isAgeOr,
                    isAgeAndQuantityOr: hasAgePolicy ? isAgeAndQuantityOr : false,
                },
            });
            if (!ok) throw new Error('Failed to create event');
            onCreated();
        } catch (err) { setError(err instanceof Error ? err.message : 'Failed to create event'); }
        setLoading(false);
    };

    return (
        <Modal title="CREATE NEW EVENT" icon="event" onClose={onClose} onSubmit={handleSubmit} loading={loading} error={error} wide>
            {/* ── Event details ── */}
            <div>
                <p className={sectionHeaderCls}>
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
                        <label className={labelCls}>Date & Time</label>
                        <input className={inputCls} type="datetime-local"
                            value={dateTime} onChange={e => setDateTime(e.target.value)} required />
                    </div>
                    <div className="space-y-1">
                        <label className={labelCls}>Total Capacity</label>
                        <input className={inputCls} type="number" min={1} placeholder="e.g. 500"
                            value={capacity} onChange={e => setCapacity(e.target.value)} required />
                    </div>
                </div>
            </div>

            {/* ── Ticket quantity policy ── */}
            <div className="border-t border-gray-800 pt-4">
                <p className={sectionHeaderCls}>
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
                <ToggleRow
                    label="Quantity uses OR logic"
                    checked={isQuantityOr}
                    onChange={setIsQuantityOr}
                    hint="OFF = buyer must satisfy BOTH min AND max. ON = satisfying either is enough."
                />
            </div>

            {/* ── Age policy ── */}
            <div className="border-t border-gray-800 pt-4">
                <p className={sectionHeaderCls}>
                    <span className="material-symbols-outlined text-[14px]">person</span>
                    Age Policy <span className="text-gray-600 normal-case font-normal">(optional)</span>
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
                <ToggleRow
                    label="Age uses OR logic"
                    checked={isAgeOr}
                    onChange={setIsAgeOr}
                    hint="OFF = buyer must be within BOTH min AND max age. ON = either condition is enough."
                />
            </div>

            {/* ── Outer composition (only shown when age fields are set) ── */}
            {hasAgePolicy && (
                <div className="border-t border-gray-800 pt-4">
                    <p className={sectionHeaderCls}>
                        <span className="material-symbols-outlined text-[14px]">merge</span>
                        Policy Composition
                    </p>
                    <ToggleRow
                        label="Age OR Quantity (outer)"
                        checked={isAgeAndQuantityOr}
                        onChange={setIsAgeAndQuantityOr}
                        hint="OFF = buyer must pass BOTH age AND quantity rules. ON = passing either block is enough."
                    />
                </div>
            )}

            {/* ── Company policy reference ── */}
            {companyPolicyDesc && (
                <div className="border-t border-gray-800 pt-4">
                    <p className={sectionHeaderCls}>
                        <span className="material-symbols-outlined text-[14px]">domain</span>
                        Company Policy (applies to all events)
                    </p>
                    <div className="bg-amber-500/5 border border-amber-500/20 rounded-lg px-3 py-2.5">
                        <p className="text-xs text-amber-300 font-mono">{companyPolicyDesc}</p>
                        <p className="text-[10px] text-gray-600 mt-1">Event policy is applied on top of this. Both must be satisfied.</p>
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
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true); setError(null);
        try {
            const eventId = event.eventId!;
            const dateChanged = dateTime !== toDatetimeLocal(event.eventDateTime ?? '');
            const capacityChanged = Number(capacity) !== event.eventCapacity;

            const ops: Promise<boolean>[] = [];
            if (dateChanged) ops.push(eventApi.editEventDate(token, eventId, { newDateTime: new Date(dateTime).toISOString().slice(0, 19) }));
            if (capacityChanged) ops.push(eventApi.editEventCapacity(token, eventId, { newCapacity: Number(capacity) }));

            if (ops.length === 0) { onClose(); return; }

            const results = await Promise.all(ops);
            if (results.some(r => !r)) throw new Error('One or more updates failed');
            onSaved();
        } catch (err) { setError(err instanceof Error ? err.message : 'Failed to update event'); }
        setLoading(false);
    };

    return (
        <Modal title={`EDIT — ${event.eventName}`} icon="edit_calendar" onClose={onClose} onSubmit={handleSubmit} loading={loading} error={error}>
            <div className="space-y-1">
                <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400">Date & Time</label>
                <input className="w-full bg-[#0b1326] border border-gray-600 rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[#00dbe7] transition-colors"
                    type="datetime-local" value={dateTime} onChange={e => setDateTime(e.target.value)} required />
            </div>
            <div className="space-y-1">
                <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400">Total Capacity</label>
                <input className="w-full bg-[#0b1326] border border-gray-600 rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[#00dbe7] transition-colors"
                    type="number" min={1} value={capacity} onChange={e => setCapacity(e.target.value)} required />
            </div>
        </Modal>
    );
}

// ─── Event card ───────────────────────────────────────────────────────────────

function EventCard({ event, onEdit, onDelete }: {
    event: EventDTO;
    onEdit: () => void;
    onDelete: () => void;
}) {
    return (
        <div className="bg-[#171f33] border border-gray-800 rounded-2xl p-5 flex flex-col gap-3">
            <div className="flex items-start justify-between">
                <div className="w-10 h-10 rounded-xl bg-[#00dbe7]/10 flex items-center justify-center flex-shrink-0">
                    <span className="material-symbols-outlined text-[22px] text-[#00dbe7]">event</span>
                </div>
                <span className={`text-[10px] font-mono font-bold px-2.5 py-1 rounded-full border ${event.isActive ? 'text-[#34d399] bg-emerald-500/10 border-emerald-500/20' : 'text-gray-500 bg-gray-500/10 border-gray-500/20'}`}>
                    {event.isActive ? 'ACTIVE' : 'INACTIVE'}
                </span>
            </div>
            <div>
                <h3 className="text-white font-black text-base tracking-tight">{event.eventName}</h3>
                {event.eventDateTime && (
                    <p className="text-xs text-gray-500 font-mono mt-1 flex items-center gap-1">
                        <span className="material-symbols-outlined text-[13px]">schedule</span>
                        {formatDate(event.eventDateTime)}
                    </p>
                )}
            </div>
            <div className="flex items-center gap-3 pt-2 border-t border-gray-800 text-[11px] font-mono text-gray-500">
                <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-[13px]">people</span>
                    {event.eventCapacity} capacity
                </span>
                {event.eventId && <span className="ml-auto">ID: {event.eventId}</span>}
            </div>
            {/* Action buttons */}
            <div className="flex gap-2 pt-1">
                <button
                    onClick={onEdit}
                    className="flex-1 flex items-center justify-center gap-1.5 py-2 bg-[#00dbe7]/10 hover:bg-[#00dbe7]/20 text-[#00dbe7] text-xs font-bold rounded-lg transition-colors border border-[#00dbe7]/20"
                >
                    <span className="material-symbols-outlined text-[15px]">edit</span>
                    EDIT
                </button>
                <button
                    onClick={onDelete}
                    className="flex-1 flex items-center justify-center gap-1.5 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 text-xs font-bold rounded-lg transition-colors border border-red-500/20"
                >
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
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showCreate, setShowCreate] = useState(false);
    const [editingEvent, setEditingEvent] = useState<EventDTO | null>(null);

    const fetchEvents = useCallback(async () => {
        setLoading(true);
        try {
            const [list, rolesTree] = await Promise.all([
                eventApi.getEventsByCompany(token, numericId),
                getRolesTree(numericId).catch(() => null),
            ]);
            setEvents(list);
            if (rolesTree?.companyName) setCompanyName(rolesTree.companyName);
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

    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen font-sans">
            {/* Header */}
            <div className="bg-[#eeefff] text-[#171f33] px-6 md:px-10 py-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                    <button onClick={() => navigate(`/company/${companyId}`)}
                        className="flex items-center gap-1.5 text-xs font-bold text-gray-500 hover:text-[#2563eb] transition-colors" title="Back to Company">
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
                <button onClick={() => setShowCreate(true)}
                    className="flex items-center gap-2 bg-[#00dbe7] hover:bg-[#00bfc9] text-[#0b1326] px-5 py-2.5 rounded-xl font-bold text-sm tracking-wider transition-colors">
                    <span className="material-symbols-outlined text-[18px]">add</span>
                    NEW EVENT
                </button>
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

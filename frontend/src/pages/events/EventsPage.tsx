import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { eventApi, EventDTO } from '../../api/eventsApi';

function formatDate(iso: string) {
    try {
        return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    } catch { return iso; }
}

function availabilityLabel(event: EventDTO): string {
    const cap = event.eventCapacity;
    if (cap <= 0) return 'SOLD OUT';
    if (cap <= 20) return `${cap} LEFT`;
    return `${cap}+ AV.`;
}

function priceDisplay(event: EventDTO): string {
    const min = event.minZonePrice;
    const max = event.maxZonePrice;
    if (min != null) {
        if (max != null && max !== min) return `$${min.toFixed(2)} – $${max.toFixed(2)}`;
        return `$${min.toFixed(2)}`;
    }
    if (event.ticketPrice != null) return `$${event.ticketPrice.toFixed(2)}`;
    return 'Free';
}

export default function EventsPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const searchQuery = searchParams.get('search') ?? '';

    const [searchInput, setSearchInput] = useState(searchQuery);
    
    const [events, setEvents] = useState<EventDTO[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        setLoading(true);
        if (searchQuery) {
            eventApi.searchEvents(searchQuery).then(list => {
                setEvents(list);
                setLoading(false);
            });
        } else {
            eventApi.getAllActiveEvents().then(list => {
                setEvents(list);
                setLoading(false);
            });
        }
    }, [searchQuery]);

    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault();
        setSearchParams(searchInput ? { search: searchInput } : {});
    };

    return (
        <div className="bg-[#0b1326] text-[#dbe2fd] min-h-screen font-sans overflow-x-hidden pb-32">

            <main className="py-10 px-6 md:px-12 min-h-screen max-w-7xl mx-auto">

                {/* Hero */}
                <section className="mb-10 flex flex-col md:flex-row md:items-end justify-between gap-6">
                    <div>
                        <h2 className="text-4xl md:text-5xl font-black text-white mb-2">
                            {searchQuery ? `Results for "${searchQuery}"` : 'Live Events'}
                        </h2>
                        <p className="text-gray-400 max-w-xl">Curated premium experiences for the bold. Secure your entry to the season's most anticipated performances.</p>
                    </div>
                    <div className="flex gap-2">
                        <span className="bg-[#03dbe7]/10 text-[#03dbe7] px-3 py-1 rounded-full text-xs font-mono border border-[#03dbe7]/20">
                            {events.length} EVENTS
                        </span>
                    </div>
                </section>

                {/* Search Bar */}
                <form onSubmit={handleSearch} className="mb-8 flex flex-col sm:flex-row gap-3">
                    <input
                        type="text"
                        value={searchInput}
                        onChange={(e) => setSearchInput(e.target.value)}
                        placeholder="Search events by name or location..."
                        className="bg-[#171f33] border border-gray-800 text-[#dbe2fd] px-4 py-3 rounded-lg flex-1 focus:outline-none focus:border-[#03dbe7] placeholder-gray-500"
                    />
                    <button
                        type="submit"
                        className="bg-[#03dbe7] text-[#00363a] px-8 py-3 rounded-lg font-bold tracking-widest hover:bg-[#75f5ff] hover:shadow-[0_0_15px_rgba(3,219,231,0.4)] transition-all active:scale-95"
                    >
                        SEARCH
                    </button>
                    {searchQuery && (
                        <button
                            type="button"
                            onClick={() => {
                                setSearchInput('');
                                setSearchParams({});
                            }}
                            className="bg-transparent border border-gray-700 text-gray-400 px-6 py-3 rounded-lg font-bold tracking-widest hover:bg-gray-800 hover:text-white transition-all"
                        >
                            CLEAR
                        </button>
                    )}
                </form>

                {loading ? (
                    <div className="flex justify-center py-24">
                        <span className="material-symbols-outlined animate-spin text-4xl text-[#03dbe7]">refresh</span>
                    </div>
                ) : events.length === 0 ? (
                    <div className="text-center py-20 text-gray-400">
                        <p className="text-2xl font-bold mb-2">{searchQuery ? 'No events found' : 'No events yet'}</p>
                        <p className="text-sm">{searchQuery ? 'Try a different search term.' : 'Check back soon!'}</p>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {events.map(event => (
                            <div key={event.eventId}
                                className="bg-[#171f33] border border-gray-800 text-[#dbe2fd] hover:border-[#75f5ff]/50 rounded-xl overflow-hidden flex flex-col shadow-xl transform transition-all hover:-translate-y-1 group">

                                {/* Image or placeholder */}
                                <div className="h-48 relative overflow-hidden bg-[#0f1627]">
                                    {event.imageUrl ? (
                                        <img
                                            src={event.imageUrl}
                                            alt={event.eventName}
                                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                        />
                                    ) : (
                                        <div className="w-full h-full flex items-center justify-center">
                                            <span className="material-symbols-outlined text-6xl text-gray-700">event</span>
                                        </div>
                                    )}
                                    {event.isActive && (
                                        <div className="absolute top-4 left-4 px-3 py-1 rounded text-xs font-mono uppercase tracking-widest font-bold bg-[#03dbe7] text-[#00363a]">
                                            Active
                                        </div>
                                    )}
                                </div>

                                <div className="p-5 flex-1 flex flex-col">
                                    <div className="flex justify-between items-start mb-2">
                                        <h3 className="text-xl font-bold leading-tight uppercase">{event.eventName}</h3>
                                    </div>

                                    <div className="flex items-center gap-2 mb-4 text-gray-400">
                                        {event.eventLocation ? (
                                            <>
                                                <span className="material-symbols-outlined text-sm">location_on</span>
                                                <p className="text-sm">{event.eventLocation}</p>
                                            </>
                                        ) : event.eventDateTime ? (
                                            <>
                                                <span className="material-symbols-outlined text-sm">calendar_today</span>
                                                <p className="text-sm">{formatDate(event.eventDateTime)}</p>
                                            </>
                                        ) : null}
                                    </div>

                                    <div className="mt-auto space-y-4">
                                        <div className="flex justify-between items-end border-t border-gray-800 pt-4">
                                            <div>
                                                <p className="text-[10px] uppercase font-bold mb-1 text-gray-400">Price</p>
                                                <p className="font-mono text-lg font-bold text-[#03dbe7]">
                                                    {priceDisplay(event)}
                                                </p>
                                            </div>
                                            <div className="text-right">
                                                <p className="text-[10px] uppercase font-bold mb-1 text-gray-400">Availability</p>
                                                <p className={`font-mono ${event.eventCapacity <= 20 ? 'text-red-500 font-bold' : ''}`}>
                                                    {availabilityLabel(event)}
                                                </p>
                                            </div>
                                        </div>

                                        <Link
                                            to={`/events/${event.eventId}`}
                                            className="w-full flex justify-center py-3 rounded-lg font-bold text-sm tracking-widest active:scale-95 transition-all shadow-md bg-[#2563eb] text-[#eeefff] border-t border-white/20"
                                        >
                                            VIEW DETAILS
                                        </Link>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </main>
        </div>
    );
}
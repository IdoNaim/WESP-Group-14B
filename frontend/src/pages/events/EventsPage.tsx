import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { eventApi, EventDTO } from '../../api/eventsApi'; // Adjust path if needed

export default function EventsPage() {
    const [events, setEvents] = useState<EventDTO[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    useEffect(() => {
        const fetchEvents = async () => {
            console.log('--- [UI START] Fetching Events List ---');
            setIsLoading(true);
            try {
                // Grab the token you saved during login!
                const token = localStorage.getItem('token') || '';

                // Using companyId 1 as default based on your backend requirement
                console.log('[UI STEP 1] Calling eventApi.getEventsByCompany...');
                const data = await eventApi.getEventsByCompany(token, 1);

                setEvents(data);
                console.log('[UI STEP 1 RESULT] State updated with events.');
            } catch (error: any) {
                console.error('[UI CATCH BLOCK] Failed to load events:', error.message);
                setErrorMessage("Unable to load the event schedule at this time.");
            } finally {
                setIsLoading(false);
                console.log('--- [UI END] Event Fetch Completed ---');
            }
        };

        fetchEvents();
    }, []);

    return (
        <div className="bg-[#0b1326] text-[#dbe2fd] min-h-screen font-sans overflow-x-hidden pb-32">
            <header className="fixed top-0 w-full z-50 bg-[#0b1326]/70 backdrop-blur-xl border-b border-gray-800 shadow-sm flex justify-between items-center px-6 md:px-12 py-4">
                <div className="flex items-center gap-4">
                    <Link to="/home" className="active:scale-95 transition-transform text-[#b4c5ff]">
                        <span className="material-symbols-outlined">arrow_back</span>
                    </Link>
                    <h1 className="text-2xl md:text-3xl font-bold tracking-tighter text-[#b4c5ff]">VELOCITY TICKETS</h1>
                </div>
            </header>

            <main className="pt-28 pb-12 px-6 md:px-12 min-h-screen max-w-7xl mx-auto">
                <section className="mb-10 flex flex-col md:flex-row md:items-end justify-between gap-6">
                    <div>
                        <h2 className="text-4xl md:text-5xl font-black text-white mb-2">Live Events</h2>
                        <p className="text-gray-400 max-w-xl">Curated premium experiences for the bold. Secure your entry to the season's most anticipated performances.</p>
                    </div>
                    <div className="flex gap-2">
                        <span className="bg-[#03dbe7]/10 text-[#03dbe7] px-3 py-1 rounded-full text-xs font-mono border border-[#03dbe7]/20">
                            {events.length} UPCOMING
                        </span>
                    </div>
                </section>

                {/* Status Displays */}
                {isLoading && (
                    <div className="flex justify-center py-20">
                        <span className="material-symbols-outlined animate-spin text-4xl text-[#2563eb]">refresh</span>
                    </div>
                )}

                {errorMessage && (
                    <div className="bg-red-900/50 border border-red-500 text-red-200 p-6 rounded-xl text-center font-bold tracking-wider">
                        {errorMessage}
                    </div>
                )}

                {/* Events Grid */}
                {!isLoading && !errorMessage && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {events.map((event) => (
                            // FIXED: Changed event.id to event.eventId
                            <div key={event.eventId} className="rounded-xl overflow-hidden flex flex-col shadow-xl transform transition-all hover:-translate-y-1 bg-[#171f33] border border-gray-800 text-[#dbe2fd] hover:border-[#75f5ff]/50 group">
                                <div className="h-48 relative overflow-hidden bg-[#2d3449]">
                                    {/* Fallback image if backend doesn't provide one */}
                                    <img
                                        src={event.image || "https://images.unsplash.com/photo-1540039155733-d7696ba45ae7?auto=format&fit=crop&q=80&w=800"}
                                        alt={event.eventName} // FIXED: eventName
                                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 opacity-60"
                                    />
                                </div>

                                <div className="p-5 flex-1 flex flex-col z-10 -mt-6 bg-[#171f33] rounded-t-xl relative">
                                    <div className="flex justify-between items-start mb-2">
                                        {/* FIXED: Changed event.title to event.eventName */}
                                        <h3 className="text-xl font-bold leading-tight uppercase">{event.eventName}</h3>
                                    </div>

                                    <div className="flex items-center gap-2 mb-4 text-gray-400">
                                        <span className="material-symbols-outlined text-sm">calendar_today</span>
                                        <p className="text-sm font-mono">
                                            {event.eventDateTime
                                                ? new Date(event.eventDateTime).toLocaleString('en-US', {
                                                    month: 'short',
                                                    day: 'numeric',
                                                    year: 'numeric',
                                                    hour: 'numeric',
                                                    minute: '2-digit'
                                                })
                                                : 'TBD'}
                                        </p>
                                    </div>

                                    <div className="mt-auto space-y-4">
                                        <div className="flex justify-between items-end border-t border-gray-800 pt-4">
                                            <div>
                                                <p className="text-[10px] uppercase font-bold mb-1 text-gray-400">Capacity</p>
                                                {/* FIXED: Changed event.capacity to event.eventCapacity */}
                                                <p className="font-mono text-lg font-bold text-[#03dbe7]">{event.eventCapacity}</p>
                                            </div>
                                        </div>

                                        {/* FIXED: Changed event.id to event.eventId in the routing link */}
                                        <Link to={`/events/${event.eventId}`} className="w-full flex justify-center py-3 rounded-lg font-bold text-sm tracking-widest active:scale-95 transition-all shadow-md bg-[#2563eb] text-[#eeefff] border-t border-white/20 hover:bg-[#0053db]">
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
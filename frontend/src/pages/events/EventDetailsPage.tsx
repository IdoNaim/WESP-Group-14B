import { useParams, Link } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { eventApi, EventDTO } from '../../api/eventsApi';

export default function EventDetailsPage() {
    const { eventId } = useParams();

    const [eventData, setEventData] = useState<EventDTO | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isAuthorized] = useState(true);

    useEffect(() => {
        const fetchSingleEvent = async () => {
            if (!eventId) return;
            console.log(`--- [UI START] Fetching Event Details for: ${eventId} ---`);
            setIsLoading(true);

            try {
                const token = localStorage.getItem('token') || '';
                const data = await eventApi.getEvent(token, eventId);
                setEventData(data);
                console.log('[UI STEP 1 RESULT] Event details mapped to state.');
            } catch (error) {
                console.error('[UI CATCH BLOCK] Failed to fetch specific event:', error);
            } finally {
                setIsLoading(false);
                console.log('--- [UI END] Event Details Fetch Completed ---');
            }
        };

        fetchSingleEvent();
    }, [eventId]);

    if (isLoading) {
        return (
            <div className="bg-[#0b1326] min-h-screen flex items-center justify-center">
                <span className="material-symbols-outlined animate-spin text-5xl text-[#2563eb]">refresh</span>
            </div>
        );
    }

    if (!eventData) {
        return (
            <div className="bg-[#0b1326] text-[#dbe2fd] min-h-screen flex flex-col items-center justify-center p-6">
                <span className="material-symbols-outlined text-6xl text-red-500 mb-4">error</span>
                <h1 className="text-3xl font-black text-white mb-2">EVENT NOT FOUND</h1>
                <p className="text-gray-400 mb-6">The event ID "{eventId}" does not exist or has been removed.</p>
                <Link to="/events" className="bg-[#2563eb] text-white px-6 py-3 rounded-lg font-bold text-sm tracking-wider hover:bg-[#0053db] transition-colors">
                    BROWSE ALL EVENTS
                </Link>
            </div>
        );
    }

    return (
        <div className="bg-[#0b1326] text-[#dbe2fd] min-h-screen font-sans pb-32">
            <header className="fixed top-0 w-full z-50 bg-[#0b1326]/70 backdrop-blur-xl border-b border-gray-800 shadow-sm flex items-center px-6 md:px-12 py-4 gap-4">
                <Link to="/events" className="text-[#b4c5ff] hover:text-white transition-colors">
                    <span className="material-symbols-outlined">arrow_back</span>
                </Link>
                <h1 className="text-xl font-bold tracking-tighter text-[#b4c5ff]">BACK TO EVENTS</h1>
            </header>

            <main className="pt-28 px-6 md:px-12 max-w-4xl mx-auto space-y-8">
                <div>
                    <span className="bg-[#03dbe7]/10 text-[#03dbe7] px-3 py-1 rounded text-xs font-mono border border-[#03dbe7]/20 uppercase">
                        {eventData.id || eventId}
                    </span>
                    <h1 className="text-4xl md:text-5xl font-black text-white mt-4 uppercase leading-tight">{eventData.title}</h1>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="bg-[#171f33] border border-gray-800 p-6 rounded-xl flex items-start gap-4">
                        <span className="material-symbols-outlined text-[#03dbe7] text-3xl">calendar_month</span>
                        <div>
                            <p className="text-xs uppercase font-bold text-gray-500 mb-1">Date & Time</p>
                            <p className="font-bold text-lg">{eventData.date}</p>
                        </div>
                    </div>

                    <div className="bg-[#171f33] border border-gray-800 p-6 rounded-xl flex items-start gap-4">
                        <span className="material-symbols-outlined text-[#03dbe7] text-3xl">group</span>
                        <div>
                            <p className="text-xs uppercase font-bold text-gray-500 mb-1">Capacity</p>
                            <p className="font-bold text-lg">{eventData.capacity} Tickets</p>
                        </div>
                    </div>
                </div>

                <div className="bg-[#171f33] border border-gray-800 p-8 rounded-xl space-y-6">
                    <div>
                        <h3 className="text-xl font-bold text-white mb-2">About This Event</h3>
                        <p className="text-gray-300 leading-relaxed">{eventData.description || "No description provided."}</p>
                    </div>
                </div>

                <div className="bg-[#eeefff] text-[#171f33] p-8 rounded-xl shadow-2xl flex flex-col md:flex-row items-center justify-between gap-6">
                    <div>
                        <p className="text-sm uppercase font-bold opacity-60 mb-1">Standard Admission</p>
                        <p className="text-4xl font-mono font-black">{eventData.price ? `$${eventData.price}` : 'TBD'}</p>
                    </div>

                    {!isAuthorized ? (
                        <div className="flex items-center gap-2 bg-red-100 text-red-800 px-6 py-4 rounded-lg font-bold border border-red-200">
                            <span className="material-symbols-outlined">block</span>
                            PURCHASE UNAVAILABLE
                        </div>
                    ) : (
                        <button className="w-full md:w-auto bg-[#2563eb] text-white px-10 py-4 rounded-xl font-bold tracking-widest hover:bg-[#0053db] transition-colors shadow-lg active:scale-95">
                            RESERVE TICKETS
                        </button>
                    )}
                </div>
            </main>
        </div>
    );
}
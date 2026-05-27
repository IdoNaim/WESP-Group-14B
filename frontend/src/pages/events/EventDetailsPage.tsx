import { useParams, Link } from 'react-router-dom';
import { useState } from 'react';

export default function EventDetailsPage() {
    const { eventId } = useParams();

    // MOCK STATES (Replace with real backend checks)
    const [isAuthorized] = useState(true); // Toggle to test forbidden state

    // Mock Database Check
    const eventData = eventId === "EVT-001" ? {
        id: "EVT-001",
        title: "Cyberpunk Symphony: Neon Dreams",
        date: "October 24, 2026",
        time: "21:00 IDT",
        location: "Velocity Arena, Sector 7",
        description: "An immersive audiovisual experience blending classical orchestration with heavy synthwave and electronic beats. Features state-of-the-art laser choreography.",
        price: "$128.00",
        company: "Elite Live Productions",
        policy: "Max 4 tickets per user. No refunds 48 hours prior to the event start time. Digital transfer enabled.",
        isSoldOut: false
    } : null;

    // --- ERROR STATE (Acceptance Criteria 5: Missing event shows error) ---
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

    // --- NORMAL VIEW ---
    return (
        <div className="bg-[#0b1326] text-[#dbe2fd] min-h-screen font-sans pb-32">
            {/* Header */}
            <header className="fixed top-0 w-full z-50 bg-[#0b1326]/70 backdrop-blur-xl border-b border-gray-800 shadow-sm flex items-center px-6 md:px-12 py-4 gap-4">
                <Link to="/events" className="text-[#b4c5ff] hover:text-white transition-colors">
                    <span className="material-symbols-outlined">arrow_back</span>
                </Link>
                <h1 className="text-xl font-bold tracking-tighter text-[#b4c5ff]">BACK TO EVENTS</h1>
            </header>

            <main className="pt-28 px-6 md:px-12 max-w-4xl mx-auto space-y-8">

                {/* Event Header Info */}
                <div>
          <span className="bg-[#03dbe7]/10 text-[#03dbe7] px-3 py-1 rounded text-xs font-mono border border-[#03dbe7]/20 uppercase">
            {eventData.id}
          </span>
                    <h1 className="text-4xl md:text-5xl font-black text-white mt-4 uppercase leading-tight">{eventData.title}</h1>
                </div>

                {/* Info Cards Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="bg-[#171f33] border border-gray-800 p-6 rounded-xl flex items-start gap-4">
                        <span className="material-symbols-outlined text-[#03dbe7] text-3xl">calendar_month</span>
                        <div>
                            <p className="text-xs uppercase font-bold text-gray-500 mb-1">Date & Time</p>
                            <p className="font-bold text-lg">{eventData.date}</p>
                            <p className="font-mono text-sm text-gray-400">{eventData.time}</p>
                        </div>
                    </div>

                    <div className="bg-[#171f33] border border-gray-800 p-6 rounded-xl flex items-start gap-4">
                        <span className="material-symbols-outlined text-[#03dbe7] text-3xl">location_on</span>
                        <div>
                            <p className="text-xs uppercase font-bold text-gray-500 mb-1">Location</p>
                            <p className="font-bold text-lg">{eventData.location}</p>
                        </div>
                    </div>
                </div>

                {/* Event Details & Policy */}
                <div className="bg-[#171f33] border border-gray-800 p-8 rounded-xl space-y-6">
                    <div>
                        <h3 className="text-xl font-bold text-white mb-2">About This Event</h3>
                        <p className="text-gray-300 leading-relaxed">{eventData.description}</p>
                    </div>

                    <hr className="border-gray-800" />

                    <div className="flex flex-col md:flex-row justify-between gap-6">
                        <div>
                            <p className="text-xs uppercase font-bold text-gray-500 mb-1 flex items-center gap-1">
                                <span className="material-symbols-outlined text-[16px]">domain</span> Production Company
                            </p>
                            <p className="font-bold">{eventData.company}</p>
                        </div>

                        <div className="md:max-w-xs">
                            <p className="text-xs uppercase font-bold text-gray-500 mb-1 flex items-center gap-1">
                                <span className="material-symbols-outlined text-[16px]">policy</span> Purchase Policy
                            </p>
                            <p className="text-sm text-gray-400">{eventData.policy}</p>
                        </div>
                    </div>
                </div>

                {/* Purchase Action Area (Acceptance Criteria 4) */}
                <div className="bg-[#eeefff] text-[#171f33] p-8 rounded-xl shadow-2xl flex flex-col md:flex-row items-center justify-between gap-6">
                    <div>
                        <p className="text-sm uppercase font-bold opacity-60 mb-1">Standard Admission</p>
                        <p className="text-4xl font-mono font-black">{eventData.price}</p>
                    </div>

                    {/* FORBIDDEN / UNAVAILABLE STATE */}
                    {!isAuthorized || eventData.isSoldOut ? (
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
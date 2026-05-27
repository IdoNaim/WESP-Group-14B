import { Link } from 'react-router-dom';

// Mock Data matching the Stitch output
const events = [
    {
        id: "EVT-001",
        title: "Cyberpunk Symphony: Neon Dreams",
        category: "MUSIC",
        date: "Oct 24 • 21:00",
        price: "$128.00",
        availability: "12 LEFT",
        image: "https://images.unsplash.com/photo-1540039155733-d7696ba45ae7?auto=format&fit=crop&q=80&w=800",
        theme: "light",
        status: "Sold Out Soon"
    },
    {
        id: "EVT-002",
        title: "Velocity FC vs. Titan United",
        category: "SPORTS",
        location: "Velocity Arena, Sector 7",
        price: "$75.50",
        availability: "450+ AV.",
        image: "https://images.unsplash.com/photo-1504450758481-7338eba7524a?auto=format&fit=crop&q=80&w=800",
        theme: "dark",
        status: "Live Now"
    },
    {
        id: "EVT-003",
        title: "The Phantom's Reflection",
        category: "THEATER",
        date: "Dec 12-15 • Multiple Times",
        price: "$210.00",
        availability: "62 AV.",
        image: "https://images.unsplash.com/photo-1507676184212-d0c30a5991de?auto=format&fit=crop&q=80&w=800",
        theme: "dark"
    }
];

export default function EventsPage() {
    return (
        <div className="bg-[#0b1326] text-[#dbe2fd] min-h-screen font-sans overflow-x-hidden pb-32">

            {/* Header */}
            <header className="fixed top-0 w-full z-50 bg-[#0b1326]/70 backdrop-blur-xl border-b border-gray-800 shadow-sm flex justify-between items-center px-6 md:px-12 py-4">
                <div className="flex items-center gap-4">
                    <Link to="/dashboard" className="active:scale-95 transition-transform text-[#b4c5ff]">
                        <span className="material-symbols-outlined">arrow_back</span>
                    </Link>
                    <h1 className="text-2xl md:text-3xl font-bold tracking-tighter text-[#b4c5ff]">VELOCITY TICKETS</h1>
                </div>
            </header>

            <main className="pt-28 pb-12 px-6 md:px-12 min-h-screen max-w-7xl mx-auto">

                {/* Hero Section */}
                <section className="mb-10 flex flex-col md:flex-row md:items-end justify-between gap-6">
                    <div>
                        <h2 className="text-4xl md:text-5xl font-black text-white mb-2">Live Events</h2>
                        <p className="text-gray-400 max-w-xl">Curated premium experiences for the bold. Secure your entry to the season's most anticipated performances.</p>
                    </div>
                    <div className="flex gap-2">
                        <span className="bg-[#03dbe7]/10 text-[#03dbe7] px-3 py-1 rounded-full text-xs font-mono border border-[#03dbe7]/20">84 UPCOMING</span>
                        <span className="bg-[#2d3449] text-gray-300 px-3 py-1 rounded-full text-xs font-mono">FILTERS</span>
                    </div>
                </section>

                {/* Events Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {events.map((event) => (
                        <div key={event.id} className={`rounded-xl overflow-hidden flex flex-col shadow-xl transform transition-all hover:-translate-y-1 ${event.theme === 'light' ? 'bg-[#eeefff] text-[#171f33]' : 'bg-[#171f33] border border-gray-800 text-[#dbe2fd] hover:border-[#75f5ff]/50 group'}`}>

                            <div className="h-48 relative overflow-hidden">
                                <img src={event.image} alt={event.title} className={`w-full h-full object-cover ${event.theme === 'dark' ? 'group-hover:scale-105 transition-transform duration-500' : ''}`} />
                                {event.status && (
                                    <div className={`absolute top-4 left-4 px-3 py-1 rounded text-xs font-mono uppercase tracking-widest font-bold ${event.theme === 'light' ? 'bg-[#171f33] text-[#eeefff]' : 'bg-[#03dbe7] text-[#00363a]'}`}>
                                        {event.status}
                                    </div>
                                )}
                            </div>

                            <div className="p-5 flex-1 flex flex-col">
                                <div className="flex justify-between items-start mb-2">
                                    <h3 className="text-xl font-bold leading-tight uppercase">{event.title}</h3>
                                    <span className={`text-xs px-2 py-0.5 rounded font-mono ${event.theme === 'light' ? 'bg-[#171f33]/10' : 'bg-[#2d3449] text-gray-400'}`}>{event.category}</span>
                                </div>

                                <div className={`flex items-center gap-2 mb-4 ${event.theme === 'light' ? 'opacity-70' : 'text-gray-400'}`}>
                                    <span className="material-symbols-outlined text-sm">{event.location ? 'location_on' : 'calendar_today'}</span>
                                    <p className="text-sm">{event.location || event.date}</p>
                                </div>

                                <div className="mt-auto space-y-4">
                                    <div className={`flex justify-between items-end border-t pt-4 ${event.theme === 'light' ? 'border-[#171f33]/10' : 'border-gray-800'}`}>
                                        <div>
                                            <p className={`text-[10px] uppercase font-bold mb-1 ${event.theme === 'light' ? 'opacity-50' : 'text-gray-400'}`}>Price</p>
                                            <p className={`font-mono text-lg font-bold ${event.theme === 'dark' ? 'text-[#03dbe7]' : ''}`}>{event.price}</p>
                                        </div>
                                        <div className="text-right">
                                            <p className={`text-[10px] uppercase font-bold mb-1 ${event.theme === 'light' ? 'opacity-50' : 'text-gray-400'}`}>Availability</p>
                                            <p className={`font-mono ${event.availability.includes('LEFT') ? 'text-red-500 font-bold' : ''}`}>{event.availability}</p>
                                        </div>
                                    </div>

                                    {/* Task: Add button/link to view event details */}
                                    <Link to={`/events/${event.id}`} className={`w-full flex justify-center py-3 rounded-lg font-bold text-sm tracking-widest active:scale-95 transition-all shadow-md ${event.theme === 'light' ? 'bg-[#2563eb] text-white hover:bg-[#0053db]' : 'bg-[#2563eb] text-[#eeefff] border-t border-white/20'}`}>
                                        VIEW DETAILS
                                    </Link>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </main>
        </div>
    );
}
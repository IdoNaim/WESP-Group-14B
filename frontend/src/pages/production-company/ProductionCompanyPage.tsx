import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

// Mock Data Interfaces (Replace these with your actual backend types later)
interface CompanyDetails {
    id: string;
    name: string;
    contactEmail: string;
    established: string;
}

interface Event {
    id: string;
    title: string;
    date: string;
    status: 'Draft' | 'Published' | 'Completed';
}

export default function ProductionCompanyPage() {
    // --- STATE & AUTHORIZATION MOCK ---
    // In reality, you will fetch this from your auth token or context
    const [isAuthorized, setIsAuthorized] = useState<boolean>(true);
    const [isLoading, setIsLoading] = useState<boolean>(true);

    // Mock Backend Data
    const companyData: CompanyDetails = {
        id: "COMP-9021",
        name: "Elite Live Productions",
        contactEmail: "admin@elitelive.com",
        established: "2018",
    };

    const eventsData: Event[] = [
        { id: "EVT-001", title: "Neon Nights Festival", date: "2026-08-15", status: "Published" },
        { id: "EVT-002", title: "Acoustic Sessions Vol 3", date: "2026-09-10", status: "Draft" },
    ];

    // Simulate network fetch
    useEffect(() => {
        setTimeout(() => setIsLoading(false), 800);
    }, []);

    // --- UNAUTHORIZED VIEW (Acceptance Criteria 3) ---
    if (!isLoading && !isAuthorized) {
        return (
            <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen flex flex-col items-center justify-center p-6 relative">
                <div className="bg-[#171f33] border border-red-500/30 p-10 rounded-2xl text-center max-w-md shadow-[0_0_30px_rgba(239,68,68,0.1)]">
                    <span className="material-symbols-outlined text-6xl text-red-500 mb-4">gpp_bad</span>
                    <h1 className="text-2xl font-black text-white mb-2">ACCESS FORBIDDEN</h1>
                    <p className="text-sm text-gray-400 mb-6">You do not have the required production manager permissions to view this company's operational data.</p>
                    <Link to="/dashboard" className="bg-[#2563eb] text-white px-6 py-3 rounded-lg font-bold text-sm tracking-wider hover:bg-[#0053db] transition-colors">
                        RETURN TO DASHBOARD
                    </Link>
                </div>

                {/* DEV TOGGLE - Remove before final submission */}
                <button onClick={() => setIsAuthorized(true)} className="fixed bottom-4 right-4 bg-gray-800 text-xs p-2 rounded text-white border border-gray-600">
                    Dev: Switch to Authorized
                </button>
            </div>
        );
    }

    // --- AUTHORIZED VIEW (Acceptance Criteria 1 & 2) ---
    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen p-6 md:p-12 font-sans relative">

            {/* Background Ambience */}
            <div className="fixed inset-0 z-0 bg-gradient-to-br from-[#060e20] to-[#0b1326] pointer-events-none"></div>

            <div className="max-w-5xl mx-auto relative z-10 space-y-8">

                {/* Header & Details (Task: Show production company details) */}
                <div className="bg-[#eeefff] rounded-2xl shadow-xl p-8 text-[#171f33] flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                    <div>
                        <div className="flex items-center gap-3 mb-2">
                            <span className="material-symbols-outlined text-[#2563eb] text-3xl">domain</span>
                            <h1 className="text-3xl font-black text-[#0b1326] tracking-tight">{companyData.name}</h1>
                        </div>
                        <div className="flex gap-4 text-sm font-mono text-gray-600">
                            <span>ID: {companyData.id}</span>
                            <span>|</span>
                            <span>Contact: {companyData.contactEmail}</span>
                        </div>
                    </div>

                    {/* Policy Link (Task: Add link to create/edit purchase policy) */}
                    <Link to="/company/policies" className="flex items-center gap-2 bg-[#0b1326] text-white px-5 py-3 rounded-xl hover:bg-[#171f33] transition-transform active:scale-95 shadow-lg">
                        <span className="material-symbols-outlined text-[20px]">policy</span>
                        <span className="text-sm font-bold tracking-wider">MANAGE POLICIES</span>
                    </Link>
                </div>

                {/* Events Section (Task: Show company events & manage them) */}
                <div className="bg-[#171f33] rounded-2xl p-8 shadow-xl border border-gray-800">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-xl font-bold text-white flex items-center gap-2">
                            <span className="material-symbols-outlined text-[#00dbe7]">event_note</span>
                            Managed Events
                        </h2>
                        <button className="text-sm bg-[#2563eb] text-white px-4 py-2 rounded-lg font-bold hover:bg-[#0053db] transition-colors flex items-center gap-2">
                            <span className="material-symbols-outlined text-[18px]">add</span>
                            NEW EVENT
                        </button>
                    </div>

                    <div className="space-y-4">
                        {eventsData.map((event) => (
                            <div key={event.id} className="bg-[#0b1326] border border-gray-700/50 rounded-xl p-5 flex flex-col md:flex-row justify-between items-center gap-4 transition-all hover:border-[#2563eb]/50">
                                <div className="flex items-center gap-4 w-full md:w-auto">
                                    <div className={`w-2 h-12 rounded-full ${event.status === 'Published' ? 'bg-[#00dbe7]' : 'bg-gray-500'}`}></div>
                                    <div>
                                        <h3 className="text-lg font-bold text-white">{event.title}</h3>
                                        <div className="flex gap-3 text-xs font-mono text-gray-400 mt-1">
                                            <span>{event.date}</span>
                                            <span className="uppercase text-[#b4c5ff]">{event.status}</span>
                                        </div>
                                    </div>
                                </div>

                                {/* Management Actions (Task: Allow authorized user to manage company events) */}
                                <div className="flex gap-2 w-full md:w-auto">
                                    <button className="flex-1 md:flex-none flex items-center justify-center gap-1 bg-gray-800 hover:bg-gray-700 text-white px-4 py-2 rounded-lg text-xs font-bold tracking-wider transition-colors">
                                        <span className="material-symbols-outlined text-[16px]">edit</span>
                                        EDIT
                                    </button>
                                    <button className="flex-1 md:flex-none flex items-center justify-center gap-1 bg-red-900/30 hover:bg-red-900/60 text-red-400 px-4 py-2 rounded-lg text-xs font-bold tracking-wider transition-colors">
                                        <span className="material-symbols-outlined text-[16px]">delete</span>
                                        CANCEL
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

            </div>

            {/* DEV TOGGLE - Remove before final submission */}
            <button onClick={() => setIsAuthorized(false)} className="fixed bottom-4 right-4 bg-gray-800 text-xs p-2 rounded text-white border border-gray-600 z-50">
                Dev: Switch to Unauthorized
            </button>

        </div>
    );
}
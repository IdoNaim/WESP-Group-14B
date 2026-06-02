import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { historyOrderApi, HistoryOrderDTO } from './../../api/historyOrderApi';
import { authApi, UserProfileDTO, UserPermissionsDTO } from '../../api/authApi';

type ViewMode = 'PERSONAL' | 'COMPANY' | 'ALL';

export default function OrderHistory() {
    const navigate = useNavigate();

    const [orders, setOrders] = useState<HistoryOrderDTO[]>([]);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [error, setError] = useState<{ status: number; message: string } | null>(null);
    
    const [username, setUsername] = useState<string | null>(null);
    const [userId, setUserId] = useState<string | null>(null);
    const [permissions, setPermissions] = useState<UserPermissionsDTO | null>(null);

    const [viewMode, setViewMode] = useState<ViewMode>('PERSONAL');
    const [selectedCompanyId, setSelectedCompanyId] = useState<number | null>(null);
    const [selectedTicket, setSelectedTicket] = useState<HistoryOrderDTO | null>(null);

    useEffect(() => {
        const initUser = async () => {
            try {
                const token = localStorage.getItem('token');
                if (!token) throw { status: 401, message: 'You are not logged in.' };

                const [profile, perms] = await Promise.all([
                    authApi.getCurrentUser(token),
                    authApi.getPermissions(token)
                ]);

                setUsername(profile.name);
                setUserId(profile.userId);
                setPermissions(perms);

                const compIds = Object.keys(perms.productionRoles || {}).map(Number);
                if (compIds.length > 0) setSelectedCompanyId(compIds[0]);

            } catch (err: any) {
                setError({ status: err.status || 401, message: err.message || "Failed to authenticate." });
                setIsLoading(false);
            }
        };
        initUser();
    }, []);

    useEffect(() => {
        if (!userId) return;

        const fetchOrders = async () => {
            setIsLoading(true);
            setError(null);
            
            try {
                const token = localStorage.getItem('token');
                if (!token) throw new Error("No token");

                let ordersData: HistoryOrderDTO[] = [];

                if (viewMode === 'PERSONAL') {
                    ordersData = await historyOrderApi.getUserOrders(token, userId);
                } else if (viewMode === 'COMPANY' && selectedCompanyId) {
                    ordersData = await historyOrderApi.getOrdersByCompany(token, selectedCompanyId);
                } else if (viewMode === 'ALL') {
                    ordersData = await historyOrderApi.getAllOrders(token);
                }

                setOrders(ordersData);

            } catch (err: any) {
                if (err.status === 403 || err.status === 401) {
                    setError({ status: err.status, message: "Access Forbidden: You do not have permission to view these records." });
                } else {
                    setError({ status: err.status || 500, message: err.message || "An unexpected error occurred while fetching orders." });
                }
            } finally {
                setIsLoading(false);
            }
        };

        fetchOrders();
    }, [viewMode, userId, selectedCompanyId]);

    const handleLogout = async () => {
        const token = localStorage.getItem('token');
        if (token && userId) {
            try { await authApi.logout(token, userId); } catch (e) {}
        }
        localStorage.removeItem('token');
        navigate('/login');
    };

    const formatDate = (dateString: string, full: boolean = false) => {
        const options: Intl.DateTimeFormatOptions = full 
            ? { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' }
            : { month: 'short', day: '2-digit', year: 'numeric' };
        return new Intl.DateTimeFormat('en-US', options).format(new Date(dateString));
    };

    const getTicketQuantity = (order: HistoryOrderDTO) => {
        const seatCount = order.seatIds ? order.seatIds.length : 0;
        const standingCount = order.standingAreaQuantities ? Object.values(order.standingAreaQuantities).reduce((sum, qty) => sum + qty, 0) : 0;
        return { seatCount, standingCount, total: seatCount + standingCount };
    };

    const isGuest = !username;
    
    const isAdmin = permissions?.isAdmin === true;
    const companyIds = permissions ? Object.keys(permissions.productionRoles || {}).map(Number) : [];
    const isCompanyManager = companyIds.length > 0;

    return (
        <div className="bg-[#0b1326] text-[#dbe2fd] min-h-screen font-sans overflow-x-hidden pb-32">
            <style>{`
                @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
                @import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap');
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24; }
                .loader-ring { width: 48px; height: 48px; border: 3px solid #2d3449; border-top: 3px solid #03dbe7; border-radius: 50%; animation: spin 1s linear infinite; }
                @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                .barcode-stripes { background: repeating-linear-gradient(90deg, #03dbe7, #03dbe7 2px, transparent 2px, transparent 6px, #03dbe7 6px, #03dbe7 10px, transparent 10px, transparent 12px, #03dbe7 12px, #03dbe7 14px); }
            `}</style>

            {/* Header */}
            <header className="fixed top-0 w-full z-50 bg-[#0b1326]/70 backdrop-blur-xl border-b border-gray-800 shadow-sm flex justify-between items-center px-6 md:px-12 py-4">
                <div className="flex items-center gap-4">
                    <Link to="/home" className="active:scale-95 transition-transform text-[#b4c5ff]">
                        <span className="material-symbols-outlined">home</span>
                    </Link>
                    <h1 className="text-2xl md:text-3xl font-bold tracking-tighter text-[#b4c5ff] uppercase">TicketFlow</h1>
                </div>
                <div className="flex items-center gap-4">
                

                    {!isGuest ? (
                        <button onClick={handleLogout} className="bg-[#2d3449] hover:bg-[#171f33] border border-gray-600 text-white px-5 py-2 rounded font-bold text-xs tracking-widest transition-colors z-50 relative">SIGN OUT</button>
                    ) : (
                        <Link to="/login" className="bg-[#2563eb] hover:bg-[#0053db] text-white px-5 py-2 rounded font-bold text-xs tracking-widest transition-colors z-50 relative">SIGN IN</Link>
                    )}
                </div>
            </header>

            <main className="pt-28 pb-12 px-6 md:px-12 min-h-screen max-w-7xl mx-auto flex flex-col relative z-0">

                <section className="mb-8">
                    <h2 className="text-4xl md:text-5xl font-black text-white mb-2 uppercase">Order History</h2>
                    <p className="text-gray-400 max-w-xl mb-6">Review your past premium experiences. Secure, encrypted, and immutable ticketing records.</p>
                    
                    
                    {(isAdmin || isCompanyManager) && (
                        <div className="flex gap-6 border-b border-gray-800 w-full mb-4 overflow-x-auto">
                            <button onClick={() => setViewMode('PERSONAL')} className={`pb-3 font-bold text-sm tracking-widest uppercase transition-colors whitespace-nowrap ${viewMode === 'PERSONAL' ? 'text-[#03dbe7] border-b-2 border-[#03dbe7]' : 'text-gray-500 hover:text-gray-300'}`}>
                                My Personal Orders
                            </button>
                            
                            
                            {isCompanyManager && (
                                <button onClick={() => setViewMode('COMPANY')} className={`pb-3 font-bold text-sm tracking-widest uppercase transition-colors whitespace-nowrap ${viewMode === 'COMPANY' ? 'text-[#03dbe7] border-b-2 border-[#03dbe7]' : 'text-gray-500 hover:text-gray-300'}`}>
                                    Company Orders
                                </button>
                            )}
                            
                        
                            {isAdmin && (
                                <button onClick={() => setViewMode('ALL')} className={`pb-3 font-bold text-sm tracking-widest uppercase transition-colors whitespace-nowrap ${viewMode === 'ALL' ? 'text-[#03dbe7] border-b-2 border-[#03dbe7]' : 'text-gray-500 hover:text-gray-300'}`}>
                                    All System Orders
                                </button>
                            )}
                        </div>
                    )}

                    {viewMode === 'COMPANY' && isCompanyManager && companyIds.length > 1 && (
                        <div className="mb-6 flex items-center gap-4 bg-[#171f33] border border-gray-800 p-4 rounded-lg w-max">
                            <span className="text-sm font-bold text-gray-400 uppercase tracking-widest">Select Company:</span>
                            <select 
                                value={selectedCompanyId || ''} 
                                onChange={(e) => setSelectedCompanyId(Number(e.target.value))}
                                className="bg-[#0b1326] text-[#03dbe7] font-mono border border-gray-700 rounded px-3 py-1 outline-none focus:border-[#03dbe7]"
                            >
                                {companyIds.map(id => (
                                    <option key={id} value={id}>Company #{id} - {permissions?.productionRoles[id]}</option>
                                ))}
                            </select>
                        </div>
                    )}
                </section>

                {isLoading && (
                    <div className="flex-grow flex flex-col items-center justify-center py-20">
                        <div className="loader-ring mb-6"></div>
                        <p className="text-[#03dbe7] font-mono text-sm tracking-widest animate-pulse uppercase">Retrieving Records...</p>
                    </div>
                )}

                {/* ERROR STATE: Shows Clear Error when accessing forbidden history */}
                {!isLoading && error && (
                    <div className="flex-grow flex items-center justify-center py-10 w-full animate-in fade-in">
                        <div className="bg-[#171f33] border border-red-900/50 p-10 md:p-16 rounded-xl text-center max-w-lg shadow-2xl relative overflow-hidden">
                            <div className="absolute top-0 left-0 w-full h-1 bg-red-600"></div>
                            <span className="material-symbols-outlined text-[64px] text-red-500 mb-4" style={{ fontVariationSettings: "'FILL' 1" }}>gpp_bad</span>
                            <h3 className="text-2xl font-bold text-white mb-2 uppercase tracking-wide">Access Denied</h3>
                            <p className="text-gray-400 mb-2">{error.message}</p>
                            <p className="text-xs text-red-400 font-mono mb-8 opacity-70">ERROR CODE: {error.status}</p>
                            <div className="flex gap-4 justify-center">
                                <button onClick={() => setViewMode('PERSONAL')} className="bg-[#2d3449] hover:bg-gray-700 text-white px-6 py-3 rounded font-bold text-xs tracking-widest transition-colors">BACK TO MY ORDERS</button>
                            </div>
                        </div>
                    </div>
                )}

                {!isLoading && !error && orders.length === 0 && (
                    <div className="flex-grow flex flex-col items-center justify-center py-20 text-center animate-in fade-in duration-700">
                        <div className="w-24 h-24 bg-[#171f33] rounded-full flex items-center justify-center border border-gray-800 mb-6 shadow-xl">
                            <span className="material-symbols-outlined text-[#2d3449] text-[48px]">receipt_long</span>
                        </div>
                        <h3 className="text-2xl font-bold text-white mb-2 uppercase tracking-wide">No Records Found</h3>
                        <p className="text-gray-400 max-w-sm mb-8">
                            {viewMode === 'PERSONAL' ? "Your transaction history is currently empty." : "No orders found in this category."}
                        </p>
                        {viewMode === 'PERSONAL' && (
                            <Link to="/events" className="bg-[#2563eb] text-white px-8 py-4 rounded font-bold text-sm tracking-widest transition-all hover:bg-[#0053db] shadow-[0_0_20px_rgba(37,99,235,0.4)] flex items-center gap-2">
                                BROWSE EVENTS <span className="material-symbols-outlined text-sm">arrow_forward</span>
                            </Link>
                        )}
                    </div>
                )}

                {/* RICH EVENT CARDS WITH SEAT DETAILS */}
                {!isLoading && !error && orders.length > 0 && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 animate-in slide-in-from-bottom-4 duration-500">
                        {orders.map((order) => {
                            const isRecent = new Date().getTime() - new Date(order.purchaseDate).getTime() < 86400000 * 7; 
                            const hasSeats = order.seatIds && order.seatIds.length > 0;
                            const hasStanding = order.standingAreaQuantities && Object.keys(order.standingAreaQuantities).length > 0;
                            return (
                                <div key={order.orderId} className="bg-[#171f33] border border-gray-800 text-[#dbe2fd] rounded-xl overflow-hidden flex flex-col shadow-xl transform transition-all hover:-translate-y-1 hover:border-[#75f5ff]/50 group relative z-10">
                                    <div className="h-48 relative overflow-hidden bg-[#0b1326] border-b border-gray-800">
                                        {order.eventImageUrl ? (
                                            <img src={order.eventImageUrl} alt="Event Cover" className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700 opacity-80" />
                                        ) : (
                                            <div className="absolute inset-0 opacity-20 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-[#2563eb] via-[#0b1326] to-[#0b1326] flex items-center justify-center">
                                                <span className="material-symbols-outlined text-[64px] text-gray-700">confirmation_number</span>
                                            </div>
                                        )}
                                        
                                        <div className="absolute top-4 left-4 flex gap-2">
                                            {isRecent && <span className="px-3 py-1 rounded text-[10px] font-mono uppercase tracking-widest font-bold bg-[#03dbe7] text-[#00363a] shadow-lg">NEW ORDER</span>}
                                            {order.category && <span className="px-3 py-1 rounded text-[10px] font-mono uppercase tracking-widest font-bold bg-[#171f33] text-[#eeefff] shadow-lg border border-gray-700">{order.category}</span>}
                                        </div>
                                        <div className="absolute bottom-4 right-4 px-3 py-1 rounded text-[10px] font-mono bg-[#0b1326]/80 text-gray-400 border border-gray-700 backdrop-blur-sm">
                                            ORD: {order.orderId.substring(0, 8)}
                                        </div>
                                    </div>

                                    <div className="p-5 flex-1 flex flex-col">
                                        <div className="mb-4 border-b border-gray-800 pb-4">
                                            <h3 className="text-xl font-bold leading-tight uppercase mb-2">
                                                {order.eventName || `Event #${order.eventId}`}
                                            </h3>
                                            <div className="flex flex-col gap-1.5 text-gray-400 text-sm">
                                                <div className="flex items-center gap-2">
                                                    <span className="material-symbols-outlined text-[16px]">calendar_today</span>
                                                    <span>{order.eventDate ? formatDate(order.eventDate, true) : "Date TBD"}</span>
                                                </div>
                                                <div className="flex items-center gap-2">
                                                    <span className="material-symbols-outlined text-[16px]">location_on</span>
                                                    <span>{order.eventLocation || "Venue TBD"}</span>
                                                </div>
                                            </div>
                                        </div>

                                        <div className="mb-6 flex-grow">
                                            {hasSeats && (
                                                <div className="mb-3">
                                                    <span className="text-[10px] text-gray-500 uppercase font-bold tracking-wider mb-1 block">Selected Seats</span>
                                                    <div className="flex flex-wrap gap-2">
                                                        {order.seatIds.map((seat, idx) => (
                                                            <span key={idx} className="bg-[#2d3449] border border-gray-700 text-[#03dbe7] px-2 py-1 rounded text-xs font-mono font-bold">
                                                                {seat}
                                                            </span>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}
                                            
                                            {hasStanding && (
                                                <div>
                                                    <span className="text-[10px] text-gray-500 uppercase font-bold tracking-wider mb-1 block">General Admission</span>
                                                    <div className="flex flex-wrap gap-2">
                                                        {Object.entries(order.standingAreaQuantities).map(([type, qty], idx) => (
                                                            <span key={idx} className="bg-[#2d3449] border border-gray-700 text-[#03dbe7] px-2 py-1 rounded text-xs font-mono font-bold">
                                                                {qty}x {type}
                                                            </span>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}

                                            {!hasSeats && !hasStanding && (
                                                <p className="text-xs text-gray-600 font-mono italic">No seating details available.</p>
                                            )}
                                        </div>

                                        <div className="mt-auto space-y-4">
                                            <div className="flex justify-between items-end border-t border-gray-800 pt-4">
                                                <div>
                                                    <p className="text-[10px] uppercase font-bold mb-1 text-gray-400 tracking-wider">Total Value</p>
                                                    <p className="font-mono text-2xl font-bold text-white">${order.price.toFixed(2)}</p>
                                                </div>
                                                <div className="text-right">
                                                    <p className="text-[10px] uppercase font-bold mb-1 text-gray-400 tracking-wider">Status</p>
                                                    <p className="font-mono text-sm text-green-400">CONFIRMED</p>
                                                </div>
                                            </div>

                                            <button onClick={() => setSelectedTicket(order)} className="w-full flex justify-center items-center gap-2 py-3 rounded-lg font-bold text-xs tracking-widest active:scale-95 transition-all shadow-md bg-[#2563eb] text-[#eeefff] hover:bg-[#0053db] border-t border-white/20">
                                                VIEW TICKET
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}

            </main>

            {/* MODAL: VIEW TICKET (POPUP) */}
            {selectedTicket && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 sm:p-6 backdrop-blur-md bg-[#0b1326]/80 overflow-y-auto animate-in fade-in duration-300">
                    <div className="absolute inset-0" onClick={() => setSelectedTicket(null)}></div>
                    
                    <div className="relative w-full max-w-4xl bg-[#171f33] border border-gray-700 rounded-2xl flex flex-col md:flex-row overflow-hidden shadow-[0_0_40px_rgba(3,219,231,0.15)] my-auto">
                        <button onClick={() => setSelectedTicket(null)} className="absolute top-4 right-4 z-50 bg-[#0b1326]/50 hover:bg-[#03dbe7] hover:text-[#0b1326] text-white p-2 rounded-full transition-colors flex items-center justify-center">
                            <span className="material-symbols-outlined text-[20px]">close</span>
                        </button>

                        <div className="p-8 md:p-10 flex-1 relative z-10">
                            <div className="absolute -right-10 -bottom-10 opacity-[0.03] text-[200px] font-black pointer-events-none select-none">TF</div>

                            <div className="flex justify-between items-start mb-8 pr-8">
                                <div>
                                    <p className="text-[#03dbe7] text-[10px] uppercase font-bold tracking-[0.2em] mb-2">Entry Pass</p>
                                    <h3 className="text-3xl font-black text-white leading-tight uppercase">
                                        {selectedTicket.eventName || `EVENT #${selectedTicket.eventId}`}
                                    </h3>
                                </div>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-8 border-y border-gray-800 py-6">
                                <div>
                                    <p className="text-gray-500 text-[10px] uppercase font-bold tracking-[0.2em] mb-1">Event Date</p>
                                    <p className="font-mono text-sm text-[#03dbe7] font-bold">
                                        {selectedTicket.eventDate ? formatDate(selectedTicket.eventDate, true) : "TBD"}
                                    </p>
                                </div>
                                <div>
                                    <p className="text-gray-500 text-[10px] uppercase font-bold tracking-[0.2em] mb-1">Purchase Date</p>
                                    <p className="font-mono text-sm text-[#dbe2fd]">{formatDate(selectedTicket.purchaseDate)}</p>
                                </div>
                                <div>
                                    <p className="text-gray-500 text-[10px] uppercase font-bold tracking-[0.2em] mb-1">Location</p>
                                    <p className="font-mono text-sm text-[#dbe2fd]">{selectedTicket.eventLocation || "TBD Sector / Main Arena"}</p>
                                </div>
                                <div>
                                    <p className="text-gray-500 text-[10px] uppercase font-bold tracking-[0.2em] mb-1">Admissions</p>
                                    <p className="font-mono text-sm text-white font-bold">
                                        {getTicketQuantity(selectedTicket).total} Total (Seats: {getTicketQuantity(selectedTicket).seatCount}, Gen: {getTicketQuantity(selectedTicket).standingCount})
                                    </p>
                                </div>
                            </div>

                            {selectedTicket.seatIds && selectedTicket.seatIds.length > 0 && (
                                <div className="mb-6">
                                    <p className="text-gray-500 text-[10px] uppercase font-bold tracking-[0.2em] mb-3">Reserved Seats</p>
                                    <div className="flex flex-wrap gap-2">
                                        {selectedTicket.seatIds.map((seat, idx) => (
                                            <span key={idx} className="bg-[#0b1326] border border-gray-700 text-[#03dbe7] px-4 py-2 rounded-md font-mono text-sm font-bold">
                                                {seat}
                                            </span>
                                        ))}
                                    </div>
                                </div>
                            )}

                            <div className="mt-8 flex flex-col sm:flex-row gap-3 pt-6 border-t border-gray-800">
                                <button className="bg-[#2d3449] hover:bg-gray-700 text-white px-6 py-3 rounded-lg font-bold text-xs tracking-widest transition-colors flex items-center justify-center gap-2 flex-1">
                                    <span className="material-symbols-outlined text-[18px]">download</span> PDF
                                </button>
                                <button className="bg-white hover:bg-gray-200 text-black px-6 py-3 rounded-lg font-bold text-xs tracking-widest transition-colors flex items-center justify-center gap-2 flex-1">
                                    <span className="material-symbols-outlined text-[18px]">apple</span> WALLET
                                </button>
                            </div>
                        </div>

                        <div className="bg-[#0b1326] p-8 md:p-10 border-t md:border-t-0 md:border-l border-dashed border-gray-700 flex flex-col items-center justify-center relative md:w-[300px] flex-shrink-0">
                            <div className="hidden md:block absolute -left-4 top-1/2 -translate-y-1/2 w-8 h-8 bg-[#0b1326] rounded-full border-r border-gray-700"></div>
                            <p className="text-gray-500 text-[10px] uppercase font-bold tracking-[0.2em] mb-6 text-center">Scan at Entrance</p>
                            <div className="w-40 h-40 bg-white p-2 rounded-lg mb-6 shadow-[0_0_20px_rgba(3,219,231,0.2)]">
                                <svg viewBox="0 0 100 100" className="w-full h-full text-black">
                                    <rect width="100" height="100" fill="white"/>
                                    <path d="M10,10 h20 v20 h-20 z M15,15 h10 v10 h-10 z M70,10 h20 v20 h-20 z M75,15 h10 v10 h-10 z M10,70 h20 v20 h-20 z M15,75 h10 v10 h-10 z M40,10 h20 v10 h-20 z M45,25 h10 v10 h-10 z M10,40 h10 v20 h-10 z M25,45 h10 v10 h-10 z M40,40 h20 v20 h-20 z M70,40 h20 v10 h-20 z M75,55 h10 v10 h-10 z M40,70 h10 v20 h-10 z M55,75 h10 v10 h-10 z M70,70 h10 v10 h-10 z M85,85 h5 v5 h-5 z M75,80 h5 v5 h-5 z" fill="black"/>
                                </svg>
                            </div>
                            <div className="w-full h-12 barcode-stripes opacity-80 mb-4"></div>
                            <p className="font-mono text-[#03dbe7] text-sm font-bold tracking-widest break-all text-center">{selectedTicket.orderId}</p>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
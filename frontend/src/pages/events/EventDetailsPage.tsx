import { useParams, Link, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { eventApi, EventDTO } from '../../api/eventsApi';
import { activeOrderApi } from '../../api/activeOrderApi';
import { getUserFriendlyError } from '../../utils/errorUtils';
export default function EventDetailsPage() {
    const { eventId } = useParams();
    const navigate = useNavigate(); // Added for redirection

    const [eventData, setEventData] = useState<EventDTO | null>(null);
    const [policyDesc, setPolicyDesc] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isCreatingOrder, setIsCreatingOrder] = useState(false); // Loading state for the button
    const [orderError, setOrderError] = useState<string | null>(null); // Error state for failed orders
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [isAuthorized] = useState(true);
    useEffect(() => {
        const fetchSingleEvent = async () => {
            if (!eventId) return;
            console.log(`--- [UI START] Fetching Event Details for: ${eventId} ---`);
            setIsLoading(true);
            setErrorMessage(null);

            try {
                const token = localStorage.getItem('token') || '';
                const data = await eventApi.getEvent(token, eventId);
                if (!data) {
                    setErrorMessage(`The event ID "${eventId}" does not exist or has been removed.`);
                } else {
                    setEventData(data);
                }
                console.log('[UI STEP 1 RESULT] Event details mapped to state.');

                // Fetch event purchase policy description
                const policyRes = await fetch(`/api/policies/event/${eventId}`, {
                    headers: { 'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}` }
                });
                if (policyRes.ok) {
                    const policyData = await policyRes.json();
                    setPolicyDesc(policyData.description || null);
                }
            } catch (error: any) {
                console.error('[UI CATCH BLOCK] Failed to fetch specific event or policy:', error);
                setErrorMessage(
                    getUserFriendlyError(error) ||
                    "Unable to load event details at this time."
                );
            } finally {
                setIsLoading(false);
                console.log('--- [UI END] Event Details Fetch Completed ---');
            }
        };

        fetchSingleEvent();
    }, [eventId]);

    // NEW: Function to handle the start order process
    const handleStartOrder = async () => {
        if (!eventId) return;

        setIsCreatingOrder(true);
        setOrderError(null);

        try {
            const token = localStorage.getItem('token') || '';
            // Assuming you store the logged-in user's ID in localStorage.
            // Adjust this if you extract it from the JWT token instead!
            const userId = localStorage.getItem('userId') || 'fallback-user-id';
            console.log("got here70");
            const existingOrder = await activeOrderApi.getActiveOrderByUserId(token, userId);
            console.log("got here72", existingOrder);
            if (existingOrder) {
                if (existingOrder.eventId === eventId) {
                    navigate('/orders/active');
                    return;
                } else {
                    const eventName = await eventApi.getEvent(token, existingOrder.eventId).then(e => e?.eventName || 'your current event');
                    setOrderError(`You already have an active order for: ${eventName}`);
                    return;
                }
            }
            console.log("got here81");
            const response = await activeOrderApi.createOrder(token, {
                userId: userId,
                eventId: eventId
            });

            if (response && response.orderId) {
                // Success! Redirect to the reserve page
                navigate(`/events/${eventId}/reserve`);
            }
        } catch (error: any) {
            console.error('[ORDER ERROR] Failed to create order:', error.message);
            const message = error?.message ?? '';
            if (message.toLowerCase().includes('active order')) {
                navigate('/orders/active');
                return;
            }

            setOrderError(
                message || "Unable to start your order. Please try again."
            );
        } finally {
            setIsCreatingOrder(false);
        }
    };

    const isPast = eventData?.eventDateTime ? new Date(eventData.eventDateTime) < new Date() : false;

    if (isLoading) {
        return (
            <div className="bg-[#0b1326] min-h-screen flex items-center justify-center">
                <span className="material-symbols-outlined animate-spin text-5xl text-[#2563eb]">refresh</span>
            </div>
        );
    }

    if (errorMessage || !eventData) {
        return (
            <div className="bg-[#0b1326] text-[#dbe2fd] min-h-screen flex flex-col items-center justify-center p-6">
                <span className="material-symbols-outlined text-6xl text-red-500 mb-4">error</span>
                <h1 className="text-3xl font-black text-white mb-2">EVENT NOT FOUND</h1>
                <p className="text-gray-400 mb-6">{errorMessage || `The event ID "${eventId}" does not exist or has been removed.`}</p>
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
                {/* Displaying Order Error if API call fails */}
                {orderError && (
                    <div className="bg-red-900/50 border border-red-500 text-red-200 p-4 rounded-lg text-center font-bold tracking-wider">
                        {orderError}
                    </div>
                )}

                <div>
                    <span className="bg-[#03dbe7]/10 text-[#03dbe7] px-3 py-1 rounded text-xs font-mono border border-[#03dbe7]/20 uppercase">
                        {eventData.eventId || eventId}
                    </span>
                    <h1 className="text-4xl md:text-5xl font-black text-white mt-4 uppercase leading-tight">{eventData.eventName}</h1>
                    {eventData.eventLocation && (
                        <p className="flex items-center gap-2 text-gray-400 mt-2 text-sm">
                            <span className="material-symbols-outlined text-[#03dbe7] text-base">location_on</span>
                            {eventData.eventLocation}
                        </p>
                    )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="bg-[#171f33] border border-gray-800 p-6 rounded-xl flex items-start gap-4">
                        <span className="material-symbols-outlined text-[#03dbe7] text-3xl">calendar_month</span>
                        <div>
                            <p className="text-xs uppercase font-bold text-gray-500 mb-1">Date & Time</p>
                            <p className="font-bold text-lg">
                                {eventData.eventDateTime
                                    ? new Date(eventData.eventDateTime).toLocaleString('en-US', {
                                        month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit'
                                    })
                                    : 'TBD'}
                            </p>
                        </div>
                    </div>

                    <div className="bg-[#171f33] border border-gray-800 p-6 rounded-xl flex items-start gap-4">
                        <span className="material-symbols-outlined text-[#03dbe7] text-3xl">location_on</span>
                        <div>
                            <p className="text-xs uppercase font-bold text-gray-500 mb-1">Venue</p>
                            <p className="font-bold text-lg">{eventData.eventLocation || 'TBD'}</p>
                        </div>
                    </div>

                    <div className="bg-[#171f33] border border-gray-800 p-6 rounded-xl flex items-start gap-4">
                        <span className="material-symbols-outlined text-[#03dbe7] text-3xl">group</span>
                        <div>
                            <p className="text-xs uppercase font-bold text-gray-500 mb-1">Tickets Available</p>
                            <p className="font-bold text-lg">{eventData.eventCapacity} Tickets</p>
                        </div>
                    </div>
                </div>

                {policyDesc && policyDesc !== 'No policy' && (
                    <div className="bg-amber-500/5 border border-amber-500/20 p-8 rounded-xl space-y-3">
                        <h3 className="text-xl font-bold text-amber-300 flex items-center gap-2">
                            <span className="material-symbols-outlined">policy</span>
                            Purchase Policy Summary
                        </h3>
                        <p className="text-gray-300 leading-relaxed font-mono text-sm">{policyDesc}</p>
                    </div>
                )}

                <div className="bg-[#eeefff] text-[#171f33] p-8 rounded-xl shadow-2xl flex flex-col md:flex-row items-center justify-between gap-6">
                    <div>
                        <p className="text-sm uppercase font-bold opacity-60 mb-1">price</p>
                        {/* FIXED: Using ticketPrice instead of price */}
                        <p className="text-4xl font-mono font-black">{eventData.minZonePrice && eventData.maxZonePrice ? `$${eventData.minZonePrice} - $${eventData.maxZonePrice}` : 'TBD'}</p>
                    </div>

                    {isPast ? (
                        <div className="flex items-center gap-2 bg-gray-700/50 text-white px-6 py-4 rounded-lg font-bold border border-gray-600 text-xs tracking-widest uppercase">
                            <span className="material-symbols-outlined">event_busy</span>
                            THIS EVENT HAS ALREADY TAKEN PLACE
                        </div>
                    ) : eventData.isActive === false ? (
                        <div className="flex items-center gap-2 bg-red-100 text-red-800 px-6 py-4 rounded-lg font-bold border border-red-200">
                            <span className="material-symbols-outlined">event_busy</span>
                            EVENT CANCELED — NO LONGER AVAILABLE
                        </div>
                    ) : !isAuthorized ? (
                        <div className="flex items-center gap-2 bg-red-100 text-red-800 px-6 py-4 rounded-lg font-bold border border-red-200">
                            <span className="material-symbols-outlined">block</span>
                            PURCHASE UNAVAILABLE
                        </div>
                    ) : (
                        <button
                            onClick={handleStartOrder}
                            disabled={isCreatingOrder}
                            className={`w-full md:w-auto text-white px-10 py-4 rounded-xl font-bold tracking-widest transition-colors shadow-lg flex items-center justify-center gap-2
                                ${isCreatingOrder
                                ? 'bg-gray-500 cursor-not-allowed'
                                : 'bg-[#2563eb] hover:bg-[#0053db] active:scale-95'}`}
                        >
                            {isCreatingOrder ? (
                                <>
                                    <span className="material-symbols-outlined animate-spin text-sm">refresh</span>
                                    STARTING ORDER...
                                </>
                            ) : (
                                "RESERVE TICKETS"
                            )}
                        </button>
                    )}
                </div>
            </main>
        </div>
    );
}
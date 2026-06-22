import { useState, useEffect, useRef } from 'react';
import { authApi, UserProfileDTO } from '../../api/authApi';
import { activeOrderApi, ActiveOrderDTO } from '../../api/activeOrderApi';
import { eventApi, EventDTO, SeatingMapDTO, PurchasePolicyDTO } from '../../api/eventsApi';

// ─── Route Constants ────────────────────────────────────────────────────────
const RESERVE_ROUTE = '/events/:eventId/reserve';
const EVENTS_ROUTE = '/events';
const CHECKOUT_ROUTE = '/checkout';

type CheckoutStatus = 'idle' | 'processing' | 'finalizing' | 'purchased' | 'expired_canceled';

// Helper function to decode the backend format "zone_row_number" (e.g., "0_1_1")
const parseSeatId = (seatId: string) => {
  const match = seatId.match(/^(\d+)_(\d+)_(\d+)$/);
  if (!match) return { zone: seatId, row: '-', number: '-' };
  return {
    zone: match[1],
    row: match[2],
    number: match[3],
  };
};

export default function ActiveOrderPage() {
  // ─── Core Data State ────────────────────────────────────────────────────────
  const [user, setUser] = useState<UserProfileDTO | null>(null);
  const [activeOrder, setActiveOrder] = useState<ActiveOrderDTO | null>(null);
  const [event, setEvent] = useState<EventDTO | null>(null);
  const [seatingMap, setSeatingMap] = useState<SeatingMapDTO | null>(null);
  const [purchasePolicy, setPurchasePolicy] = useState<PurchasePolicyDTO | null>(null);
  const minSeatsAllowed = purchasePolicy?.minTickets || 1;
  // ─── UX/UI State ────────────────────────────────────────────────────────────
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isCanceling, setIsCanceling] = useState<boolean>(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [hasNoActiveOrder, setHasNoActiveOrder] = useState<boolean>(false);
  const [timeLeft, setTimeLeft] = useState<number>(900); // 15:00 default fallback
  const [isPolicyErrorVisible, setIsPolicyErrorVisible] = useState<boolean>(false);
  const [checkoutState, setCheckoutState] = useState<CheckoutStatus>('idle');
  const [showToast, setShowToast] = useState<boolean>(false);

  // Ref tracking cancellation status prevents double invocation during tight state loops
  const hasCanceledRef = useRef<boolean>(false);

  // ─── Data Initialization Pipeline ──────────────────────────────────────────
  useEffect(() => {
    const fetchPageData = async () => {
      try {
        setIsLoading(true);
        setApiError(null);

        // 1. Get session token from localStorage
        const token = localStorage.getItem('token');
        if (!token) {
          throw new Error('No session token found. Please log in to proceed.');
        }

        // 2. Fetch User Profile
        const userProfile = await authApi.getCurrentUser(token);
        setUser(userProfile);

        // 3. Fetch Active Order — null result OR a thrown error both mean the
        //    user has no active order, so we handle them identically.
        let orderData: ActiveOrderDTO | null = null;
        try {
          orderData = await activeOrderApi.getActiveOrderByUserId(token, userProfile.userId);
        } catch {
          setHasNoActiveOrder(true);
          setIsLoading(false);
          return;
        }

        if (!orderData || !orderData.eventId) {
          setHasNoActiveOrder(true);
          setIsLoading(false);
          return;
        }

        setActiveOrder(orderData);

        // 4. Fetch Event Details, Seating Map, & Purchase Policy concurrently
        const [eventDetails, seatingMapData, policyData] = await Promise.all([
          eventApi.getEvent(token, orderData.eventId),
          eventApi.getEventSeatingMap(token, orderData.eventId),
          eventApi.getEventPurchasePolicy(token, orderData.eventId) // Add this line!
        ]);

        if (!eventDetails) {
          throw new Error('Failed to retrieve details for the assigned event.');
        }

        console.log('[ActiveOrderPage Debug] Fetched Event Data:', eventDetails);

        setEvent(eventDetails);
        setSeatingMap(seatingMapData);
        setPurchasePolicy(policyData); // Save the fetched policy to state

        // 5. Setup Timer
        const rawData = orderData as any;
        const orderCreatedMs = rawData.createdAt || rawData.createdTime || rawData.timestamp || Date.now();
        
        const elapsedSeconds = Math.floor((Date.now() - new Date(orderCreatedMs).getTime()) / 1000);
        const maxReservationSeconds = 15 * 60; 
        const remainingSeconds = maxReservationSeconds - elapsedSeconds;

        if (remainingSeconds <= 0) {
          setTimeLeft(0);
        } else {
          setTimeLeft(remainingSeconds <= maxReservationSeconds ? remainingSeconds : maxReservationSeconds);
        }

      } catch (err: any) {
        setApiError(err.message || 'An error occurred while setting up your checkout.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchPageData();
  }, []);

  // ─── Reservation Timer Effect ────────────────────────────────────────────────
  useEffect(() => {
    if (isLoading || apiError || checkoutState === 'expired_canceled') return;
    
    if (timeLeft <= 0) {
      handleOrderExpiration();
      return;
    }

    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isLoading, apiError, timeLeft, checkoutState]);

  // ─── Server-Side Expiration Handshake ───────────────────────────────────────
  const handleOrderExpiration = async () => {
    if (hasCanceledRef.current) return;
    hasCanceledRef.current = true;

    const token = localStorage.getItem('token');
    const orderId = activeOrder?.orderId;
    const userId = user?.userId;

    if (token && orderId && userId) {
      try {
        setCheckoutState('expired_canceled');
        await activeOrderApi.cancelOrder(token, orderId.toString(), userId.toString());
      } catch (err) {
        console.error('Failed to auto-cancel expired order on server:', err);
      }
    } else {
      setCheckoutState('expired_canceled');
    }
  };

  // ─── Manual Order Cancellation Handler ──────────────────────────────────────
  const handleManualCancel = async () => {
    if (isCanceling || checkoutState !== 'idle') return;
    
    const confirmCancel = window.confirm("Are you sure you want to cancel your order? Your reserved seats will be released.");
    if (!confirmCancel) return;

    const token = localStorage.getItem('token');
    const orderId = activeOrder?.orderId;
    const userId = user?.userId;

    if (token && orderId && userId) {
      try {
        setIsCanceling(true);
        hasCanceledRef.current = true; // Block the background expiration mechanism
        await activeOrderApi.cancelOrder(token, orderId.toString(), userId.toString());
        setCheckoutState('expired_canceled');
      } catch (err: any) {
        console.error('Failed to cancel order:', err);
        alert(err.message || 'Failed to cancel order securely. Please try again.');
        hasCanceledRef.current = false;
      } finally {
        setIsCanceling(false);
      }
    } else {
      setCheckoutState('expired_canceled');
    }
  };

  // ─── Keydown Listener for Policy Toggle ('e' key) ──────────────────────────
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key.toLowerCase() === 'e') {
        setIsPolicyErrorVisible((prev) => !prev);
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // ─── Calculations & Ticket Pricing Engine ──────────────────────────────────
  const seatIdsArray: string[] = activeOrder?.seatIds || [];
  
  const rawOrder = activeOrder as any;
  const standingAreaQuantitiesMap = rawOrder?.StandingAreaQuantities || rawOrder?.standingAreaQuantities || rawOrder?.standinAreaQuantities || {};

  const seatTicketsWithPrices = seatIdsArray.map((seatId) => {
    const matchedSeatConfig = seatingMap?.assignedSeats?.find((s) => s.id === seatId);
    return {
      seatId,
      price: matchedSeatConfig ? matchedSeatConfig.priceForTicket : 0,
    };
  });

  const standingAreasWithPrices = Object.entries(standingAreaQuantitiesMap).map(([areaId, quantity]) => {
    const matchedAreaConfig = seatingMap?.standingAreas?.find((a) => a.areaId === areaId);
    return {
      areaId,
      quantity: Number(quantity),
      pricePerTicket: matchedAreaConfig ? matchedAreaConfig.priceForTicket : 0,
      totalTierPrice: (matchedAreaConfig ? matchedAreaConfig.priceForTicket : 0) * Number(quantity),
    };
  });

  const totalSeatsPrice = seatTicketsWithPrices.reduce((sum, item) => sum + item.price, 0);
  const totalStandingPrice = standingAreasWithPrices.reduce((sum, item) => sum + item.totalTierPrice, 0);
  const computedSubtotal = totalSeatsPrice + totalStandingPrice;

  const totalStandingTicketsQty = Object.values(standingAreaQuantitiesMap).reduce((sum: number, qty: any) => sum + Number(qty), 0);
  const totalTicketsCount = seatIdsArray.length + totalStandingTicketsQty;

  const totalDue = Math.max(0, computedSubtotal);

  // ─── Helpers ────────────────────────────────────────────────────────────────
  const formatTime = (totalSeconds: number) => {
    if (totalSeconds <= 0) return 'EXPIRED';
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  const formatDate = (isoString?: string) => {
    if (!isoString) return 'Date TBD';
    return new Date(isoString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const handleCheckoutSimulation = () => {
    if (checkoutState !== 'idle') return;
    setCheckoutState('processing');
    setTimeout(() => {
      setCheckoutState('finalizing');
      setTimeout(() => {
        setCheckoutState('purchased');
        setShowToast(true);
        setTimeout(() => {
          window.location.href = CHECKOUT_ROUTE;
        }, 800);
      }, 1000);
    }, 1500);
  };

  // ─── View Controller Returns ────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#f8f9ff]">
        <div className="w-12 h-12 border-4 border-black border-t-transparent rounded-full animate-spin mb-4" />
        <p className="text-sm font-semibold tracking-wide text-[#4c4546]">Securing your session data...</p>
      </div>
    );
  }

  if (hasNoActiveOrder) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#f8f9ff] p-6">
        <div className="bg-white border border-[#e2e2e9] p-10 rounded-2xl max-w-lg text-center shadow-lg">
          <div className="w-16 h-16 bg-[#f3f3fa] rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="material-symbols-outlined text-[#4c4546] text-3xl">receipt_long</span>
          </div>
          <h3 className="text-xl font-bold text-black mb-2">No Active Order Found</h3>
          <p className="text-sm text-[#4c4546] leading-relaxed mb-8">
            You don't have an active order. Browse our events and select your tickets to get started.
          </p>
          <a
            href={EVENTS_ROUTE}
            className="inline-block bg-black text-white w-full py-3 rounded-lg font-bold text-sm hover:opacity-90 transition-opacity"
          >
            Browse Events
          </a>
        </div>
      </div>
    );
  }

  if (apiError && checkoutState === 'idle') {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#f8f9ff] p-6">
        <div className="bg-white border border-[#e2e2e9] p-8 rounded-xl max-w-md text-center shadow-sm">
          <span className="material-symbols-outlined text-red-500 text-5xl mb-3">error_circle</span>
          <h3 className="text-lg font-bold text-black mb-2">Checkout Interrupted</h3>
          <p className="text-sm text-[#4c4546] mb-6">{apiError}</p>
          <button onClick={() => window.location.reload()} className="bg-black text-white px-6 py-2 rounded-lg font-semibold text-sm hover:opacity-90 transition-opacity">
            Retry Connection
          </button>
        </div>
      </div>
    );
  }

  if (checkoutState === 'expired_canceled') {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#f8f9ff] p-6">
        <div className="bg-white border border-[#e2e2e9] p-10 rounded-2xl max-w-lg text-center shadow-lg">
          <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="material-symbols-outlined text-red-600 text-3xl">gavel</span>
          </div>
          <h3 className="text-xl font-bold text-black mb-2">Reservation Handled / Expired</h3>
          <p className="text-sm text-[#4c4546] leading-relaxed mb-8">
            Your ticket holding session has ended or been canceled. To ensure equal platform availability, any pending allocations have been released back into general public stock.
          </p>
          <a href={RESERVE_ROUTE} className="inline-block bg-black text-white w-full py-3 rounded-lg font-bold text-sm hover:opacity-90 transition-opacity">
            Return to Ticket Selection
          </a>
        </div>
      </div>
    );
  }

  return (
    <div className="font-sans antialiased text-[#1a1b20] min-h-screen bg-[#f8f9ff]" style={{ fontFamily: "'Geist', sans-serif" }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Geist:wght@100..900&family=Geist+Mono:wght@100..900&display=swap');
        @import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap');
        .checkout-gradient { background: linear-gradient(135deg, #000000 0%, #1b1b1b 100%); }
        .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24; }
      `}</style>

      <main className="min-h-screen p-6 md:p-12">
        <div className="max-w-[1440px] mx-auto">
          
          <nav className="flex items-center gap-1 text-[#4c4546] text-xs font-semibold mb-6">
            <a className="hover:text-black" href="#">Events</a>
            <span className="material-symbols-outlined text-sm">chevron_right</span>
            <span className="hover:text-black">{event?.eventName || 'Loading Event Name...'}</span>
            <span className="material-symbols-outlined text-sm">chevron_right</span>
            <span className="text-black font-bold">Checkout</span>
          </nav>

          <div className="mb-6 p-4 rounded-lg flex items-center justify-between border transition-colors duration-300 bg-[#ffdad6] text-[#93000a] border-red-200 animate-pulse">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined">timer</span>
              <p className="text-sm font-bold">
                Reservation expires in <span>{formatTime(timeLeft)}</span>
              </p>
            </div>
            <p className="text-xs">Complete your purchase to secure these seats.</p>
          </div>

          {isPolicyErrorVisible && (
            <div className="mb-6 bg-amber-50 border-l-4 border-amber-500 p-4 flex gap-4 transition-all duration-300">
              <span className="material-symbols-outlined text-amber-600">warning</span>
              <div>
                <p className="text-sm font-semibold text-amber-900">Policy Violation Detected</p>
                <p className="text-amber-800 text-xs mt-0.5">Maximum 4 tickets per user for this event tier. Please adjust your quantity to proceed.</p>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
            
            <div className="lg:col-span-2 space-y-6">
              
              <div className="bg-white border border-[#e2e2e9] rounded-xl overflow-hidden shadow-sm">
                <div className="h-32 checkout-gradient relative">
                  <div className="absolute inset-0 opacity-20" style={{ backgroundImage: 'radial-gradient(circle at 2px 2px, white 1px, transparent 0)', backgroundSize: '24px 24px' }}></div>
                  <div className="absolute bottom-4 left-6">
                    <span className="bg-[#dcfce7] text-[#15803d] text-xs font-semibold px-3 py-1 rounded-full inline-flex items-center gap-1 mb-2">
                      <span className="w-2 h-2 rounded-full bg-[#15803d] animate-ping"></span>
                      Live Inventory
                    </span>
                    <h2 className="text-white text-2xl font-bold leading-tight">
                      {event?.eventName || 'Unnamed Event'}
                    </h2>
                    <p className="text-white/70 text-xs">
                      {formatDate(event?.eventDateTime)} • {event?.eventLocation || 'No Location Specified'}
                    </p>
                  </div>
                </div>
                
                <div className="p-6">
                  <h3 className="text-base font-semibold mb-4 flex items-center gap-2">
                    <span className="material-symbols-outlined">confirmation_number</span>
                    Selected Tickets ({totalTicketsCount})
                  </h3>
                  
                  <div className="space-y-3">
                    {/* Render Assigned Seat Tickets */}
                    {seatTicketsWithPrices.map((ticket, idx: number) => {
                      const { zone, row, number } = parseSeatId(ticket.seatId);
                      return (
                        <div key={`seat-${idx}`} className="flex items-center justify-between p-4 border border-[#e2e2e9] rounded-lg hover:border-black transition-colors">
                          <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded bg-black text-white flex items-center justify-center">
                              <span className="material-symbols-outlined">event_seat</span>
                            </div>
                            <div>
                              <p className="text-sm font-bold text-black">Zone {zone}</p>
                              <p className="text-[#4c4546] text-xs font-mono">Row {row} • Assigned Seat {number}</p>
                            </div>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-semibold">${ticket.price.toFixed(2)}</p>
                            <p className="text-[#4c4546] text-xs">Ticket ID: {ticket.seatId}</p>
                          </div>
                        </div>
                      );
                    })}

                    {/* Render Standing Area Entries */}
                    {standingAreasWithPrices.map((tier) => {
                      if (tier.quantity <= 0) return null;
                      return (
                        <div key={`standing-${tier.areaId}`} className="flex items-center justify-between p-4 border border-dashed border-[#e2e2e9] bg-[#fdfdfd] rounded-lg">
                          <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded bg-[#e8e7ee] flex items-center justify-center text-black">
                              <span className="material-symbols-outlined">groups</span>
                            </div>
                            <div>
                              <p className="text-sm font-semibold text-black">General Standing Admission</p>
                              <p className="text-[#4c4546] text-xs">Section Allocation ID: {tier.areaId} • (${tier.pricePerTicket.toFixed(2)} each)</p>
                            </div>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-semibold">${tier.totalTierPrice.toFixed(2)}</p>
                            <p className="text-[#4c4546] text-xs">Quantity: {tier.quantity}</p>
                          </div>
                        </div>
                      );
                    })}

                    {totalTicketsCount === 0 && (
                      <div className="p-6 border border-dashed border-amber-200 bg-amber-50/50 rounded-lg text-center">
                        <p className="text-sm font-medium text-amber-800">Your order selection is currently empty.</p>
                        <p className="text-xs text-amber-600 mt-0.5">Please add seats or standing areas to proceed with your checkout configuration.</p>
                      </div>
                    )}
                  </div>

                  <a 
                    href={RESERVE_ROUTE}
                    className="mt-5 inline-flex text-[#3980f4] text-xs font-semibold items-center gap-1 hover:underline transition-all"
                  >
                    <span className="material-symbols-outlined text-sm">edit</span>
                    Modify Selection / Edit Order
                  </a>
                </div>      
              </div>

              <div className="bg-white border border-[#e2e2e9] rounded-xl p-6 shadow-sm">
                <h3 className="text-base font-semibold mb-4 flex items-center gap-2">
                  <span className="material-symbols-outlined">receipt_long</span>
                  Billing Details
                </h3>
                <div className="space-y-1 text-sm text-[#4c4546]">
                  <p className="text-black font-bold">{user?.name || 'User Account'}</p>
                  <p>Profile ID Account: {user?.userId}</p>
                </div>
              </div>

            </div>

            {/* Right Column: Dynamic Price Summary Block */}
            <div className="space-y-6 lg:sticky lg:top-12">
              <div className="bg-white border border-[#e2e2e9] rounded-xl shadow-md overflow-hidden">
                <div className="bg-[#f3f3fa] p-6 border-b border-[#e2e2e9]">
                  <h3 className="text-lg font-bold">Order Summary</h3>
                  <p className="text-[#4c4546] text-xs font-medium font-mono">Invoice #{activeOrder?.orderId ? activeOrder.orderId.toString().substring(0, 8).toUpperCase() : 'PENDING'}</p>
                </div>
                <div className="p-6 space-y-4">
                  <div className="space-y-2 pb-4 border-b border-[#e2e2e9]">
                    <div className="flex justify-between text-sm">
                      <span className="text-[#4c4546]">Subtotal ({totalTicketsCount} Tickets)</span>
                      <span className="font-semibold text-black">${computedSubtotal.toFixed(2)}</span>
                    </div>
                  </div>
                  <div className="flex justify-between items-end pt-2">
                    <span className="text-sm font-semibold">Total Due</span>
                    <span className="text-xl font-bold text-black">${totalDue.toFixed(2)}</span>
                  </div>
                  <div className="pt-4 space-y-2">
                    {/* 1. Show a warning if they haven't met the minimum */}
                    {totalTicketsCount < minSeatsAllowed && (
                        <div className="bg-[#ffdad6] text-[#93000a] p-3 rounded-lg flex items-center gap-2 mb-2">
                          <span className="material-symbols-outlined text-sm">info</span>
                          <p className="text-xs font-semibold">
                            Policy requires a minimum of {minSeatsAllowed} tickets.
                          </p>
                        </div>
                    )}

                    <button
                        onClick={handleCheckoutSimulation}
                        // 2. Disable the button if the minimum isn't met!
                        disabled={checkoutState !== 'idle' || isCanceling || totalTicketsCount < minSeatsAllowed}
                        className={`w-full font-bold py-3 rounded-lg flex items-center justify-center gap-3 transition-all duration-300 ${
                            checkoutState === 'purchased'
                                ? 'bg-[#15803d] text-white cursor-default'
                                : 'bg-black text-white hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none'
                        }`}
                    >
                      <span>
                        {checkoutState === 'idle' && 'Go to Checkout'}
                        {checkoutState === 'processing' && 'Processing...'}
                        {checkoutState === 'finalizing' && 'Finalizing...'}
                        {checkoutState === 'purchased' && 'Redirecting...'}
                      </span>
                      {(checkoutState === 'processing' || checkoutState === 'finalizing') && (
                          <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                      )}
                    </button>

                    {/* ─── Cancel Order Button ─── */}
                    <button
                        onClick={handleManualCancel}
                        disabled={checkoutState !== 'idle' || isCanceling}
                        className="w-full bg-white text-red-600 border border-[#e2e2e9] hover:border-red-600 font-bold py-3 rounded-lg flex items-center justify-center gap-2 hover:bg-red-50/50 active:scale-[0.98] transition-all duration-300 disabled:opacity-40 disabled:pointer-events-none text-sm"
                    >
                      <span className="material-symbols-outlined text-lg leading-none">close</span>
                      <span>{isCanceling ? 'Canceling Order...' : 'Cancel Order'}</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
      </main>

      {/* Success Toast */}
      <div className={`fixed bottom-6 right-6 transform transition-all duration-500 z-[100] ${showToast ? 'translate-y-0 opacity-100' : 'translate-y-full opacity-0 pointer-events-none'}`}>
        <div className="bg-[#dcfce7] border-l-4 border-[#15803d] p-6 rounded-xl shadow-xl flex items-center gap-4 min-w-[320px]">
          <div className="w-10 h-10 bg-[#15803d] text-white rounded-full flex items-center justify-center">
            <span className="material-symbols-outlined">check</span>
          </div>
          <div>
            <p className="text-sm font-semibold text-[#15803d]">Order Confirmed!</p>
            <p className="text-xs text-[#4c4546]">Receipt securely dispatched to {user?.email || 'your email'}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
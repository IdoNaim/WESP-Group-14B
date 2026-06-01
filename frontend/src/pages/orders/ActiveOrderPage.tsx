import React, { useState, useEffect } from 'react';
import { authApi, UserProfileDTO } from '../../api/authApi';
import { activeOrderApi, ActiveOrderDTO } from '../../api/activeOrderApi';
import { eventApi, EventDTO } from '../../api/eventsApi';

// ─── Route Constants ────────────────────────────────────────────────────────
const RESERVE_ROUTE = '/reserve';
const CHECKOUT_ROUTE = '/checkout';

type CheckoutStatus = 'idle' | 'processing' | 'finalizing' | 'purchased';

// Helper function to decode the backend String.format("%s%d%d", zone, row, number)
const parseSeatId = (seatId: string) => {
  // Matches any text for zone, followed by exactly 1 digit for row, followed by digits for seat number
  const match = seatId.match(/^([A-Za-z\s_-]+)(\d)(\d+)$/);
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
  
  // ─── UX/UI State ────────────────────────────────────────────────────────────
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [apiError, setApiError] = useState<string | null>(null);
  const [timeLeft, setTimeLeft] = useState<number>(900); // 15:00 default fallback
  const [isPolicyErrorVisible, setIsPolicyErrorVisible] = useState<boolean>(false);
  const [checkoutState, setCheckoutState] = useState<CheckoutStatus>('idle');
  const [showToast, setShowToast] = useState<boolean>(false);

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

        // 3. Fetch Active Order by User ID
        const orderData = await activeOrderApi.getActiveOrderByUserId(token, userProfile.userId);
        setActiveOrder(orderData);

        if (!orderData || !orderData.eventId) {
          throw new Error('No active order or assigned event found for this account.');
        }

        // 4. Fetch Event Details by Event ID
        const eventDetails = await eventApi.getEvent(token, orderData.eventId);
        if (!eventDetails) {
          throw new Error('Failed to retrieve details for the assigned event.');
        }
        setEvent(eventDetails);

        // 5. Synchronize 15-Minute Expiry Countdown From Order Lifetime
        let orderCreatedMs = Date.now();
        const hexRegex = /^[0-9a-fA-F]{24}$/;
        
        if (orderData.orderId && hexRegex.test(orderData.orderId)) {
          // Extract timestamp from first 8 chars of a MongoDB hex ID
          orderCreatedMs = parseInt(orderData.orderId.substring(0, 8), 16) * 1000;
        } else {
          // Fallback: Check if any unmapped custom timestamp field was returned dynamically
          const rawData = orderData as any;
          const dynamicTime = rawData.createdAt || rawData.createdTime || rawData.timestamp;
          if (dynamicTime) orderCreatedMs = new Date(dynamicTime).getTime();
        }

        const elapsedSeconds = Math.floor((Date.now() - orderCreatedMs) / 1000);
        const maxReservationSeconds = 15 * 60; // 15 Minutes
        const remainingSeconds = maxReservationSeconds - elapsedSeconds;

        // Sync timer state, capping it safely between 0 and 15 mins
        setTimeLeft(remainingSeconds > 0 && remainingSeconds <= maxReservationSeconds ? remainingSeconds : maxReservationSeconds);

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
    if (isLoading || apiError) return;
    const timer = setInterval(() => {
      setTimeLeft((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(timer);
  }, [isLoading, apiError]);

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

  // ─── Calculations & Pricing Engine ──────────────────────────────────────────
  const ticketBasePrice = event?.ticketPrice || 0;
  
  // Extract assigned seat lists safely from the DTO
  const seatTicketsArray: string[] = activeOrder?.seats || [];
  
  // Parse standingAreas Map: Record<string, number> -> sum up quantities
  const standingAreasMap = activeOrder?.standingAreas || {};
  const totalStandingTicketsQty = Object.values(standingAreasMap).reduce((sum, qty) => sum + qty, 0);

  // Totals calculations
  const totalTicketsCount = seatTicketsArray.length + totalStandingTicketsQty;
  const computedSubtotal = totalTicketsCount * ticketBasePrice;
  const discountAmount = user?.userGroupDiscount ? 50.00 : 0.00; 
  const totalDue = Math.max(0, computedSubtotal - discountAmount);

  // ─── Helpers ────────────────────────────────────────────────────────────────
  const formatTime = (totalSeconds: number) => {
    if (totalSeconds <= 0) return 'EXPIRED';
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  const formatDate = (isoString?: string) => {
    if (!isoString) return '';
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
        // Redirect user to checkout url path after a brief delay
        setTimeout(() => {
          window.location.href = CHECKOUT_ROUTE;
        }, 800);
      }, 1000);
    }, 1500);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#f8f9ff]">
        <div className="w-12 h-12 border-4 border-black border-t-transparent rounded-full animate-spin mb-4" />
        <p className="text-sm font-semibold tracking-wide text-[#4c4546]">Securing your session data...</p>
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
          
          {/* Breadcrumbs */}
          <nav className="flex items-center gap-1 text-[#4c4546] text-xs font-semibold mb-6">
            <a className="hover:text-black" href="#">Events</a>
            <span className="material-symbols-outlined text-sm">chevron_right</span>
            <a className="hover:text-black" href="#">{event?.eventName}</a>
            <span className="material-symbols-outlined text-sm">chevron_right</span>
            <span className="text-black font-bold">Checkout</span>
          </nav>

          {/* Dynamic Reservation Timer Alert */}
          {checkoutState !== 'purchased' && (
            <div className={`mb-6 p-4 rounded-lg flex items-center justify-between border transition-colors duration-300 ${timeLeft <= 0 ? 'bg-red-900 text-white border-transparent' : 'bg-[#ffdad6] text-[#93000a] border-red-200 animate-pulse'}`}>
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined">timer</span>
                <p className="text-sm font-bold">
                  Reservation {timeLeft <= 0 ? 'status:' : 'expires in'} <span>{formatTime(timeLeft)}</span>
                </p>
              </div>
              <p className="text-xs">{timeLeft <= 0 ? 'Please restart ticket selection process.' : 'Complete your purchase to secure these seats.'}</p>
            </div>
          )}

          {/* Conditional Policy Violation Warning */}
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
            
            {/* Left Column: Order details */}
            <div className="lg:col-span-2 space-y-6">
              
              {/* Event Card Header */}
              <div className="bg-white border border-[#e2e2e9] rounded-xl overflow-hidden shadow-sm">
                <div className="h-32 checkout-gradient relative">
                  <div className="absolute inset-0 opacity-20" style={{ backgroundImage: 'radial-gradient(circle at 2px 2px, white 1px, transparent 0)', backgroundSize: '24px 24px' }}></div>
                  <div className="absolute bottom-4 left-6">
                    <span className="bg-[#dcfce7] text-[#15803d] text-xs font-semibold px-3 py-1 rounded-full inline-flex items-center gap-1 mb-2">
                      <span className="w-2 h-2 rounded-full bg-[#15803d] animate-ping"></span>
                      Live Inventory
                    </span>
                    <h2 className="text-white text-2xl font-bold leading-tight">{event?.eventName}</h2>
                    <p className="text-white/70 text-xs">{formatDate(event?.eventDateTime)} • {event?.eventLocation || 'Main Arena Center'}</p>
                  </div>
                </div>
                
                <div className="p-6">
                  <h3 className="text-base font-semibold mb-4 flex items-center gap-2">
                    <span className="material-symbols-outlined">confirmation_number</span>
                    Selected Tickets ({totalTicketsCount})
                  </h3>
                  
                  <div className="space-y-3">
                    {/* Render Numbered/Assigned Seat Tickets */}
                    {seatTicketsArray.map((seatId: string, idx: number) => {
                      const { zone, row, number } = parseSeatId(seatId);
                      return (
                        <div key={`seat-${idx}`} className="flex items-center justify-between p-4 border border-[#e2e2e9] rounded-lg hover:border-black transition-colors">
                          <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded bg-black text-white flex items-center justify-center">
                              <span className="material-symbols-outlined">event_seat</span>
                            </div>
                            <div>
                              <p className="text-sm font-bold text-black">Zone: {zone}</p>
                              <p className="text-[#4c4546] text-xs font-mono">Row {row} • Assigned Seat {number}</p>
                            </div>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-semibold">${ticketBasePrice.toFixed(2)}</p>
                            <p className="text-[#4c4546] text-xs">Ticket ID: {seatId}</p>
                          </div>
                        </div>
                      );
                    })}

                    {/* Render Standing Area Entries Map */}
                    {Object.entries(standingAreasMap).map(([areaId, quantity]) => {
                      if (quantity <= 0) return null;
                      return (
                        <div key={`standing-${areaId}`} className="flex items-center justify-between p-4 border border-dashed border-[#e2e2e9] bg-[#fdfdfd] rounded-lg">
                          <div className="flex items-center gap-4">
                            <div className="w-12 h-12 rounded bg-[#e8e7ee] flex items-center justify-center text-black">
                              <span className="material-symbols-outlined">groups</span>
                            </div>
                            <div>
                              <p className="text-sm font-semibold text-black">General Standing Admission</p>
                              <p className="text-[#4c4546] text-xs">Section Allocation ID: {areaId}</p>
                            </div>
                          </div>
                          <div className="text-right">
                            <p className="text-sm font-semibold">${(ticketBasePrice * quantity).toFixed(2)}</p>
                            <p className="text-[#4c4546] text-xs">Quantity: {quantity}</p>
                          </div>
                        </div>
                      );
                    })}

                    {/* Fallback View when cart is completely empty */}
                    {totalTicketsCount === 0 && (
                      <div className="p-6 border border-dashed border-amber-200 bg-amber-50/50 rounded-lg text-center">
                        <p className="text-sm font-medium text-amber-800">Your order selection is currently empty.</p>
                        <p className="text-xs text-amber-600 mt-0.5">Please add seats or standing areas to proceed with your checkout configuration.</p>
                      </div>
                    )}
                  </div>

                  {/* Edit/Modify anchor targeted at the configurable constant route */}
                  <a 
                    href={RESERVE_ROUTE}
                    className="mt-5 inline-flex text-[#3980f4] text-xs font-semibold items-center gap-1 hover:underline transition-all"
                  >
                    <span className="material-symbols-outlined text-sm">edit</span>
                    Modify Selection / Edit Order
                  </a>
                </div>      
              </div>

              {/* Billing Details Module */}
              <div className="bg-white border border-[#e2e2e9] rounded-xl p-6 shadow-sm">
                <h3 className="text-base font-semibold mb-4 flex items-center gap-2">
                  <span className="material-symbols-outlined">receipt_long</span>
                  Billing Details
                </h3>
                <div className="space-y-1 text-sm text-[#4c4546]">
                  <p className="text-black font-bold">{user?.name || 'User Account'}</p>
                  <p>Profile ID Account: {user?.userId}</p>
                  <p>Discount Group Classification: {user?.userGroupDiscount || 'NONE'}</p>
                </div>
              </div>

            </div>

            {/* Right Column: Order Summary Pricing */}
            <div className="space-y-6 lg:sticky lg:top-12">
              <div className="bg-white border border-[#e2e2e9] rounded-xl shadow-md overflow-hidden">
                <div className="bg-[#f3f3fa] p-6 border-b border-[#e2e2e9]">
                  <h3 className="text-lg font-bold">Order Summary</h3>
                  <p className="text-[#4c4546] text-xs font-medium font-mono">Invoice #{activeOrder?.orderId ? activeOrder.orderId.substring(0, 8).toUpperCase() : 'PENDING'}</p>
                </div>
                <div className="p-6 space-y-4">
                  <div className="space-y-2 pb-4 border-b border-[#e2e2e9]">
                    <div className="flex justify-between text-sm">
                      <span className="text-[#4c4546]">Subtotal ({totalTicketsCount} Tickets)</span>
                      <span>${computedSubtotal.toFixed(2)}</span>
                    </div>
                    {discountAmount > 0 && (
                      <div className="flex justify-between text-sm text-[#15803d]">
                        <span className="flex items-center gap-1">
                          <span className="material-symbols-outlined text-sm">sell</span>
                          Enterprise Discount ({user?.userGroupDiscount})
                        </span>
                        <span>-${discountAmount.toFixed(2)}</span>
                      </div>
                    )}
                  </div>
                  <div className="flex justify-between items-end pt-2">
                    <span className="text-sm font-semibold">Total Due</span>
                    <span className="text-xl font-bold text-black">${totalDue.toFixed(2)}</span>
                  </div>
                  <div className="pt-4">
                    <button 
                      onClick={handleCheckoutSimulation}
                      disabled={checkoutState !== 'idle' || timeLeft <= 0}
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
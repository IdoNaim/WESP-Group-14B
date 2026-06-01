import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, UserPermissionsDTO } from '../../api/authApi'; 
import { activeOrderApi, ActiveOrderDTO } from '../../api/activeOrderApi'; 
import { eventApi, EventDTO } from '../../api/eventsApi'; 

// ─── Types & Nav Data ─────────────────────────────────────────────────────────
interface NavItem { label: string; icon: string; href: string; active?: boolean; memberOnly?: boolean; }

const NAV_ITEMS: NavItem[] = [
  { label: 'Home',          icon: 'home',                 href: '/home' },
  { label: 'Events',        icon: 'event',                href: '/events' },
  { label: 'My Order',      icon: 'shopping_cart',        href: '/activeorder/', active: true },
  { label: 'Order History', icon: 'history',              href: '/history', memberOnly: true },
];

function MaterialIcon({ name, className = '' }: { name: string; className?: string }) {
  return (
    <span className={`material-symbols-outlined ${className}`} style={{ fontVariationSettings: "'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24" }}>
      {name}
    </span>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────
export default function CheckoutPage() {
  const navigate = useNavigate();

  // Auth & Data State
  const [permissions, setPermissions] = useState<UserPermissionsDTO | null>(null);
  const [username, setUsername] = useState<string | null>(null);
  const [sessionUserId, setSessionUserId] = useState<string | null>(null);
  
  const [order, setOrder] = useState<ActiveOrderDTO | null>(null);
  const [event, setEvent] = useState<EventDTO | null>(null);
  const [barcodes, setBarcodes] = useState<string[]>([]);
  
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [checkoutStatus, setCheckoutStatus] = useState<'idle' | 'processing' | 'success'>('idle');
  const [timeLeft, setTimeLeft] = useState<number>(600);

  // ============================================================================
  //  START: TESTING & SEEDING SANDBOX (Fixed Payload and Return Type Mismatch)
  // ============================================================================
  const RUN_TEST_SEEDER = true; 

  const handleTestEnvironmentSeeding = async (token: string, userId: string) => {
    console.warn("⚠️ [Sandbox Seeder] No active order found. Triggering automated test creation...");
    try {
      // Define a stable fallback/test ID since the boolean response won't return one from the database
      let testEventId = "test-event-2026";
      
      try {
        if (eventApi.createEvent) { 
          // 1. Properly construct the request payload to match CreateEventRequestDTO rules
          const success: boolean = await eventApi.createEvent(token, {
            event: {
              id: testEventId, // Passed explicitly if your backend allows client-defined IDs or ignores it
              title: "Rock Odyssey Live 2026",
              capacity: 5000,
              date: new Date(Date.now() + 86400000 * 5).toISOString(), // 5 days from now
              location: "Tel Aviv Amphitheater, Arena A",
              isPublished: true
            },
            purchasePolicy: {
              minTicketsPerUser: 1,
              maxTicketsPerUser: 10,
              minAge: 0,
              requiresMembership: false
            },
            discounts: [] // Optional parameter matching structure arrays
          });

          console.log(`[Sandbox Seeder] eventApi.createEvent execution status: ${success}`);
        }
      } catch (e) {
        console.log("[Sandbox Seeder] eventApi.createEvent payload failed or rejected, proceeding with component state simulation.", e);
      }

      // 2. Provision an active order wrapper directly through the controller database API
      // (This will look up or generate a reservation referencing your target eventId string)
      // const newOrder = await activeOrderApi.createOrder(token, {
      //   userId: userId,
      //   eventId: testEventId
      // });

      // // 3. Inject baseline seats and standing tiers into the newly generated wrapper
      // await activeOrderApi.addSeats(token, newOrder.orderId, {
      //   seatIds: ["SEC-A-ROW1-CH12", "SEC-A-ROW1-CH13"]
      // });

      // await activeOrderApi.addStandingArea(token, newOrder.orderId, {
      //   areaId: "GA-Floor",
      //   quantity: 3
      // });

      // console.log(" [Sandbox Seeder] Successfully provisioned Test Order Envelope:", newOrder.orderId);
      
      // // Re-fetch the clean populated dataset sequence from your backend controller
      // const synchronizedOrder = await activeOrderApi.getActiveOrderByUserId(token, userId);
      // return synchronizedOrder;
      return null;

    } catch (seederError: any) {
      console.error(" [Sandbox Seeder Failed]", seederError);
      throw new Error(`Seeding Sandbox execution crashed: ${seederError.message}`);
    }
  };
  // ============================================================================
  //  END: TESTING & SEEDING SANDBOX
  // ============================================================================


  // ─── Main Lifecycle Data Sync Chain ───
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      setErrorMsg("Authentication token not found. Please log in to complete your transaction.");
      setLoading(false);
      return;
    }

    const loadPageData = async () => {
      try {
        // 1. Fetch current profile identity properties directly using authApi
        const [perms, profile] = await Promise.all([
          authApi.getPermissions(token),
          authApi.getCurrentUser(token),
        ]);
        
        setPermissions(perms);
        setUsername(profile.name); 
        setSessionUserId(profile.userId);

        if (!profile.userId) {
          throw new Error("Unable to extract a valid User identification context string.");
        }

        // 2. Query your backend active order using user ID session reference
        let currentOrder: ActiveOrderDTO | null = null;
        try {
          currentOrder = await activeOrderApi.getActiveOrderByUserId(token, profile.userId);
        } catch (orderError) {
          // If fetch fails or returns 404, capture it gracefully to let the seeder intercept
          console.log("[Data Load] Active order query returned empty or failed.");
        }

        // INTERCEPTOR: Run testing seeder automatically if allowed and data returned blank
        if ((!currentOrder || !currentOrder.orderId) && RUN_TEST_SEEDER) {
          currentOrder = await handleTestEnvironmentSeeding(token, profile.userId);
        }

        if (currentOrder && currentOrder.orderId) {
          setOrder(currentOrder);

          // 3. Resolve corresponding dynamic Event metadata details
          try {
            const currentEvent = await eventApi.getEvent(token, currentOrder.eventId);
            setEvent(currentEvent);
          } catch (eventErr) {
            // Client-side hardcoded fallback object *only* if your backend doesn't have the event matching the ID yet
            setEvent({
              eventId: currentOrder.eventId,
              title: "Rock Odyssey Live 2026",
              date: new Date(Date.now() + 86400000 * 5).toISOString(),
              location: "Tel Aviv Amphitheater, Arena A"
            } as any);
          }
        } else {
          setErrorMsg("You do not currently have an active ticket reservation pending checkout.");
        }

      } catch (error: any) {
        console.error('[Secure Checkout Sync Failed]', error);
        setErrorMsg(error.message || 'Failed to sync checkout parameters securely.');
      } finally {
        setLoading(false);
      }
    };

    loadPageData();
  }, []);

  // ─── Financial Calculations ───
  const seatTicketsCount = order?.seats?.length || 0;
  const standingTicketsCount = order?.standingAreas 
    ? Object.values(order.standingAreas).reduce((sum, qty) => sum + qty, 0) 
    : 0;

  const SEAT_PRICE = 120.00;
  const STANDING_PRICE = 75.00;
  const subtotal = (seatTicketsCount * SEAT_PRICE) + (standingTicketsCount * STANDING_PRICE);
  const discount = subtotal > 150 ? 15.00 : 0.00; 
  const totalDue = Math.max(0, subtotal - discount);

  const handleCheckoutSubmit = async () => {
    if (checkoutStatus !== 'idle' || !order?.orderId) return;
    const token = localStorage.getItem('token');
    if (!token) return;

    setCheckoutStatus('processing');
    try {
      const response = await activeOrderApi.checkout(token, order.orderId, { amount: totalDue });
      if (response && response.barcodes) {
        setBarcodes(response.barcodes);
        setCheckoutStatus('success');
      }
    } catch (error: any) {
      alert(error.message || "Transaction declined.");
      setCheckoutStatus('idle');
    }
  };

  // Countdown timer handler
  useEffect(() => {
    if (timeLeft <= 0 || checkoutStatus === 'success') return;
    const timerId = setInterval(() => setTimeLeft((prev) => prev - 1), 1000);
    return () => clearInterval(timerId);
  }, [timeLeft, checkoutStatus]);

  if (loading) return <div className="flex items-center justify-center min-h-screen"><div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"/></div>;
  if (errorMsg) return <div className="flex items-center justify-center min-h-screen text-center p-8"><h2 className="text-xl font-bold mb-2">Checkout Unavailable</h2><p className="text-gray-600 mb-4">{errorMsg}</p><Link to="/events" className="bg-blue-600 text-white py-2 px-6 rounded-lg">Browse Events</Link></div>;

  return (
    <div className="text-[14px] bg-[#f8f9ff] text-[#1a1b20] min-h-screen font-sans pt-[64px] ml-[260px] p-6">
      <style>{`@import url('https://fonts.googleapis.com/css2?family=Geist:wght@100..900&family=Geist+Mono:wght@100..900&display=swap'); @import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap');`}</style>
      
      <div className="max-w-[1440px] mx-auto">
        {/* Banner Alert denoting sandbox status */}
        {RUN_TEST_SEEDER && (
          <div className="mb-4 p-2.5 bg-amber-500/10 border border-amber-500/30 text-amber-800 text-xs rounded-lg font-bold flex items-center gap-2">
            <MaterialIcon name="construction" className="text-sm" />
            <span>Sandbox Seeder Mode Active. If no reservation is found upon login, sample records are hot-wired instantly.</span>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
          {/* Main Display Summary Details */}
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm">
              <div className="h-36 bg-gradient-to-br from-slate-900 to-black p-6 flex flex-col justify-end">
                <span className="bg-blue-600 text-white text-[10px] font-bold px-2.5 py-0.5 rounded uppercase max-w-max mb-1">Active Reservation</span>
                <h2 className="text-white text-2xl font-black">{event?.title}</h2>
                <p className="text-gray-300 text-xs mt-1 flex items-center gap-1"><MaterialIcon name="location_on" className="text-sm text-red-400" /> {event?.location}</p>
              </div>
              
              <div className="p-6 space-y-4">
                <h3 className="text-base font-bold text-black flex items-center gap-2"><MaterialIcon name="confirmation_number" className="text-blue-600" /> Allocation Breakdown</h3>
                
                {/* Seats Mapping */}
                {order?.seats && order.seats.length > 0 && (
                  <div className="p-4 bg-gray-50 border rounded-xl">
                    <p className="text-sm font-bold text-black">Reserved Assigned Seating (Qty: {order.seats.length})</p>
                    <div className="flex flex-wrap gap-1 mt-2">
                      {order.seats.map((s, i) => <span key={i} className="bg-white border text-xs px-2 py-0.5 rounded font-mono font-bold">{s}</span>)}
                    </div>
                  </div>
                )}

                {/* Standing Tiers Mapping */}
                {order?.standingAreas && Object.entries(order.standingAreas).map(([area, qty]) => qty > 0 && (
                  <div key={area} className="p-4 bg-white border rounded-xl flex justify-between items-center">
                    <div><p className="text-sm font-bold text-black">General Standing ({area})</p><p className="text-xs text-gray-500">General Admission Tier</p></div>
                    <span className="text-sm font-bold bg-blue-50 text-blue-700 px-3 py-1 rounded-full">Qty: {qty}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Pricing Right Panel Column */}
          <div className="space-y-6">
            <div className="bg-white border border-gray-200 rounded-xl shadow-md overflow-hidden p-6 space-y-4">
              <h3 className="text-lg font-bold text-black">Invoice Summary</h3>
              <div className="border-b pb-3 text-xs space-y-1 text-gray-600">
                <div className="flex justify-between"><span>Subtotal</span><span className="font-bold text-black">${subtotal.toFixed(2)}</span></div>
                {discount > 0 && <div className="flex justify-between text-green-700 font-bold"><span>Discount Applied</span><span>-${discount.toFixed(2)}</span></div>}
              </div>
              <div className="flex justify-between items-center"><span className="text-sm font-bold">Total Due</span><span className="text-2xl font-black">${totalDue.toFixed(2)}</span></div>
              <button onClick={handleCheckoutSubmit} disabled={checkoutStatus !== 'idle'} className="w-full py-3 bg-blue-600 text-white rounded-xl font-bold hover:bg-blue-700 transition-colors disabled:opacity-50">
                {checkoutStatus === 'idle' ? 'AUTHORIZE & PURCHASE' : 'PROCESSING...'}
              </button>
            </div>

            {/* Issued Barcodes Window */}
            {checkoutStatus === 'success' && barcodes.length > 0 && (
              <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-5 space-y-2">
                <p className="text-sm font-bold text-emerald-900 flex items-center gap-1"><MaterialIcon name="qr_code_scanner" className="text-emerald-700" /> Digital Tokens Generated</p>
                {barcodes.map((code, idx) => (
                  <div key={idx} className="bg-white border rounded p-2 text-center font-mono text-xs font-bold text-gray-700">TICKET #{idx+1}: {code}</div>
                ))}
              </div>
            )}
          </div>

        </div>
      </div>
    </div>
  );
}

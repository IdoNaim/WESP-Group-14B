import { useState, useEffect, useRef } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { activeOrderApi, ActiveOrderDTO, CheckoutRequestDTO } from '../../api/activeOrderApi';
import { eventApi, EventDTO, SeatingMapDTO, PurchasePolicyDTO } from '../../api/eventsApi';
import { authApi, UserProfileDTO } from '../../api/authApi';
import { historyOrderApi } from '../../api/historyOrderApi';

// ─── Route Constants ────────────────────────────────────────────────────────
const FALLBACK_RESERVE_ROUTE = '/reserve';
const EVENTS_ROUTE = '/events';
const GET_RESERVE_ROUTE = (eventId: string | number) => `/events/${eventId}/reserve`;

type CheckoutStatus = 'idle' | 'processing' | 'success' | 'expired_canceled';

// ─── Validation Helpers ──────────────────────────────────────────────────────

// An event requires age verification only when its policy defines a min and/or
// max age. Without an age policy, there is nothing to verify.
function policyRequiresAge(policy: PurchasePolicyDTO | null): boolean {
  return !!policy && (policy.minAge != null || policy.maxAge != null);
}

function isValidCardNumber(value: string): boolean {
  const digits = value.replace(/\s/g, '');
  return /^\d{13,19}$/.test(digits);
}

function isValidExpiry(value: string): boolean {
  // Accepts MM/YY or MM / YY
  const match = value.replace(/\s/g, '').match(/^(\d{2})\/(\d{2})$/);
  if (!match) return false;
  const month = parseInt(match[1], 10);
  const year = parseInt('20' + match[2], 10);
  if (month < 1 || month > 12) return false;
  const now = new Date();
  const expiry = new Date(year, month - 1, 1);
  return expiry >= new Date(now.getFullYear(), now.getMonth(), 1);
}

function isValidCvv(value: string): boolean {
  return /^\d{3,4}$/.test(value.trim());
}

function isValidCardholderName(value: string): boolean {
  const trimmedValue = value.trim();
    // Validates that the length is at least 2 and contains only uppercase/lowercase letters and spaces
    return trimmedValue.length >= 2 && /^[A-Za-z\s]+$/.test(trimmedValue);
}

function isValidId(value: string): boolean {
  const trimmed = value.trim();
  return /^\d{9}$/.test(trimmed);
}

/*
// Luhn Algorithm validation for future use:
function isValidIsraeliIdLuhn(id: string): boolean {
  const trimmed = id.trim();
  if (!/^\d{9}$/.test(trimmed)) return false;
  let sum = 0;
  for (let i = 0; i < 9; i++) {
    let digit = parseInt(trimmed.charAt(i), 10);
    let step = digit * ((i % 2) + 1);
    if (step > 9) step -= 9;
    sum += step;
  }
  return sum % 10 === 0;
}
*/

interface ValidationErrors {
  cardholderName?: string;
  cardholderId?: string;
  cardNumber?: string;
  expiryDate?: string;
  cvv?: string;
}

function validatePaymentFields(fields: {
  cardholderName: string;
  cardholderId: string;
  cardNumber: string;
  expiryDate: string;
  cvv: string;
}): ValidationErrors {
  const errors: ValidationErrors = {};
  if (!isValidCardholderName(fields.cardholderName)) {
    errors.cardholderName = 'Please enter the cardholder name.';
  }
  if (!isValidId(fields.cardholderId)) {
    errors.cardholderId = 'Please enter a valid 9-digit Cardholder ID.';
  }
  if (!isValidCardNumber(fields.cardNumber)) {
    errors.cardNumber = 'Please enter a valid card number (13–19 digits).';
  }
  if (!isValidExpiry(fields.expiryDate)) {
    errors.expiryDate = 'Please enter a valid expiry date (MM/YY) that has not passed.';
  }
  if (!isValidCvv(fields.cvv)) {
    errors.cvv = 'Please enter a valid CVV (3 or 4 digits).';
  }
  return errors;
}

export default function CheckoutPage() {
  // ─── Component State ──────────────────────────────────────────────────────
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [hasNoActiveOrder, setHasNoActiveOrder] = useState(false);
  const [processState, setProcessState] = useState<CheckoutStatus>('idle');
  const [successBarcodes, setSuccessBarcodes] = useState<string[]>([]);

  // ─── Age Verification State ────────────────────────────────────────────────
  const [isAgeModalOpen, setIsAgeModalOpen] = useState(false);
  const [ageInput, setAgeInput] = useState<string>('');

  // ─── Payment Field State ───────────────────────────────────────────────────
  const [cardholderName, setCardholderName] = useState('');
  const [cardholderId, setCardholderId] = useState('');
  const [cardNumber, setCardNumber] = useState('');
  const [expiryDate, setExpiryDate] = useState('');
  const [cvv, setCvv] = useState('');
  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({});
  const [paymentError, setPaymentError] = useState<string>('');

  // ─── Domain Data State ──────────────────────────────────────────────────────
  const [user, setUser] = useState<UserProfileDTO | null>(null);
  const [order, setOrder] = useState<ActiveOrderDTO | null>(null);
  const [eventDetails, setEventDetails] = useState<EventDTO | null>(null);
  const [seatingMap, setSeatingMap] = useState<SeatingMapDTO | null>(null);
  const [purchasePolicy, setPurchasePolicy] = useState<PurchasePolicyDTO | null>(null);

  // ─── UX/UI Timer State ──────────────────────────────────────────────────────
  const [timeLeft, setTimeLeft] = useState<number>(900);

  const hasCanceledRef = useRef<boolean>(false);

  // ─── Financial State ────────────────────────────────────────────────────────
  const [pricing, setPricing] = useState({
    subtotal: 0,
    fee: 2.50,
    total: 2.50
  });

  // ─── Data parsing (Moved up so handlers can access totalTicketsCount) ──────
  const rawOrder = order as any;
  const standingAreaQuantitiesMap = rawOrder?.StandingAreaQuantities || rawOrder?.standingAreaQuantities || rawOrder?.standinAreaQuantities || {};
  const totalStandingTicketsQty = Object.values(standingAreaQuantitiesMap).reduce((sum: number, qty: any) => sum + Number(qty), 0);
  const totalTicketsCount = (order?.seatIds?.length || 0) + totalStandingTicketsQty;

  // ─── Life Cycle: Data Hydration Pipeline ──────────────────────────────────
  useEffect(() => {
    async function hydrateCheckoutData() {
      try {
        setIsLoading(true);
        setErrorMessage('');

        const token = localStorage.getItem('token') || sessionStorage.getItem('authToken') || '';
        if (!token) {
          throw new Error('Authentication token missing. Please log in to complete checkout.');
        }

        const profile = await authApi.getCurrentUser(token);
        if (!profile || !profile.userId) {
          throw new Error('Unable to load your account information. Please try logging in again.');
        }
        setUser(profile);

        let activeOrder: ActiveOrderDTO | null = null;
        try {
          activeOrder = await activeOrderApi.getActiveOrderByUserId(token, profile.userId);
        } catch {
          setHasNoActiveOrder(true);
          setIsLoading(false);
          return;
        }

        if (!activeOrder || !activeOrder.orderId) {
          setHasNoActiveOrder(true);
          setIsLoading(false);
          return;
        }

        setOrder(activeOrder);

        const [eventMeta, seatConfig, policy] = await Promise.all([
          eventApi.getEvent(token, activeOrder.eventId),
          eventApi.getEventSeatingMap(token, activeOrder.eventId),
          eventApi.getEventPurchasePolicy(token, activeOrder.eventId).catch(() => null)
        ]);

        if (!eventMeta) {
          throw new Error('Could not load event details. Please go back and try again.');
        }
        setEventDetails(eventMeta);
        setSeatingMap(seatConfig);
        setPurchasePolicy(policy);

        calculateOrderTotals(activeOrder, seatConfig);

        const rawData = activeOrder as any;
        const orderCreatedMs = rawData.createdAt || rawData.createdTime || rawData.timestamp || Date.now();
        const elapsedSeconds = Math.floor((Date.now() - new Date(orderCreatedMs).getTime()) / 1000);
        const maxReservationSeconds = 15 * 60;
        const remainingSeconds = maxReservationSeconds - elapsedSeconds;
        setTimeLeft(remainingSeconds <= 0 ? 0 : Math.min(remainingSeconds, maxReservationSeconds));

      } catch (err: any) {
        console.error('[Checkout Data Hydration Error]:', err);
        setErrorMessage(err.message || 'Something went wrong while loading your order. Please try again.');
      } finally {
        setIsLoading(false);
      }
    }

    hydrateCheckoutData();
  }, []);

  // ─── Reservation Timer Effect ────────────────────────────────────────────────
  useEffect(() => {
    if (isLoading || errorMessage || processState === 'expired_canceled' || processState === 'success') return;
    if (timeLeft <= 0) { handleOrderExpiration(); return; }

    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) { clearInterval(timer); return 0; }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isLoading, errorMessage, timeLeft, processState]);

  // ─── Fetch Completed Order Barcodes Effect ─────────────────────────────────
  useEffect(() => {
    const orderId = order?.orderId;
    if (processState === 'success' && orderId) {
      async function fetchCompletedOrderBarcodes() {
        try {
          const token = localStorage.getItem('token') || sessionStorage.getItem('authToken') || '';
          if (token && user?.userId) {
            const historyOrders = await historyOrderApi.getUserOrders(token, user.userId);
            const matchingOrder = historyOrders.find(o => o.orderId === orderId);
            if (matchingOrder && matchingOrder.barcodes) {
              setSuccessBarcodes(matchingOrder.barcodes);
            }
          }
        } catch (err) {
          console.error("Failed to fetch completed order barcodes:", err);
        }
      }
      fetchCompletedOrderBarcodes();
    }
  }, [processState, order?.orderId, user?.userId]);

  // ─── Server-Side Expiration Handshake ───────────────────────────────────────
  const handleOrderExpiration = async () => {
    if (hasCanceledRef.current) return;
    hasCanceledRef.current = true;

    const token = localStorage.getItem('token') || localStorage.getItem('authToken') || '';
    const orderId = order?.orderId;
    const userId = user?.userId;

    if (token && orderId && userId) {
      try {
        setProcessState('expired_canceled');
        await activeOrderApi.cancelOrder(token, orderId.toString(), userId.toString());
      } catch (err) {
        console.error('Failed to auto-cancel expired order on server:', err);
      }
    } else {
      setProcessState('expired_canceled');
    }
  };

  // ─── Order Processing Calculator ──────────────────────────────────────────
  const calculateOrderTotals = (activeOrder: ActiveOrderDTO, mapData: SeatingMapDTO | null) => {
    let computedSubtotal = 0;
    if (!mapData) return;

    if (activeOrder.seatIds && activeOrder.seatIds.length > 0 && mapData.assignedSeats) {
      activeOrder.seatIds.forEach(id => {
        const matchingSeat = mapData.assignedSeats.find(s => s.id === id);
        if (matchingSeat) computedSubtotal += matchingSeat.priceForTicket;
      });
    }

    const rawOrder = activeOrder as any;
    const standingAreaQuantitiesMap = rawOrder?.StandingAreaQuantities || rawOrder?.standingAreaQuantities || rawOrder?.standinAreaQuantities || {};

    if (standingAreaQuantitiesMap && mapData.standingAreas) {
      Object.entries(standingAreaQuantitiesMap).forEach(([areaId, quantity]) => {
        const matchingArea = mapData.standingAreas.find(a => a.areaId === areaId);
        if (matchingArea) computedSubtotal += matchingArea.priceForTicket * Number(quantity);
      });
    }

    const standardProcessingFee = 2.50;
    setPricing({ subtotal: computedSubtotal, fee: standardProcessingFee, total: computedSubtotal + standardProcessingFee });
  };

  // ─── Main Payment Authorization Execution ─────────────────────────────────
  const handlePayment = async (age: number | null = null) => {
    try {
      setProcessState('processing');
      const token = localStorage.getItem('token') || localStorage.getItem('authToken') || '';

      if (!order || !order.orderId) return;

      const checkoutPayload: CheckoutRequestDTO = {
        amount: pricing.total,
        creditCardNumber: cardNumber.replace(/\s/g, ''),
        cardHolderName: cardholderName.trim(),
        expirationDate: expiryDate.replace(/\s/g, ''),
        cvv: cvv.trim(),
        id: cardholderId.trim(),
        age:age
      };

      const checkoutResult = await activeOrderApi.checkout(token, order.orderId, checkoutPayload);

      if (checkoutResult && checkoutResult.barcodes) {
        setSuccessBarcodes(checkoutResult.barcodes);
        setProcessState('success');
      } else {
        throw new Error('Payment was processed but your tickets could not be issued. Please contact support.');
      }

    } catch (err: any) {
      console.error('[Checkout Execution Failure]:', err);
      setPaymentError(err.message || 'Payment failed. Please check your details and try again.');
      setProcessState('idle');
    }
  };


  const proceedToValidatePolicyAndPay = async (age: number | null) => {
    const token = localStorage.getItem('token') || localStorage.getItem('authToken') || sessionStorage.getItem('authToken') || '';

    if (order && order.eventId) {
      try {
        const policyViolation = await eventApi.validatePurchasePolicy(token, order.eventId, totalTicketsCount, age ?? 0);
        if (policyViolation) {
          setPaymentError(`Policy Violation: ${policyViolation}`);
          return;
        }
      } catch (err: any) {
        setPaymentError(err.message || "Failed to validate event purchase policy.");
        return;
      }
    }

    // If the policy passes validation, call the payment routine
    await handlePayment(age);
  }

  const handleConfirmAge = () => {
    const age = parseInt(ageInput, 10);
    if (isNaN(age) || age <= 0) {
      setPaymentError("Please enter a valid age to proceed.");
      setIsAgeModalOpen(false);
      return;
    }
    
    setIsAgeModalOpen(false);
    proceedToValidatePolicyAndPay(age);
  }

  // ─── Pre-Payment Policy Verification ──────────────────────────────────────
  const handleInitiatePayment = async () => {
    // Prevent double-submission: if already in flight or done, do nothing
    if (processState !== 'idle') return;

    // Lock the button immediately — before any awaits
    setProcessState('processing');
    setPaymentError('');

    // Guard: make sure the event wasn't canceled while the user was on this page.
    if (order?.eventId) {
      const token = localStorage.getItem('token') || localStorage.getItem('authToken') || sessionStorage.getItem('authToken') || '';
      const latestEvent = await eventApi.getEvent(token, order.eventId);
      if (latestEvent && latestEvent.isActive === false) {
        setPaymentError('Event got canceled');
        setProcessState('idle');
        return;
      }
    }

    const errors = validatePaymentFields({ cardholderName, cardholderId, cardNumber, expiryDate, cvv });
    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      setProcessState('idle');
      return;
    }
    setValidationErrors({});

    if (policyRequiresAge(purchasePolicy)) {
      // Keep processState='processing' so button stays disabled while modal is open.
      // Age modal cancel must reset it back to idle (see Cancel button onClick below).
      setIsAgeModalOpen(true);
    } else {
      proceedToValidatePolicyAndPay(null);
    }
  };

  // ─── Helpers ────────────────────────────────────────────────────────────────
  const formatTime = (totalSeconds: number) => {
    if (totalSeconds <= 0) return 'EXPIRED';
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  // ─── Embedded Style Definitions ────────────────────────────────────────────
  const globalStyles = (
    <style>{`
      .material-symbols-outlined {
        font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
      }
      @keyframes spin-slow {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
      }
      .animate-spin-slow {
        animation: spin-slow 1s linear infinite;
      }
    `}</style>
  );

  // ─── Footer ────────────────────────────────────────────────────────────────
  const Footer = () => (
    <footer className="bg-[#f2f3f9] border-t border-[#c7c6cb] mt-20">
      <div className="flex flex-col md:flex-row justify-between items-center w-full px-4 md:px-12 py-8 max-w-[1120px] mx-auto gap-6">
        <div className="font-sans text-xs tracking-widest text-[#46464b] font-semibold">
          © {new Date().getFullYear()} SECUREPAY INC. ALL TRANSACTIONS ARE ENCRYPTED.
        </div>
        <div className="flex gap-6">
          {["Terms of Service", "Privacy Policy", "Contact Support"].map(link => (
            <a key={link} href="#" className="text-sm text-[#46464b] hover:text-[#1a1b20] underline transition-all">
              {link}
            </a>
          ))}
        </div>
      </div>
    </footer>
  );

  // ─── View Controller Returns ────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="fixed inset-0 bg-[#f8f9ff] z-[100] flex flex-col items-center justify-center">
        {globalStyles}
        <div className="w-12 h-12 border-4 border-[#c7c6cb] border-t-[#1a1b20] rounded-full animate-spin-slow mb-4"></div>
        <p className="font-sans text-xs tracking-widest text-[#46464b] uppercase font-semibold">
          Loading your order...
        </p>
      </div>
    );
  }

  if (hasNoActiveOrder) {
    return (
      <div className="min-h-screen bg-[#f8f9ff] flex flex-col items-center justify-center p-4">
        {globalStyles}
        <div className="bg-white border border-[#c7c6cb] p-10 rounded-2xl max-w-lg text-center shadow-lg">
          <div className="w-16 h-16 bg-[#eceef3] rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="material-symbols-outlined text-[#46464b] text-3xl">receipt_long</span>
          </div>
          <h3 className="text-xl font-bold text-[#191c20] mb-2">No Active Order Found</h3>
          <p className="text-sm text-[#46464b] leading-relaxed mb-8">
            You don't have an active order. Browse our events and select your tickets to get started.
          </p>
          <a href={EVENTS_ROUTE} className="inline-block bg-[#1a1b20] text-white w-full py-3 rounded-lg font-bold text-sm hover:opacity-90 transition-opacity">
            Browse Events
          </a>
        </div>
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className="min-h-screen bg-[#f8f9ff] flex flex-col items-center justify-center p-4">
        <div className="bg-white border border-red-200 p-8 rounded-xl shadow-sm text-center max-w-md">
          <span className="material-symbols-outlined text-red-500 text-5xl mb-4">error</span>
          <h2 className="text-xl font-bold text-[#191c20] mb-2">Checkout Error</h2>
          <p className="text-[#46464b] text-sm mb-6">{errorMessage}</p>
          <button onClick={() => window.location.reload()} className="bg-[#1a1b20] text-white px-6 py-3 rounded font-medium transition-all hover:opacity-90">
            Retry Verification Connection
          </button>
        </div>
      </div>
    );
  }

  if (processState === 'expired_canceled') {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#f8f9ff] p-6">
        <div className="bg-white border border-[#e2e2e9] p-10 rounded-2xl max-w-lg text-center shadow-lg">
          <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <span className="material-symbols-outlined text-red-600 text-3xl">gavel</span>
          </div>
          <h3 className="text-xl font-bold text-black mb-2">Reservation Period Expired</h3>
          <p className="text-sm text-[#4c4546] leading-relaxed mb-8">
            The 15-minute selection holding timeframe has concluded. To ensure equal platform availability, your pending tickets have been securely released back into general public stock.
          </p>
          <a href={order?.eventId ? GET_RESERVE_ROUTE(order.eventId) : FALLBACK_RESERVE_ROUTE} className="inline-block bg-black text-white w-full py-3 rounded-lg font-bold text-sm hover:opacity-90 transition-opacity">
            Return to Ticket Selection
          </a>
        </div>
      </div>
    );
  }

  if (processState === 'success') {
    return (
      <div className="bg-[#f8f9ff] text-[#191c20] min-h-screen flex flex-col font-sans">
        <main className="flex-grow pt-20 pb-20 max-w-md mx-auto px-4 text-center flex flex-col items-center justify-center">
          <div className="w-20 h-20 bg-[#006c49] rounded-full flex items-center justify-center mb-8">
            <span className="material-symbols-outlined text-white text-4xl" style={{ fontVariationSettings: "'FILL' 1" }}>
              check_circle
            </span>
          </div>
          <h1 className="text-3xl font-bold tracking-tight text-[#191c20] mb-2">Transaction Successful</h1>
          <p className="text-[#46464b] mb-6">Your ticket authorization processing sequence has closed effectively.</p>
          <div className="bg-white rounded-xl p-6 mb-10 w-full border border-[#c7c6cb] text-left space-y-4">
            <div className="border-b border-gray-100 pb-2">
              <p className="text-xs font-semibold tracking-widest text-[#46464b] uppercase">ORDER CONTEXT ID</p>
              <p className="text-md font-mono text-[#191c20] font-bold">{order?.orderId}</p>
            </div>
            <div>
              <p className="text-xs font-semibold tracking-widest text-[#46464b] mb-2 uppercase">SECURE TICKET ACCESS BARCODES ({successBarcodes.length})</p>
              <div className="space-y-2 max-h-60 overflow-y-auto pr-2">
                {successBarcodes.map((code, index) => (
                  <div key={index} className="bg-[#f2f3f9] px-3 py-2 text-xs font-mono font-bold rounded flex items-center justify-between gap-3 border border-[#e2e2e9]">
                    <div className="flex flex-col gap-1 min-w-0">
                      <span className="text-[#46464b]">Ticket #{index + 1}</span>
                      <span className="text-blue-700 tracking-wider font-extrabold break-all">{code}</span>
                    </div>
                    <div className="bg-white p-1 rounded flex-shrink-0">
                      <QRCodeSVG value={code} size={52} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
          <button onClick={() => window.location.href = '/'} className="bg-[#1a1b20] text-white px-8 py-4 font-bold transition-all hover:opacity-90 rounded w-full">
            Return to Store
          </button>
        </main>
        <Footer />
      </div>
    );
  }

  // ─── Main Checkout View ────────────────────────────────────────────────────
  return (
    <div className="bg-[#f8f9ff] text-[#191c20] min-h-screen flex flex-col font-sans relative">
      {globalStyles}

      <main className="flex-grow pt-20 pb-20 max-w-[1120px] mx-auto px-4 md:px-12 w-full">
        <div className="mb-6 text-center">
          <div className="text-xl font-bold text-[#191c20] mb-2">SecurePay Checkout Gateway</div>
          <p className="text-[#46464b] text-sm">Review your order and complete your payment details below</p>
        </div>

        {/* Countdown Banner */}
        <div className="mb-6 p-4 rounded-lg flex items-center justify-between border transition-colors duration-300 bg-[#ffdad6] text-[#93000a] border-red-200 animate-pulse">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined">timer</span>
            <p className="text-sm font-bold">
              Reservation expires in <span>{formatTime(timeLeft)}</span>
            </p>
          </div>
          <p className="text-xs hidden sm:block">Complete your purchase to secure these seats.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">

          {/* Left Column */}
          <div className="lg:col-span-8 space-y-8">

            {/* Selected Tickets */}
            <div className="bg-white border border-[#c7c6cb] p-8 rounded shadow-sm">
              <div className="flex items-center gap-2 mb-6">
                <span className="material-symbols-outlined">confirmation_number</span>
                <h2 className="text-xl font-bold text-[#191c20]">Selected Tickets ({totalTicketsCount})</h2>
              </div>
              <div className="space-y-4 mb-6">
                {order?.seatIds?.map(seatId => {
                  const details = seatingMap?.assignedSeats?.find(s => s.id === seatId);
                  return (
                    <div key={seatId} className="flex items-center justify-between p-4 border border-[#c7c6cb] rounded bg-white">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-[#1a1b20] text-white rounded flex items-center justify-center">
                          <span className="material-symbols-outlined">chair</span>
                        </div>
                        <div>
                          <p className="font-bold text-[#191c20]">Assigned Seating Node</p>
                          <p className="text-xs text-[#46464b]">Seat Unit Core Identifier: {seatId}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="font-bold text-[#191c20]">${details ? details.priceForTicket.toFixed(2) : '0.00'}</p>
                        <p className="text-[10px] text-[#46464b] uppercase tracking-tighter font-medium">RESERVED</p>
                      </div>
                    </div>
                  );
                })}
                {Object.entries(standingAreaQuantitiesMap).map(([areaId, quantity]) => {
                  const qty = Number(quantity);
                  if (qty <= 0) return null;
                  const details = seatingMap?.standingAreas?.find(a => a.areaId === areaId);
                  return (
                    <div key={areaId} className="flex items-center justify-between p-4 border border-dashed border-[#c7c6cb] rounded bg-white">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 bg-[#eceef3] text-[#191c20] rounded flex items-center justify-center">
                          <span className="material-symbols-outlined">groups</span>
                        </div>
                        <div>
                          <p className="font-bold text-[#191c20]">Standing Deck Allocation</p>
                          <p className="text-xs text-[#46464b]">Zone Target Ref: {areaId} • (${details ? details.priceForTicket.toFixed(2) : '0.00'} each)</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="font-bold text-[#191c20]">${details ? (details.priceForTicket * qty).toFixed(2) : '0.00'}</p>
                        <p className="text-[10px] text-[#46464b] uppercase tracking-tighter font-medium">QTY: {qty}</p>
                      </div>
                    </div>
                  );
                })}
              </div>
              <a href={order?.eventId ? GET_RESERVE_ROUTE(order.eventId) : FALLBACK_RESERVE_ROUTE} className="flex items-center gap-2 text-[#1a1b20] hover:underline font-medium text-sm transition-colors">
                <span className="material-symbols-outlined text-base">edit</span>
                Modify Selection / Edit Order
              </a>
            </div>

            {/* ── Payment Form ─────────────────────────────────────────────── */}
            <div className="bg-white border border-[#c7c6cb] p-8 rounded shadow-sm">
              <h2 className="text-xl font-bold text-[#191c20] mb-8">Billing Details</h2>

              {/* Top-level payment error from API */}
              {paymentError && (
                <div className="mb-6 flex items-start gap-3 px-4 py-3 rounded-lg bg-red-50 border border-red-200 text-red-800 text-sm">
                  <span className="material-symbols-outlined text-red-500 shrink-0 text-base mt-0.5">error</span>
                  <span>{paymentError}</span>
                </div>
              )}

              <div className="space-y-6">
                {/* Cardholder Name */}
                <div>
                  <label className="block text-xs font-semibold tracking-widest text-[#46464b] mb-2 uppercase">Cardholder Name</label>
                  <input
                    type="text"
                    placeholder="Johnathan Doe"
                    value={cardholderName}
                    onChange={(e) => { setCardholderName(e.target.value); setValidationErrors(prev => ({ ...prev, cardholderName: undefined })); }}
                    className={`w-full bg-white border p-4 focus:outline-none transition-all rounded ${validationErrors.cardholderName ? 'border-red-400 focus:border-red-500' : 'border-[#c7c6cb] focus:border-[#1a1b20]'}`}
                  />
                  {validationErrors.cardholderName && (
                    <p className="mt-1.5 text-xs text-red-600 flex items-center gap-1">
                      <span className="material-symbols-outlined text-sm">error</span>
                      {validationErrors.cardholderName}
                    </p>
                  )}
                </div>

                {/* Cardholder ID */}
                <div>
                  <label className="block text-xs font-semibold tracking-widest text-[#46464b] mb-2 uppercase">Cardholder ID</label>
                  <input
                    type="text"
                    placeholder="123456789"
                    value={cardholderId}
                    onChange={(e) => { setCardholderId(e.target.value); setValidationErrors(prev => ({ ...prev, cardholderId: undefined })); }}
                    className={`w-full bg-white border p-4 focus:outline-none transition-all rounded ${validationErrors.cardholderId ? 'border-red-400 focus:border-red-500' : 'border-[#c7c6cb] focus:border-[#1a1b20]'}`}
                  />
                  {validationErrors.cardholderId && (
                    <p className="mt-1.5 text-xs text-red-600 flex items-center gap-1">
                      <span className="material-symbols-outlined text-sm">error</span>
                      {validationErrors.cardholderId}
                    </p>
                  )}
                </div>

                {/* Card Number */}
                <div>
                  <label className="block text-xs font-semibold tracking-widests text-[#46464b] mb-2 uppercase">Card Number</label>
                  <div className="relative">
                    <input
                      type="text"
                      placeholder="4111 2222 3333 4444"
                      value={cardNumber}
                      onChange={(e) => { setCardNumber(e.target.value); setValidationErrors(prev => ({ ...prev, cardNumber: undefined })); }}
                      className={`w-full bg-white border p-4 pr-12 focus:outline-none transition-all rounded ${validationErrors.cardNumber ? 'border-red-400 focus:border-red-500' : 'border-[#c7c6cb] focus:border-[#1a1b20]'}`}
                    />
                    <span className="absolute right-4 top-1/2 -translate-y-1/2 material-symbols-outlined text-[#46464b]">credit_card</span>
                  </div>
                  {validationErrors.cardNumber && (
                    <p className="mt-1.5 text-xs text-red-600 flex items-center gap-1">
                      <span className="material-symbols-outlined text-sm">error</span>
                      {validationErrors.cardNumber}
                    </p>
                  )}
                </div>

                {/* Expiry + CVV */}
                <div className="grid grid-cols-2 gap-6">
                  <div>
                    <label className="block text-xs font-semibold tracking-widest text-[#46464b] mb-2 uppercase">Expiry Date</label>
                    <input
                      type="text"
                      placeholder="MM / YY"
                      value={expiryDate}
                      onChange={(e) => { setExpiryDate(e.target.value); setValidationErrors(prev => ({ ...prev, expiryDate: undefined })); }}
                      className={`w-full bg-white border p-4 focus:outline-none transition-all rounded ${validationErrors.expiryDate ? 'border-red-400 focus:border-red-500' : 'border-[#c7c6cb] focus:border-[#1a1b20]'}`}
                    />
                    {validationErrors.expiryDate && (
                      <p className="mt-1.5 text-xs text-red-600 flex items-center gap-1">
                        <span className="material-symbols-outlined text-sm">error</span>
                        {validationErrors.expiryDate}
                      </p>
                    )}
                  </div>
                  <div>
                    <label className="block text-xs font-semibold tracking-widest text-[#46464b] mb-2 uppercase">CVV</label>
                    <input
                      type="password"
                      placeholder="•••"
                      value={cvv}
                      onChange={(e) => { setCvv(e.target.value); setValidationErrors(prev => ({ ...prev, cvv: undefined })); }}
                      className={`w-full bg-white border p-4 focus:outline-none transition-all rounded ${validationErrors.cvv ? 'border-red-400 focus:border-red-500' : 'border-[#c7c6cb] focus:border-[#1a1b20]'}`}
                    />
                    {validationErrors.cvv && (
                      <p className="mt-1.5 text-xs text-red-600 flex items-center gap-1">
                        <span className="material-symbols-outlined text-sm">error</span>
                        {validationErrors.cvv}
                      </p>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column: Summary */}
          <div className="lg:col-span-4 lg:sticky lg:top-8">
            <div className="bg-white border border-[#c7c6cb] rounded overflow-hidden shadow-sm">
              <div className="p-6 bg-[#f2f3f9] border-b border-[#c7c6cb]">
                <h3 className="text-xl font-bold tracking-tight text-[#191c20]">Order Summary</h3>
              </div>
              <div className="p-6 space-y-4">
                <div className="flex justify-between items-center">
                  <span className="text-[#46464b]">Subtotal ({totalTicketsCount} Tickets)</span>
                  <span className="font-medium text-[#191c20]">${pricing.subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-[#46464b] flex items-center gap-1">
                    Processing Fee
                    <span className="material-symbols-outlined text-sm cursor-help">info</span>
                  </span>
                  <span className="font-medium text-[#191c20]">${pricing.fee.toFixed(2)}</span>
                </div>
                <div className="border-t border-dashed border-[#c7c6cb] pt-4 flex justify-between items-end">
                  <div>
                    <p className="text-xs font-semibold tracking-widest text-[#46464b] uppercase">Total Due</p>
                    <p className="text-3xl font-bold text-[#191c20] tracking-tighter">${pricing.total.toFixed(2)}</p>
                  </div>
                  <span className="material-symbols-outlined text-[#46464b] mb-1">verified_user</span>
                </div>
                <div className="pt-6">
                  {/* MODIFIED: Trigger the new verification flow instead of straight payment */}
                  <button
                    onClick={handleInitiatePayment}
                    disabled={processState === 'processing' || totalTicketsCount === 0}
                    className="w-full bg-[#1a1b20] text-white py-5 font-bold rounded flex items-center justify-center gap-3 transition-all hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {processState === 'processing' ? (
                      <>
                        <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                        <span>Processing payment...</span>
                      </>
                    ) : (
                      <span>Authorize & Pay ${pricing.total.toFixed(2)}</span>
                    )}
                  </button>
                </div>
              </div>
            </div>

            {eventDetails && (
              <div className="mt-6 p-4 border border-[#c7c6cb] rounded flex gap-4 items-center bg-white">
                <div className="w-16 h-16 bg-[#eceef3] flex items-center justify-center rounded text-[#191c20]">
                  <span className="material-symbols-outlined text-3xl">event</span>
                </div>
                <div>
                  <p className="font-bold text-sm text-[#191c20]">{eventDetails.eventName}</p>
                  <p className="text-xs text-[#46464b]">{eventDetails.eventLocation || 'Venue Map Unassigned'}</p>
                  <p className="text-xs font-mono mt-1 text-gray-500">
                    {eventDetails.eventDateTime ? new Date(eventDetails.eventDateTime).toLocaleString() : 'Date TBD'}
                  </p>
                </div>
              </div>
            )}
          </div>

        </div>
      </main>

      {/* ── Age Verification Modal ── */}
      {isAgeModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-sm space-y-4 border border-gray-200">
            <h3 className="text-lg font-bold text-[#0A192F]">Age Verification</h3>
            <p className="text-sm text-gray-600">Please enter your age to verify event policies before checking out.</p>
            <input
              type="number"
              value={ageInput}
              onChange={(e) => setAgeInput(e.target.value)}
              placeholder="e.g., 25"
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              autoFocus
              onKeyDown={(e) => {
                if (e.key === "Enter") handleConfirmAge();
              }}
            />
            <div className="flex justify-end gap-3 pt-2">
              <button
                onClick={() => { setIsAgeModalOpen(false); setProcessState('idle'); }}
                className="px-4 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-100 rounded transition"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmAge}
                className="px-4 py-2 text-sm font-bold text-white bg-[#0A192F] hover:bg-[#112a4f] rounded transition"
              >
                Verify & Checkout
              </button>
            </div>
          </div>
        </div>
      )}

      <Footer />
    </div>
  );
}
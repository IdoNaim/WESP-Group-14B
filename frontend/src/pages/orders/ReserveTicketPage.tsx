import { useState, useEffect, useCallback } from "react";

// ── Types ─────────────────────────────────────────────────────────────────────

type SeatStatus = "available" | "taken" | "selected";

interface Seat {
  id: string;
  row: number;
  number: number;
  status: SeatStatus;
  price: number;
  section: string;
}

interface StandingZone {
  id: string;
  name: string;
  available: number;
  capacity: number;
  price: number;
  selected: number;
}

interface OrderItem {
  id: string;
  label: string;
  detail: string;
  qty: number;
  unitPrice: number;
}

// ── Constants ─────────────────────────────────────────────────────────────────

const RESERVATION_SECONDS = 10 * 60; // 10 minutes
const MAX_TICKETS = 4;

const SECTION_A_SEATS: Seat[] = Array.from({ length: 16 }, (_, i) => ({
  id: `A-${i}`,
  row: Math.floor(i / 4) + 1,
  number: (i % 4) + 1,
  status: (Math.random() > 0.4 ? "available" : "taken") as SeatStatus,
  price: 120,
  section: "Section A",
}));

const CENTER_STALLS_SEATS: Seat[] = Array.from({ length: 50 }, (_, i) => ({
  id: `C-${i}`,
  row: Math.floor(i / 10) + 1,
  number: (i % 10) + 1,
  status: (Math.random() < 0.3 ? "taken" : "available") as SeatStatus,
  price: 150,
  section: "Center Stalls",
}));

const SECTION_B_SEATS: Seat[] = Array.from({ length: 16 }, (_, i) => ({
  id: `B-${i}`,
  row: Math.floor(i / 4) + 1,
  number: (i % 4) + 1,
  status: (Math.random() > 0.5 ? "available" : "taken") as SeatStatus,
  price: 120,
  section: "Section B",
}));

const INITIAL_STANDING: StandingZone[] = [
  { id: "floor-ga", name: "Floor GA", available: 142, capacity: 200, price: 65, selected: 0 },
  { id: "balcony", name: "Balcony Standing", available: 58, capacity: 100, price: 45, selected: 0 },
];

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60).toString().padStart(2, "0");
  const s = (seconds % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
}

// ── Sub-components ────────────────────────────────────────────────────────────

function SeatDot({
  seat,
  onClick,
}: {
  seat: Seat;
  onClick: (seat: Seat) => void;
}) {
  const colorMap: Record<SeatStatus, string> = {
    available: "#22c55e",
    taken: "#d1d5db",
    selected: "#3b82f6",
  };
  const cursor = seat.status === "taken" ? "cursor-not-allowed" : "cursor-pointer";
  return (
    <div
      title={`${seat.section} Row ${seat.row}, Seat ${seat.number} — $${seat.price}`}
      className={`w-5 h-5 rounded-sm transition-transform hover:scale-110 ${cursor}`}
      style={{ backgroundColor: colorMap[seat.status] }}
      onClick={() => seat.status !== "taken" && onClick(seat)}
    />
  );
}

function StandingZoneCard({
  zone,
  onAdd,
  onRemove,
  totalSelected,
}: {
  zone: StandingZone;
  onAdd: (id: string) => void;
  onRemove: (id: string) => void;
  totalSelected: number;
}) {
  const canAdd = zone.selected < zone.available && totalSelected < MAX_TICKETS;
  const canRemove = zone.selected > 0;
  const isActive = zone.selected > 0;

  return (
    <div className="flex-1">
      <div
        className={`relative w-full h-28 rounded border-2 border-dashed transition-all
          ${isActive ? "border-blue-400 bg-blue-50/40" : "border-gray-300 bg-gray-50"}
        `}
        style={{
          backgroundImage: "radial-gradient(#c5c6cd 0.5px, transparent 0.5px)",
          backgroundSize: "6px 6px",
        }}
      >
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2">
          <div className="bg-white/90 backdrop-blur-sm px-4 py-2 rounded border border-gray-200 shadow-sm text-center">
            <p className="text-[11px] font-bold uppercase tracking-wider text-[#0A192F]">
              {zone.name}
            </p>
            <p className="text-[10px] font-semibold text-green-600">
              {zone.available} available • ${zone.price}/ticket
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              disabled={!canRemove}
              onClick={() => onRemove(zone.id)}
              className="w-6 h-6 rounded-full bg-white border border-gray-300 text-gray-700 flex items-center justify-center text-sm font-bold disabled:opacity-30 hover:bg-gray-100 transition"
            >
              −
            </button>
            <span className="text-sm font-semibold w-4 text-center text-[#0A192F]">
              {zone.selected}
            </span>
            <button
              disabled={!canAdd}
              onClick={() => onAdd(zone.id)}
              className="w-6 h-6 rounded-full bg-white border border-gray-300 text-gray-700 flex items-center justify-center text-sm font-bold disabled:opacity-30 hover:bg-gray-100 transition"
            >
              +
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function PurchaseRules() {
  const [open, setOpen] = useState(false);
  return (
    <div className="rounded-lg overflow-hidden border border-amber-300/50 bg-amber-50/40">
      <button
        className="w-full flex items-center justify-between p-4 hover:bg-amber-50/70 transition-colors"
        onClick={() => setOpen(!open)}
      >
        <div className="flex items-center gap-2 text-amber-800 text-sm font-semibold uppercase tracking-wider">
          <svg className="w-4 h-4 fill-amber-600" viewBox="0 0 24 24">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
          </svg>
          Purchase Rules &amp; Policies
        </div>
        <svg
          className={`w-4 h-4 text-amber-700 transition-transform ${open ? "rotate-180" : ""}`}
          viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}
        >
          <path d="M6 9l6 6 6-6" />
        </svg>
      </button>
      {open && (
        <ul className="px-5 pb-4 space-y-1.5 text-sm text-amber-900">
          <li className="flex items-start gap-2">
            <span className="mt-0.5 text-amber-600">•</span>
            <span><strong>Max {MAX_TICKETS} tickets per buyer</strong> to ensure fair distribution.</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 text-amber-600">•</span>
            <span>Age 12+ recommended; under 16s must be accompanied by an adult.</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 text-amber-600">•</span>
            <span>Regular Sale — resale strictly through EliteTickets platform only.</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-0.5 text-amber-600">•</span>
            <span>Reserved tickets are held for <strong>10 minutes</strong>. Uncompleted orders are released automatically.</span>
          </li>
        </ul>
      )}
    </div>
  );
}

function ActiveOrderPanel({
  items,
  timeLeft,
  onRemove,
  onCheckout,
}: {
  items: OrderItem[];
  timeLeft: number;
  onRemove: (id: string) => void;
  onCheckout: () => void;
}) {
  const isExpired = timeLeft <= 0;
  const isUrgent = timeLeft <= 120 && timeLeft > 0;
  const subtotal = items.reduce((acc, i) => acc + i.unitPrice * i.qty, 0);

  return (
    <div className="bg-[#0A192F] text-white rounded-lg shadow-xl overflow-hidden flex flex-col border border-white/10">
      {/* Header */}
      <div className="p-5 border-b border-white/10 flex justify-between items-center">
        <h2 className="text-lg font-bold">Your Active Order</h2>
        {items.length > 0 && !isExpired && (
          <div
            className={`px-3 py-1 rounded-full flex items-center gap-1.5 text-xs font-mono font-semibold border transition-colors
              ${isUrgent
                ? "bg-red-500/20 text-red-400 border-red-400/40"
                : "bg-amber-400/20 text-amber-400 border-amber-400/30"
              }`}
          >
            <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
              <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
            </svg>
            {formatTime(timeLeft)} remaining
          </div>
        )}
      </div>

      {/* Body */}
      {isExpired ? (
        <div className="p-6 flex flex-col items-center gap-4 text-center">
          <div className="w-12 h-12 rounded-full bg-red-500/20 flex items-center justify-center">
            <svg className="w-6 h-6 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path d="M12 8v4m0 4h.01M21 12A9 9 0 1 1 3 12a9 9 0 0 1 18 0z" />
            </svg>
          </div>
          <div>
            <p className="font-semibold text-red-400">Reservation expired</p>
            <p className="text-sm text-white/50 mt-1">Tickets have been released back to the pool.</p>
          </div>
          <button
            className="mt-2 px-6 py-2.5 bg-white text-[#0A192F] text-sm font-bold rounded uppercase tracking-widest hover:bg-gray-100 transition"
            onClick={() => window.location.reload()}
          >
            Start Over
          </button>
        </div>
      ) : items.length === 0 ? (
        <div className="p-8 flex flex-col items-center gap-3 text-center">
          <div className="w-14 h-14 rounded-full bg-white/5 flex items-center justify-center">
            <svg className="w-7 h-7 text-white/30" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
            </svg>
          </div>
          <p className="text-white/40 text-sm">No active order.</p>
          <p className="text-white/25 text-xs">Select tickets to get started.</p>
        </div>
      ) : (
        <>
          <div className="p-5 space-y-3 max-h-64 overflow-y-auto">
            {items.map((item) => (
              <div
                key={item.id}
                className="flex items-start justify-between gap-3 p-3 bg-white/5 rounded border border-white/10"
              >
                <div className="space-y-0.5">
                  <p className="text-xs font-bold text-amber-400 uppercase tracking-wide">{item.label}</p>
                  <p className="text-xs text-white/50">{item.detail}</p>
                  <p className="text-sm font-semibold">${(item.unitPrice * item.qty).toFixed(2)}</p>
                </div>
                <button
                  className="text-white/30 hover:text-red-400 transition-colors p-1 shrink-0"
                  onClick={() => onRemove(item.id)}
                  title="Remove"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M1 7h22M8 7V5a2 2 0 012-2h4a2 2 0 012 2v2" />
                  </svg>
                </button>
              </div>
            ))}
          </div>

          {/* Summary */}
          <div className="p-5 bg-white/5 border-t border-white/10 space-y-4">
            <div className="flex justify-between text-sm text-white/60">
              <span>Subtotal</span>
              <span>${subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between font-bold text-base border-t border-white/10 pt-3">
              <span>Total</span>
              <span className="text-amber-400">${subtotal.toFixed(2)}</span>
            </div>
            <button
              disabled={items.length === 0}
              onClick={onCheckout}
              className="w-full py-3.5 bg-white text-[#0A192F] text-sm font-bold rounded uppercase tracking-widest hover:bg-gray-100 transition disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Proceed to Checkout
            </button>
          </div>
        </>
      )}
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function ReserveTicketsPage() {
  const [sectionA, setSectionA] = useState<Seat[]>(SECTION_A_SEATS);
  const [centerStalls, setCenterStalls] = useState<Seat[]>(CENTER_STALLS_SEATS);
  const [sectionB, setSectionB] = useState<Seat[]>(SECTION_B_SEATS);
  const [standingZones, setStandingZones] = useState<StandingZone[]>(INITIAL_STANDING);

  const [orderItems, setOrderItems] = useState<OrderItem[]>([]);
  const [timeLeft, setTimeLeft] = useState<number>(RESERVATION_SECONDS);
  const [timerActive, setTimerActive] = useState(false);

  const [banner, setBanner] = useState<{ type: "success" | "error"; message: string } | null>(null);

  // Compute total selected across everything
  const totalSelected =
    sectionA.filter((s) => s.status === "selected").length +
    centerStalls.filter((s) => s.status === "selected").length +
    sectionB.filter((s) => s.status === "selected").length +
    standingZones.reduce((acc, z) => acc + z.selected, 0);

  // Timer
  useEffect(() => {
    if (!timerActive) return;
    if (timeLeft <= 0) return;
    const id = setInterval(() => setTimeLeft((t) => Math.max(0, t - 1)), 1000);
    return () => clearInterval(id);
  }, [timerActive, timeLeft]);

  // Start timer when first item added
  useEffect(() => {
    if (orderItems.length > 0 && !timerActive) setTimerActive(true);
  }, [orderItems, timerActive]);

  // Auto-clear order on expiry
  useEffect(() => {
    if (timeLeft === 0 && orderItems.length > 0) {
      setOrderItems([]);
      setSectionA((prev) =>
        prev.map((s) => (s.status === "selected" ? { ...s, status: "available" } : s))
      );
      setCenterStalls((prev) =>
        prev.map((s) => (s.status === "selected" ? { ...s, status: "available" } : s))
      );
      setSectionB((prev) =>
        prev.map((s) => (s.status === "selected" ? { ...s, status: "available" } : s))
      );
      setStandingZones((prev) => prev.map((z) => ({ ...z, selected: 0 })));
    }
  }, [timeLeft]);

  const showBanner = useCallback(
    (type: "success" | "error", message: string) => {
      setBanner({ type, message });
      setTimeout(() => setBanner(null), 4000);
    },
    []
  );

  // ── Seat click ──
  const handleSeatClick = useCallback(
    (seat: Seat, setFn: React.Dispatch<React.SetStateAction<Seat[]>>) => {
      if (timeLeft === 0) return;

      if (seat.status === "selected") {
        // Deselect
        setFn((prev) =>
          prev.map((s) => (s.id === seat.id ? { ...s, status: "available" } : s))
        );
        setOrderItems((prev) => prev.filter((i) => i.id !== seat.id));
        return;
      }

      if (totalSelected >= MAX_TICKETS) {
        showBanner("error", `Max ${MAX_TICKETS} tickets per order as per purchase policy.`);
        return;
      }

      // Simulate async reservation
      setFn((prev) =>
        prev.map((s) => (s.id === seat.id ? { ...s, status: "selected" } : s))
      );
      setOrderItems((prev) => [
        ...prev,
        {
          id: seat.id,
          label: seat.section,
          detail: `Row ${seat.row}, Seat ${seat.number} • Qty 1`,
          qty: 1,
          unitPrice: seat.price,
        },
      ]);
      showBanner("success", "Ticket reserved! Added to your active order.");
    },
    [totalSelected, timeLeft, showBanner]
  );

  // ── Standing zone ──
  const handleStandingAdd = useCallback(
    (zoneId: string) => {
      if (totalSelected >= MAX_TICKETS) {
        showBanner("error", `Max ${MAX_TICKETS} tickets per order as per purchase policy.`);
        return;
      }
      setStandingZones((prev) =>
        prev.map((z) =>
          z.id === zoneId && z.selected < z.available
            ? { ...z, selected: z.selected + 1 }
            : z
        )
      );
      setStandingZones((zones) => {
        const zone = zones.find((z) => z.id === zoneId)!;
        setOrderItems((prev) => {
          const existing = prev.find((i) => i.id === zoneId);
          if (existing) {
            return prev.map((i) => i.id === zoneId ? { ...i, qty: i.qty + 1 } : i);
          }
          return [
            ...prev,
            {
              id: zone.id,
              label: zone.name,
              detail: `Standing • General Admission`,
              qty: 1,
              unitPrice: zone.price,
            },
          ];
        });
        return zones;
      });
      showBanner("success", "Ticket reserved! Added to your active order.");
    },
    [totalSelected, showBanner]
  );

  const handleStandingRemove = useCallback((zoneId: string) => {
    setStandingZones((prev) =>
      prev.map((z) =>
        z.id === zoneId && z.selected > 0 ? { ...z, selected: z.selected - 1 } : z
      )
    );
    setOrderItems((prev) => {
      const existing = prev.find((i) => i.id === zoneId);
      if (!existing) return prev;
      if (existing.qty <= 1) return prev.filter((i) => i.id !== zoneId);
      return prev.map((i) => i.id === zoneId ? { ...i, qty: i.qty - 1 } : i);
    });
  }, []);

  // ── Remove from order ──
  const handleRemoveItem = useCallback((id: string) => {
    setOrderItems((prev) => prev.filter((i) => i.id !== id));
    setSectionA((prev) =>
      prev.map((s) => (s.id === id ? { ...s, status: "available" } : s))
    );
    setCenterStalls((prev) =>
      prev.map((s) => (s.id === id ? { ...s, status: "available" } : s))
    );
    setSectionB((prev) =>
      prev.map((s) => (s.id === id ? { ...s, status: "available" } : s))
    );
    setStandingZones((prev) =>
      prev.map((z) => (z.id === id ? { ...z, selected: 0 } : z))
    );
  }, []);

  // ── Checkout ──
  const handleCheckout = useCallback(() => {
    alert("Proceeding to checkout…");
  }, []);

  // ── Render ──
  return (
    <div
      className="flex flex-col min-h-screen"
      style={{ fontFamily: "'Inter', sans-serif", backgroundColor: "#fbf9fb", color: "#1b1b1d" }}
    >
      {/* Nav */}
      <header
        style={{ borderBottom: "1px solid #c5c6cd", backgroundColor: "#fbf9fb" }}
        className="sticky top-0 z-50"
      >
        <nav
          className="flex justify-between items-center px-12 py-4 mx-auto"
          style={{ maxWidth: 1280 }}
        >
          <span style={{ fontSize: 22, fontWeight: 700, color: "#0A192F" }}>EliteTickets</span>
          <div className="hidden md:flex items-center gap-8">
            {["Events", "Venues", "My Tickets", "Support"].map((link) => (
              <a
                key={link}
                href="#"
                style={{
                  fontSize: 16,
                  color: link === "My Tickets" ? "#0A192F" : "#5d5f5f",
                  borderBottom: link === "My Tickets" ? "2px solid #0A192F" : "none",
                  paddingBottom: link === "My Tickets" ? 4 : 0,
                  textDecoration: "none",
                }}
              >
                {link}
              </a>
            ))}
          </div>
          <div className="flex items-center gap-4">
            <svg className="w-5 h-5 cursor-pointer" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
            </svg>
            <svg className="w-5 h-5 cursor-pointer" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z" />
            </svg>
          </div>
        </nav>
      </header>

      <main className="flex-grow mx-auto w-full px-12 py-8" style={{ maxWidth: 1280 }}>
        {/* Status Banner */}
        {banner && (
          <div
            className={`mb-5 flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all ${
              banner.type === "success"
                ? "bg-green-50 border border-green-200 text-green-800"
                : "bg-red-50 border border-red-200 text-red-800"
            }`}
          >
            {banner.type === "success" ? (
              <svg className="w-5 h-5 text-green-500 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2zm-1.707 14.621L6.586 12.914l1.414-1.414 2.293 2.293 5.707-5.707 1.414 1.414-7.121 7.121z" />
              </svg>
            ) : (
              <svg className="w-5 h-5 text-red-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path d="M12 8v4m0 4h.01M21 12A9 9 0 1 1 3 12a9 9 0 0 1 18 0z" />
              </svg>
            )}
            {banner.message}
          </div>
        )}

        <div className="grid grid-cols-12 gap-6 items-start">
          {/* ── Left Panel ── */}
          <section
            className="col-span-12 lg:col-span-8 space-y-6 p-8 border rounded-lg"
            style={{ backgroundColor: "#fff", borderColor: "#c5c6cd" }}
          >
            {/* Event Header */}
            <div className="space-y-2">
              <h1 style={{ fontSize: 28, fontWeight: 700, color: "#0A192F", lineHeight: 1.2 }}>
                The Grand Symphony Orchestra — Live in London
              </h1>
              <div className="flex flex-wrap items-center gap-5 text-sm" style={{ color: "#5d5f5f" }}>
                <span className="flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
                  </svg>
                  Sept 24, 2024
                </span>
                <span className="flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
                  </svg>
                  19:30
                </span>
                <span className="flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path d="M12 21s-8-5.686-8-11A8 8 0 0 1 20 10c0 5.314-8 11-8 11z" /><circle cx="12" cy="10" r="3" />
                  </svg>
                  Royal Albert Hall
                </span>
              </div>
            </div>

            {/* Venue Map */}
            <div
              className="p-6 rounded-lg"
              style={{ backgroundColor: "#f5f3f5", border: "1px solid #c5c6cd" }}
            >
              <div className="flex flex-wrap justify-between items-center gap-3 mb-5">
                <h3 style={{ fontSize: 20, fontWeight: 600, color: "#0A192F" }}>Venue Map</h3>
                <div className="flex flex-wrap gap-4 text-xs text-gray-600">
                  {[
                    { color: "#22c55e", label: "Available" },
                    { color: "#d1d5db", label: "Taken" },
                    { color: "#3b82f6", label: "Selected" },
                  ].map((leg) => (
                    <span key={leg.label} className="flex items-center gap-1.5">
                      <span
                        className="w-3.5 h-3.5 rounded-sm inline-block"
                        style={{ backgroundColor: leg.color }}
                      />
                      {leg.label}
                    </span>
                  ))}
                </div>
              </div>

              <div
                className="bg-white border p-6 rounded shadow-sm overflow-x-auto"
                style={{ borderColor: "#c5c6cd" }}
              >
                <div className="min-w-[650px] flex flex-col items-center gap-8">
                  {/* Stage */}
                  <div
                    className="w-1/2 h-10 flex items-center justify-center rounded text-white text-xs font-bold uppercase tracking-widest shadow-sm"
                    style={{ backgroundColor: "#0A192F" }}
                  >
                    STAGE
                  </div>

                  {/* Seated sections */}
                  <div className="flex justify-center items-start w-full gap-8">
                    {[
                      { label: "Section A", seats: sectionA, setFn: setSectionA, cols: 4 },
                      { label: "Center Stalls", seats: centerStalls, setFn: setCenterStalls, cols: 10 },
                      { label: "Section B", seats: sectionB, setFn: setSectionB, cols: 4 },
                    ].map(({ label, seats, setFn, cols }) => (
                      <div key={label} className="flex flex-col items-center gap-2">
                        <span
                          className="text-[11px] uppercase font-semibold tracking-wider"
                          style={{ color: label === "Center Stalls" ? "#0A192F" : "#5d5f5f" }}
                        >
                          {label}
                        </span>
                        <div
                          className="grid gap-1.5"
                          style={{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }}
                        >
                          {seats.map((seat) => (
                            <SeatDot
                              key={seat.id}
                              seat={seat}
                              onClick={(s) => handleSeatClick(s, setFn)}
                            />
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Divider */}
                  <div className="w-full h-px" style={{ backgroundColor: "#c5c6cd" }} />

                  {/* Standing zones */}
                  <div className="w-full flex flex-col md:flex-row gap-6">
                    {standingZones.map((zone) => (
                      <StandingZoneCard
                        key={zone.id}
                        zone={zone}
                        onAdd={handleStandingAdd}
                        onRemove={handleStandingRemove}
                        totalSelected={totalSelected}
                      />
                    ))}
                  </div>
                </div>
              </div>
            </div>

            {/* Purchase Rules */}
            <PurchaseRules />

            {/* Policy validation hint */}
            {totalSelected >= MAX_TICKETS && (
              <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 px-4 py-2 rounded">
                You've reached the maximum of <strong>{MAX_TICKETS} tickets</strong> per order as per purchase policy.
              </p>
            )}

            {/* Reserve CTA */}
            <div className="flex justify-end pt-2">
              <button
                disabled={totalSelected === 0 || timeLeft === 0}
                className="px-12 py-4 text-sm font-bold uppercase tracking-widest rounded shadow transition-all hover:brightness-110 active:opacity-80 disabled:opacity-40 disabled:cursor-not-allowed"
                style={{ backgroundColor: "#FFB400", color: "#0A192F" }}
                onClick={() =>
                  showBanner("success", "All selected tickets have been reserved in your active order.")
                }
              >
                Reserve Tickets
              </button>
            </div>
          </section>

          {/* ── Right Panel ── */}
          <aside className="col-span-12 lg:col-span-4 sticky top-24 space-y-4">
            <ActiveOrderPanel
              items={orderItems}
              timeLeft={timeLeft}
              onRemove={handleRemoveItem}
              onCheckout={handleCheckout}
            />

          </aside>
        </div>
      </main>

      {/* Footer */}
      <footer
        className="w-full mt-auto border-t"
        style={{ borderColor: "#c5c6cd", backgroundColor: "#fff" }}
      >
        <div
          className="flex flex-col md:flex-row justify-between items-center px-12 py-8 gap-3 mx-auto"
          style={{ maxWidth: 1280 }}
        >
          <span className="text-xs font-bold uppercase tracking-widest" style={{ color: "#1b1b1d" }}>
            EliteTickets Global
          </span>
          <div className="flex gap-6">
            {["Privacy Policy", "Terms of Service", "Cookie Settings", "Contact Us"].map((t) => (
              <a key={t} href="#" className="text-xs hover:text-gray-800 transition-colors" style={{ color: "#5d5f5f" }}>
                {t}
              </a>
            ))}
          </div>
          <span className="text-xs" style={{ color: "#5d5f5f" }}>
            © 2024 EliteTickets Global. All rights reserved.
          </span>
        </div>
      </footer>
    </div>
  );
}
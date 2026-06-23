import { useState, useEffect, useCallback } from "react";
import { eventApi, EventDTO, SeatingMapDTO, AssignedSeatDTO, PurchasePolicyDTO } from "../../api/eventsApi";
import { authApi } from "../../api/authApi";
import { activeOrderApi, ActiveOrderDTO } from "../../api/activeOrderApi";
import { historyOrderApi } from "../../api/historyOrderApi";
import { getCompanyPurchasePolicy } from "../../api/productionCompanyApi";
import { useNavigate } from "react-router-dom";

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

interface ZoneData {
  zone: string;
  seats: Seat[];
  cols: number;
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
const RESERVATION_MINUTES = 15;
const RESERVATION_SECONDS = RESERVATION_MINUTES * 60;
const checkoutWindowURL = "/checkout";

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60).toString().padStart(2, "0");
  const s = (seconds % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
}

function getZonePrefix(id: string): string {
  return id.split("_")[0] ?? id;
}

function parseSeatId(id: string): { zone: string; row: number; number: number } | null {
  const match = id.match(/^(.+)_(\d+)_(\d+)$/);
  if (!match) return null;
  return {
    zone: match[1],
    row: parseInt(match[2], 10),
    number: parseInt(match[3], 10),
  };
}

function buildZonesFromSeatingMap(
  dto: SeatingMapDTO,
  activeOrder: ActiveOrderDTO | null
): { zones: ZoneData[]; standing: StandingZone[] } {
  const activeSeatIds = new Set(activeOrder?.seatIds ?? []);

  const zoneMap = new Map<string, AssignedSeatDTO[]>();
  for (const seat of dto.assignedSeats) {
    const zone = getZonePrefix(seat.id);
    if (!zoneMap.has(zone)) zoneMap.set(zone, []);
    zoneMap.get(zone)!.push(seat);
  }

  const zones: ZoneData[] = [];
  for (const [zone, seatDTOs] of zoneMap) {
    const seats: Seat[] = seatDTOs.flatMap((s) => {
      const p = parseSeatId(s.id);
      if (!p) return [];
      const status: SeatStatus = activeSeatIds.has(s.id)
        ? "selected"
        : s.isBooked
        ? "taken"
        : "available";
      return [
        {
          id: s.id,
          row: p.row,
          number: p.number,
          status,
          price: s.priceForTicket,
          section: zone,
        },
      ];
    });
    seats.sort((a, b) => a.row - b.row || a.number - b.number);
    const cols = seats.length > 0 ? Math.max(...seats.map((s) => s.number)) : 1;
    zones.push({ zone, seats, cols });
  }

  const activeStandingMap = new Map(
    Object.entries(activeOrder?.StandingAreaQuantities ?? {})
  );

  const standing: StandingZone[] = dto.standingAreas.map((a) => ({
    id: a.areaId,
    name: a.areaId,
    available: a.availableSeats,
    capacity: a.capacity,
    price: a.priceForTicket,
    selected: activeStandingMap.get(a.areaId) ?? 0,
  }));

  return { zones, standing };
}

function buildOrderItemsFromActiveOrder(
  activeOrder: ActiveOrderDTO,
  zones: ZoneData[],
  standingZones: StandingZone[]
): OrderItem[] {
  const items: OrderItem[] = [];

  for (const seatId of activeOrder.seatIds ?? []) {
    const parsed = parseSeatId(seatId);
    if (!parsed) continue;
    const zoneData = zones.find((z) => z.zone === parsed.zone);
    const seat = zoneData?.seats.find((s) => s.id === seatId);
    if (!seat) continue;
    items.push({
      id: seatId,
      label: `Zone ${parsed.zone}`,
      detail: `Row ${parsed.row}, Seat ${parsed.number} • Qty 1`,
      qty: 1,
      unitPrice: seat.price,
    });
  }

  for (const [areaId, quantity] of Object.entries(activeOrder.StandingAreaQuantities ?? {})) {
    if (quantity <= 0) continue;
    const zone = standingZones.find((z) => z.id === areaId);
    if (!zone) continue;
    items.push({
      id: areaId,
      label: zone.name,
      detail: "Standing • General Admission",
      qty: quantity,
      unitPrice: zone.price,
    });
  }

  return items;
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
      title={`Zone ${seat.section} Row ${seat.row}, Seat ${seat.number} — $${seat.price}`}
      className={`w-5 h-5 rounded-sm transition-transform hover:scale-110 ${cursor}`}
      style={{ backgroundColor: colorMap[seat.status] }}
      onClick={() => seat.status !== "taken" && onClick(seat)}
    />
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

function StandingZoneCard({
  zone,
  onAdd,
  onRemove,
  totalSelected,
  maxTickets,
}: {
  zone: StandingZone;
  onAdd: (id: string) => void;
  onRemove: (id: string) => void;
  totalSelected: number;
  maxTickets: number | null;
}) {
  const canAdd = zone.selected < zone.available && (maxTickets === null || totalSelected < maxTickets);
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

function PurchaseRules({ policy }: { policy: PurchasePolicyDTO | null }) {
  const [open, setOpen] = useState(false);
  const rules: string[] = [];

  if (policy) {
    if (policy.minTickets !== null && policy.maxTickets !== null) {
      rules.push(
        policy.isQuantityOr
          ? `Purchase up to ${policy.maxTickets} tickets, or buy in bulk (minimum ${policy.minTickets}).`
          : `Between ${policy.minTickets} and ${policy.maxTickets} tickets per order.`
      );
    } else if (policy.minTickets !== null) {
      rules.push(`Minimum ${policy.minTickets} ticket(s) per order.`);
    } else if (policy.maxTickets !== null) {
      rules.push(`Maximum ${policy.maxTickets} ticket(s) per order.`);
    }

    if (policy.minAge !== null && policy.maxAge !== null) {
      rules.push(
        policy.isAgeOr
          ? `Open to attendees under ${policy.maxAge} or over ${policy.minAge} years old.`
          : `Attendees must be between ${policy.minAge} and ${policy.maxAge} years old.`
      );
    } else if (policy.minAge !== null) {
      rules.push(`Minimum age requirement: ${policy.minAge} years old.`);
    } else if (policy.maxAge !== null) {
      rules.push(`Maximum age requirement: ${policy.maxAge} years old.`);
    }

    if (
      (policy.minAge !== null || policy.maxAge !== null) &&
      (policy.minTickets !== null || policy.maxTickets !== null)
    ) {
      rules.push(
        policy.isAgeAndQuantityOr
          ? "Either the age policy or the ticket quantity policy must be satisfied."
          : "Both the age policy and the ticket quantity policy must be satisfied."
      );
    }
  } else {
    rules.push("Minimum of 1 ticket required per order. Maximum amount is unlimited.");
  }

  rules.push("Resale strictly through Idodo Tickets platform only.");
  rules.push(
    `Reserved tickets are held for ${RESERVATION_MINUTES} minutes. Uncompleted orders are released automatically.`
  );

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
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth={2}
        >
          <path d="M6 9l6 6 6-6" />
        </svg>
      </button>
      {open && (
        <ul className="px-5 pb-4 space-y-1.5 text-sm text-amber-900">
          {rules.map((rule, i) => (
            <li key={i} className="flex items-start gap-2">
              <span className="mt-0.5 text-amber-600">•</span>
              <span>{rule}</span>
            </li>
          ))}
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
          <div className="p-5 bg-white/5 border-t border-white/10 space-y-4">
            <div className="flex justify-between text-sm text-white/60">
              <span>Subtotal</span>
              <span>${subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between font-bold text-base border-t border-white/10 pt-3">
              <span>Total</span>
              <span className="text-amber-400">${subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-sm border-t border-white/10 pt-3 text-white/60">
              <span>Tickets selected</span>
              <span className="text-white font-semibold">
                {items.reduce((acc, i) => acc + i.qty, 0)}
              </span>
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
  const navigate = useNavigate();
  const [currentOrder, setCurrentOrder] = useState<ActiveOrderDTO | null>(null);

  const [zones, setZones] = useState<ZoneData[]>([]);
  const [standingZones, setStandingZones] = useState<StandingZone[]>([]);

  const [orderItems, setOrderItems] = useState<OrderItem[]>([]);
  const [timeLeft, setTimeLeft] = useState<number>(RESERVATION_SECONDS);
  const [timerActive, setTimerActive] = useState(false);

  const [banner, setBanner] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const [event, setEvent] = useState<EventDTO | null>(null);
  const [eventLoading, setEventLoading] = useState(true);
  const [eventError, setEventError] = useState(false);

  const [mapLoading, setMapLoading] = useState(true);
  const [mapError, setMapError] = useState(false);

  const [purchasePolicy, setPurchasePolicy] = useState<PurchasePolicyDTO | null>(null);
  const [companyPurchasePolicy, setCompanyPurchasePolicy] = useState<PurchasePolicyDTO | null>(null);
  const [alreadyPurchased, setAlreadyPurchased] = useState(0);

  // Derived merged policy
  const mergedPolicy: PurchasePolicyDTO | null = (() => {
    if (!purchasePolicy && !companyPurchasePolicy) return null;
    const base = purchasePolicy ?? companyPurchasePolicy!;
    if (!purchasePolicy || !companyPurchasePolicy) return base;
    return {
      ...base,
      maxTickets: (() => {
        const vals = [purchasePolicy.maxTickets, companyPurchasePolicy.maxTickets]
          .filter((v): v is number => v !== null && v !== undefined);
        return vals.length > 0 ? Math.min(...vals) : null;
      })(),
      minTickets: (() => {
        const vals = [purchasePolicy.minTickets, companyPurchasePolicy.minTickets]
          .filter((v): v is number => v !== null && v !== undefined);
        return vals.length > 0 ? Math.max(...vals) : null;
      })(),
    };
  })();

  const effectiveMaxTickets = mergedPolicy?.maxTickets ?? null;

  const remainingAllowed = effectiveMaxTickets !== null ? Math.max(0, effectiveMaxTickets - alreadyPurchased) : null;

  useEffect(() => {
    const token = localStorage.getItem("token") ?? "";

    authApi.getCurrentUser(token)
      .then((user) => {
        return activeOrderApi.getActiveOrderByUserId(token, user.userId).then((activeOrder) => ({ user, activeOrder }));
      })
      .then(({ user, activeOrder }) => {
        if (!activeOrder) {
          setEventLoading(false);
          setMapLoading(false);
          return;
        }

        setCurrentOrder(activeOrder);

        const createdTime = new Date(activeOrder.createdAt ?? '').getTime();
        const expiresAtTime = createdTime + RESERVATION_MINUTES * 60 * 1000;
        const remainingSeconds = Math.floor((expiresAtTime - Date.now()) / 1000);
        setTimeLeft(Math.max(0, remainingSeconds));

        const { eventId } = activeOrder;

        historyOrderApi.getUserOrders(token, user.userId)
          .then((pastOrders) => {
            const count = pastOrders
              .filter((o) => o.eventId === eventId)
              .reduce((sum, o) => {
                const seats = o.seatIds?.length ?? 0;
                const standing = Object.values(o.standingAreaQuantities ?? {}).reduce((a, b) => a + b, 0);
                return sum + seats + standing;
              }, 0);
            setAlreadyPurchased(count);
          })
          .catch(() => {});

        eventApi.getEvent(token, eventId)
          .then((data) => {
            if (!data) { setEventError(true); return; }
            setEvent(data);
          })
          .catch(() => setEventError(true))
          .finally(() => setEventLoading(false));

        eventApi.getEventPurchasePolicy(token, eventId)
          .then((policy) => {
            if (policy) setPurchasePolicy(policy);
          })
          .catch(() => {});

        // Fetch company policy via the event's companyId
        eventApi.getEventCompanyId(token, eventId)
          .then((companyId) => {
            if (companyId == null) return null;
            return getCompanyPurchasePolicy(companyId);
          })
          .then((policy) => {
            if (policy) setCompanyPurchasePolicy(policy);
          })
          .catch(() => {});

        eventApi.getEventSeatingMap(token, eventId)
          .then((mapData) => {
            if (!mapData) { setMapError(true); return; }
            const { zones: z, standing } = buildZonesFromSeatingMap(mapData, activeOrder);
            setZones(z);
            setStandingZones(standing);
            const items = buildOrderItemsFromActiveOrder(activeOrder, z, standing);
            setOrderItems(items);
            if (items.length > 0) setTimerActive(true);
          })
          .catch((err) => {
            console.log("seating map error:", err);
            setMapError(true);
          })
          .finally(() => setMapLoading(false));
      })
      .catch(() => {
        setEventError(true);
        setMapError(true);
        setEventLoading(false);
        setMapLoading(false);
      });
  }, []);

  const totalSelected =
    zones.reduce((acc, z) => acc + z.seats.filter((s) => s.status === "selected").length, 0) +
    standingZones.reduce((acc, z) => acc + z.selected, 0);

  useEffect(() => {
    if (!timerActive) return;
    if (timeLeft <= 0) return;
    const id = setInterval(() => setTimeLeft((t) => Math.max(0, t - 1)), 1000);
    return () => clearInterval(id);
  }, [timerActive, timeLeft]);

  useEffect(() => {
    if (orderItems.length > 0 && !timerActive) setTimerActive(true);
  }, [orderItems, timerActive]);

  useEffect(() => {
    if (timeLeft === 0 && orderItems.length > 0) {
      setOrderItems([]);
      setZones((prev) =>
        prev.map((z) => ({
          ...z,
          seats: z.seats.map((s) =>
            s.status === "selected" ? { ...s, status: "available" as SeatStatus } : s
          ),
        }))
      );
      setStandingZones((prev) => prev.map((z) => ({ ...z, selected: 0 })));
    }
  }, [timeLeft, orderItems.length]);

  const showBanner = useCallback(
    (type: "success" | "error", message: string) => {
      setBanner({ type, message });
      setTimeout(() => setBanner(null), 4000);
    },
    []
  );

  const handleSeatClick = useCallback(
    (seat: Seat) => {
      if (timeLeft === 0) return;

      if (seat.status === "selected") {
        setZones((prev) =>
          prev.map((z) =>
            z.zone === seat.section
              ? { ...z, seats: z.seats.map((s) => s.id === seat.id ? { ...s, status: "available" as SeatStatus } : s) }
              : z
          )
        );
        setOrderItems((prev) => prev.filter((i) => i.id !== seat.id));
        return;
      }

      if (remainingAllowed !== null && totalSelected >= remainingAllowed) {
        const msg = alreadyPurchased > 0
          ? `You have already purchased ${alreadyPurchased} ticket(s) for this event. Only ${remainingAllowed} more allowed (limit is ${effectiveMaxTickets}).`
          : `Maximum ${effectiveMaxTickets} ticket(s) allowed per person for this event.`;
        showBanner("error", msg);
        return;
      }

      setZones((prev) =>
        prev.map((z) =>
          z.zone === seat.section
            ? { ...z, seats: z.seats.map((s) => s.id === seat.id ? { ...s, status: "selected" as SeatStatus } : s) }
            : z
        )
      );
      setOrderItems((prev) => [
        ...prev,
        {
          id: seat.id,
          label: `Zone ${seat.section}`,
          detail: `Row ${seat.row}, Seat ${seat.number} • Qty 1`,
          qty: 1,
          unitPrice: seat.price,
        },
      ]);
    },
    [totalSelected, timeLeft, showBanner, effectiveMaxTickets, remainingAllowed, alreadyPurchased]
  );

  const handleStandingAdd = useCallback(
    (zoneId: string) => {
      if (remainingAllowed !== null && totalSelected >= remainingAllowed) {
        const msg = alreadyPurchased > 0
          ? `You have already purchased ${alreadyPurchased} ticket(s) for this event. Only ${remainingAllowed} more allowed (limit is ${effectiveMaxTickets}).`
          : `Maximum ${effectiveMaxTickets} ticket(s) allowed per person for this event.`;
        showBanner("error", msg);
        return;
      }
      const zone = standingZones.find((z) => z.id === zoneId);
      if (!zone || zone.selected >= zone.available) return;

      setStandingZones((prev) =>
        prev.map((z) => z.id === zoneId ? { ...z, selected: z.selected + 1 } : z)
      );
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
            detail: "Standing • General Admission",
            qty: 1,
            unitPrice: zone.price,
          },
        ];
      });
    },
    [totalSelected, showBanner, standingZones, effectiveMaxTickets, remainingAllowed, alreadyPurchased]
  );

  const handleStandingRemove = useCallback((zoneId: string) => {
    setStandingZones((prev) =>
      prev.map((z) => z.id === zoneId && z.selected > 0 ? { ...z, selected: z.selected - 1 } : z)
    );
    setOrderItems((prev) => {
      const existing = prev.find((i) => i.id === zoneId);
      if (!existing) return prev;
      if (existing.qty <= 1) return prev.filter((i) => i.id !== zoneId);
      return prev.map((i) => i.id === zoneId ? { ...i, qty: i.qty - 1 } : i);
    });
  }, []);

  const handleRemoveItem = useCallback((id: string) => {
    setOrderItems((prev) => prev.filter((i) => i.id !== id));
    setZones((prev) =>
      prev.map((z) => ({
        ...z,
        seats: z.seats.map((s) => s.id === id ? { ...s, status: "available" as SeatStatus } : s),
      }))
    );
    setStandingZones((prev) =>
      prev.map((z) => z.id === id ? { ...z, selected: 0 } : z)
    );
  }, []);

  const handleReserveTickets = async (): Promise<boolean> => {
    if (!currentOrder) return false;

    const token = localStorage.getItem("token") ?? "";

    const selectedSeatIds = zones
      .flatMap((z) => z.seats)
      .filter((s) => s.status === "selected")
      .map((s) => s.id);

    const standingQuantities: Record<string, number> = {};
    standingZones.forEach((z) => {
      if (z.selected > 0) {
        standingQuantities[z.id] = z.selected;
      }
    });

    const updatedOrder: ActiveOrderDTO = {
      ...currentOrder,
      seatIds: selectedSeatIds,
      StandingAreaQuantities: standingQuantities,
    };

    try {
      const success = await activeOrderApi.updateActiveOrder(token, currentOrder.orderId, updatedOrder);
      if (success) {
        return true;
      } else {
        showBanner("error", "Failed to reserve tickets. They might no longer be available.");
        return false;
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "An error occurred while reserving tickets. Please try again.";
      showBanner("error", message);
      return false;
    }
  };

  const handleInitiateCheckout = () => {
    proceedToCheckout();
  };

  const proceedToCheckout = async () => {
    if (!purchasePolicy && totalSelected < 1) {
      showBanner("error", "Policy Violation: You must select at least 1 ticket when no custom policy is defined.");
      return;
    }

    const isSuccess = await handleReserveTickets();
    if (isSuccess) {
      navigate(checkoutWindowURL);
    } else {
      showBanner("error", "Unable to reserve tickets for checkout. Please review your selection and try again.");
    }
  };

  return (
    <div
      className="flex flex-col min-h-screen relative"
      style={{ fontFamily: "'Inter', sans-serif", backgroundColor: "#fbf9fb", color: "#1b1b1d" }}
    >
      <main className="flex-grow mx-auto w-full px-12 py-8" style={{ maxWidth: 1280 }}>
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
            {eventLoading ? (
              <div className="space-y-3 animate-pulse">
                <div className="h-8 bg-gray-200 rounded w-3/4" />
                <div className="h-4 bg-gray-100 rounded w-1/2" />
              </div>
            ) : eventError || !event ? (
              <div className="flex items-center gap-3 px-4 py-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
                <svg className="w-5 h-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path d="M12 8v4m0 4h.01M21 12A9 9 0 1 1 3 12a9 9 0 0 1 18 0z" />
                </svg>
                Could not load event details. Please refresh or try again later.
              </div>
            ) : (
              <div className="space-y-2">
                <h1 style={{ fontSize: 28, fontWeight: 700, color: "#0A192F", lineHeight: 1.2 }}>
                  {event.eventName}
                </h1>
                <div className="flex flex-wrap items-center gap-5 text-sm" style={{ color: "#5d5f5f" }}>
                  {event.eventDateTime && (
                    <>
                      <span className="flex items-center gap-1">
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" />
                        </svg>
                        {new Date(event.eventDateTime).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" })}
                      </span>
                      <span className="flex items-center gap-1">
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                          <circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" />
                        </svg>
                        {new Date(event.eventDateTime).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })}
                      </span>
                    </>
                  )}
                  {event.eventLocation && (
                    <span className="flex items-center gap-1">
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path d="M12 21s-8-5.686-8-11A8 8 0 0 1 20 10c0 5.314-8 11-8 11z" /><circle cx="12" cy="10" r="3" />
                      </svg>
                      {event.eventLocation}
                    </span>
                  )}
                </div>
              </div>
            )}

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
                      <span className="w-3.5 h-3.5 rounded-sm inline-block" style={{ backgroundColor: leg.color }} />
                      {leg.label}
                    </span>
                  ))}
                </div>
              </div>

              <div className="bg-white border p-6 rounded shadow-sm overflow-x-auto" style={{ borderColor: "#c5c6cd" }}>
                {mapLoading ? (
                  <div className="flex justify-center items-center py-16">
                    <div className="w-8 h-8 border-4 border-gray-200 border-t-blue-500 rounded-full animate-spin" />
                  </div>
                ) : mapError ? (
                  <div className="flex items-center gap-3 px-4 py-3 rounded-lg bg-red-50 border border-red-200 text-red-700 text-sm">
                    <svg className="w-5 h-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path d="M12 8v4m0 4h.01M21 12A9 9 0 1 1 3 12a9 9 0 0 1 18 0z" />
                    </svg>
                    Could not load seating map. Please refresh or try again later.
                  </div>
                ) : zones.length === 0 && standingZones.length === 0 ? (
                  <div className="py-10 text-center text-sm text-gray-400">
                    No seating map has been configured for this event.
                  </div>
                ) : (
                  <div className="min-w-[650px] flex flex-col items-center gap-8">
                    <div
                      className="w-1/2 h-10 flex items-center justify-center rounded text-white text-xs font-bold uppercase tracking-widest shadow-sm"
                      style={{ backgroundColor: "#0A192F" }}
                    >
                      STAGE
                    </div>

                    {zones.length > 0 && (
                      <div className="flex justify-center items-start w-full gap-8 flex-wrap">
                        {zones.map(({ zone, seats, cols }) => (
                          <div key={zone} className="flex flex-col items-center gap-2">
                            <span className="text-[11px] uppercase font-semibold tracking-wider" style={{ color: "#5d5f5f" }}>
                              Zone {zone}
                            </span>
                            <div className="grid gap-1.5" style={{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }}>
                              {seats.map((seat) => (
                                <SeatDot key={seat.id} seat={seat} onClick={handleSeatClick} />
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}

                    {zones.length > 0 && standingZones.length > 0 && (
                      <div className="w-full h-px" style={{ backgroundColor: "#c5c6cd" }} />
                    )}

                    {standingZones.length > 0 && (
                      <div className="w-full flex flex-col md:flex-row gap-6">
                        {standingZones.map((zone) => (
                          <StandingZoneCard
                            key={zone.id}
                            zone={zone}
                            onAdd={handleStandingAdd}
                            onRemove={handleStandingRemove}
                            totalSelected={totalSelected}
                            maxTickets={remainingAllowed}
                          />
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* Fixed component reference */}
            <PurchaseRules policy={mergedPolicy} />

            {effectiveMaxTickets !== null && alreadyPurchased >= effectiveMaxTickets && (
              <p className="text-sm text-red-700 bg-red-50 border border-red-200 px-4 py-2 rounded">
                You have already purchased the maximum of <strong>{effectiveMaxTickets} ticket(s)</strong> allowed for this event. No further purchases are permitted.
              </p>
            )}
            {effectiveMaxTickets !== null && alreadyPurchased > 0 && alreadyPurchased < effectiveMaxTickets && (
              <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 px-4 py-2 rounded">
                You have already purchased <strong>{alreadyPurchased}</strong> ticket(s) for this event. You may add up to <strong>{remainingAllowed}</strong> more (limit is {effectiveMaxTickets}).
              </p>
            )}
            {effectiveMaxTickets !== null && alreadyPurchased === 0 && totalSelected >= effectiveMaxTickets && (
              <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 px-4 py-2 rounded">
                You've reached the maximum of <strong>{effectiveMaxTickets} ticket(s)</strong> allowed per person for this event.
              </p>
            )}
          </section>

          {/* ── Right Panel ── */}
          <aside className="col-span-12 lg:col-span-4 sticky top-24 space-y-4">
            <ActiveOrderPanel
              items={orderItems}
              timeLeft={timeLeft}
              onRemove={handleRemoveItem}
              onCheckout={handleInitiateCheckout}
            />

            {event?.eventLocation && (
              <div className="p-4 border rounded flex items-center gap-4 bg-white" style={{ borderColor: "#c5c6cd" }}>
                <div className="w-16 h-16 rounded bg-gray-100 flex items-center justify-center shrink-0">
                  <svg className="w-7 h-7 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                    <path d="M12 21s-8-5.686-8-11A8 8 0 0 1 20 10c0 5.314-8 11-8 11z" /><circle cx="12" cy="10" r="3" />
                  </svg>
                </div>
                <div>
                  <p className="font-semibold text-sm" style={{ color: "#0A192F" }}>{event.eventLocation}</p>
                  <p className="text-xs mt-0.5" style={{ color: "#5d5f5f" }}>Event venue</p>
                </div>
              </div>
            )}
          </aside>
        </div>
      </main>
    </div>
  );
}
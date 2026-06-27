
const BASE_URL = '/api/events';

// ==========================================
// TypeScript Interfaces (DTOs)
// ==========================================

export interface EventDTO {
    eventId?: string;
    companyId?: number;
    eventName: string;
    eventCapacity: number;
    availableTickets: availableTickets;
    eventDateTime: string; // ISO 8601 e.g. "2026-10-24T21:00:00"
    isActive?: boolean;
    eventLocation?: string | null;
    ticketPrice?: number | null;
    imageUrl?: string | null;
    minZonePrice?: number | null;
    maxZonePrice?: number | null;
}

export interface PurchasePolicyDTO {
    minTickets?: number | null;
    maxTickets?: number | null;
    isQuantityOr: boolean;
    minAge?: number | null;
    maxAge?: number | null;
    isAgeOr: boolean;
    isAgeAndQuantityOr: boolean;
}

export interface DiscountDTO {
    discountPercentage: number;
    description: string;
    validUntil?: string;
}

export interface CreateEventRequestDTO {
    event: EventDTO;
    purchasePolicy: PurchasePolicyDTO;
    discounts?: DiscountDTO[];
}

export interface EditEventDateRequestDTO {
    newDateTime: string;
}

export interface EditEventCapacityRequestDTO {
    newCapacity: number;
}

export interface SeatingAreaConfig {
    rows: number;
    seatsPerRow: number;
    price: number;
}

export interface AssignedSeatDTO {
    id: string;
    isBooked: boolean;
    orderId?: string;
    priceForTicket: number;
}
export interface StandingAreaDTO {
    areaId: string;
    availableSeats: number;
    capacity: number;
    priceForTicket: number;
}

export interface SeatingMapDTO {
    assignedSeats: AssignedSeatDTO[];
    standingAreas: StandingAreaDTO[];
}

export interface StandingAreaConfig {
    capacity: number;
    price: number;
}

export interface ConfigureSeatingMapRequestDTO {
    seatingAreas?: SeatingAreaConfig[];
    standingAreas?: StandingAreaConfig[];
}

// ==========================================
// Helper: Header Generator
// ==========================================
const getHeaders = (token: string) => ({
    'Content-Type': 'application/json',
    'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}`
});

// ==========================================
// API Service Methods
// ==========================================

export const eventApi = {
    /**
     * POST /api/events
     * Creates a new event with its purchase policy and optional discounts.
     */
    createEvent: async (token: string, data: CreateEventRequestDTO): Promise<string | null> => {
        const response = await fetch(`${BASE_URL}`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        if (!response.ok) return null;
        return response.text(); // Returns the created event ID
    },

    /**
     * GET /api/events/active
     * Retrieves all active events (public, no auth required).
     */
    getAllActiveEvents: async (): Promise<EventDTO[]> => {
        const response = await fetch(`${BASE_URL}/active`, { method: 'GET' });
        if (!response.ok) return [];
        return response.json();
    },

    /**
     * GET /api/events/{eventId}
     * Retrieves a specific event by its ID.
     */
    getEvent: async (token: string, eventId: string | number): Promise<EventDTO | null> => {
        const response = await fetch(`${BASE_URL}/${eventId}`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        if (!response.ok) return null;
        return response.json();
    },

    /**
     * GET /api/events?companyId={companyId}
     * Retrieves all events associated with a specific production company.
     */
    getEventsByCompany: async (token: string, companyId: number): Promise<EventDTO[]> => {
        const response = await fetch(`${BASE_URL}?companyId=${companyId}`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        if (!response.ok) throw new Error('Failed to fetch events for this company');
        return response.json();
    },

    getEventCompanyId: async (token: string, eventId: string): Promise<number> => {
    const response = await fetch(`${BASE_URL}/companyId?eventId=${eventId}`, {
        method: 'GET',
        headers: getHeaders(token),
    });
    if (!response.ok) throw new Error("failed to get events companyId");
    
    // Parse the plain numeric string into a TypeScript number
    return Number(await response.text());
    },

    /**
     * PUT /api/events/{eventId}/date
     * Updates the date and time of an existing event.
     */
    editEventDate: async (token: string, eventId: string | number, data: EditEventDateRequestDTO): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${eventId}/date`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return response.ok;
    },

    /**
     * PUT /api/events/{eventId}/capacity
     * Updates the total capacity for an event.
     */
    editEventCapacity: async (token: string, eventId: string | number, data: EditEventCapacityRequestDTO): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${eventId}/capacity`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return response.ok;
    },

    /**
     * DELETE /api/events/{eventId}
     * Removes an event from the system.
     */
    removeEvent: async (token: string, eventId: string | number): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${eventId}`, {
            method: 'DELETE',
            headers: getHeaders(token),
        });
        return response.ok;
    },

    /**
     * PUT /api/events/{eventId}/location
     */
    editEventLocation: async (token: string, eventId: string | number, newLocation: string | null): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${eventId}/location`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify({ newLocation }),
        });
        return response.ok;
    },

    /**
     * PUT /api/events/{eventId}/price
     */
    editEventPrice: async (token: string, eventId: string | number, newPrice: number | null): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${eventId}/price`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify({ newPrice }),
        });
        return response.ok;
    },

    /**
     * PUT /api/events/{eventId}/policy
     */
    editEventPolicy: async (token: string, eventId: string | number, policy: PurchasePolicyDTO): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${eventId}/policy`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify(policy),
        });
        console.log(policy);
        return response.ok;
    },

    /**
     * PUT /api/events/{eventId}/image
     * Updates the event's photo (base64 data URL or null to remove).
     */
    editEventImage: async (token: string, eventId: string | number, newImageUrl: string | null): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${eventId}/image`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify({ newImageUrl }),
        });
        return response.ok;
    },

    /**
     * PUT /api/events/{eventId}/seating-map
     * Configures the seating and standing areas mapping for an event.
     */
    editSeatingMap: async (token: string, eventId: string | number, data: ConfigureSeatingMapRequestDTO): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${eventId}/seating-map`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return response.ok;
    },

    getEventSeatingMap: async (token: string, eventId: string | number): Promise<SeatingMapDTO | null> => {
        const response = await fetch(`${BASE_URL}/${eventId}/seating-map`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        if (!response.ok) return null;
        return response.json();
    },

    getEventPurchasePolicy: async (token: string, eventId: string | number): Promise<PurchasePolicyDTO | null> => {
        const response = await fetch(`${BASE_URL}/${eventId}/purchase-policy`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        if (!response.ok) return null;
        return response.json();
    },
     validatePurchasePolicy: async (
        token: string,
        eventId: string | number,
        quantity: number,
        userAge: number
    ): Promise<string | null> => {
        console.log('[validatePurchasePolicy] eventId:', eventId, 'quantity:', quantity, 'userAge:', userAge);
        const response = await fetch(`${BASE_URL}/${eventId}/validate-policy`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify({ quantity, userAge }),
        });
        const bodyText = await response.text();
        console.log('[validatePurchasePolicy] status:', response.status, 'body:', bodyText);
        if (response.status === 422) return bodyText;
        if (!response.ok) return 'Failed to validate purchase policy.';
        return null;
    },


    /**
     * GET /api/events/search?q={query}
     * Searches active events by a search term.
     */
    searchEvents: async (query: string): Promise<EventDTO[]> => {
        const response = await fetch(`${BASE_URL}/search?q=${encodeURIComponent(query)}`, { method: 'GET' });
        if (!response.ok) return [];
        return response.json();
    },
};
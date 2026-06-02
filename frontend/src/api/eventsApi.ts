const BASE_URL = '/api/events';

// ==========================================
// TypeScript Interfaces (DTOs)
// ==========================================

export interface EventDTO {
    eventId?: string;
    companyId?: number;
    eventName: string;
    eventCapacity: number;
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
    requiresMembership?: boolean;
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

export interface StandingAreaConfig {
    capacity: number;
    price: number;
}

export interface ConfigureSeatingMapRequestDTO {
    seatingAreas: any[];
    standingAreas: any[];
}

// ==========================================
// Helper: Header Generator
// ==========================================
const getHeaders = (token: string) => ({
    'Content-Type': 'application/json',
    'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}`
});

// Helper: Response parser
const parseResponse = async (response: Response) => {
    if (!response.ok) {
        const err = await response.text();
        throw new Error(err || `HTTP Error: ${response.status}`);
    }
    const text = await response.text();
    return text ? JSON.parse(text) : {};
};

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

    getEvent: async (token: string, eventId: string | number): Promise<EventDTO | null> => {
        console.log(`[API CALL] GET /api/events/${eventId}`);
        try {
            const response = await fetch(`${BASE_URL}/${eventId}`, {
                method: 'GET',
                headers: getHeaders(token),
            });

            const data = await parseResponse(response);
            console.log(`[API RESULT] Data for ${eventId}:`, data);
            return data;
        } catch (error: any) {
            console.error(`[API ERROR] Failed to fetch event ${eventId}:`, error.message);
            return null;
        }
    },

    getEventsByCompany: async (token: string, companyId: number): Promise<EventDTO[]> => {
        console.log(`[API CALL] GET /api/events?companyId=${companyId}`);
        try {
            const response = await fetch(`${BASE_URL}?companyId=${companyId}`, {
                method: 'GET',
                headers: getHeaders(token),
            });

            const data = await parseResponse(response);
            console.log(`[API RESULT] Events fetched for company ${companyId}:`, data);
            return data;
        } catch (error: any) {
            console.error(`[API ERROR] Failed to fetch events for company ${companyId}:`, error.message);
            return [];
        }
    },

    /**
     * PUT /api/events/{eventId}/date
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
    }
};
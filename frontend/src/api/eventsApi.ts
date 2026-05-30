
const BASE_URL = '/api/events';

// ==========================================
// TypeScript Interfaces (DTOs)
// ==========================================

export interface EventDTO {
    eventId?: string | number;
    companyId?: number;
    eventName: string;
    eventCapacity?: number;
    eventDateTime?: string; // ISO 8601 string recommended (e.g., "2026-10-24T21:00:00")
    location?: string;
    isPublished?: boolean;
    // Add any other fields your Java EventDTO contains
}

export interface PurchasePolicyDTO {
    minTicketsPerUser: number;
    maxTicketsPerUser: number;
    minAge?: number;
    maxAge?: number;
    requiresMembership?: boolean;
    // Add any other fields your Java PurchasePolicyDTO contains
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
    createEvent: async (token: string, data: CreateEventRequestDTO): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return response.ok; // Returns true if status is 201 Created
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

    /**
     * GET /api/events/{eventId}/purchase-policy
     * Retrieves the purchase policy for a specific event.
     */
    getEventPurchasePolicy: async (token: string, eventId: string | number): Promise<PurchasePolicyDTO | null> => {
        const response = await fetch(`${BASE_URL}/${eventId}/purchase-policy`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        if (!response.ok) return null;
        return response.json();
    }
};
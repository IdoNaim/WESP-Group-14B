const BASE_URL = '/api/events';

// ==========================================
// TypeScript Interfaces (DTOs)
// ==========================================

export interface EventDTO {
    id?: string | number;
    title: string;
    description?: string;
    capacity: number;
    date: string;
    location?: string;
    isPublished?: boolean;
    // UI Fallbacks (In case your backend doesn't supply these yet)
    price?: number;
    category?: string;
    image?: string;
}

export interface PurchasePolicyDTO {
    minTicketsPerUser: number;
    maxTicketsPerUser: number;
    minAge?: number;
    maxAge?: number;
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

// ==========================================
// Helper: Header Generator
// ==========================================
const getHeaders = (token: string) => ({
    'Content-Type': 'application/json',
    'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}`
});

// Helper: Response parser (NOW PROPERLY USED)
const parseResponse = async (response: Response) => {
    if (!response.ok) {
        const err = await response.text();
        throw new Error(err || `HTTP Error: ${response.status}`);
    }
    // Handle empty 200/201 responses safely
    const text = await response.text();
    return text ? JSON.parse(text) : {};
};

// ==========================================
// API Service Methods
// ==========================================

export const eventApi = {
    createEvent: async (token: string, data: CreateEventRequestDTO): Promise<boolean> => {
        console.log('[API CALL] POST /api/events | Payload:', data);
        const response = await fetch(`${BASE_URL}`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });

        // We can just use parseResponse to ensure errors are thrown if it fails
        await parseResponse(response);
        return true;
    },

    getEvent: async (token: string, eventId: string | number): Promise<EventDTO | null> => {
        console.log(`[API CALL] GET /api/events/${eventId}`);
        try {
            const response = await fetch(`${BASE_URL}/${eventId}`, {
                method: 'GET',
                headers: getHeaders(token),
            });

            // Using the helper here!
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
        const response = await fetch(`${BASE_URL}?companyId=${companyId}`, {
            method: 'GET',
            headers: getHeaders(token),
        });

        // Using the helper here!
        const data = await parseResponse(response);
        console.log(`[API RESULT] Events fetched for company ${companyId}:`, data);
        return data;
    }
};
<<<<<<< HEAD
// Define your base URL
const BASE_URL = '/api/orders';

// ==========================================
// TypeScript Interfaces (DTOs)
// ==========================================

export interface CreateOrderRequestDTO {
    userId: string;
    eventId: string;
}

export interface CreateOrderResponseDTO {
    orderId: string;
    userId: string;
    eventId: string;
}

export interface AddSeatsRequestDTO {
    seatIds: string[];
}

export interface AddStandingAreaRequestDTO {
=======
const BASE_URL = '/api/orders';

export interface ActiveOrderDTO {
  orderId: string;
  userId: string;
  eventId: string;
  createdAt: string;
  seatIds: string[];
  
  StandingAreaQuantities: Record<string, number>; // capital S, plain object not array
}
export interface StandingAreaTicketsDTO{
>>>>>>> aa2ad2b1ccbc0c0ec7f0157ca3d781933a0c5c42
    areaId: string;
    quantity: number;
}

<<<<<<< HEAD
export interface CheckoutRequestDTO {
    amount: number;
    creditCardNumber: string;
    cardHolderName: string;
    expirationDate: string; // Format: "MM/YY"
    cvv: string;
}

export interface CheckoutResponseDTO {
    barcodes: string[];
}

// You can flesh this out further based on your backend domain models
export interface ActiveOrderDTO {
    orderId: string;
    userId: string;
    eventId: string;
    seatIds?: string[]; 
    StandingAreaQuantities?: Record<string, number>;
}

// ==========================================
// Helper: Header Generator
// ==========================================
const getHeaders = (token?: string) => {
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
    };
    if (token) {
        headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    }
    return headers;
};

// Helper: Response parser
const parseResponse = async (response: Response) => {
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.error || 'An unexpected error occurred');
    }
    return data;
};

// ==========================================
// API Service Methods
// ==========================================

export const activeOrderApi = {
    /**
     * POST /api/orders
     * Creates a new pending order for the authenticated user.
     */
    createOrder: async (token: string, data: CreateOrderRequestDTO): Promise<CreateOrderResponseDTO> => {
        const response = await fetch(`${BASE_URL}`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return parseResponse(response);
    },

    getActiveOrderByUserId: async (token: string, userId: string): Promise<ActiveOrderDTO> => {
=======
const getHeaders = (token: string) => ({
    'Content-Type': 'application/json',
    'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}`
});

export const activeOrderApi = {
    getActiveOrderByUserId: async (token : string, userId: string): Promise<ActiveOrderDTO | null> => {
>>>>>>> aa2ad2b1ccbc0c0ec7f0157ca3d781933a0c5c42
        const response = await fetch(`${BASE_URL}/user/${userId}`, {
            method: 'GET',
            headers: getHeaders(token),
        });
<<<<<<< HEAD
        return parseResponse(response);
    },
    /**
     * GET /api/orders/{orderId}
     * Returns the active order details for the authenticated user.
     */
    getActiveOrder: async (token: string, orderId: string): Promise<ActiveOrderDTO> => {
        const response = await fetch(`${BASE_URL}/${orderId}`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        return parseResponse(response);
    },

    /**
     * DELETE /api/orders/{orderId}
     * Cancels the active order and releases all reserved tickets.
     */
    cancelOrder: async (token: string, orderId: string, userId: string): Promise<{ message: string }> => {
        const response = await fetch(`${BASE_URL}/${orderId}`, {
            method: 'DELETE',
            headers: getHeaders(token),
            body: JSON.stringify({ userId }),
        });
        return parseResponse(response);
    },

    /**
     * POST /api/orders/{orderId}/seats
     * Reserves additional seats and adds them to the active order.
     */
    addSeats: async (token: string, orderId: string, data: AddSeatsRequestDTO): Promise<{ message: string }> => {
        const response = await fetch(`${BASE_URL}/${orderId}/seats`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return parseResponse(response);
    },

    /**
     * POST /api/orders/{orderId}/standing
     * Reserves standing-area tickets and adds them to the active order.
     */
    addStandingArea: async (token: string, orderId: string, data: AddStandingAreaRequestDTO): Promise<{ message: string }> => {
        const response = await fetch(`${BASE_URL}/${orderId}/standing`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return parseResponse(response);
    },

    /**
     * PUT /api/orders/{orderId}
     * Replaces the full ticket selection in the order.
     */
    updateOrder: async (token: string, orderId: string, data: ActiveOrderDTO): Promise<{ message: string }> => {
        const response = await fetch(`${BASE_URL}/${orderId}`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return parseResponse(response);
    },

    /**
     * POST /api/orders/{orderId}/checkout
     * Completes the order: checks purchase policy, charges payment, issues barcodes.
     */
    checkout: async (token: string, orderId: string, data: CheckoutRequestDTO): Promise<CheckoutResponseDTO> => {
        const response = await fetch(`${BASE_URL}/${orderId}/checkout`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return parseResponse(response);
=======
        if (!response.ok) return null;
        return response.json();
    },
    updateActiveOrder: async (token: string, orderId: string, order: ActiveOrderDTO): Promise<boolean> => {
        const response = await fetch(`${BASE_URL}/${orderId}`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify(order),
        });
        return response.ok;
>>>>>>> aa2ad2b1ccbc0c0ec7f0157ca3d781933a0c5c42
    }
};
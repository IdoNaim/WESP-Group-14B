const BASE_URL = "/api/admin";

const getHeaders = (token?: string) => {
    const headers: HeadersInit = {
        "Content-Type": "application/json",
    };

    if (token) {
        headers["Authorization"] = token.startsWith("Bearer ")
            ? token
            : `Bearer ${token}`;
    }

    return headers;
};

const parseResponse = async (response: Response) => {
    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        const errorMessage = (data as any).error || (data as any).message || "An error occurred while processing the request.";
        throw new Error(errorMessage);
    }

    return data;
}

export type SystemUserDTO = {
  id?: string;
  userId?: string;
  username?: string;
  name?: string;
  email?: string;
  userState?: string;
  groupDiscount?: string;
  [key: string]: unknown;
};

export type SystemActiveOrderDTO = {
  orderId?: string;
  userId?: string;
  eventId?: string;
  companyId?: number;
  price?: number;
  [key: string]: unknown;
};

export type SystemHistoryOrderDTO = {
  orderId?: string;
  userId?: string;
  eventId?: string;
  companyId?: number;
  purchaseDate?: string;
  price?: number;
  seatIds?: string[];
  standingAreaQuantities?: Record<string, number>;
  [key: string]: unknown;
};

export const adminApi = {
    getActiveOrders: async (token: string): Promise<SystemActiveOrderDTO[]> => {
        const response = await fetch(`${BASE_URL}/active-orders`, {
            method: "GET",
            headers: getHeaders(token),
        });

        const data = await parseResponse(response);
        if (!Array.isArray(data)) throw new Error("Active orders response is not an array.");
        return data;
    },

    getOrderHistory: async (token: string): Promise<SystemHistoryOrderDTO[]> => {
        const response = await fetch(`${BASE_URL}/history-orders`, {
            method: "GET",
            headers: getHeaders(token),
        });

        const data = await parseResponse(response);
        if (!Array.isArray(data)) throw new Error("History orders response is not an array.");
        return data;
    },

    getSystemUsers: async (token: string): Promise<SystemUserDTO[]> => {
        const response = await fetch(`${BASE_URL}/users`, {
            method: "GET",
            headers: getHeaders(token),
        });

        const data = await parseResponse(response);
        if (!Array.isArray(data)) throw new Error("Users response is not an array.");
        return data;
    },
};


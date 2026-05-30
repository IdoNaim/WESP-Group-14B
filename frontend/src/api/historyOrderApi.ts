// מעודכן לפי ה-Controller האמיתי של הפרויקט
const BASE_URL = '/api/history';

export interface HistoryOrderDTO {
    orderId: string;
    userId: string;
    eventId: string;
    companyId: number;
    purchaseDate: string; 
    price: number;
    seatIds: string[];
    standingAreaQuantities: Record<string, number>;
}

const getHeaders = (token?: string) => {
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
    };
    if (token) {
        headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    }
    return headers;
};

const parseResponse = async (response: Response) => {
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw { status: response.status, message: data.error || 'An unexpected error occurred' };
    }
    return data;
};

export const historyOrderApi = {
    
    // GET /api/history?userId={userId}
    getUserOrders: async (token: string, userId: string): Promise<HistoryOrderDTO[]> => {
        const response = await fetch(`${BASE_URL}?userId=${encodeURIComponent(userId)}`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        return parseResponse(response);
    },

    // GET /api/history/{orderId}
    getOrderById: async (token: string, orderId: string): Promise<HistoryOrderDTO> => {
        const response = await fetch(`${BASE_URL}/${encodeURIComponent(orderId)}`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        return parseResponse(response);
    },

    // GET /api/history?companyId={companyId}
    getOrdersByCompany: async (token: string, companyId: number): Promise<HistoryOrderDTO[]> => {
        const response = await fetch(`${BASE_URL}?companyId=${companyId}`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        return parseResponse(response);
    },

    // GET /api/history (Admin Only)
    getAllOrders: async (token: string): Promise<HistoryOrderDTO[]> => {
        const response = await fetch(`${BASE_URL}`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        return parseResponse(response);
    }
};
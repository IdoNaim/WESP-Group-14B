const HISTORY_BASE_URL = '/api/history';

export interface HistoryOrderDTO {
    orderId: string;
    userId: string;
    eventId: string;
    companyId: number;
    purchaseDate: string; 
    price: number;
    seatIds: string[];
    standingAreaQuantities: Record<string, number>;
    
    // שדות מורחבים לעיצוב העשיר
    eventName?: string;
    eventLocation?: string;
    eventImageUrl?: string;
    category?: string;
    eventDate?: string;
}

const getHeaders = (token?: string) => {
    const headers: HeadersInit = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
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
    
    // שליפת הזמנות של משתמש ספציפי
    getUserOrders: async (token: string, userId: string): Promise<HistoryOrderDTO[]> => {
        const response = await fetch(`${HISTORY_BASE_URL}?userId=${encodeURIComponent(userId)}`, {
            method: 'GET', 
            headers: getHeaders(token),
        });
        const data = await parseResponse(response);
        return mapStandingAreas(data);
    },

    // 🔴 התיקון: שליפת הזמנות של חברה ספציפית דרך HistoryOrderController
    getOrdersByCompany: async (token: string, companyId: number): Promise<HistoryOrderDTO[]> => {
        const response = await fetch(`${HISTORY_BASE_URL}?companyId=${companyId}`, {
            method: 'GET', 
            headers: getHeaders(token),
        });
        const data = await parseResponse(response);
        return mapStandingAreas(data);
    },

    // שליפת כל ההזמנות במערכת (אדמין בלבד)
    getAllOrders: async (token: string): Promise<HistoryOrderDTO[]> => {
        const response = await fetch(`${HISTORY_BASE_URL}`, {
            method: 'GET', 
            headers: getHeaders(token),
        });
        const data = await parseResponse(response);
        return mapStandingAreas(data);
    }
};

// פונקציית עזר למיפוי במקרה שהשרת מחזיר את המפתח עם אות גדולה (StandingAreaQuantities)
const mapStandingAreas = (data: any[]) => {
    if (!Array.isArray(data)) return [];
    return data.map((item: any) => ({
        ...item,
        standingAreaQuantities: item.standingAreaQuantities || item.StandingAreaQuantities || {}
    }));
};
// מעודכן לפי ה-Controller האמיתי של הפרויקט
const HISTORY_BASE_URL = '/api/history';
const PRODUCTION_BASE_URL = '/api/production';

export interface HistoryOrderDTO {
    orderId: string;
    userId: string;
    eventId: string;
    companyId: number;
    purchaseDate: string; 
    price: number;
    seatIds: string[];
    standingAreaQuantities: Record<string, number>;
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
    // של המשתמש (HistoryOrderController)
    getUserOrders: async (token: string, userId: string): Promise<HistoryOrderDTO[]> => {
        const response = await fetch(`${HISTORY_BASE_URL}?userId=${encodeURIComponent(userId)}`, {
            method: 'GET', headers: getHeaders(token),
        });
        return parseResponse(response);
    },

    // של כל המערכת - אדמין בלבד (HistoryOrderController)
    getAllOrders: async (token: string): Promise<HistoryOrderDTO[]> => {
        const response = await fetch(`${HISTORY_BASE_URL}`, {
            method: 'GET', headers: getHeaders(token),
        });
        return parseResponse(response);
    },

    // 🔴 עודכן: של חברה ספציפית מתוך ה- ProductionController
    getOrdersByCompany: async (token: string, companyId: number): Promise<HistoryOrderDTO[]> => {
        const response = await fetch(`${PRODUCTION_BASE_URL}/companies/${companyId}/history`, {
            method: 'GET', headers: getHeaders(token),
        });
        
        const data = await parseResponse(response);
        // מבצעים מיפוי למקרה שה-Backend מחזיר StandingAreaQuantities עם S גדולה
        return data.map((item: any) => ({
            ...item,
            standingAreaQuantities: item.standingAreaQuantities || item.StandingAreaQuantities || {}
        }));
    }
};

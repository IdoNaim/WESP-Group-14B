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
    areaId: string;
    quantity: number;
}

const getHeaders = (token: string) => ({
    'Content-Type': 'application/json',
    'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}`
});

export const activeOrderApi = {
    getActiveOrderByUserId: async (token : string, userId: string): Promise<ActiveOrderDTO | null> => {
        const response = await fetch(`${BASE_URL}/user/${userId}`, {
            method: 'GET',
            headers: getHeaders(token),
        });
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
    }
};

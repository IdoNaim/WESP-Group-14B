const BASE_URL = '/api/orders';

export interface ActiveOrderDTO {
    orderId: string;
    userId: string;
    eventId: string;
    createdAt: string; // ISO 8601 format
    seatIds : string[]; // List of seat IDs associated with the order
    standingAreaQuantities: StandingAreaTicketsDTO[]; // List of standing area quantities
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
};
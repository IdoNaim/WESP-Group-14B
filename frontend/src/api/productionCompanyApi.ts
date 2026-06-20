const BASE_URL = '/api/production';

function getToken(): string {
    return localStorage.getItem('token') || '';
}

export interface CompanySummary {
    companyId: number;
    companyName: string;
    companyDescription: string;
    companyEmail: string;
    role: 'FOUNDER' | 'OWNER' | 'MANAGER';
}

export interface OwnerDTO {
    userId: string;
    appointerId: string | null;
}

export interface ManagerDTO {
    userId: string;
    appointerId: string;
    permissions: ManagerPermission[];
}

export interface RolesTreeDTO {
    companyId: number;
    companyName: string;
    founderId: string;
    ownershipTree: Record<string, OwnerDTO>;
    managerTree: Record<string, ManagerDTO>;
    managerPermissions: Record<string, ManagerPermission[]>;
}

export interface HistoryOrderItem {
    orderId: string;
    userId: string;
    eventId: string;
    companyId: number;
    purchaseDate: string;
    price: number;
    seatIds: string[];
    standingAreaQuantities: Record<string, number>;
}

export type ManagerPermission =
    | 'INVENTORY_MANAGEMENT'
    | 'VENUE_CONFIGURATION_AND_EVENT_MAPPING'
    | 'COMPANY_POLICY_MANAGEMENT'
    | 'PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT'
    | 'CUSTOMER_INQUIRY_AND_RESPONSE_MANAGEMENT'
    | 'PURCHASE_AND_ORDER_HISTORY_ACCESS'
    | 'SALES_REPORT_GENERATION';

export const ALL_PERMISSIONS: ManagerPermission[] = [
    'INVENTORY_MANAGEMENT',
    'VENUE_CONFIGURATION_AND_EVENT_MAPPING',
    'COMPANY_POLICY_MANAGEMENT',
    'PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT',
    'CUSTOMER_INQUIRY_AND_RESPONSE_MANAGEMENT',
    'PURCHASE_AND_ORDER_HISTORY_ACCESS',
    'SALES_REPORT_GENERATION',
];

function cleanErrorMessage(raw: string | undefined, status: number): string {
    if (raw) {
        const cleaned = raw.replace(/(?:[\w]+\.)+\w+(?:Exception|Error):\s*/g, '').trim();
        if (cleaned.length > 0) return cleaned;
    }
    switch (status) {
        case 400: return 'The request could not be completed. Please check the information and try again.';
        case 401: return 'Your session has expired. Please log in again.';
        case 403: return 'You do not have permission to perform this action.';
        case 404: return 'The requested resource was not found.';
        case 409: return 'This action conflicts with the current state. Please refresh and try again.';
        default:  return 'An unexpected error occurred. Please try again.';
    }
}

async function apiRequest<T>(method: string, path: string, body?: unknown): Promise<T> {
    const res = await fetch(`${BASE_URL}${path}`, {
        method,
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${getToken()}`,
        },
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (!res.ok) {
        const err = await res.json().catch(() => null);
        throw new Error(cleanErrorMessage(err?.error || err?.message, res.status));
    }
    return res.json();
}

export const getRolesTree = (companyId: number): Promise<RolesTreeDTO> =>
    apiRequest('GET', `/companies/${companyId}/roles`);

export const getPurchaseHistory = (companyId: number): Promise<HistoryOrderItem[]> =>
    apiRequest('GET', `/companies/${companyId}/history`);

export const assignOwner = (companyId: number, appointeeUserId: string): Promise<{ message: string }> =>
    apiRequest('POST', `/companies/${companyId}/owners`, { appointeeUserId });

export const appointManager = (
    companyId: number,
    managerId: string,
    permissions: ManagerPermission[]
): Promise<{ message: string }> =>
    apiRequest('POST', `/companies/${companyId}/managers`, { managerId, permissions });

export const modifyManagerPermissions = (
    companyId: number,
    managerId: string,
    permissions: ManagerPermission[]
): Promise<{ message: string }> =>
    apiRequest('PUT', `/companies/${companyId}/managers/${managerId}/permissions`, { permissions });

export const removeManager = (companyId: number, managerId: string): Promise<{ message: string }> =>
    apiRequest('DELETE', `/companies/${companyId}/managers/${managerId}`);

export const removeOwner = (companyId: number, ownerId: string): Promise<{ message: string }> =>
    apiRequest('DELETE', `/companies/${companyId}/owners/${ownerId}`);

export const getMyCompanies = (): Promise<CompanySummary[]> =>
    apiRequest('GET', '/companies/my');

export const createCompany = (companyName: string, companyDescription: string, companyEmail: string): Promise<{ message: string; companyId: string }> =>
    apiRequest('POST', '/companies', { companyName, companyDescription, companyEmail });

export interface MemberInfo {
    role: 'FOUNDER' | 'OWNER' | 'MANAGER';
    permissions: ManagerPermission[];
    companyName: string;
    founderId: string;
    ownershipTree: Record<string, OwnerDTO>;
    managerTree: Record<string, ManagerDTO>;
    managerPermissions: Record<string, ManagerPermission[]>;
}

export const getMyMemberInfo = (companyId: number): Promise<MemberInfo> =>
    apiRequest('GET', `/companies/${companyId}/my-role`);

export interface PendingAppointment {
    companyId: number;
    companyName: string;
    role: 'OWNER' | 'MANAGER';
    appointerId: string;
    permissions: ManagerPermission[];
}

export const getPendingAppointments = (): Promise<PendingAppointment[]> =>
    apiRequest('GET', '/appointments/pending');

export const acceptAppointment = (companyId: number): Promise<{ message: string }> =>
    apiRequest('POST', `/appointments/${companyId}/accept`);

export const denyAppointment = (companyId: number): Promise<{ message: string }> =>
    apiRequest('POST', `/appointments/${companyId}/deny`);

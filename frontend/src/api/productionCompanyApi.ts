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
        const err = await res.json().catch(() => ({ error: 'Request failed' }));
        throw new Error(err.error || `Request failed (${res.status})`);
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

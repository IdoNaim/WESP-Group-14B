const API_BASE = 'http://localhost:8080/api/production';

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

export const PERMISSION_LABELS: Record<ManagerPermission, string> = {
  INVENTORY_MANAGEMENT: 'Inventory',
  VENUE_CONFIGURATION_AND_EVENT_MAPPING: 'Venue Config',
  COMPANY_POLICY_MANAGEMENT: 'Company Policy',
  PURCHASING_AND_DISCOUNT_POLICY_MANAGEMENT: 'Purchase Policy',
  CUSTOMER_INQUIRY_AND_RESPONSE_MANAGEMENT: 'Customer Support',
  PURCHASE_AND_ORDER_HISTORY_ACCESS: 'Order History',
  SALES_REPORT_GENERATION: 'Sales Reports',
};

export interface OwnerDTO {
  userId: string;
  appointerId: string | null;
}

export interface ManagerDTO {
  userId: string;
  appointerId: string;
}

export interface RolesTreeDTO {
  companyId: number;
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

function authHeaders(token: string): HeadersInit {
  return {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
}

async function handleResponse<T>(res: Response): Promise<T> {
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error((data as Record<string, string>).error ?? `Request failed (${res.status})`);
  return data as T;
}

export async function createProductionCompany(
  token: string,
  companyName: string,
  companyDescription: string,
  companyEmail: string
): Promise<{ message: string; companyId: string }> {
  const res = await fetch(`${API_BASE}/companies`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({ companyName, companyDescription, companyEmail }),
  });
  return handleResponse(res);
}

export async function assignOwner(
  token: string,
  companyId: number,
  appointeeUserId: string
): Promise<{ message: string }> {
  const res = await fetch(`${API_BASE}/companies/${companyId}/owners`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({ appointeeUserId }),
  });
  return handleResponse(res);
}

export async function appointManager(
  token: string,
  companyId: number,
  managerId: string,
  permissions: ManagerPermission[]
): Promise<{ message: string }> {
  const res = await fetch(`${API_BASE}/companies/${companyId}/managers`, {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify({ managerId, permissions }),
  });
  return handleResponse(res);
}

export async function modifyManagerPermissions(
  token: string,
  companyId: number,
  managerId: string,
  permissions: ManagerPermission[]
): Promise<{ message: string }> {
  const res = await fetch(`${API_BASE}/companies/${companyId}/managers/${managerId}/permissions`, {
    method: 'PUT',
    headers: authHeaders(token),
    body: JSON.stringify({ permissions }),
  });
  return handleResponse(res);
}

export async function removeManager(
  token: string,
  companyId: number,
  managerId: string
): Promise<{ message: string }> {
  const res = await fetch(`${API_BASE}/companies/${companyId}/managers/${managerId}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });
  return handleResponse(res);
}

export async function getRolesTree(token: string, companyId: number): Promise<RolesTreeDTO> {
  const res = await fetch(`${API_BASE}/companies/${companyId}/roles`, {
    headers: authHeaders(token),
  });
  return handleResponse(res);
}

export async function getCompanyPurchaseHistory(
  token: string,
  companyId: number
): Promise<HistoryOrderItem[]> {
  const res = await fetch(`${API_BASE}/companies/${companyId}/history`, {
    headers: authHeaders(token),
  });
  return handleResponse(res);
}

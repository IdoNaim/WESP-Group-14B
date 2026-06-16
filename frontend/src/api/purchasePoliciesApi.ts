import type { PurchasePolicyDTO } from './eventsApi';

const BASE_URL = '/api/policies';
const PROD_URL = '/api/production';

// ── New DTO-based company policy endpoints (DB-backed) ────────────────────────

export async function getCompanyPolicyDTO(companyId: number): Promise<PurchasePolicyDTO | null> {
    const token = localStorage.getItem('token') || '';
    const res = await fetch(`${PROD_URL}/companies/${companyId}/purchase-policy`, {
        headers: { Authorization: `Bearer ${token}` },
    });
    if (res.status === 403 || res.status === 404) return null;
    if (!res.ok) throw new Error(`Failed to fetch company policy (${res.status})`);
    return res.json();
}

export async function setCompanyPolicyDTO(companyId: number, dto: PurchasePolicyDTO): Promise<void> {
    const token = localStorage.getItem('token') || '';
    const res = await fetch(`${PROD_URL}/companies/${companyId}/purchase-policy`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(dto),
    });
    if (!res.ok) {
        const msg = await res.text();
        throw new Error(msg || `Failed to save company policy (${res.status})`);
    }
}

// ─────────────────────────────────────────────────────────────────────────────

export interface PolicyResponse {
    description: string;
}

export interface PolicyFormData {
    minTickets?: number | null;
    maxTickets?: number | null;
    minAge?: number | null;
    maxAge?: number | null;
    composition?: 'AND' | 'OR';
}

interface PolicyRequest {
    type?: string;
    minAge?: number | null;
    maxAge?: number | null;
    minTickets?: number | null;
    maxTickets?: number | null;
    subPolicies?: PolicyRequest[];
}

function buildPolicyRequest(form: PolicyFormData): PolicyRequest {
    const subs: PolicyRequest[] = [];
    if (form.minTickets != null && form.minTickets > 0) {
        subs.push({ type: 'MIN_TICKETS', minTickets: form.minTickets });
    }
    if (form.maxTickets != null && form.maxTickets > 0) {
        subs.push({ type: 'MAX_TICKETS', maxTickets: form.maxTickets });
    }
    if (form.minAge != null || form.maxAge != null) {
        subs.push({ type: 'AGE', minAge: form.minAge ?? null, maxAge: form.maxAge ?? null });
    }
    if (subs.length === 0) return {};
    if (subs.length === 1) return subs[0];
    return { type: form.composition ?? 'AND', subPolicies: subs };
}

export async function getCompanyPolicy(companyId: number): Promise<PolicyResponse | null> {
    const token = localStorage.getItem('token') || '';
    const res = await fetch(`${BASE_URL}/company/${companyId}`, {
        headers: { Authorization: `Bearer ${token}` },
    });
    if (res.status === 404) return null;
    if (!res.ok) throw new Error(`Failed to fetch company policy (${res.status})`);
    return res.json();
}

export async function assignCompanyPolicy(companyId: number, form: PolicyFormData): Promise<void> {
    const token = localStorage.getItem('token') || '';
    const request = buildPolicyRequest(form);
    const res = await fetch(`${BASE_URL}/assign/company/${companyId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(request),
    });
    if (!res.ok) {
        const msg = await res.text();
        throw new Error(msg || `Failed to assign company policy (${res.status})`);
    }
}

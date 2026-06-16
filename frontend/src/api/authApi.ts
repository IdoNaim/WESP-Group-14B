import { getUserFriendlyError } from '../utils/errorUtils';

// Define your base URL
const BASE_URL = '/api/identity';

// ==========================================
// TypeScript Interfaces (DTOs)
// ==========================================

export type UserGroupDiscount = 'NONE' | 'STUDENT' | 'SOLDIER' | 'PENSIONER' | string;

export interface RegisterRequestDTO {
    userId: string;
    name: string;
    password?: string;
    email: string;
    userGroupDiscount?: UserGroupDiscount;
}

export interface LoginRequestDTO {
    userId: string;
    password?: string;
}

export interface ProfileUpdateRequestDTO {
    name?: string;
    email?: string;
    userGroupDiscount?: UserGroupDiscount;
}

export interface UserProfileDTO {
    userId: string;
    name: string;
    email: string;
    userGroupDiscount: UserGroupDiscount;
}

export interface UserPermissionsDTO {
    userId: string;
    state: string; // e.g., "GUEST", "REGISTERED"
    isAdmin: boolean;
    productionRoles: Record<number, string>; // Maps companyId to role (e.g., { 1: "OWNER", 2: "MANAGER" })
}

export interface AuthResponse {
    token?: string;
    userId?: string;
    message?: string;
    error?: string;
}
export interface PasswordUpdateRequestDTO{
    currentPassword : string;
    newPassword : string;
}
// ==========================================
// Helper: Header Generator
// ==========================================
const getHeaders = (token?: string) => {
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
    };
    if (token) {
        headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    }
    return headers;
};

// Helper: Response parser
const parseResponse = async (response: Response) => {
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(getUserFriendlyError(data) || 'An unexpected error occurred');
    }
    return data;
};

// ==========================================
// API Service Methods
// ==========================================

export const authApi = {
    /**
     * POST /api/identity/guest
     * Obtains a guest session token. Must be called before register/login.
     */
    guestEntry: async (): Promise<{ token: string }> => {
        const response = await fetch(`${BASE_URL}/guest`, {
            method: 'POST',
            headers: getHeaders(),
        });
        return parseResponse(response);
    },

    /**
     * POST /api/identity/register
     * Registers a new user. Requires a valid guest token in the header.
     */
    register: async (token: string, data: any) => {
        console.log('[API CALL] Registering user via POST /register payload:', data);
        const response = await fetch(`${BASE_URL}/register`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
        return parseResponse(response);
    },

    /**
     * POST /api/identity/login
     * Logs in a user. Requires the current token (guest or existing session) in the header.
     */
    login: async (token: string, data: LoginRequestDTO): Promise<{ token: string; userId: string }> => {
    let response: Response; 
    
    if (data.userId === "admin@gmail.com") {
        response = await fetch(`${BASE_URL}/admin/login`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
    } else {
        response = await fetch(`${BASE_URL}/login`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });
    }
    return parseResponse(response);
},

    /**
     * POST /api/identity/logout
     * Logs out the current user.
     */
    logout: async (token: string, userId: string): Promise<{ message: string }> => {
        const response = await fetch(`${BASE_URL}/logout`, {
            method: 'POST',
            headers: getHeaders(token),
            body: JSON.stringify({ userId }),
        });
        return parseResponse(response);
    },

    /**
     * POST /api/identity/exit
     * Fully exits the platform, removing the guest session or logging out the user.
     */
    exit: async (token: string): Promise<{ message: string }> => {
        const response = await fetch(`${BASE_URL}/exit`, {
            method: 'POST',
            headers: getHeaders(token),
        });
        return parseResponse(response);
    },

    /**
     * GET /api/identity/me
     * Retrieves the current user's profile details.
     */
    getCurrentUser: async (token: string): Promise<UserProfileDTO> => {
        const response = await fetch(`${BASE_URL}/me`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        return parseResponse(response);
    },

    /**
     * PUT /api/identity/profile
     * Updates the current user's profile details securely.
     */
    updateProfile: async (token: string, data: ProfileUpdateRequestDTO): Promise<{ message: string }> => {
        const response = await fetch(`${BASE_URL}/profile`, {
            method: 'PUT',
            headers: getHeaders(token),
            body: JSON.stringify(data),
        });

        return parseResponse(response);
    },

    /**
     * GET /api/identity/permissions
     * Retrieves the current user's state, admin status, and production company roles.
     */
    getPermissions: async (token: string): Promise<UserPermissionsDTO> => {
        const response = await fetch(`${BASE_URL}/permissions`, {
            method: 'GET',
            headers: getHeaders(token),
        });
        
        // Await and store the parsed data first
        const permissions: UserPermissionsDTO = await parseResponse(response);
        
        // Print out if isAdmin is true or false
        console.log(`[Permissions API] User is admin: ${permissions.isAdmin}`);
        
        // Return the data so the rest of the app can still use it
        return permissions;
    },

    editPassword: async(token: string, data: PasswordUpdateRequestDTO) : Promise<{message : string}> => {
        const response = await fetch(`${BASE_URL}/editPassword`, {
            method: 'PUT',
            headers: getHeaders(token),
            body : JSON.stringify(data),
        });
        return parseResponse(response);
    }
};

import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
} from 'react';
import { authApi, type UserPermissionsDTO } from '../api/authApi';
import { connectPresence, disconnectPresence } from '../api/presenceSocket';

const TOKEN_KEY = "token";
const USER_ID_KEY = "userId";

type AuthContextValue = {
    token: string | null;
    permissions: UserPermissionsDTO | null;
    loading: boolean;
    
    isGuest: boolean;
    isMember: boolean;
    isProductionUser: boolean;
    isAdmin: boolean;

    refreshPermissions: () => Promise<void>;
    loginWithToken: (token: string, userId: string) => Promise<void>;
    ensureGuestToken: () => Promise<string>;
    logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue|undefined>(undefined);

function hasProductionRole(permissions: UserPermissionsDTO | null): boolean {
    return Object.keys(permissions?.productionRoles ?? {}).length > 0;
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [token, setToken] = useState<string | null>(() => 
        localStorage.getItem(TOKEN_KEY)
    );

    const [permissions, setPermissions] = useState<UserPermissionsDTO | null>(null);

    const [loading, setLoading] = useState(true);

    const ensureGuestToken = useCallback(async (): Promise<string> => {
        const response = await authApi.guestEntry();

        localStorage.setItem(TOKEN_KEY, response.token);
        localStorage.removeItem(USER_ID_KEY); // Clear any existing userId since we're now a guest

        // Becoming a guest: no presence channel (guests are handled by the sweep).
        disconnectPresence();
        setToken(response.token);
        return response.token;
    }, []);

    const refreshPermissions = useCallback(async () => {
        setLoading(true);
        try {
            const existingToken = localStorage.getItem(TOKEN_KEY);

            if (existingToken) {
                try {
                    const currentPermissions = await authApi.getPermissions(existingToken);
                    setPermissions(currentPermissions);

                    if (currentPermissions.userId) {
                        localStorage.setItem(USER_ID_KEY, currentPermissions.userId);
                    }
                    // Reopen the presence channel after a reload if still a member.
                    if (currentPermissions.state?.toUpperCase() !== 'GUEST') {
                        connectPresence(existingToken);
                    }
                    return;
                } catch {
                // Existing token is invalid/expired.
                // Fall through and create a new guest session.
                }
            }
            const guestToken = await ensureGuestToken();
            const guestPermissions = await authApi.getPermissions(guestToken);
            setPermissions(guestPermissions);

            if (guestPermissions.userId) {
                localStorage.setItem(USER_ID_KEY, guestPermissions.userId);
            }
        } finally {
            setLoading(false);
        }
    }, [ensureGuestToken]);

    const loginWithToken = useCallback(async (newToken: string, userId: string) => {
        localStorage.setItem(TOKEN_KEY, newToken);

        if (userId) {
            localStorage.setItem(USER_ID_KEY, userId);
        }

        setToken(newToken);

        // Open the presence channel so an irregular exit (browser X) is detected.
        connectPresence(newToken);

        const permissionsResponse = await authApi.getPermissions(newToken);
        setPermissions(permissionsResponse);

        if (permissionsResponse.userId) {
            localStorage.setItem(USER_ID_KEY, permissionsResponse.userId);
        }
    }, []
    );

    const logout = useCallback(async () => {
        const currentToken = localStorage.getItem(TOKEN_KEY);
        const userId = permissions?.userId ?? localStorage.getItem(USER_ID_KEY);

        if (!currentToken || !userId) {
            throw new Error("No valid session token found or no user ID found cant logout.");
        }

        await authApi.logout(currentToken, userId);

        const guestToken = await ensureGuestToken();
        const guestPermissions = await authApi.getPermissions(guestToken);
        setPermissions(guestPermissions);

        if (guestPermissions.userId) {
            localStorage.setItem(USER_ID_KEY, guestPermissions.userId);
        }
    }, [ensureGuestToken, permissions]);

    useEffect(() => {
        refreshPermissions();
    }, [refreshPermissions]);

    const value = useMemo<AuthContextValue>(() => {
        const normalizedState = permissions?.state?.toUpperCase();
        const isGuest = !permissions || normalizedState === 'GUEST';
        const isMember = !!permissions && !isGuest;
        const isProductionUser = isMember && hasProductionRole(permissions);
        const isAdmin = permissions?.isAdmin === true;

        return {
            token,
            permissions,
            loading,

            isGuest,
            isMember,
            isProductionUser,
            isAdmin,

            refreshPermissions,
            loginWithToken,
            ensureGuestToken,
            logout,
        };
    }, [token, permissions, loading, refreshPermissions, loginWithToken, ensureGuestToken, logout]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}
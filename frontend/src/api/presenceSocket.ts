// Presence WebSocket: kept open while a user is logged in. Its sole purpose is
// liveness — when the tab closes the socket drops and the server detects the
// irregular exit (see PresenceWebSocketHandler on the backend). We never send or
// read messages; the connection itself is the signal.

let socket: WebSocket | null = null;

function buildUrl(token: string): string {
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    // Same host as the page; Vite proxies /ws to the backend in dev.
    return `${proto}://${window.location.host}/ws/presence?token=${encodeURIComponent(token)}`;
}

/** Opens (or replaces) the presence connection for the given session token. */
export function connectPresence(token: string): void {
    if (!token) return;
    disconnectPresence();
    try {
        socket = new WebSocket(buildUrl(token));
    } catch {
        // Presence is best-effort; the scheduled sweep is the backstop.
        socket = null;
    }
}

/** Closes the presence connection on a clean logout/exit. */
export function disconnectPresence(): void {
    if (socket) {
        try {
            socket.close();
        } catch {
            // ignore
        }
        socket = null;
    }
}

import { useState, FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';

// Central configuration for landing/dashboard path
const DEFAULT_REDIRECT_PATH = '/dashboard';

export default function LoginPage() {
    const navigate = useNavigate();
    const { isMember, loading, ensureGuestToken, loginWithToken } = useAuth();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [rememberMe, setRememberMe] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    // Wait for the session to be resolved before deciding what to render.
    // Without this guard, the form flashes briefly before the redirect fires.
    if (loading) {
        return null;
    }

    // A logged-in member (user or admin) should never see the login form again.
    // We use replace so the /login entry is removed from the browser history stack —
    // pressing Back will not return to this page.
    if (isMember) {
        return <Navigate to={DEFAULT_REDIRECT_PATH} replace />;
    }

    const handleLoginSubmit = async (event: FormEvent) => {
        event.preventDefault();
        setIsLoading(true);
        setErrorMessage(null);

        try {
            // The backend login endpoint requires a guest token as the caller identity.
            // ensureGuestToken() creates a fresh guest session and replaces any existing
            // guest token in context. Guests upgrading to a real account intentionally
            // discard their old guest session here.
            const guestToken = await ensureGuestToken();

            const result = await authApi.login(guestToken, {
                userId: email,
                password: password,
            });

            // Route all token persistence through loginWithToken so that context
            // state and localStorage stay in sync in a single call.
            await loginWithToken(result.token, result.userId);

            setEmail('');
            setPassword('');

            // replace: true removes /login from the history stack so the Back button
            // does not return the user to the login form after a successful sign-in.
            navigate(DEFAULT_REDIRECT_PATH, { replace: true });

        } catch (error: any) {
            setErrorMessage(error.message || "Failed to sign in. Please check your credentials.");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen flex flex-col items-center justify-center p-6 relative overflow-hidden font-sans">

            {/* Premium Atmospheric Background Stage */}
            <div className="fixed inset-0 z-0">
                <img
                    alt="Background Arena"
                    className="w-full h-full object-cover opacity-40 blur-[3px]"
                    src="https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=1200"
                />
                <div className="absolute inset-0 bg-gradient-to-b from-[#0b1326]/60 to-[#0b1326]/90 backdrop-blur-sm"></div>
            </div>

            {/* Main Container */}
            <main className="w-full max-w-sm z-10 relative">

                {/* Ticket Stub Card */}
                <div className="bg-[#eeefff] rounded-2xl shadow-2xl pt-8 pb-10 px-8 text-[#171f33] transition-all duration-300 transform hover:-translate-y-1">

                    {/* Header Identity & Home Access Button */}
                    <div className="flex flex-col items-center mb-6 relative">
                        {/* Clean Home Button pinned to the top right of the card header */}
                        <Link 
                            to={DEFAULT_REDIRECT_PATH}
                            className="absolute right-0 top-0 text-[#8d90a0] hover:text-[#2563eb] transition-colors p-1 flex items-center justify-center rounded-lg hover:bg-gray-200/50"
                            title="Go to Home/Dashboard"
                        >
                            <span className="material-symbols-outlined text-[22px]">home</span>
                        </Link>

                        <div className="text-3xl font-black tracking-tighter text-[#0b1326] mb-1">IDODO TICKETS</div>
                        <p className="text-[11px] font-mono text-[#2563eb] tracking-widest uppercase font-bold">Premium Access Gate</p>
                    </div>

                    {/* Ticket Perforation Visual Row */}
                    <div className="relative w-full mb-6">
                        <div className="border-t-2 border-dashed border-[#cbd5e1] my-4"></div>
                        <div className="absolute -left-[46px] -top-[11px] w-6 h-6 rounded-full bg-[#0b1326] shadow-inner"></div>
                        <div className="absolute -right-[46px] -top-[11px] w-6 h-6 rounded-full bg-[#0b1326] shadow-inner"></div>
                    </div>

                    {/* --- ERROR MESSAGE DISPLAY --- */}
                    {errorMessage && (
                        <div className="mb-4 p-3 rounded-lg bg-red-100 text-red-600 text-[11px] font-bold text-center border border-red-200">
                            {errorMessage}
                        </div>
                    )}

                    {/* Login Form */}
                    <form onSubmit={handleLoginSubmit} className="space-y-4">

                        {/* Account ID / Email */}
                        <div className="space-y-1">
                            <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#2d3449] ml-1">
                                Account ID / Email
                            </label>
                            <div className="relative group">
                                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8d90a0] group-focus-within:text-[#2563eb] transition-colors">
                                alternate_email
                                </span>
                                <input
                                    className="w-full bg-[#060e20]/5 border border-[#434655]/20 rounded-xl pl-10 pr-4 py-3 text-[#0b1326] placeholder-[#8d90a0] focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/10 outline-none transition-all"
                                    placeholder="name@event.com"
                                    type="text"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                />
                            </div>
                        </div>

                        {/* Access Key */}
                        <div className="space-y-1">
                            <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#2d3449] ml-1">
                                Access Key
                            </label>
                            <div className="relative group">
                                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8d90a0] group-focus-within:text-[#2563eb] transition-colors">
                                lock
                                </span>
                                <input
                                    className="w-full bg-[#060e20]/5 border border-[#434655]/20 rounded-xl pl-10 pr-10 py-3 text-[#0b1326] placeholder-[#8d90a0] focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/10 outline-none transition-all"
                                    placeholder="••••••••"
                                    type={showPassword ? "text" : "password"}
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                />
                                <button
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-[#8d90a0] hover:text-[#171f33]"
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                >
                                <span className="material-symbols-outlined text-[20px]">
                                    {showPassword ? 'visibility_off' : 'visibility'}
                                </span>
                                </button>
                            </div>
                        </div>

                        {/* Remember & Forgot Options */}
                        <div className="flex items-center justify-between py-1">
                            <label className="flex items-center space-x-2 cursor-pointer">
                                <input
                                    className="w-4 h-4 rounded border-gray-300 text-[#2563eb] focus:ring-[#2563eb]"
                                    type="checkbox"
                                    checked={rememberMe}
                                    onChange={(e) => setRememberMe(e.target.checked)}
                                />
                                <span className="text-[10px] font-mono text-gray-500 uppercase font-semibold">Remember Device</span>
                            </label>
                            <Link className="text-[10px] font-mono text-[#2563eb] hover:underline uppercase font-bold" to="/forgot-password">
                                Lost Access?
                            </Link>
                        </div>

                        {/* Submit Action Button */}
                        <button
                            className="w-full py-4 rounded-xl bg-gradient-to-b from-[#2563eb] to-[#0053db] text-white font-bold tracking-wide shadow-lg hover:shadow-[#2563eb]/20 active:scale-[0.98] transition-all flex items-center justify-center gap-2 group"
                            type="submit"
                            disabled={isLoading}
                        >
                            {isLoading ? (
                                <span className="material-symbols-outlined animate-spin">refresh</span>
                            ) : (
                                <>
                                    <span>SIGN IN</span>
                                    <span className="material-symbols-outlined group-hover:translate-x-1 transition-transform">arrow_forward</span>
                                </>
                            )}
                        </button>
                    </form>

                    {/* Card Navigation Footer */}
                    <div className="mt-8 text-center border-t border-gray-200 pt-5">
                        <p className="text-sm text-[#2d3449]">
                            New to the arena?{' '}
                            <Link className="text-[#2563eb] font-bold hover:underline" to="/register">
                                Register here
                            </Link>
                        </p>
                    </div>
                </div>

                {/* System Status Indicators */}
                <div className="mt-6 flex justify-center items-center gap-6 opacity-60">
                    <div className="flex items-center gap-1.5">
                        <div className="w-2 h-2 rounded-full bg-[#00dbe7] animate-pulse"></div>
                        <span className="text-[10px] font-mono text-[#b4c5ff]">LIVE AUTH SERVER</span>
                    </div>
                    <div className="h-3 w-px bg-gray-700"></div>
                    <div className="flex items-center gap-1.5">
                        <span className="material-symbols-outlined text-[14px] text-[#b4c5ff]">verified_user</span>
                        <span className="text-[10px] font-mono text-[#b4c5ff]">ENCRYPTED TICKET GATE</span>
                    </div>
                </div>
            </main>
        </div>
    );
}
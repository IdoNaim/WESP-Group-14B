import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';

export default function LoginPage() {
    const navigate = useNavigate();

    // Changed email to userId
    const [userId, setUserId] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [rememberMe, setRememberMe] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const handleLoginSubmit = async (event: FormEvent) => {
        event.preventDefault();
        console.log('--- [UI START] Login Form Submitted ---');
        console.log('[UI DATA] Attempting login with ID:', userId);

        setIsLoading(true);
        setErrorMessage(null);

        try {
            console.log('[UI STEP 1] Requesting guest token for login flow...');
            const guestResponse = await authApi.guestEntry();
            const guestToken = guestResponse.token;

            console.log('[UI STEP 2] Submitting login payload to backend...');
            const result = await authApi.login(guestToken, {
                userId: userId, // Perfectly matches DTO now
                password: password
            });

            console.log('[UI STEP 3] Saving tokens to localStorage and redirecting...');
            localStorage.setItem('token', result.token);
            localStorage.setItem('userId', result.userId);

            setUserId('');
            setPassword('');

            // Adjusted route to /home as requested previously
            navigate('/home');

        } catch (error: any) {
            console.error('[UI CATCH BLOCK] Login flow crashed:', error.message);
            setErrorMessage(error.message || "Failed to sign in. Please check your credentials.");
        } finally {
            console.log('--- [UI END] Login Process Completed ---');
            setIsLoading(false);
        }
    };

    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen flex flex-col items-center justify-center p-6 relative overflow-hidden font-sans">
            <div className="fixed inset-0 z-0">
                <img
                    alt="Background Arena"
                    className="w-full h-full object-cover opacity-40 blur-[3px]"
                    src="https://images.unsplash.com/photo-1506157786151-b8491531f063?q=80&w=1200"
                />
                <div className="absolute inset-0 bg-gradient-to-b from-[#0b1326]/60 to-[#0b1326]/90 backdrop-blur-sm"></div>
            </div>

            <main className="w-full max-w-sm z-10 relative">
                <div className="bg-[#eeefff] rounded-2xl shadow-2xl pt-8 pb-10 px-8 text-[#171f33] transition-all duration-300 transform hover:-translate-y-1">

                    <div className="flex flex-col items-center mb-6">
                        <div className="text-3xl font-black tracking-tighter text-[#0b1326] mb-1">IDODO TICKETS</div>
                        <p className="text-[11px] font-mono text-[#2563eb] tracking-widest uppercase font-bold">Premium Access Gate</p>
                    </div>

                    <div className="relative w-full mb-6">
                        <div className="border-t-2 border-dashed border-[#cbd5e1] my-4"></div>
                        <div className="absolute -left-[46px] -top-[11px] w-6 h-6 rounded-full bg-[#0b1326] shadow-inner"></div>
                        <div className="absolute -right-[46px] -top-[11px] w-6 h-6 rounded-full bg-[#0b1326] shadow-inner"></div>
                    </div>

                    {errorMessage && (
                        <div className="mb-4 p-3 rounded-lg bg-red-100 text-red-600 text-[11px] font-bold text-center border border-red-200">
                            {errorMessage}
                        </div>
                    )}

                    <form onSubmit={handleLoginSubmit} className="space-y-4">

                        {/* Account ID Input */}
                        <div className="space-y-1">
                            <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#2d3449] ml-1">
                                Account ID
                            </label>
                            <div className="relative group">
                                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8d90a0] group-focus-within:text-[#2563eb] transition-colors">
                                badge
                                </span>
                                <input
                                    className="w-full bg-[#060e20]/5 border border-[#434655]/20 rounded-xl pl-10 pr-4 py-3 text-[#0b1326] placeholder-[#8d90a0] focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/10 outline-none transition-all"
                                    placeholder="Enter your ID Number"
                                    type="text"
                                    value={userId}
                                    onChange={(e) => setUserId(e.target.value)}
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

                    <div className="mt-8 text-center border-t border-gray-200 pt-5">
                        <p className="text-sm text-[#2d3449]">
                            New to the arena?{' '}
                            <Link className="text-[#2563eb] font-bold hover:underline" to="/register">
                                Register here
                            </Link>
                        </p>
                    </div>
                </div>

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
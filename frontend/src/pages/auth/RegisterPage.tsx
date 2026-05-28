import { useState, FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi'; // Corrected path based on your folder structure

export default function RegisterPage() {
    const navigate = useNavigate();

    const [fullName, setFullName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const handleRegisterSubmit = async (event: FormEvent) => {
        event.preventDefault();
        setIsLoading(true);
        setErrorMessage(null);

        try {
            // Step 1: Obtain the guest token required by your backend
            console.log('[UI STEP 1] Requesting guest token from API...');
            const guestResponse = await authApi.guestEntry();
            const guestToken = guestResponse.token;
            console.log('[UI STEP 1 RESULT] Guest token received:', guestResponse.token);

            // Step 2: Submit the registration payload
            // Mapping 'email' to 'userId' since your backend requires a userId
            console.log('[UI STEP 2] Submitting user registration form payload...');
            await authApi.register(guestToken, {
                userId: email,
                name: fullName,
                email: email,
                password: password,
                userGroupDiscount: 'NONE' // Optional based on your DTO, but safe to default
            });
            console.log('[UI STEP 2 RESULT] Registration succeeded!');

            // Step 3: Success! Redirect them to the login page so they can sign in
            console.log('[UI STEP 3] Redirecting user to login page...');
            navigate('/login');

        } catch (error: any) {
            // Step 4: Catch and display any errors (e.g., "User already exists")
            console.error('[UI CATCH BLOCK] Registration flow crashed:', error.message);
            setErrorMessage(error.message || "Failed to register. Please try again.");
        } finally {
            // Step 5: Turn off the loading spinner
            console.log('--- [UI END] Registration Process Completed ---');
            setIsLoading(false);
        }
    };

    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen flex flex-col items-center justify-center p-6 relative overflow-hidden font-sans">

            {/* Background Stage Wrapper */}
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
                        <div className="text-3xl font-black tracking-tighter text-[#0b1326] mb-1">ELITE TICKETS</div>
                        <p className="text-[11px] font-mono text-[#2563eb] tracking-widest uppercase font-bold">Claim Access Pass</p>
                    </div>

                    <div className="relative w-full mb-6">
                        <div className="border-t-2 border-dashed border-[#cbd5e1] my-4"></div>
                        <div className="absolute -left-[46px] -top-[11px] w-6 h-6 rounded-full bg-[#0b1326]"></div>
                        <div className="absolute -right-[46px] -top-[11px] w-6 h-6 rounded-full bg-[#0b1326]"></div>
                    </div>

                    {/* --- ERROR MESSAGE DISPLAY --- */}
                    {errorMessage && (
                        <div className="mb-4 p-3 rounded-lg bg-red-100 text-red-600 text-[11px] font-bold text-center border border-red-200">
                            {errorMessage}
                        </div>
                    )}

                    <form onSubmit={handleRegisterSubmit} className="space-y-4">

                        {/* Name Input */}
                        <div className="space-y-1">
                            <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#2d3449] ml-1">
                                Full Name
                            </label>
                            <div className="relative group">
                                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8d90a0] group-focus-within:text-[#2563eb] transition-colors">
                                  person
                                </span>
                                <input
                                    className="w-full bg-[#060e20]/5 border border-[#434655]/20 rounded-xl pl-10 pr-4 py-3 text-[#0b1326] placeholder-[#8d90a0] focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/10 outline-none transition-all"
                                    placeholder="John Doe"
                                    type="text"
                                    value={fullName}
                                    onChange={(e) => setFullName(e.target.value)}
                                    required
                                />
                            </div>
                        </div>

                        {/* Email Input */}
                        <div className="space-y-1">
                            <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#2d3449] ml-1">
                                Email Address
                            </label>
                            <div className="relative group">
                                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8d90a0] group-focus-within:text-[#2563eb] transition-colors">
                                  alternate_email
                                </span>
                                <input
                                    className="w-full bg-[#060e20]/5 border border-[#434655]/20 rounded-xl pl-10 pr-4 py-3 text-[#0b1326] placeholder-[#8d90a0] focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/10 outline-none transition-all"
                                    placeholder="name@event.com"
                                    type="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                />
                            </div>
                        </div>

                        {/* Password Input */}
                        <div className="space-y-1">
                            <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#2d3449] ml-1">
                                Create Access Key
                            </label>
                            <div className="relative group">
                                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#8d90a0] group-focus-within:text-[#2563eb] transition-colors">
                                  lock_open
                                </span>
                                <input
                                    className="w-full bg-[#060e20]/5 border border-[#434655]/20 rounded-xl pl-10 pr-4 py-3 text-[#0b1326] placeholder-[#8d90a0] focus:border-[#2563eb] focus:ring-2 focus:ring-[#2563eb]/10 outline-none transition-all"
                                    placeholder="••••••••"
                                    type="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                />
                            </div>
                        </div>

                        <button
                            className="w-full py-4 mt-2 rounded-xl bg-gradient-to-b from-[#2563eb] to-[#0053db] text-white font-bold tracking-wide shadow-lg active:scale-[0.98] transition-all flex items-center justify-center gap-2 group"
                            type="submit"
                            disabled={isLoading}
                        >
                            {isLoading ? (
                                <span className="material-symbols-outlined animate-spin">refresh</span>
                            ) : (
                                <>
                                    <span>GENERATE TICKET</span>
                                    <span className="material-symbols-outlined">confirmation_number</span>
                                </>
                            )}
                        </button>
                    </form>

                    <div className="mt-8 text-center border-t border-gray-200 pt-5">
                        <p className="text-sm text-[#2d3449]">
                            Already in the arena?{' '}
                            <Link className="text-[#2563eb] font-bold hover:underline" to="/login">
                                Sign In
                            </Link>
                        </p>
                    </div>
                </div>
            </main>
        </div>
    );
}
import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, UserPermissionsDTO, UserProfileDTO } from '../../api/authApi'; // Adjust path if needed

// ─── Types ────────────────────────────────────────────────────────────────────

interface NavItem {
  label: string;
  icon: string;
  href: string;
  active?: boolean;
  memberOnly?: boolean;
}

// ─── Data ─────────────────────────────────────────────────────────────────────

const NAV_ITEMS: NavItem[] = [
  { label: 'Home',          icon: 'home',                 href: '/home' },
  { label: 'Events',        icon: 'event',                href: '/events' },
  // Set "My Order" as the active tab for the Checkout Page
  { label: 'My Order',      icon: 'shopping_cart',        href: '/activeorder/', active: true },
  { label: 'Order History', icon: 'history',              href: '/history', memberOnly: true },
  { label: 'Notifications', icon: 'notifications',        href: '#', memberOnly: true },
  { label: 'My Companies',  icon: 'business',             href: '#', memberOnly: true },
  { label: 'Admin Panel',   icon: 'admin_panel_settings', href: '#', memberOnly: true },
];

// ─── Sub-components ───────────────────────────────────────────────────────────

function MaterialIcon({ name, className = '' }: { name: string; className?: string }) {
  return (
    <span
      className={`material-symbols-outlined ${className}`}
      style={{ fontVariationSettings: "'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24" }}
    >
      {name}
    </span>
  );
}

function SidebarNavItem({ item }: { item: NavItem }) {
  if (item.active) {
    return (
      <Link
        to={item.href}
        className="flex items-center gap-4 px-4 py-2 border-l-4 border-black bg-gray-100 text-black font-bold transition-colors"
      >
        <MaterialIcon name={item.icon} />
        <span className="text-sm font-semibold">{item.label}</span>
      </Link>
    );
  }
  return (
    <Link
      to={item.href}
      className="flex items-center gap-4 px-4 py-2 rounded-lg hover:bg-gray-50 transition-colors text-gray-600"
    >
      <MaterialIcon name={item.icon} />
      <span className="text-sm font-semibold">{item.label}</span>
    </Link>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────

export default function CheckoutPage() {
  const navigate = useNavigate();

  // Auth & User State
  const [permissions, setPermissions] = useState<UserPermissionsDTO | null>(null);
  const [username, setUsername] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // State for checkout simulation
  const [checkoutStatus, setCheckoutStatus] = useState<'idle' | 'processing' | 'finalizing' | 'success'>('idle');
  
  // State for reservation timer
  const [timeLeft, setTimeLeft] = useState<number>(594); // 9:54
  
  // State for policy error demo
  const [showPolicyError, setShowPolicyError] = useState<boolean>(false);

  // ─── Data Fetching ───
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      setLoading(false);
      return;
    }

    const loadUserData = async () => {
      try {
        const [perms, profile] = await Promise.all([
          authApi.getPermissions(token),
          authApi.getCurrentUser(token),
        ]);
        setPermissions(perms);
        setUsername(profile.name); 
      } catch (error: any) {
        console.error('[Checkout] Failed to load user data:', error.message);
        setPermissions(null);
        setUsername(null);
      } finally {
        setLoading(false);
      }
    };

    loadUserData();
  }, []);

  const isGuest = !username;

  // Filter nav items based on guest/member mode and admin status
  const visibleNavItems = NAV_ITEMS.filter(item => !item.memberOnly || !isGuest);
  const visibleNavItemsFiltered = visibleNavItems.filter(item =>
    item.label !== 'Admin Panel' || (permissions?.isAdmin ?? false)
  );

  // Initials for avatar
  const initials = username
    ? username.slice(0, 2).toUpperCase()
    : 'G'; // Guest

  const handleLogout = async () => {
    const token = localStorage.getItem('token');
    if (!token || !permissions?.userId) return;
    try {
      await authApi.logout(token, permissions.userId);
    } catch (e) {
      // Ignore errors on logout
    } finally {
      localStorage.removeItem('token');
      window.location.reload();
    }
  };

  // ─── Timers & Events ───
  useEffect(() => {
    if (timeLeft <= 0) return;
    const timerId = setInterval(() => setTimeLeft((prev) => prev - 1), 1000);
    return () => clearInterval(timerId);
  }, [timeLeft]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'e' || e.key === 'E') {
        setShowPolicyError((prev) => {
          if (!prev) window.scrollTo({ top: 0, behavior: 'smooth' });
          return !prev;
        });
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const simulateCheckout = () => {
    if (checkoutStatus !== 'idle') return;
    setCheckoutStatus('processing');
    setTimeout(() => {
      setCheckoutStatus('finalizing');
      setTimeout(() => {
        setCheckoutStatus('success');
      }, 800);
    }, 1000);
  };

  const formatTime = (seconds: number) => {
    if (seconds <= 0) return "EXPIRED";
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#f8f9ff]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-gray-500">Loading checkout...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="text-[14px] overflow-x-hidden bg-[#f8f9ff] text-[#1a1b20] min-h-screen font-sans">
      
      {/* ── Google Fonts ── */}
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Geist:wght@100..900&family=Geist+Mono:wght@100..900&display=swap');
        @import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap');
      `}</style>

      {/* SideNavBar */}
      <aside className="fixed left-0 top-0 h-full w-[260px] bg-white border-r border-gray-200 flex flex-col py-6 z-50">
        <div className="px-6 mb-10">
          <h1 className="text-2xl font-bold text-black tracking-tight flex items-center gap-2">
            <MaterialIcon name="confirmation_number" className="text-blue-600" />
            TicketFlow
          </h1>
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-widest mt-1">Enterprise Edition</p>
        </div>
        
        {/* Dynamic Navigation Map based on Auth Status */}
        <nav className="flex-1 space-y-1 px-2">
          {visibleNavItemsFiltered.map((item) => (
            <SidebarNavItem key={item.label} item={item} />
          ))}
        </nav>

        {/* Guest mode prompt in sidebar */}
        {isGuest && (
          <div className="px-6 mt-auto">
            <div className="bg-blue-50 border border-gray-200 rounded-lg p-3 text-center">
              <p className="text-xs text-gray-500 mb-2">Browsing as guest</p>
              <Link to="/login" className="block w-full bg-blue-600 text-white text-xs font-bold py-2 rounded-lg hover:opacity-90 transition-opacity">
                Sign In
              </Link>
            </div>
          </div>
        )}
      </aside>

      {/* TopNavBar */}
      <header className="fixed top-0 right-0 h-[64px] w-[calc(100%-260px)] bg-white border-b border-gray-200 flex justify-between items-center px-6 z-40">
        <div className="flex items-center flex-1 max-w-xl">
          <div className="relative w-full group">
            <MaterialIcon name="search" className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-blue-600 transition-colors" />
            <input type="text" placeholder="Search orders, tickets, or events..." className="w-full bg-gray-50 border-none rounded-full py-2 pl-12 pr-12 text-sm focus:ring-1 focus:ring-blue-600 placeholder:text-gray-400 transition-all outline-none" />
            <kbd className="absolute right-4 top-1/2 -translate-y-1/2 bg-white px-2 py-0.5 rounded text-[10px] font-mono text-gray-500 border border-gray-200 shadow-sm">⌘K</kbd>
          </div>
        </div>

        <div className="flex items-center gap-6">
          <div className="flex items-center gap-2">
            {/* Dynamic User Profile / Login Link */}
            {isGuest ? (
              <Link to="/login" className="text-sm font-bold text-blue-600 hover:underline">
                Sign In
              </Link>
            ) : (
              <div className="flex items-center gap-3 pl-3 border-l border-gray-200">
                <div className="text-right hidden sm:block">
                  <p className="text-xs font-bold text-black">{username}</p>
                  <p className="text-[10px] text-gray-500 uppercase tracking-tighter">
                    {permissions?.isAdmin ? 'Admin Access' : 'User Access'}
                  </p>
                </div>
                <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white font-bold text-xs border border-gray-200">
                  {initials}
                </div>
                <button onClick={handleLogout} className="text-gray-500 hover:text-red-600 transition-all ml-2" title="Logout">
                  <MaterialIcon name="logout" />
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="ml-[260px] pt-[64px] min-h-screen p-6">
        <div className="max-w-[1440px] mx-auto">
          
          {/* Breadcrumbs */}
          <nav className="flex items-center gap-1 text-gray-500 text-xs font-semibold mb-6">
            <Link className="hover:text-black" to="/events">Events</Link>
            <MaterialIcon name="chevron_right" className="text-sm" />
            <Link className="hover:text-black" to="/events/tech-summit">Global Tech Summit 2024</Link>
            <MaterialIcon name="chevron_right" className="text-sm" />
            <span className="text-black font-bold">Checkout</span>
          </nav>

          {/* Error Banner (Conditional State) */}
          {checkoutStatus !== 'success' && (
            <div className={`mb-6 p-4 rounded-lg flex items-center justify-between border ${timeLeft > 0 ? 'bg-red-50 text-red-900 border-red-200 animate-pulse' : 'bg-red-900 text-white border-red-900'}`}>
              <div className="flex items-center gap-2">
                <MaterialIcon name="timer" />
                <p className="text-sm font-bold">
                  {timeLeft > 0 ? 'Reservation expires in ' : ''}
                  <span>{formatTime(timeLeft)}</span>
                </p>
              </div>
              <p className="text-xs">
                {timeLeft > 0 ? 'Complete your purchase to secure these seats.' : 'Your reservation has expired.'}
              </p>
            </div>
          )}

          {/* Policy Violation Warning */}
          {showPolicyError && (
            <div className="mb-6 bg-amber-50 border-l-4 border-amber-500 p-4 flex gap-4">
              <MaterialIcon name="warning" className="text-amber-600" />
              <div>
                <p className="text-lg font-bold text-amber-900">Policy Violation Detected</p>
                <p className="text-amber-800 text-sm">Maximum 4 tickets per user for this event tier. Please adjust your quantity to proceed.</p>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
            {/* Left Column: Order Details */}
            <div className="lg:col-span-2 space-y-6">
              {/* Event Header Card */}
              <div className="bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm">
                <div className="h-32 relative bg-gradient-to-br from-black to-gray-900">
                  <div className="absolute inset-0 opacity-20" style={{ backgroundImage: 'radial-gradient(circle at 2px 2px, white 1px, transparent 0)', backgroundSize: '24px 24px' }}></div>
                  <div className="absolute bottom-4 left-6">
                    <span className="bg-green-100 text-green-800 text-xs font-bold px-3 py-1 rounded-full inline-flex items-center gap-1.5 mb-2 shadow-sm">
                      <span className="w-2 h-2 rounded-full bg-green-600 animate-ping"></span>
                      Live Inventory
                    </span>
                    <h2 className="text-white text-2xl font-bold leading-tight">Global Tech Summit 2024</h2>
                    <p className="text-gray-300 text-xs font-semibold mt-1">Nov 15, 2024 • Moscone Center, San Francisco</p>
                  </div>
                </div>
                
                <div className="p-6">
                  <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
                    <MaterialIcon name="confirmation_number" />
                    Selected Tickets
                  </h3>
                  <div className="space-y-2">
                    {/* VIP Pass */}
                    <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:border-blue-500 transition-colors group">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded bg-gray-100 flex items-center justify-center text-black">
                          <MaterialIcon name="star" />
                        </div>
                        <div>
                          <p className="text-base font-bold text-black">VIP Pass (Full Access)</p>
                          <p className="text-gray-500 text-xs font-semibold">Includes Lounge & Networking Dinner</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-base font-bold text-black">$250.00</p>
                        <p className="text-gray-500 text-xs font-semibold">Qty: 2</p>
                      </div>
                    </div>
                    {/* Standard Entry */}
                    <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:border-blue-500 transition-colors group">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded bg-gray-100 flex items-center justify-center text-black">
                          <MaterialIcon name="person" />
                        </div>
                        <div>
                          <p className="text-base font-bold text-black">Standard Entry</p>
                          <p className="text-gray-500 text-xs font-semibold">Main Stage & Expo Hall Access</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-base font-bold text-black">$100.00</p>
                        <p className="text-gray-500 text-xs font-semibold">Qty: 1</p>
                      </div>
                    </div>
                  </div>
                  <button className="mt-4 text-blue-600 text-xs font-bold flex items-center gap-1 hover:underline">
                    <MaterialIcon name="edit" className="text-sm" />
                    Modify Selection
                  </button>
                </div>
              </div>

              {/* Checkout Policy & Details */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                
                {/* Billing Details Card */}
                <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
                  <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
                    <MaterialIcon name="receipt_long" />
                    Billing Details
                  </h3>
                  <div className="space-y-1 text-sm text-gray-600">
                    <p className="text-black font-bold">{username ? `${username}'s Account` : 'Guest Checkout'}</p>
                    <p>123 Innovation Way</p>
                    <p>San Francisco, CA 94103</p>
                    <button className="text-blue-600 text-xs mt-2 hover:underline font-semibold">Change Billing Address</button>
                  </div>
                </div>

                {/* Payment Method Card */}
                <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
                  <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
                    <MaterialIcon name="credit_card" />
                    Payment Method
                  </h3>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-xs font-bold text-gray-600 mb-1">Cardholder Name</label>
                      <input 
                        className="w-full bg-gray-50 border border-gray-200 rounded-lg py-2 px-4 text-sm focus:ring-2 focus:ring-blue-600 focus:border-transparent transition-all outline-none" 
                        placeholder="Alex Rivera" 
                        type="text" 
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-gray-600 mb-1">Card Number</label>
                      <div className="relative">
                        <input 
                          className="w-full bg-gray-50 border border-gray-200 rounded-lg py-2 pl-4 pr-10 text-sm focus:ring-2 focus:ring-blue-600 focus:border-transparent transition-all outline-none" 
                          placeholder="•••• •••• •••• 4242" 
                          type="text" 
                        />
                        <MaterialIcon name="payments" className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400" />
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-bold text-gray-600 mb-1">Expiration Date</label>
                        <input 
                          className="w-full bg-gray-50 border border-gray-200 rounded-lg py-2 px-4 text-sm focus:ring-2 focus:ring-blue-600 focus:border-transparent transition-all outline-none" 
                          placeholder="MM / YY" 
                          type="text" 
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-bold text-gray-600 mb-1">CVC</label>
                        <input 
                          className="w-full bg-gray-50 border border-gray-200 rounded-lg py-2 px-4 text-sm focus:ring-2 focus:ring-blue-600 focus:border-transparent transition-all outline-none" 
                          placeholder="123" 
                          type="text" 
                        />
                      </div>
                    </div>
                  </div>
                </div>

              </div>
            </div>

            {/* Right Column: Summary & CTA */}
            <div className="space-y-6 sticky top-[88px]">
              <div className="bg-white border border-gray-200 rounded-xl shadow-md overflow-hidden">
                <div className="bg-gray-50 p-6 border-b border-gray-200">
                  <h3 className="text-xl font-bold text-black">Order Summary</h3>
                  <p className="text-gray-500 text-xs font-semibold mt-1">Invoice #TF-2024-8842</p>
                </div>
                <div className="p-6 space-y-4">
                  <div className="space-y-2 pb-4 border-b border-gray-200">
                    <div className="flex justify-between text-sm">
                      <span className="text-gray-600">Subtotal (3 Tickets)</span>
                      <span className="font-semibold text-black">$600.00</span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-gray-600">Service Fee</span>
                      <span className="font-semibold text-black">$0.00</span>
                    </div>
                    <div className="flex justify-between text-sm text-green-700 font-semibold">
                      <span className="flex items-center gap-1">
                        <MaterialIcon name="sell" className="text-sm" />
                        Enterprise Discount
                      </span>
                      <span>-$50.00</span>
                    </div>
                  </div>
                  <div className="flex justify-between items-end pt-2">
                    <span className="text-lg font-bold text-black">Total Due</span>
                    <span className="text-2xl font-black text-black">$550.00</span>
                  </div>
                  
                  <div className="pt-6">
                    <button 
                      onClick={simulateCheckout}
                      disabled={checkoutStatus !== 'idle'}
                      className={`w-full py-4 rounded-xl font-bold tracking-wide shadow-lg flex items-center justify-center gap-2 transition-all ${checkoutStatus === 'success' ? 'bg-green-600 text-white shadow-green-600/20' : 'bg-gradient-to-b from-blue-600 to-blue-700 text-white shadow-blue-600/20 hover:shadow-blue-600/40 active:scale-[0.98]'}`}
                    >
                      <span className={checkoutStatus !== 'idle' && checkoutStatus !== 'success' ? 'opacity-50' : ''}>
                        {checkoutStatus === 'idle' && 'CONFIRM PURCHASE'}
                        {checkoutStatus === 'processing' && 'PROCESSING...'}
                        {checkoutStatus === 'finalizing' && 'FINALIZING...'}
                        {checkoutStatus === 'success' && 'PURCHASED!'}
                      </span>
                      {checkoutStatus !== 'idle' && checkoutStatus !== 'success' && (
                        <MaterialIcon name="refresh" className="animate-spin text-white opacity-50" />
                      )}
                      {checkoutStatus === 'success' && (
                          <MaterialIcon name="check_circle" className="text-white" />
                      )}
                    </button>
                    <p className="text-[10px] text-gray-500 text-center mt-4 leading-relaxed px-4 font-semibold uppercase tracking-wider">
                      By clicking Confirm, you agree to the <a className="text-blue-600 hover:underline" href="#tos">Terms of Service</a> and <a className="text-blue-600 hover:underline" href="#conduct">Code of Conduct</a>.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Success Toast */}
      <div 
        className={`fixed bottom-6 right-6 transform transition-all duration-500 z-[100] ${checkoutStatus === 'success' ? 'translate-y-0 opacity-100' : 'translate-y-[150%] opacity-0'}`}
      >
        <div className="bg-green-50 border-l-4 border-green-600 p-6 rounded-xl shadow-2xl flex items-center gap-4 min-w-[320px]">
          <div className="w-10 h-10 bg-green-600 text-white rounded-full flex items-center justify-center">
            <MaterialIcon name="check" />
          </div>
          <div>
            <p className="text-lg font-bold text-green-900">Order Confirmed!</p>
            <p className="text-sm font-semibold text-green-700">Tickets sent to {username ? `${username}'s email` : 'your email'}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

export default function CheckoutPage() {
  // State for checkout simulation
  const [checkoutStatus, setCheckoutStatus] = useState<'idle' | 'processing' | 'finalizing' | 'success'>('idle');
  
  // State for reservation timer
  const [timeLeft, setTimeLeft] = useState<number>(594); // 9:54
  
  // State for policy error demo
  const [showPolicyError, setShowPolicyError] = useState<boolean>(false);

  // Timer Effect
  useEffect(() => {
    if (timeLeft <= 0) return;

    const timerId = setInterval(() => {
      setTimeLeft((prev) => prev - 1);
    }, 1000);

    return () => clearInterval(timerId);
  }, [timeLeft]);

  // Policy Error Toggle Demo (Press 'e')
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

  // Helper for formatting time
  const formatTime = (seconds: number) => {
    if (seconds <= 0) return "EXPIRED";
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="text-[14px] overflow-x-hidden bg-[#f8f9ff] text-[#1a1b20] min-h-screen font-sans">
      
      {/* SideNavBar */}
      <aside className="fixed left-0 top-0 h-full w-[260px] bg-white border-r border-gray-200 flex flex-col py-6 z-50">
        <div className="px-6 mb-10">
          <h1 className="text-2xl font-bold text-black tracking-tight">TicketFlow Pro</h1>
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-widest mt-1">Enterprise Edition</p>
        </div>
        <nav className="flex-1 space-y-1 px-2">
          <Link to="/" className="flex items-center gap-4 px-4 py-2 rounded-lg hover:bg-gray-50 transition-colors text-gray-600">
            <span className="material-symbols-outlined">home</span>
            <span className="text-sm font-semibold">Home</span>
          </Link>
          <Link to="/events" className="flex items-center gap-4 px-4 py-2 rounded-lg hover:bg-gray-50 transition-colors text-gray-600">
            <span className="material-symbols-outlined">event</span>
            <span className="text-sm font-semibold">Events</span>
          </Link>
          <Link to="/orders" className="flex items-center gap-4 px-4 py-2 border-l-4 border-black bg-gray-100 text-black font-bold">
            <span className="material-symbols-outlined">confirmation_number</span>
            <span className="text-sm font-semibold">My Orders</span>
          </Link>
          <Link to="/history" className="flex items-center gap-4 px-4 py-2 rounded-lg hover:bg-gray-50 transition-colors text-gray-600">
            <span className="material-symbols-outlined">history</span>
            <span className="text-sm font-semibold">Order History</span>
          </Link>
        </nav>
      </aside>

      {/* TopNavBar */}
      <header className="fixed top-0 right-0 h-[64px] w-[calc(100%-260px)] bg-white border-b border-gray-200 flex justify-between items-center px-6 z-40">
        <div className="flex items-center flex-1 max-w-xl">
          <div className="relative w-full group">
            <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-blue-600 transition-colors">search</span>
            <input type="text" placeholder="Search orders, tickets, or events..." className="w-full bg-gray-50 border-none rounded-full py-2 pl-12 pr-12 text-sm focus:ring-1 focus:ring-blue-600 placeholder:text-gray-400 transition-all outline-none" />
            <kbd className="absolute right-4 top-1/2 -translate-y-1/2 bg-white px-2 py-0.5 rounded text-[10px] font-mono text-gray-500 border border-gray-200 shadow-sm">⌘K</kbd>
          </div>
        </div>

        <div className="flex items-center gap-6">
          <div className="flex gap-4">
            <button className="material-symbols-outlined text-gray-500 hover:text-black transition-all">settings</button>
            <button className="material-symbols-outlined text-gray-500 hover:text-black transition-all">logout</button>
          </div>
          <div className="flex items-center gap-2">
            <div className="text-right hidden sm:block">
              <p className="text-xs font-bold text-black">Alex Rivera</p>
              <p className="text-[10px] text-gray-500 uppercase tracking-tighter">Admin Access</p>
            </div>
            <div className="w-8 h-8 rounded-full border border-gray-200 bg-gray-300 overflow-hidden">
                <img alt="User" src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=facearea&facepad=2&w=256&h=256&q=80" />
            </div>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="ml-[260px] pt-[64px] min-h-screen p-6">
        <div className="max-w-[1440px] mx-auto">
          
          {/* Breadcrumbs */}
          <nav className="flex items-center gap-1 text-gray-500 text-xs font-semibold mb-6">
            <Link className="hover:text-black" to="/events">Events</Link>
            <span className="material-symbols-outlined text-sm">chevron_right</span>
            <Link className="hover:text-black" to="/events/tech-summit">Global Tech Summit 2024</Link>
            <span className="material-symbols-outlined text-sm">chevron_right</span>
            <span className="text-black font-bold">Checkout</span>
          </nav>

          {/* Error Banner (Conditional State) */}
          {checkoutStatus !== 'success' && (
            <div className={`mb-6 p-4 rounded-lg flex items-center justify-between border ${timeLeft > 0 ? 'bg-red-50 text-red-900 border-red-200 animate-pulse' : 'bg-red-900 text-white border-red-900'}`}>
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined">timer</span>
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
              <span className="material-symbols-outlined text-amber-600">warning</span>
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
                    <span className="material-symbols-outlined">confirmation_number</span>
                    Selected Tickets
                  </h3>
                  <div className="space-y-2">
                    {/* VIP Pass */}
                    <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:border-blue-500 transition-colors group">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded bg-gray-100 flex items-center justify-center text-black">
                          <span className="material-symbols-outlined">star</span>
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
                          <span className="material-symbols-outlined">person</span>
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
                    <span className="material-symbols-outlined text-sm">edit</span>
                    Modify Selection
                  </button>
                </div>
              </div>

              {/* Checkout Policy & Details */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm">
                  <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
                    <span className="material-symbols-outlined">receipt_long</span>
                    Billing Details
                  </h3>
                  <div className="space-y-1 text-sm text-gray-600">
                    <p className="text-black font-bold">Acme Corp HQ</p>
                    <p>123 Innovation Way</p>
                    <p>San Francisco, CA 94103</p>
                    <button className="text-blue-600 text-xs mt-2 hover:underline font-semibold">Change Billing Address</button>
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
                        <span className="material-symbols-outlined text-sm">sell</span>
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
                        <span className="material-symbols-outlined animate-spin text-white opacity-50">refresh</span>
                      )}
                      {checkoutStatus === 'success' && (
                          <span className="material-symbols-outlined text-white">check_circle</span>
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
            <span className="material-symbols-outlined">check</span>
          </div>
          <div>
            <p className="text-lg font-bold text-green-900">Order Confirmed!</p>
            <p className="text-sm font-semibold text-green-700">Tickets sent to alex@acme.com</p>
          </div>
        </div>
      </div>
    </div>
  );
}
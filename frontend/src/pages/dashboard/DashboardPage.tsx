import { useEffect, useState } from 'react'; // 💡 Removed useRef since it's no longer used
import { Link } from 'react-router-dom';
import { authApi, UserPermissionsDTO } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext'; // 💡 Double check this relative path matches your folder structure!

interface QuickLinkCard {
  icon: string;
  title: string;
  subtitle: string;
  href: string;
  memberOnly?: boolean;
}

const QUICK_LINKS: QuickLinkCard[] = [
  { icon: 'explore', title: 'Browse Events', subtitle: 'Discover trending and upcoming events worldwide.', href: '/events' },
  { icon: 'shopping_bag', title: 'Active Order', subtitle: 'View status of your current pending transactions.', href: '/orders/active' },
  { icon: 'receipt_long', title: 'Order History', subtitle: 'Review past purchases and download invoices.', href: '/orders/history', memberOnly: true },
  { icon: 'notifications_active', title: 'Notifications', subtitle: 'Stay updated with real-time event alerts.', href: '/notifications', memberOnly: true },
  { icon: 'monitoring', title: 'My Companies', subtitle: 'Manage your production companies and events.', href: '/production-company', memberOnly: true },
  { icon: 'admin_panel_settings', title: 'Admin Panel', subtitle: 'Manage system settings and user permissions.', href: '/admin', memberOnly: true },
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

function QuickLinkCardComponent({ card }: { card: QuickLinkCard }) {
  return (
    <Link
      to={card.href}
      className="h-full flex flex-col bg-white border border-[#c6c6cd] p-6 rounded-xl hover:border-[#3980f4] transition-all group cursor-pointer"
      style={{ boxShadow: '0px 1px 3px rgba(0,0,0,0.05)' }}
    >
      <MaterialIcon
        name={card.icon}
        className="text-[#3980f4] mb-4 text-[32px] block group-hover:scale-110 transition-transform"
      />
      <h4 className="text-sm font-bold mb-1">{card.title}</h4>
      <p className="text-xs leading-4 tracking-wide text-[#5c5f61]">{card.subtitle}</p>
    </Link>
  );
}

// ─── Main Component ───────────────────────────────────────────────────────────

export default function DashboardPage() {
  
  // ── COMMENTED OUT UNUSED SEARCH LOGIC TO FIX LINE 209 ERROR ──
  // const searchRef = useRef<HTMLInputElement>(null);
  // const [searchQuery, setSearchQuery] = useState('');
  // const handleSearch = (e: React.KeyboardEvent<HTMLInputElement>) => {
  //   if (e.key === 'Enter' && searchQuery.trim()) {
  //     navigate(`/events?search=${encodeURIComponent(searchQuery.trim())}`);
  //   }
  // };

  // Get the isProductionUser state from your AuthContext hook
  const { isProductionUser } = useAuth();
  console.log('Is production user:', isProductionUser); // Debug log to verify the value
  const [permissions, setPermissions] = useState<UserPermissionsDTO | null>(null);
  const [username, setUsername] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Fetch permissions + profile on mount
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
        console.error('[Dashboard] Failed to load user data:', error.message);
        setPermissions(null);
        setUsername(null);
      } finally {
        setLoading(false);
      }
    };

    loadUserData();
  }, []);

  const isGuest = !username;

  // ── COMMENTED OUT UNUSED KEYBOARD SHORTCUT ──
  // useEffect(() => {
  //   const handler = (e: KeyboardEvent) => {
  //     if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
  //       e.preventDefault();
  //       searchRef.current?.focus();
  //     }
  //   };
  //   window.addEventListener('keydown', handler);
  //   return () => window.removeEventListener('keydown', handler);
  // }, []);



  const visibleQuickLinks = QUICK_LINKS
    .filter(card => !card.memberOnly || !isGuest)
    .filter(card => card.title !== 'Admin Panel' || (permissions?.isAdmin ?? false))
    .filter(card => card.title !== 'My Companies' || isProductionUser);

  // Subtle card hover lift
  useEffect(() => {
    const cards = document.querySelectorAll<HTMLElement>('.card-lift');
    const onEnter = (card: HTMLElement) => {
      card.style.transform = 'translateY(-2px)';
      card.style.transition = 'transform 0.2s ease-out';
    };
    const onLeave = (card: HTMLElement) => {
      card.style.transform = 'translateY(0px)';
    };
    cards.forEach(card => {
      card.addEventListener('mouseenter', () => onEnter(card));
      card.addEventListener('mouseleave', () => onLeave(card));
    });
    return () => {
      cards.forEach(card => {
        card.removeEventListener('mouseenter', () => onEnter(card));
        card.removeEventListener('mouseleave', () => onLeave(card));
      });
    };
  }, [loading]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#f8f9ff]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-4 border-[#3980f4] border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-[#5c5f61]">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div
      className="font-sans text-[#0b1c30] select-none"
      style={{ fontFamily: "'Geist', sans-serif", backgroundColor: '#f8f9ff' }}
    >
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Geist:wght@100..900&family=Geist+Mono:wght@100..900&display=swap');
        @import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap');
      `}</style>

      {/* ── MAIN CONTENT ── */}
      <main className="min-h-screen">
        <div className="max-w-[1280px] mx-auto p-10">

          {/* Welcome Banner (members only) */}
          {!isGuest && (
            <section className="relative mb-10 overflow-hidden rounded-xl bg-[#131b2e] p-10 text-white border border-[#c6c6cd]">
              <div className="absolute right-0 top-0 w-1/3 h-full opacity-10 pointer-events-none">
                <div className="w-full h-full bg-gradient-to-l from-[#3980f4] to-transparent" />
              </div>
              <div className="relative z-10">
                <h2 className="text-3xl font-semibold leading-10">Welcome back, {username}!</h2>
              </div>
            </section>
          )}

          {/* Guest banner */}
          {isGuest && (
            <section className="relative mb-10 overflow-hidden rounded-xl bg-[#eff4ff] p-10 border border-[#c6c6cd]">
              <div className="relative z-10">
                <h2 className="text-3xl font-semibold leading-10 text-[#0b1c30]">Welcome to TicketFlow</h2>
                <p className="text-[#5c5f61] text-base mt-2">
                  You're browsing as a guest.{' '}
                  <Link to="/login" className="text-[#3980f4] font-bold hover:underline">
                    Sign in
                  </Link>{' '}
                  or Suscribe to access your orders, companies, and more.
                </p>
              </div>
            </section>
          )}

          {/* Quick Access Grid */}
          <div>
            <h3 className="text-lg font-semibold leading-7 mb-6 flex items-center gap-2">
              <MaterialIcon name="grid_view" className="text-[#3980f4]" />
              Quick Access
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 auto-rows-fr">
              {visibleQuickLinks.map(card => (
                <div key={card.title} className="card-lift h-full">
                  <QuickLinkCardComponent card={card} />
                </div>
              ))}
            </div>
          </div>

        </div>
      </main>
    </div>
  );
}

// DashboardPage.tsx
import React, { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, UserPermissionsDTO, UserProfileDTO } from '../../api/authApi';

// ─── Types ────────────────────────────────────────────────────────────────────

interface NavItem {
  label: string;
  icon: string;
  href: string;
  active?: boolean;
  memberOnly?: boolean;
}

interface QuickLinkCard {
  icon: string;
  title: string;
  subtitle: string;
  href: string;
  memberOnly?: boolean;
}

// ─── Data ─────────────────────────────────────────────────────────────────────

const NAV_ITEMS: NavItem[] = [
  { label: 'Home',          icon: 'home',                 href: '/home',   active: true },
  { label: 'Events',        icon: 'event',                href: '/events' },
  { label: 'My Order',      icon: 'shopping_cart',        href: '/activeorder/' },
  { label: 'Order History', icon: 'history',              href: '#',       memberOnly: true },
  { label: 'Notifications', icon: 'notifications',        href: '#',       memberOnly: true },
  { label: 'My Companies',  icon: 'business',             href: '#',       memberOnly: true },
  { label: 'Admin Panel',   icon: 'admin_panel_settings', href: '#',       memberOnly: true },
];

const QUICK_LINKS: QuickLinkCard[] = [
  {
    icon: 'explore',
    title: 'Browse Events',
    subtitle: 'Discover trending and upcoming events worldwide.',
    href: '/events',
  },
  {
    icon: 'shopping_bag',
    title: 'Active Order',
    subtitle: 'View status of your current pending transactions.',
    href: '/activeorder/',
  },
  {
    icon: 'receipt_long',
    title: 'Order History',
    subtitle: 'Review past purchases and download invoices.',
    href: '#',
    memberOnly: true,
  },
  {
    icon: 'notifications_active',
    title: 'Notifications',
    subtitle: 'Stay updated with real-time event alerts.',
    href: '#',
    memberOnly: true,
  },
  {
    icon: 'monitoring',
    title: 'My Companies',
    subtitle: 'Manage your production companies and events.',
    href: '#',
    memberOnly: true,
  },
  {
    icon: 'admin_panel_settings',
    title: 'Admin Panel',
    subtitle: 'Manage system settings and user permissions.',
    href: '#',
    memberOnly: true,
  },
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
        className="relative flex items-center px-6 py-3 rounded-lg border-l-4 border-[#3980f4] bg-[#e5eeff] text-[#3980f4] font-bold transition-opacity duration-200"
      >
        <MaterialIcon name={item.icon} className="mr-4" />
        <span className="text-sm leading-5">{item.label}</span>
      </Link>
    );
  }
  return (
    <Link
      to={item.href}
      className="flex items-center px-6 py-3 rounded-lg text-[#5c5f61] hover:bg-[#e0e3e5] hover:text-[#444749] transition-colors duration-200"
    >
      <MaterialIcon name={item.icon} className="mr-4" />
      <span className="text-sm leading-5">{item.label}</span>
    </Link>
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
  const searchRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearch = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && searchQuery.trim()) {
      navigate(`/events?search=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

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
        // Run both calls in parallel
        const [perms, profile] = await Promise.all([
          authApi.getPermissions(token),
          authApi.getCurrentUser(token),
        ]);
        setPermissions(perms);
        setUsername(profile.name); // use the display name, not the userId
      } catch (error: any) {
        console.error('[Dashboard] Failed to load user data:', error.message);
        // Invalid/expired token → guest mode
        setPermissions(null);
        setUsername(null);
      } finally {
        setLoading(false);
      }
    };

    loadUserData();
  }, []);

  const isGuest = !username;

  const handleLogout = async () => {
    const token = localStorage.getItem('token');
    if (!token || !permissions?.userId) return;
    try {
      await authApi.logout(token, permissions.userId);
    } catch (e) {
      // even if the call fails, clear local state and redirect
    } finally {
      localStorage.removeItem('token');
      window.location.reload();    
    }
  };

  // Filter nav items and quick links based on guest/member mode
  const visibleNavItems = NAV_ITEMS.filter(item => !item.memberOnly || !isGuest);
  
  // Admin Panel: only show if user is actually an admin
  const visibleNavItemsFiltered = visibleNavItems.filter(item =>
    item.label !== 'Admin Panel' || (permissions?.isAdmin ?? false)
  );

  const visibleQuickLinks = QUICK_LINKS
    .filter(card => !card.memberOnly || !isGuest)
    .filter(card => card.title !== 'Admin Panel' || (permissions?.isAdmin ?? false));

  // Initials for avatar
  const initials = username
    ? username.slice(0, 2).toUpperCase()
    : 'G'; // Guest

  // ⌘K / Ctrl+K focuses the search bar
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        searchRef.current?.focus();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

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
  }, [loading]); // re-run after loading so cards are in the DOM

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
      {/* ── Google Fonts & Material Symbols ── */}
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Geist:wght@100..900&family=Geist+Mono:wght@100..900&display=swap');
        @import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap');
      `}</style>

      {/* ════════════════════════════════════════
          SIDEBAR
      ════════════════════════════════════════ */}
      <aside className="fixed left-0 top-0 h-full w-[260px] bg-white border-r border-[#c6c6cd] flex flex-col py-6 z-50">
        {/* Logo */}
        <div className="px-6 mb-10">
          <h1 className="text-2xl font-semibold leading-8 text-[#0b1c30] flex items-center gap-2">
            <MaterialIcon name="confirmation_number" className="text-[#3980f4]" />
            TicketFlow
          </h1>
          <p className="text-[#5c5f61] text-xs leading-4 tracking-wide mt-1">Event Management</p>
        </div>

        {/* Nav Links */}
        <nav className="flex-1 space-y-1 px-2">
          {visibleNavItemsFiltered.map(item => (
            <SidebarNavItem key={item.label} item={item} />
          ))}
        </nav>

        {/* Guest mode indicator */}
        {isGuest && (
          <div className="px-6 mt-auto">
            <div className="bg-[#eff4ff] border border-[#c6c6cd] rounded-lg p-3 text-center">
              <p className="text-xs text-[#5c5f61] mb-2">Browsing as guest</p>
              <Link
                to="/login"
                className="block w-full bg-[#3980f4] text-white text-xs font-bold py-2 rounded-lg hover:opacity-90 transition-opacity"
              >
                Sign In
              </Link>
            </div>
          </div>
        )}
      </aside>

      {/* ════════════════════════════════════════
          TOP NAV
      ════════════════════════════════════════ */}
      <header className="fixed top-0 right-0 w-[calc(100%-260px)] h-16 bg-white border-b border-[#c6c6cd] flex items-center justify-between px-6 z-40">
        {/* Search */}
        <div className="flex items-center flex-1 max-w-xl">
          <div className="relative w-full">
            <MaterialIcon name="search" className="absolute left-3 top-1/2 -translate-y-1/2 text-[#76777d]" />
            <input
              ref={searchRef}
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={handleSearch}
              placeholder="Search events..."
              className="w-full bg-[#eff4ff] border-none rounded-lg pl-10 pr-16 py-2 focus:ring-2 focus:ring-[#3980f4] outline-none transition-all text-sm"
            />
            <kbd className="absolute right-3 top-1/2 -translate-y-1/2 font-mono text-xs text-[#76777d] bg-[#e5eeff] px-1.5 py-0.5 rounded border border-[#c6c6cd]">
              ⌘K
            </kbd>
          </div>
        </div>

        {/* Right controls */}
        <div className="flex items-center gap-2">
          {/* Notification bell — only for members */}
          {!isGuest && (
            <button className="p-2 text-[#5c5f61] hover:text-[#3980f4] transition-colors relative">
              <MaterialIcon name="notifications" />
              <span className="absolute top-2 right-2 w-2 h-2 bg-[#ba1a1a] rounded-full border-2 border-white" />
            </button>
          )}

          {/* Settings gear — only for members */}
          {!isGuest && (
            <button className="p-2 text-[#5c5f61] hover:text-[#3980f4] transition-colors">
              <MaterialIcon name="settings" />
            </button>
          )}

          {/* User name + avatar  OR  guest login link */}
          <div className="flex items-center gap-3 pl-3 border-l border-[#c6c6cd]">
            {isGuest ? (
              <Link
                to="/login"
                className="text-sm font-bold text-[#3980f4] hover:underline"
              >
                Sign In
              </Link>
            ) : (
              <>
                <p className="text-sm font-bold text-[#0b1c30] hidden sm:block">{username}</p>
                <div className="w-10 h-10 rounded-full bg-[#3980f4] flex items-center justify-center text-white font-bold text-sm border border-[#c6c6cd]">
                  {initials}
                </div>
                <button
                  onClick={handleLogout}
                  className="p-2 text-[#5c5f61] hover:text-[#ba1a1a] transition-colors"
                  title="Logout"
                >
                  <MaterialIcon name="logout" />
                </button>
              </>
            )}
          </div>
        </div>
      </header>

      {/* ════════════════════════════════════════
          MAIN CONTENT
      ════════════════════════════════════════ */}
      <main className="ml-[260px] pt-16 min-h-screen">
        <div className="max-w-[1280px] mx-auto p-10">

          {/* ── Welcome Banner (members only) ── */}
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

          {/* ── Guest banner ── */}
          {isGuest && (
            <section className="relative mb-10 overflow-hidden rounded-xl bg-[#eff4ff] p-10 border border-[#c6c6cd]">
              <div className="relative z-10">
                <h2 className="text-3xl font-semibold leading-10 text-[#0b1c30]">Welcome to TicketFlow</h2>
                <p className="text-[#5c5f61] text-base mt-2">
                  You're browsing as a guest.{' '}
                  <Link to="/login" className="text-[#3980f4] font-bold hover:underline">
                    Sign in
                  </Link>{' '}
                  or{' '}
                  <Link to="/register" className="text-[#3980f4] font-bold hover:underline">
                    create an account
                  </Link>{' '}
                  to access your orders, companies, and more.
                </p>
              </div>
            </section>
          )}

          {/* ── Quick Access Grid ── */}
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
// AccountSettingsPage.tsx
import React, { useEffect, useRef, useState, ChangeEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi, UserPermissionsDTO } from '../../api/authApi';
import { getUserFriendlyError } from '../../utils/errorUtils';

// ─── Types ────────────────────────────────────────────────────────────────────

interface NavItem {
  label: string;
  icon: string;
  href: string;
  active?: boolean;
  memberOnly?: boolean;
}

interface UserProfileData {
  username: string;
  email: string;
}

// ─── Data ─────────────────────────────────────────────────────────────────────

const NAV_ITEMS: NavItem[] = [
  { label: 'Home',          icon: 'home',                 href: '/home' },
  { label: 'Events',        icon: 'event',                href: '/events' },
  { label: 'My Order',      icon: 'shopping_cart',        href: '/activeorder/' },
  { label: 'Order History', icon: 'history',              href: '#',       memberOnly: true },
  { label: 'Notifications', icon: 'notifications',        href: '#',       memberOnly: true },
  { label: 'My Companies',  icon: 'business',             href: '#',       memberOnly: true },
  { label: 'Admin Panel',   icon: 'admin_panel_settings', href: '#',       memberOnly: true, active: true },
];

function getAccountSaveError(error: unknown): string {
  const message = getUserFriendlyError(error);

  if (/invalid email format/i.test(message)) {
    return 'Enter a valid email address.';
  }
  if (/old password does not match current password/i.test(message)) {
    return 'Current password is incorrect.';
  }
  if (/password cannot be empty/i.test(message)) {
    return 'Enter a new password before saving.';
  }
  if (/password must be at least 7 characters/i.test(message)) {
    return 'Password must be at least 7 characters long and include both letters and numbers.';
  }

  return message || 'We could not save your account changes. Please try again.';
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function MaterialIcon({ name, className = '', style = {} }: { name: string; className?: string; style?: React.CSSProperties }) {
  return (
    <span
      className={`material-symbols-outlined ${className}`}
      style={{ fontVariationSettings: "'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24", ...style }}
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

// ─── Main Component ───────────────────────────────────────────────────────────

export default function AccountSettingsPage() {
  const searchRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();
  
  // App Shell States
  const [searchQuery, setSearchQuery] = useState('');
  const [permissions, setPermissions] = useState<UserPermissionsDTO | null>(null);
  const [username, setUsername] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Core profile workspace form states
  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [profileData, setProfileData] = useState<UserProfileData>({
    username: '',
    email: ''
  });
  const [tempProfileData, setTempProfileData] = useState<UserProfileData>({ ...profileData });
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState<string | null>(null);

  // Password fields state
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmNewPassword: ''
  });

  // Password visibility toggles
  const [showCurrentPass, setShowCurrentPass] = useState<boolean>(false);
  const [showNewPass, setShowNewPass] = useState<boolean>(false);
  const [showConfirmPass, setShowConfirmPass] = useState<boolean>(false);

  // Auth synchronization lifecycle hooks
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
        
        // Seed the profile forms directly with authentic user database values
        const fetchedProfile = {
          username: profile.name,
          email: profile.email
        };
        setProfileData(fetchedProfile);
        setTempProfileData(fetchedProfile);
      } catch (error: any) {
        console.error('[Settings] Failed to load user security context:', error.message);
        setPermissions(null);
        setUsername(null);
      } finally {
        setLoading(false);
      }
    };

    loadUserData();
  }, []);

  const isGuest = !username;

  // Global action listener hooks
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

  // Form Change Handlers
  const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    const { id, value } = e.target;
    setSaveError(null);
    setSaveSuccess(null);
    if (id === 'input-username') {
      setTempProfileData(prev => ({ ...prev, username: value }));
    } else if (id === 'input-email') {
      setTempProfileData(prev => ({ ...prev, email: value }));
    }
  };

  const handlePasswordChange = (e: ChangeEvent<HTMLInputElement>, field: keyof typeof passwordForm) => {
    setSaveError(null);
    setSaveSuccess(null);
    setPasswordForm(prev => ({ ...prev, [field]: e.target.value }));
  };

  const handleToggleEdit = () => {
    setSaveError(null);
    setSaveSuccess(null);
    if (isEditing) {
      setTempProfileData({ ...profileData });
      setIsEditing(false);
    } else {
      setIsEditing(true);
    }
  };

  const handleCancel = () => {
    setTempProfileData({ ...profileData });
    setPasswordForm({ currentPassword: '', newPassword: '', confirmNewPassword: '' });
    setSaveError(null);
    setSaveSuccess(null);
    setIsEditing(false);
  };

  const handleSave = async () => {
    const token = localStorage.getItem('token');
    if (!token) return;
    setSaveError(null);
    setSaveSuccess(null);

    // 1. Destructure the password form values
    const { currentPassword, newPassword, confirmNewPassword } = passwordForm;
    
    // Check if the user has typed anything into any password field
    const isChangingPassword = currentPassword || newPassword || confirmNewPassword;

    // 2. Validate password fields if a password change is being attempted
    if (isChangingPassword) {
      if (!currentPassword || !newPassword || !confirmNewPassword) {
        setSaveError('Complete all password fields to update your password.');
        return;
      }
      if (newPassword !== confirmNewPassword) {
        setSaveError('New password and confirmation do not match.');
        return;
      }
    }

    try {
      // 3. Step 1: If changing password, call the updated backend endpoint
      if (isChangingPassword) {
        await authApi.editPassword(token, {
          currentPassword: currentPassword, 
          newPassword: newPassword
        });
      }

      // 4. Step 2: Update username and email profile details
      await authApi.updateProfile(token, {
        name: tempProfileData.username,
        email: tempProfileData.email
      });

      // 5. Step 3: Synchronize UI states upon complete success
      setProfileData({ ...tempProfileData });
      setUsername(tempProfileData.username);
      setPasswordForm({ currentPassword: '', newPassword: '', confirmNewPassword: '' });
      setIsEditing(false);
      setSaveSuccess('Your account changes were saved.');
    } catch (error: any) {
      console.error('[Settings] Update failed:', error.message);
      setSaveError(getAccountSaveError(error));
    }
  };

  const handleSearch = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && searchQuery.trim()) {
      navigate(`/events?search=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  const handleLogout = async () => {
    const token = localStorage.getItem('token');
    if (!token || !permissions?.userId) return;
    try {
      await authApi.logout(token, permissions.userId);
    } catch (e) {
      // safe fallback redirection
    } finally {
      localStorage.removeItem('token');
      navigate('/dashboard');
      // window.location.reload();
    }
  };
  // const handlePasswordUpdate = async () =>
  // Nav security filters
  const visibleNavItems = NAV_ITEMS.filter(item => !item.memberOnly || !isGuest);
  const visibleNavItemsFiltered = visibleNavItems.filter(item =>
    item.label !== 'Admin Panel' || (permissions?.isAdmin ?? false)
  );

  const initials = username ? username.slice(0, 2).toUpperCase() : 'G';
  const hasEmailError = Boolean(saveError && /email/i.test(saveError));

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#f8f9ff]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-4 border-[#3980f4] border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-[#5c5f61]">Loading configuration settings...</p>
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

      {/* ════════════════════════════════════════
          SIDEBAR
      ════════════════════════════════════════ */}
      <aside className="fixed left-0 top-0 h-full w-[260px] bg-white border-r border-[#c6c6cd] flex flex-col py-6 z-50">
        <div className="px-6 mb-10">
          <h1 className="text-2xl font-semibold leading-8 text-[#0b1c30] flex items-center gap-2">
            <MaterialIcon name="confirmation_number" className="text-[#3980f4]" />
            TicketFlow
          </h1>
          <p className="text-[#5c5f61] text-xs leading-4 tracking-wide mt-1">Event Management</p>
        </div>

        <nav className="flex-1 space-y-1 px-2">
          {visibleNavItemsFiltered.map(item => (
            <SidebarNavItem key={item.label} item={item} />
          ))}
        </nav>

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

        <div className="flex items-center gap-2">
          {!isGuest && (
            <button className="p-2 text-[#5c5f61] hover:text-[#3980f4] transition-colors">
              <MaterialIcon name="settings" />
            </button>
          )}

          <div className="flex items-center gap-3 pl-3 border-l border-[#c6c6cd]">
            {isGuest ? (
              <Link to="/login" className="text-sm font-bold text-[#3980f4] hover:underline">
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
          WORKSPACE PANEL CONTENT
      ════════════════════════════════════════ */}
      <main className="ml-[260px] pt-16 min-h-screen block">
        <div className="max-w-[1280px] mx-auto p-10">
          
          <div className="flex items-baseline justify-between mb-8">
            <div>
              <h2 className="text-3xl font-semibold tracking-tight text-[#0b1c30]">Account Settings</h2>
              <p className="text-sm text-[#5c5f61] mt-1">Manage your identity profile info and systemic credentials.</p>
            </div>
          </div>

          <div className="grid grid-cols-12 gap-6 items-start">
            
            {/* Left Frame column: Profile Summary card */}
            <div className="col-span-12 lg:col-span-4 xl:col-span-3 space-y-6">
              <div className="bg-white p-6 rounded-xl border border-[#c6c6cd] flex flex-col items-center text-center" style={{ boxShadow: '0px 1px 3px rgba(0,0,0,0.05)' }}>
                <div className="relative mb-4">
                  <div className="w-24 h-24 rounded-full bg-[#3980f4] flex items-center justify-center text-white font-bold text-3xl border border-[#c6c6cd]">
                    {initials}
                  </div>
                  {!isGuest && (
                    <span className="absolute bottom-1 right-1 bg-[#3980f4] text-white p-1 rounded-full flex items-center justify-center border-2 border-white" title="Verified Account Tier">
                      <MaterialIcon name="verified" className="text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }} />
                    </span>
                  )}
                </div>
                <h3 className="text-lg font-bold text-[#0b1c30]">{username || 'Guest Identity'}</h3>
                <p className="text-[10px] font-bold text-[#3980f4] bg-[#e5eeff] px-2.5 py-1 rounded-full mb-4 uppercase tracking-wider">
                  {isGuest ? 'Public Visitor' : 'Verified Member'}
                </p>
                
                <div className="w-full space-y-1 pt-6 border-t border-[#c6c6cd]">
                  <a className="flex items-center justify-between w-full p-3 rounded-lg text-[#5c5f61] hover:bg-[#e0e3e5] hover:text-[#444749] transition-colors group" href="#complaints">
                    <div className="flex items-center gap-3">
                      <MaterialIcon name="support_agent" />
                      <span className="text-sm">Complaints Hub</span>
                    </div>
                    <MaterialIcon name="chevron_right" className="text-[#76777d] group-hover:translate-x-1 transition-transform" />
                  </a>
                </div>
              </div>
            </div>

            {/* Right Form Workspace */}
            <div className="col-span-12 lg:col-span-8 xl:col-span-9">
              <div className="bg-white rounded-xl border border-[#c6c6cd] overflow-hidden flex flex-col" style={{ boxShadow: '0px 1px 3px rgba(0,0,0,0.05)' }}>
                
                {/* Panel Title Header */}
                <div className="px-6 py-5 border-b border-[#c6c6cd] flex items-center justify-between bg-[#f8f9ff]">
                  <div>
                    <h3 className="text-base font-bold text-[#0b1c30]">Personal Information</h3>
                    <p className="text-xs text-[#5c5f61] mt-0.5">Update profile details</p>
                  </div>
                  {!isGuest && (
                    <button 
                      onClick={handleToggleEdit}
                      className="flex items-center gap-2 px-4 py-2 border border-[#c6c6cd] bg-white rounded-lg text-xs font-bold text-[#444749] hover:bg-[#e0e3e5] transition-colors focus:ring-2 focus:ring-[#3980f4]"
                    >
                      <MaterialIcon name={isEditing ? 'close' : 'edit'} className="text-[18px]" />
                      <span>{isEditing ? 'Cancel' : 'Edit Details'}</span>
                    </button>
                  )}
                </div>

                {(saveError || saveSuccess) && (
                  <div
                    role={saveError ? 'alert' : 'status'}
                    className={`mx-6 mt-6 flex items-start gap-3 rounded-lg border p-4 ${
                      saveError
                        ? 'border-[#f2b8b5] bg-[#fff8f7] text-[#410002]'
                        : 'border-[#b8d7c0] bg-[#f7fff9] text-[#0d2818]'
                    }`}
                  >
                    <MaterialIcon
                      name={saveError ? 'error' : 'check_circle'}
                      className={`mt-0.5 text-[20px] ${saveError ? 'text-[#ba1a1a]' : 'text-[#247a3d]'}`}
                      style={{ fontVariationSettings: "'FILL' 1" }}
                    />
                    <div>
                      <p className="text-sm font-bold">
                        {saveError ? "Couldn't save changes" : 'Changes saved'}
                      </p>
                      <p className="text-sm leading-5 mt-1">
                        {saveError || saveSuccess}
                      </p>
                    </div>
                  </div>
                )}

                {/* Form Controls Workspace */}
                <div className="p-6 space-y-6">
                  
                  {/* Row 1: Username & Email */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="space-y-2">
                      <label className="block text-xs font-bold text-[#444749] flex justify-between" htmlFor="input-username">
                        <span>Username</span>
                      </label>
                      <div className="relative">
                        <input 
                          id="input-username"
                          type="text" 
                          readOnly={!isEditing}
                          value={isEditing ? tempProfileData.username : profileData.username}
                          onChange={handleInputChange}
                          className={`w-full px-4 py-2.5 border rounded-lg text-sm text-[#0b1c30] focus:ring-2 focus:ring-[#3980f4] focus:outline-none transition-all ${
                            isEditing 
                              ? 'bg-white border-[#3980f4]' 
                              : 'bg-[#eff4ff] border-transparent opacity-75 cursor-not-allowed'
                          }`}
                        />
                        <MaterialIcon name="check_circle" className="absolute right-3 top-1/2 -translate-y-1/2 text-[#3980f4]" style={{ fontVariationSettings: "'FILL' 1" }} />
                      </div>
                    </div>

                    <div className="space-y-2">
                      <label className="block text-xs font-bold text-[#444749] flex justify-between" htmlFor="input-email">
                        <span>Email Address</span>
                      </label>
                      <div className="relative">
                        <input 
                          id="input-email"
                          type="email" 
                          readOnly={!isEditing}
                          value={isEditing ? tempProfileData.email : profileData.email}
                          onChange={handleInputChange}
                          className={`w-full px-4 py-2.5 border rounded-lg text-sm text-[#0b1c30] focus:ring-2 focus:ring-[#3980f4] focus:outline-none transition-all ${
                            isEditing 
                              ? hasEmailError
                                ? 'bg-white border-[#ba1a1a] focus:ring-[#ba1a1a]'
                                : 'bg-white border-[#3980f4]'
                              : 'bg-[#eff4ff] border-transparent opacity-75 cursor-not-allowed'
                          }`}
                        />
                        <MaterialIcon
                          name={hasEmailError ? 'error' : 'check_circle'}
                          className={`absolute right-3 top-1/2 -translate-y-1/2 ${hasEmailError ? 'text-[#ba1a1a]' : 'text-[#3980f4]'}`}
                          style={{ fontVariationSettings: "'FILL' 1" }}
                        />
                      </div>
                      {hasEmailError && (
                        <p className="text-xs font-medium text-[#ba1a1a]">
                          Enter a valid email address.
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Row 2: Secured Credentials Inputs Area */}
                  <div className="space-y-2">
                    <label className="block text-xs font-bold text-[#444749] flex justify-between">
                      <span>Password</span>
                    </label>

                    {/* Masked View Mode */}
                    {!isEditing && (
                      <div className="flex items-center justify-between p-4 bg-[#eff4ff] rounded-lg border border-transparent">
                        <div className="flex items-center gap-3">
                          <MaterialIcon name="lock" className="text-[#5c5f61]" />
                          <span className="text-sm text-[#0b1c30] tracking-widest">••••••••••••</span>
                        </div>
                      </div>
                    )}

                    {/* Active Form Edit Fields Mode */}
                    {isEditing && (
                      <div className="space-y-4 pt-1">
                        <div className="relative">
                          <input 
                            type={showCurrentPass ? 'text' : 'password'} 
                            placeholder="Current Password"
                            value={passwordForm.currentPassword}
                            onChange={(e) => handlePasswordChange(e, 'currentPassword')}
                            className="w-full px-4 py-2.5 bg-white border border-[#c6c6cd] rounded-lg text-sm text-[#0b1c30] focus:ring-2 focus:ring-[#3980f4] outline-none"
                          />
                          <button 
                            type="button" 
                            onClick={() => setShowCurrentPass(!showCurrentPass)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-[#5c5f61] hover:text-[#3980f4]"
                          >
                            <MaterialIcon name={showCurrentPass ? 'visibility_off' : 'visibility'} className="text-[20px]" />
                          </button>
                        </div>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div className="relative">
                            <input 
                              type={showNewPass ? 'text' : 'password'} 
                              placeholder="New Password"
                              value={passwordForm.newPassword}
                              onChange={(e) => handlePasswordChange(e, 'newPassword')}
                              className="w-full px-4 py-2.5 bg-white border border-[#c6c6cd] rounded-lg text-sm text-[#0b1c30] focus:ring-2 focus:ring-[#3980f4] outline-none"
                            />
                            <button 
                              type="button" 
                              onClick={() => setShowNewPass(!showNewPass)}
                              className="absolute right-3 top-1/2 -translate-y-1/2 text-[#5c5f61] hover:text-[#3980f4]"
                            >
                              <MaterialIcon name={showNewPass ? 'visibility_off' : 'visibility'} className="text-[20px]" />
                            </button>
                          </div>
                          
                          <div className="relative">
                            <input 
                              type={showConfirmPass ? 'text' : 'password'} 
                              placeholder="Confirm New Password"
                              value={passwordForm.confirmNewPassword}
                              onChange={(e) => handlePasswordChange(e, 'confirmNewPassword')}
                              className="w-full px-4 py-2.5 bg-white border border-[#c6c6cd] rounded-lg text-sm text-[#0b1c30] focus:ring-2 focus:ring-[#3980f4] outline-none"
                            />
                            <button 
                              type="button" 
                              onClick={() => setShowConfirmPass(!showConfirmPass)}
                              className="absolute right-3 top-1/2 -translate-y-1/2 text-[#5c5f61] hover:text-[#3980f4]"
                            >
                              <MaterialIcon name={showConfirmPass ? 'visibility_off' : 'visibility'} className="text-[20px]" />
                            </button>
                          </div>
                        </div>
                        
                        <div className="bg-[#eff4ff] p-3 rounded-lg border-l-4 border-[#3980f4]">
                          <p className="text-xs text-[#444749] leading-relaxed">
                            Password must be at least 7 characters long and include both letters and numbers.
                          </p>
                        </div>
                      </div>
                    )}
                  </div>
                </div>

                {/* Submitting Actions Form Footer Panel */}
                <div className="px-6 py-4 bg-[#f8f9ff] border-t border-[#c6c6cd] flex justify-end gap-3 min-h-[65px]">
                  {isEditing && (
                    <>
                      <button 
                        onClick={handleCancel}
                        className="px-5 py-2 rounded-lg text-xs font-bold text-[#5c5f61] hover:bg-[#e0e3e5] transition-colors"
                      >
                        Cancel
                      </button>
                      <button 
                        onClick={handleSave}
                        className="px-6 py-2 bg-[#3980f4] text-white rounded-lg text-xs font-bold hover:opacity-90 transition-opacity"
                      >
                        Save Changes
                      </button>
                    </>
                  )}
                </div>

              </div>
            </div>

          </div>
        </div>
      </main>
    </div>
  );
}

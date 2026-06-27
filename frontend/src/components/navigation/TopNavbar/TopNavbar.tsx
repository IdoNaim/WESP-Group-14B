import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { useState } from 'react';
import logo from '../../../assets/Logo.png';
import "./TopNavbar.scss";

export default function TopNavbar() {
    const location = useLocation();
    const navigate = useNavigate();

    const { isGuest, isMember, isProductionUser, isAdmin, logout } = useAuth();
    
    const [isLoggingOut, setIsLoggingOut] = useState(false);
    const [logoutError, setLogoutError] = useState<string | null>(null);

    const returnTo = location.pathname + location.search;

    const goToLogin = () => {
        navigate(`/login`, {
            state: { returnTo },
        });
    }
        
    const goToRegister = () => {
        navigate(`/register`, {
            state: { returnTo },
        });
    }

    const handleLogout = async () => {
        setLogoutError(null);
        setIsLoggingOut(true);

        try {
            await logout();
            window.location.href = '/dashboard';
        }
        catch (error: any) {
            console.error('Logout failed:', error);
            setLogoutError(error.message || 'An error occurred during logout. Please try again.');
        }
        finally {
            setIsLoggingOut(false);
        }
    };

    return (
        <header className="top-navbar">
            <Link to="/dashboard" className="top-navbar__brand">
                <img src={logo} alt="Idodo Tickets" className="top-navbar__logo" />
            </Link>

            <div className="top-navbar__actions">
                { logoutError && (
                    <span className="top-navbar__error">{logoutError}</span>
                )}

                {isGuest && (
                    <>
                        <span className="top-navbar__status">Browsing as Guest</span>

                        <button
                            type="button"
                            onClick={goToLogin}
                            className="top-navbar__button"
                        >
                            Login
                        </button>

                        <button
                            type="button"
                            onClick={goToRegister}
                            className="top-navbar__button top-navbar__button--primary"
                        >
                            Register
                        </button>
                    </>
                )}

                {isMember && (
                    <>
                        {isProductionUser && (
                            <span className="top-navbar__status top-navbar__status--production">
                                Production User
                            </span>
                        )}

                        {isAdmin && (
                            <span className="top-navbar__status top-navbar__status--admin">
                                Admin
                            </span>
                        )}

                        <Link to="/account" className="top-navbar__link-button">
                            Account
                        </Link>

                        <button
                            type="button"
                            onClick={handleLogout}
                            disabled={isLoggingOut}
                            className="top-navbar__button top-navbar__button--primary"
                        >
                            {isLoggingOut ? 'Logging out...' : 'Logout'}
                        </button>
                    </>
                )}
            </div>
        </header>
    );
}
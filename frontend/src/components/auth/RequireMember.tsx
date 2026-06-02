import { useLocation, Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function RequireMember() {
    const location = useLocation();
    const { isMember, loading } = useAuth();

    if (loading) {
        return null;
    }

    if (!isMember) {
        return (
            <Navigate 
                to="/login"
                replace
                state={{ returnTo: location.pathname + location.search }}
            />
        );
    }

    return <Outlet />;
}
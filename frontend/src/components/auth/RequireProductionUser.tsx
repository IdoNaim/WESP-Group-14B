import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function RequireProductionUser() {
    const { isProductionUser, loading } = useAuth();

    if (loading) {
        return null;
    }

    if (!isProductionUser) {
        return <Navigate to="/dashboard" replace />;
    }

    return <Outlet />;
}

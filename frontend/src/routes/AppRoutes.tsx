import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from '../pages/auth/LoginPage';
import RegisterPage from '../pages/auth/RegisterPage';
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage.tsx';
import ProductionCompanyPage from '../pages/ProductionCompanyPage';
import EventsPage from "../pages/events/EventsPage.tsx";
import EventDetailsPage from "../pages/events/EventDetailsPage.tsx";
import DashboardPage from '../pages/dashboard/DashboardPage.tsx';
import ActiveOrderPage from '../pages/orders/ActiveOrderPage.tsx';


export default function AppRoutes() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                <Route path="/company" element={<ProductionCompanyPage />} />
                {/* Wildcard: Redirects any unknown paths straight to login */}
                <Route path="/events" element={<EventsPage />} />
                <Route path="/events/:eventId" element={<EventDetailsPage />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
                <Route path="/home/" element={<DashboardPage />} />
                <Route path="/activeorder/" element={<ActiveOrderPage />} />
                
            </Routes>
        </BrowserRouter>
    );
}
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from '../pages/auth/LoginPage';
import RegisterPage from '../pages/auth/RegisterPage';
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage.tsx';
import ProductionCompanyPage from '../pages/production-company/ProductionCompanyPage.tsx';
import MyCompaniesPage from '../pages/production-company/MyCompaniesPage.tsx';
import CompanyEventsPage from '../pages/production-company/CompanyEventsPage.tsx';
import EventsPage from "../pages/events/EventsPage.tsx";
import EventDetailsPage from "../pages/events/EventDetailsPage.tsx";


export default function AppRoutes() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                <Route path="/dashboard" element={<MyCompaniesPage />} />
                <Route path="/company/:companyId" element={<ProductionCompanyPage />} />
                <Route path="/company/:companyId/events" element={<CompanyEventsPage />} />
                {/* Wildcard: Redirects any unknown paths straight to login */}
                <Route path="/events" element={<EventsPage />} />
                <Route path="/events/:eventId" element={<EventDetailsPage />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
}
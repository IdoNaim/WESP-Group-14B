import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

import MainLayout from "../layouts/MainLayout/MainLayout";

import LoginPage from "../pages/auth/LoginPage";
import RegisterPage from "../pages/auth/RegisterPage";
import ForgotPasswordPage from "../pages/auth/ForgotPasswordPage";

import DashboardPage from "../pages/dashboard/DashboardPage";

import EventsPage from "../pages/events/EventsPage";
import EventDetailsPage from "../pages/events/EventDetailsPage";

import ActiveOrderPage from "../pages/orders/ActiveOrderPage";
import OrderHistoryPage from "../pages/orders/OrderHistoryPage";
import ReserveTicketPage from "../pages/orders/ReserveTicketPage";

import NotificationsPage from "../pages/notifications/NotificationsPage";

import ProductionCompaniesPage from "../pages/production-company/MyCompaniesPage";
import PurchasePolicyPage from "../pages/policies/PurchasePolicyPage";

import AdminPage from "../pages/admin/AdminPage";
import AccountPage from "../pages/user/AccountPage";

import RequireMember from "../components/auth/RequireMember";
import RequireProductionUser from "../components/auth/RequireProductionUser";
import RequireAdmin from "../components/auth/RequireAdmin";

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Auth pages - outside the main app shell */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />

        {/* Main application shell */}
        <Route element={<MainLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />

          {/* Public routes - guests and members */}
          <Route path="/events" element={<EventsPage />} />
          <Route path="/events/:eventId" element={<EventDetailsPage />} />
          {/* <Route path="reserve/:eventId/:userId" element={<ReserveTicketPage/>} /> */}
          <Route
            path="/events/:eventId/reserve"
            element={<ReserveTicketPage />}
          />

          <Route path="/orders/active" element={<ActiveOrderPage />} />

          {/* Member-only routes */}
          <Route element={<RequireMember />}>
            <Route path="/account" element={<AccountPage />} />
            <Route path="/orders/history" element={<OrderHistoryPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
          </Route>

          {/* Production-user-only routes */}
          {/* <Route element={<RequireProductionUser />}> */}
          <Route
            path="/production-company"
            element={<ProductionCompaniesPage />}
          />
          <Route path="/policies" element={<PurchasePolicyPage />} />
          {/* </Route> */}

          {/* Admin-only routes */}
          <Route element={<RequireAdmin />}>
            <Route path="/admin" element={<AdminPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
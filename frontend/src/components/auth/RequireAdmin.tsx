import { Outlet } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import ForbiddenAccess from "./ForbiddenAccess/ForbiddenAccess";

export default function RequireAdmin() {
  const { isAdmin, loading } = useAuth();

  if (loading) {
    return null;
  }

  if (!isAdmin) {
    return (
      <ForbiddenAccess
        title="Admin Access Required"
        message="This page is restricted to system administrators."
        requiredRole="System Admin"
      />
    );
  }

  return <Outlet />;
}
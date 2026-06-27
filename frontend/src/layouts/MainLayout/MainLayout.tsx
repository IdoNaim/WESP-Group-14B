import { Outlet } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import Sidebar from "../../components/navigation/Sidebar/Sidebar";
import TopNavbar from "../../components/navigation/TopNavbar/TopNavbar";
import "./MainLayout.scss";

export default function MainLayout() {
    const { loading, error } = useAuth();

    if (loading) {
        return (
            <div className="main-layout__loading">
                <div className="main-layout__loading-content">
                    <div className="main-layout__loading-title">Loading platform...</div>
                    <div className="main-layout__loading-subtitle">
                        Preparing your ticket session and fetching the latest events for you.
                    </div>
                </div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="main-layout__loading">
                <div className="main-layout__loading-content">
                    <div className="main-layout__loading-title">Service Unavailable</div>
                    <div className="main-layout__loading-subtitle">{error}</div>
                </div>
            </div>
        );
    }

    return (
        <div className="main-layout">
            <TopNavbar />
            <Sidebar />
            <main className="main-layout__content">
                <Outlet />
            </main>
        </div>
    );
}

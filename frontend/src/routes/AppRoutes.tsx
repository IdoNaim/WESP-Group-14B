import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

// 1. We import the pages we want to show
import LoginPage from '../pages/login/LoginPage';

export default function AppRoutes() {
    return (
        <BrowserRouter>
            <Routes>
                {/* 2. When the URL is exactly "/login", render the LoginPage component */}
                <Route path="/login" element={<LoginPage />} />

                {/* 3. Catch-all: If someone goes to a URL that doesn't exist, send them to /login */}
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
}
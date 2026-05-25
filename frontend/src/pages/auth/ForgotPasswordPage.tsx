import { Link } from "react-router-dom";

export default function ForgotPasswordPage() {
    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen flex flex-col items-center justify-center p-6 relative">
            <main className="w-full max-w-sm z-10 relative text-center bg-[#eeefff] text-[#171f33] p-8 rounded-xl">
                <span className="material-symbols-outlined text-5xl text-[#2563eb] mb-2">vpn_key</span>
                <h2 className="text-xl font-bold mb-2">Reset Access Key</h2>
                <p className="text-sm text-[#2d3449] mb-6">Contact your system administrator or check back soon for automated credential restoration.</p>
                <Link to="/login" className="text-[#2563eb] font-bold hover:underline">Return to Gate</Link>
            </main>
        </div>
    );
}
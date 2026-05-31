import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import * as api from '../../api/productionCompanyApi';
import { authApi } from '../../api/authApi';

const ROLE_COLOR: Record<api.CompanySummary['role'], string> = {
    FOUNDER: 'text-amber-400 bg-amber-400/10 border-amber-400/20',
    OWNER: 'text-[#60a5fa] bg-[#2563eb]/10 border-[#2563eb]/20',
    MANAGER: 'text-[#34d399] bg-emerald-500/10 border-emerald-500/20',
};

const ROLE_ICON: Record<api.CompanySummary['role'], string> = {
    FOUNDER: 'star',
    OWNER: 'manage_accounts',
    MANAGER: 'badge',
};

function CreateCompanyModal({ onClose, onCreated }: { onClose: () => void; onCreated: (id: number) => void }) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            const res = await api.createCompany(name, description, email);
            onCreated(Number(res.companyId));
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to create company');
        }
        setLoading(false);
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
            <div className="bg-[#171f33] border border-gray-700 rounded-2xl w-full max-w-md shadow-2xl">
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-700">
                    <h3 className="font-black text-white tracking-wide text-sm flex items-center gap-2">
                        <span className="material-symbols-outlined text-[#2563eb] text-[20px]">add_business</span>
                        CREATE NEW COMPANY
                    </h3>
                    <button onClick={onClose} className="text-gray-400 hover:text-white">
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>
                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    <div className="space-y-1">
                        <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400">Company Name</label>
                        <input
                            className="w-full bg-[#0b1326] border border-gray-600 rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[#2563eb] transition-colors"
                            placeholder="e.g. Awesome Events Co."
                            value={name}
                            onChange={e => setName(e.target.value)}
                            required
                        />
                    </div>
                    <div className="space-y-1">
                        <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400">Description</label>
                        <textarea
                            className="w-full bg-[#0b1326] border border-gray-600 rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[#2563eb] transition-colors resize-none"
                            placeholder="What does your company do?"
                            rows={3}
                            value={description}
                            onChange={e => setDescription(e.target.value)}
                            required
                        />
                    </div>
                    <div className="space-y-1">
                        <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-gray-400">Contact Email</label>
                        <input
                            className="w-full bg-[#0b1326] border border-gray-600 rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[#2563eb] transition-colors"
                            type="email"
                            placeholder="contact@company.com"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            required
                        />
                    </div>
                    {error && (
                        <p className="text-red-400 text-xs bg-red-500/10 border border-red-500/20 rounded-lg px-3 py-2">{error}</p>
                    )}
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 bg-[#2563eb] hover:bg-[#0053db] text-white font-bold text-sm rounded-xl transition-colors flex items-center justify-center gap-2 disabled:opacity-60"
                    >
                        {loading && <span className="material-symbols-outlined animate-spin text-[18px]">refresh</span>}
                        CREATE COMPANY
                    </button>
                </form>
            </div>
        </div>
    );
}

export default function MyCompaniesPage() {
    const navigate = useNavigate();
    const userId = localStorage.getItem('userId') ?? '';

    const handleLogout = async () => {
        const token = localStorage.getItem('token') ?? '';
        try { await authApi.logout(token, userId); } catch { /* ignore */ }
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        navigate('/login');
    };

    const [companies, setCompanies] = useState<api.CompanySummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showCreate, setShowCreate] = useState(false);

    useEffect(() => {
        (async () => {
            try {
                const list = await api.getMyCompanies();
                setCompanies(list);
            } catch (e) {
                setError(e instanceof Error ? e.message : 'Failed to load companies');
            }
            setLoading(false);
        })();
    }, []);

    const handleCompanyCreated = (companyId: number) => {
        navigate(`/company/${companyId}`);
    };

    if (loading) {
        return (
            <div className="bg-[#0b1326] min-h-screen flex items-center justify-center">
                <div className="flex items-center gap-3 text-[#dae2fd]">
                    <span className="material-symbols-outlined animate-spin text-3xl text-[#2563eb]">refresh</span>
                    <span className="font-mono text-sm tracking-widest">LOADING YOUR COMPANIES...</span>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-[#0b1326] min-h-screen flex items-center justify-center p-6">
                <div className="bg-[#171f33] border border-red-500/30 p-10 rounded-2xl text-center max-w-md">
                    <span className="material-symbols-outlined text-6xl text-red-500 mb-4 block">error</span>
                    <p className="text-sm text-gray-400 mb-6">{error}</p>
                    <button
                        onClick={() => navigate('/login')}
                        className="bg-[#2563eb] text-white px-6 py-3 rounded-lg font-bold text-sm"
                    >
                        RETURN TO LOGIN
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-[#0b1326] text-[#dae2fd] min-h-screen font-sans">
            {/* Header */}
            <div className="bg-[#eeefff] text-[#171f33] px-6 md:px-10 py-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <div className="flex items-center gap-2.5 mb-1">
                        <span className="material-symbols-outlined text-[#2563eb] text-3xl">domain</span>
                        <h1 className="text-2xl font-black tracking-tight text-[#0b1326]">MY COMPANIES</h1>
                    </div>
                    <p className="text-sm text-gray-500 font-mono ml-0.5">
                        Welcome back, <strong className="text-[#0b1326]">{userId}</strong>
                        {companies.length > 0 && (
                            <span> · {companies.length} {companies.length === 1 ? 'company' : 'companies'}</span>
                        )}
                    </p>
                </div>
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => setShowCreate(true)}
                        className="flex items-center gap-2 bg-[#2563eb] hover:bg-[#0053db] text-white px-5 py-2.5 rounded-xl font-bold text-sm tracking-wider transition-colors"
                    >
                        <span className="material-symbols-outlined text-[18px]">add_business</span>
                        NEW COMPANY
                    </button>
                    <button
                        onClick={handleLogout}
                        className="flex items-center gap-2 border border-gray-400 hover:border-red-400 text-gray-500 hover:text-red-500 px-4 py-2.5 rounded-xl font-bold text-sm tracking-wider transition-colors"
                    >
                        <span className="material-symbols-outlined text-[18px]">logout</span>
                        LOGOUT
                    </button>
                </div>
            </div>

            {/* Company grid */}
            <div className="p-4 md:p-8 max-w-5xl mx-auto">
                {companies.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-24 text-center">
                        <span className="material-symbols-outlined text-7xl text-gray-700 mb-4">domain_disabled</span>
                        <h2 className="text-xl font-black text-gray-400 mb-2">No companies yet</h2>
                        <p className="text-sm text-gray-600 font-mono mb-8">Create your first production company to get started.</p>
                        <button
                            onClick={() => setShowCreate(true)}
                            className="flex items-center gap-2 bg-[#2563eb] hover:bg-[#0053db] text-white px-6 py-3 rounded-xl font-bold text-sm tracking-wider transition-colors"
                        >
                            <span className="material-symbols-outlined text-[18px]">add_business</span>
                            CREATE COMPANY
                        </button>
                    </div>
                ) : (
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {companies.map(company => (
                            <button
                                key={company.companyId}
                                onClick={() => navigate(`/company/${company.companyId}`)}
                                className="bg-[#171f33] border border-gray-800 hover:border-[#2563eb]/50 rounded-2xl p-5 text-left transition-all group hover:shadow-lg hover:shadow-[#2563eb]/5"
                            >
                                <div className="flex items-start justify-between mb-3">
                                    <div className="w-11 h-11 rounded-xl bg-[#2563eb]/10 flex items-center justify-center flex-shrink-0">
                                        <span className="material-symbols-outlined text-[24px] text-[#2563eb]">domain</span>
                                    </div>
                                    <span className={`text-[10px] font-mono font-bold px-2.5 py-1 rounded-full border flex items-center gap-1 ${ROLE_COLOR[company.role]}`}>
                                        <span className="material-symbols-outlined text-[12px]">{ROLE_ICON[company.role]}</span>
                                        {company.role}
                                    </span>
                                </div>
                                <h3 className="text-white font-black text-base tracking-tight mb-1 group-hover:text-[#60a5fa] transition-colors">
                                    {company.companyName}
                                </h3>
                                {company.companyDescription && (
                                    <p className="text-xs text-gray-500 line-clamp-2 mb-3">{company.companyDescription}</p>
                                )}
                                {company.companyEmail && (
                                    <div className="flex items-center gap-1.5 text-[11px] font-mono text-gray-600">
                                        <span className="material-symbols-outlined text-[13px]">mail</span>
                                        {company.companyEmail}
                                    </div>
                                )}
                                <div className="flex items-center justify-between mt-3 pt-3 border-t border-gray-800">
                                    <span className="text-[10px] font-mono text-gray-600">ID #{company.companyId}</span>
                                    <span className="material-symbols-outlined text-[18px] text-gray-600 group-hover:text-[#2563eb] transition-colors">arrow_forward</span>
                                </div>
                            </button>
                        ))}
                    </div>
                )}
            </div>

            {showCreate && (
                <CreateCompanyModal
                    onClose={() => setShowCreate(false)}
                    onCreated={handleCompanyCreated}
                />
            )}
        </div>
    );
}

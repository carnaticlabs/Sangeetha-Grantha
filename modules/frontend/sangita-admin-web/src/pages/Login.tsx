import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/client';

const DEFAULT_ADMIN_EMAIL = 'admin@sangitagrantha.org';

const Login: React.FC = () => {
    const navigate = useNavigate();
    const [adminToken, setAdminToken] = useState('dev-admin-token');
    const [email, setEmail] = useState(DEFAULT_ADMIN_EMAIL);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            await login(adminToken, { email });
            navigate('/');
        } catch (err: unknown) {
            console.error('Login failed', err);
            setError(err instanceof Error ? err.message : 'Login failed. Please check your credentials.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
            <div className="card w-full max-w-md p-8 bg-white rounded-xl shadow-lg">
                <div className="mb-8 text-center">
                    <h2 className="text-3xl font-serif text-gray-900 mb-2">Sangita Grantha</h2>
                    <p className="text-gray-500 uppercase tracking-widest text-xs font-bold">Admin Console</p>
                </div>

                <form onSubmit={handleSubmit} className="flex flex-col gap-6">
                    <div className="flex flex-col gap-2">
                        <label htmlFor="adminToken" className="text-sm font-bold uppercase tracking-wider text-gray-500">
                            Admin Token
                        </label>
                        <input
                            id="adminToken"
                            type="password"
                            value={adminToken}
                            onChange={(e) => setAdminToken(e.target.value)}
                            className="w-full text-base py-3 px-4 rounded-lg border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all"
                            placeholder="Enter admin token"
                            required
                        />
                    </div>

                    <div className="flex flex-col gap-2">
                        <label htmlFor="email" className="text-sm font-bold uppercase tracking-wider text-gray-500">
                            Email
                        </label>
                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full text-base py-3 px-4 rounded-lg border border-gray-200 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition-all"
                            placeholder="admin@sangitagrantha.org"
                            required
                        />
                        <p className="text-xs text-gray-400">
                            Use the seeded admin email (or any existing user) so you don’t need to look up a UUID.
                        </p>
                    </div>

                    {error && (
                        <div className="p-3 bg-red-50 text-red-600 text-sm rounded-lg border border-red-100">
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 px-6 rounded-full bg-blue-600 text-white font-semibold hover:bg-blue-700 disabled:opacity-70 disabled:cursor-not-allowed transition-colors"
                    >
                        {loading ? 'Authenticating...' : 'Sign In'}
                    </button>

                    <div className="mt-4 text-center">
                        <p className="text-xs text-gray-400">
                            First time? Ensure you have run migrations and seeded a user.
                        </p>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default Login;

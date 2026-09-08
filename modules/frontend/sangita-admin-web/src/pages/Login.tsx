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
        <div className="flex min-h-screen items-center justify-center bg-cream p-4">
            <div className="card w-full max-w-md bg-surface-light rounded-xl border border-border-light shadow-soft overflow-hidden">
                <div className="rasika-cornice" aria-hidden="true">
                    <i className="band-green" /><i className="band-gold" /><i className="band-teal" /><i className="band-coral" />
                </div>
                <div className="p-8">
                    <div className="mb-8 text-center">
                        <h2 className="text-3xl font-display font-medium text-ink-900 mb-2">Sangīta Grantha</h2>
                        <p className="text-primary-dark uppercase tracking-[0.16em] text-xs font-semibold">Curator Console</p>
                    </div>

                    <form onSubmit={handleSubmit} className="flex flex-col gap-6">
                        <div className="flex flex-col gap-2">
                            <label htmlFor="adminToken" className="text-xs font-semibold uppercase tracking-[0.14em] text-primary-dark">
                                Admin Token
                            </label>
                            <input
                                id="adminToken"
                                type="password"
                                value={adminToken}
                                onChange={(e) => setAdminToken(e.target.value)}
                                className="w-full text-base py-3 px-4 rounded-lg border border-border-light bg-cream text-ink-900 caret-primary focus:border-primary/40 focus:ring-2 focus:ring-primary/20 outline-none transition-all"
                                placeholder="Enter admin token"
                                required
                            />
                        </div>

                        <div className="flex flex-col gap-2">
                            <label htmlFor="email" className="text-xs font-semibold uppercase tracking-[0.14em] text-primary-dark">
                                Email
                            </label>
                            <input
                                id="email"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="w-full text-base py-3 px-4 rounded-lg border border-border-light bg-cream text-ink-900 caret-primary focus:border-primary/40 focus:ring-2 focus:ring-primary/20 outline-none transition-all"
                                placeholder="admin@sangitagrantha.org"
                                required
                            />
                            <p className="text-xs text-ink-400">
                                Use the seeded admin email (or any existing user) so you don’t need to look up a UUID.
                            </p>
                        </div>

                        {error && (
                            <div className="p-3 bg-red-50 text-red-700 text-sm rounded-lg border border-red-100">
                                {error}
                            </div>
                        )}

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full py-3 px-6 rounded-full bg-primary text-white font-semibold hover:bg-primary-dark disabled:opacity-70 disabled:cursor-not-allowed transition-colors shadow-sm shadow-primary/20"
                        >
                            {loading ? 'Authenticating...' : 'Sign In'}
                        </button>

                        <div className="mt-4 text-center">
                            <p className="text-xs text-ink-400">
                                First time? Ensure you have run migrations and seeded a user.
                            </p>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default Login;

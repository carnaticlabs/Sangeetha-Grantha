import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAuditLogs, getDashboardStats } from '../api/client';
import { AuditLog, DashboardStats } from '../types';

import { StatCard, RecentItem } from '../components/dashboard';

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [activities, setActivities] = useState<AuditLog[]>([]);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // setLoading(true);
    setError(null);

    Promise.all([
      getAuditLogs().catch(err => {
        console.error('Failed to load audit logs:', err);
        return [];
      }),
      getDashboardStats().catch(err => {
        console.error('Failed to load dashboard stats:', err);
        return null;
      })
    ])
      .then(([auditLogs, dashboardStats]) => {
        setActivities(auditLogs);
        setStats(dashboardStats);
      })
      .catch(err => {
        console.error('Dashboard initialization error:', err);
        setError(err instanceof Error ? err.message : 'Failed to load dashboard data');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto space-y-8">
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary mb-4"></div>
            <p className="text-ink-500">Loading dashboard...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto space-y-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-center gap-2 mb-2">
            <span className="material-symbols-outlined text-red-600">error</span>
            <h3 className="text-red-900 font-bold">Error loading dashboard</h3>
          </div>
          <p className="text-red-700 text-sm">{error}</p>
          <button
            onClick={() => window.location.reload()}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 transition-colors"
          >
            Reload Page
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">Dashboard</h1>
          <p className="text-ink-500 mt-1">Overview of the Sangita Grantha archive.</p>
        </div>
        <button
          onClick={() => navigate('/krithis/new')}
          className="flex items-center gap-2 px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors shadow-sm shadow-blue-500/20"
        >
          <span className="material-symbols-outlined text-[20px]">add</span>
          New Composition
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          label="Total Kritis"
          value={stats ? stats.totalKrithis.toLocaleString() : '-'}
          icon="music_note"
          onClick={() => navigate('/krithis')}
        />
        <StatCard
          label="Composers"
          value={stats ? stats.totalComposers.toLocaleString() : '-'}
          icon="person_book"
          onClick={() => navigate('/reference')}
        />
        <StatCard
          label="Ragas"
          value={stats ? stats.totalRagas.toLocaleString() : '-'}
          icon="library_music"
          onClick={() => navigate('/reference')}
        />
        <StatCard
          label="Pending Review"
          value={stats ? stats.pendingReview.toLocaleString() : '-'}
          icon="rate_review"
          onClick={() => navigate('/krithis')}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Feed */}
        <div className="lg:col-span-2 bg-surface-light rounded-xl border border-border-light shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="font-display text-lg font-bold text-ink-900">Recent Edits</h2>
            <button className="text-sm font-bold text-primary hover:text-primary-dark transition-colors">View All</button>
          </div>
          <div>
            {activities.slice(0, 5).map(activity => (
              <RecentItem
                key={activity.id}
                title={activity.entityType || 'Unknown Entity'}
                subtitle={`${activity.action || 'Unknown Action'} by ${activity.actor || 'Unknown User'}`}
                time={activity.timestamp ? new Date(activity.timestamp).toLocaleDateString() : 'Unknown Date'}
                status={activity.action || 'Unknown'} // Mapping action to status pill for now
              />
            ))}
            {activities.length === 0 && (
              <div className="p-4 text-center text-ink-500 text-sm">No recent activity</div>
            )}
          </div>
        </div>

        {/* Quick Links / Tasks */}
        <div className="space-y-6">
          <div className="bg-surface-light rounded-xl border border-border-light shadow-sm p-6">
            <h2 className="font-display text-lg font-bold text-ink-900 mb-4">Curator Tasks</h2>
            <div className="space-y-3">
              <div className="p-3 bg-amber-50 border border-amber-100 rounded-lg flex gap-3">
                <span className="material-symbols-outlined text-amber-600 mt-0.5">warning</span>
                <div>
                  <h5 className="text-sm font-bold text-amber-900">Missing Metadata</h5>
                  <p className="text-xs text-amber-700 mt-1">15 records are missing 'Tala' information.</p>
                </div>
              </div>
              <div className="p-3 bg-blue-50 border border-blue-100 rounded-lg flex gap-3">
                <span className="material-symbols-outlined text-blue-600 mt-0.5">verified</span>
                <div>
                  <h5 className="text-sm font-bold text-blue-900">Validation Required</h5>
                  <p className="text-xs text-blue-700 mt-1">New batch of 50 Kritis imported.</p>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-primary rounded-xl shadow-lg shadow-blue-900/20 p-6 text-white relative overflow-hidden">
            <div className="relative z-10">
              <h3 className="font-display text-lg font-bold mb-2">Reference Library</h3>
              <p className="text-blue-100 text-sm mb-4">Manage the taxonomy of Ragas, Talas, Composers, Temples, and Deities.</p>
              <button
                onClick={() => navigate('/reference')}
                className="text-xs font-bold uppercase tracking-wider bg-white/10 hover:bg-white/20 px-4 py-2 rounded border border-white/20 transition-colors"
              >            Manage Taxonomy
              </button>
            </div>
            <div className="absolute -bottom-8 -right-8 w-32 h-32 bg-white/10 rounded-full blur-2xl"></div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
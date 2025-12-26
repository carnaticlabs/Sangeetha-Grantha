import React from 'react';
import { ViewState } from '../types';

interface DashboardProps {
  onNavigate: (view: ViewState) => void;
}

const StatCard: React.FC<{ label: string; value: string; icon: string; onClick?: () => void }> = ({ label, value, icon, onClick }) => (
  <div 
    onClick={onClick}
    className={`bg-surface-light p-6 rounded-xl border border-border-light shadow-sm flex items-start justify-between transition-all duration-200 ${
        onClick 
            ? 'cursor-pointer hover:border-primary hover:shadow-md hover:-translate-y-0.5' 
            : 'hover:border-slate-300'
    }`}
  >
    <div>
      <p className="text-sm font-bold text-ink-500 uppercase tracking-wide mb-1">{label}</p>
      <h3 className="text-3xl font-display font-bold text-ink-900 tracking-tight">{value}</h3>
    </div>
    <div className="p-3 bg-slate-50 rounded-lg text-ink-700">
      <span className="material-symbols-outlined text-[28px] text-primary">{icon}</span>
    </div>
  </div>
);

const RecentItem: React.FC<{ title: string; subtitle: string; time: string; status: string }> = ({ title, subtitle, time, status }) => (
  <div className="flex items-center justify-between py-4 border-b border-slate-100 last:border-0 hover:bg-slate-50 -mx-4 px-4 transition-colors cursor-pointer">
    <div className="flex items-center gap-4">
      <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-ink-500 font-bold text-sm">
        {title.charAt(0)}
      </div>
      <div>
        <h4 className="text-sm font-bold text-ink-900">{title}</h4>
        <p className="text-xs text-ink-500">{subtitle}</p>
      </div>
    </div>
    <div className="flex items-center gap-4 text-right">
      <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border ${
        status === 'Published' ? 'bg-green-50 text-green-700 border-green-200' : 
        status === 'Review' ? 'bg-amber-50 text-amber-700 border-amber-200' :
        'bg-slate-100 text-slate-600 border-slate-200'
      }`}>
        {status}
      </span>
      <span className="text-xs text-ink-500 font-medium tabular-nums">{time}</span>
    </div>
  </div>
);

const Dashboard: React.FC<DashboardProps> = ({ onNavigate }) => {
  return (
    <div className="max-w-7xl mx-auto space-y-8 animate-fadeIn">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">Dashboard</h1>
          <p className="text-ink-500 mt-1">Overview of the Sangita Grantha archive.</p>
        </div>
        <button 
            onClick={() => onNavigate(ViewState.KRITHIS)}
            className="flex items-center gap-2 px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors shadow-sm shadow-blue-500/20"
        >
            <span className="material-symbols-outlined text-[20px]">add</span>
            New Composition
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard 
            label="Total Kritis" 
            value="4,291" 
            icon="music_note" 
            onClick={() => onNavigate(ViewState.KRITHIS)}
        />
        <StatCard 
            label="Composers" 
            value="86" 
            icon="person_book" 
            onClick={() => onNavigate(ViewState.REFERENCE)}
        />
        <StatCard 
            label="Ragas" 
            value="342" 
            icon="library_music" 
            onClick={() => onNavigate(ViewState.REFERENCE)}
        />
        <StatCard 
            label="Pending Review" 
            value="12" 
            icon="rate_review" 
            onClick={() => onNavigate(ViewState.KRITHIS)}
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
            <RecentItem 
                title="Endaro Mahanubhavulu" 
                subtitle="Tyagaraja • Sri Raga" 
                time="2 hrs ago" 
                status="Published" 
            />
            <RecentItem 
                title="Vatapi Ganapatim" 
                subtitle="Muthuswami Dikshitar • Hamsadhwani" 
                time="5 hrs ago" 
                status="Review" 
            />
            <RecentItem 
                title="Jagadodharana" 
                subtitle="Purandara Dasa • Kapi" 
                time="1 day ago" 
                status="Published" 
            />
            <RecentItem 
                title="Sree Jalandhara" 
                subtitle="Mysore Vasudevachar • Gambhiranata" 
                time="2 days ago" 
                status="Draft" 
            />
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
                    <p className="text-blue-100 text-sm mb-4">Manage the taxonomy of Ragas, Talas, and Composers.</p>
                    <button 
                        onClick={() => onNavigate(ViewState.REFERENCE)}
                        className="text-xs font-bold uppercase tracking-wider bg-white/10 hover:bg-white/20 px-4 py-2 rounded border border-white/20 transition-colors"
                    >
                        Manage Taxonomy
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
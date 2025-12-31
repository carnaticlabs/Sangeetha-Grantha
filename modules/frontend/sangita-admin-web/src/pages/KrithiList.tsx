import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchKrithis } from '../api/client';
import { KrithiSummary } from '../types';

const STATUS_Styles: Record<string, string> = {
  'PUBLISHED': 'bg-green-50 text-green-700 border-green-200',
  'IN_REVIEW': 'bg-amber-50 text-amber-700 border-amber-200',
  'DRAFT': 'bg-slate-100 text-slate-600 border-slate-200',
  'ARCHIVED': 'bg-gray-100 text-gray-800 border-gray-200'
};

const KrithiList: React.FC = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [krithis, setKrithis] = useState<KrithiSummary[]>([]);
  const [loading, setLoading] = useState(false);

  React.useEffect(() => {
    const loadKrithis = async () => {
      setLoading(true);
      try {
        const data = await searchKrithis(searchTerm);
        setKrithis(data.items || []);
      } catch (e) {
        console.error("Failed to load krithis", e);
        setKrithis([]); // Clear on error
      } finally {
        setLoading(false);
      }
    };

    // Debounce searches, but load immediately if search term is empty (initial load)
    const delay = searchTerm === '' ? 0 : 500;
    const timer = setTimeout(loadKrithis, delay);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  return (
    <div className="max-w-7xl mx-auto h-full flex flex-col space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">Kritis</h1>
          <p className="text-ink-500 mt-2">Manage the core catalog of compositions.</p>
        </div>
        <button
          onClick={() => navigate('/krithis/new')}
          className="flex items-center gap-2 px-6 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors shadow-sm shadow-blue-500/20"
        >
          <span className="material-symbols-outlined text-[20px]">add</span>
          Add New
        </button>
      </div>

      <div className="bg-surface-light border border-border-light rounded-xl shadow-sm flex flex-col overflow-hidden">
        {/* Toolbar */}
        <div className="p-4 border-b border-border-light flex gap-4 bg-slate-50/50">
          <div className="relative flex-1 max-w-lg">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 text-[20px]">search</span>
            <input
              type="text"
              placeholder="Search title, raga, or composer..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-white border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent transition-shadow"
            />
          </div>
          <button className="flex items-center gap-2 px-4 py-2 bg-white border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors">
            <span className="material-symbols-outlined text-[20px]">filter_list</span>
            Filter
          </button>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/50 border-b border-border-light text-xs font-semibold uppercase tracking-wider text-ink-500">
                <th className="px-6 py-4">Title</th>
                <th className="px-6 py-4">Composer</th>
                <th className="px-6 py-4">Raga</th>
                <th className="px-6 py-4">Language</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {loading ? (
                <tr><td colSpan={5} className="px-6 py-8 text-center text-ink-500">Loading...</td></tr>
              ) : krithis.length === 0 ? (
                <tr><td colSpan={5} className="px-6 py-8 text-center text-ink-500">No krithis found.</td></tr>
              ) : krithis.map((item) => (
                <tr
                  key={item.id}
                  onClick={() => navigate(`/krithis/${item.id}`)}
                  className="hover:bg-slate-50 cursor-pointer transition-colors group"
                >
                  <td className="px-6 py-4">
                    <span className="font-display font-medium text-ink-900 text-sm">{item.name}</span>
                  </td>
                  <td className="px-6 py-4 text-sm text-ink-700">{item.composerName}</td>
                  <td className="px-6 py-4 text-sm text-ink-700">
                    <div className="flex flex-wrap gap-1">
                      {item.ragas.map(r => (
                        <span key={r.id} className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
                          {r.name}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm text-ink-700 uppercase">{item.primaryLanguage}</td>
                  <td className="px-6 py-4 text-right">
                    <button className="text-ink-400 hover:text-primary p-1 transition-colors">
                      <span className="material-symbols-outlined text-[20px]">edit</span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="p-4 border-t border-border-light bg-white flex items-center justify-between text-sm text-ink-500">
          <span>Showing {krithis.length} results</span>
          <div className="flex gap-2">
            <button className="px-3 py-1.5 border border-border-light rounded-md bg-white disabled:opacity-50 hover:bg-slate-50 transition-colors" disabled>Previous</button>
            <button className="px-3 py-1.5 border border-border-light rounded-md bg-white hover:bg-slate-50 transition-colors">Next</button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default KrithiList;
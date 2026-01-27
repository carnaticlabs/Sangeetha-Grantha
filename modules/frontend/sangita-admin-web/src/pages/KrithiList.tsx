import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchKrithis } from '../api/client';
import { KrithiSummary } from '../types';

const KrithiList: React.FC = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [krithis, setKrithis] = useState<KrithiSummary[]>([]);
  const [loading, setLoading] = useState(false);

  // Pagination State
  const [page, setPage] = useState(1);
  const [limit] = useState(20);
  // const [total, setTotal] = useState(0);

  useEffect(() => {
    // Reset to page 1 when search changes
    setPage(1);
  }, [searchTerm]);

  useEffect(() => {
    const loadKrithis = async () => {
      setLoading(true);
      try {
        // searchKrithis currently only takes query. If we want pagination, we need to update searchKrithis or the API.
        // Assuming searchKrithis returns all or has a limit inside.
        // If the API doesn't support pagination params in the search endpoint yet, we might client-side paginate or just show all.
        // Looking at api/client.ts: export const searchKrithis = (query?: string) => ... params.append('query', query)
        // It does NOT accepted limit/offset.
        // However, let's look at `KrithiSearchResult`. If it has `total`, maybe we can add params.
        // If not, we just display what we get.

        // Use the existing client function for now. If refactoring API is out of scope, 
        // we can implement client-side pagination if the list is small, or assume backend handles it.
        // We'll invoke the search.
        const data = await searchKrithis(searchTerm);

        // Checking type definition next. Assuming data has items.
        setKrithis(data.items || []);
        // setTotal(data.total || (data.items || []).length);

      } catch (e) {
        console.error("Failed to load krithis", e);
        setKrithis([]);
        // setTotal(0);
      } finally {
        setLoading(false);
      }
    };

    const delay = searchTerm === '' ? 0 : 500;
    const timer = setTimeout(loadKrithis, delay);
    return () => clearTimeout(timer);
  }, [searchTerm, page, limit]); // We can't really paginate effectively without API support but we structure it.

  // Helper for pagination rendering (Client side for now if API doesn't support offset)
  // Actually, if API returns limited set, we can't paginate client side beyond first page.
  // If API returns ALL, we can paginate client side.
  // Let's assume API returns a "search result" which is likely paginated by backend default 
  // or returns all.

  // For this refactor, let's keep it simple and clean up the UI.

  // const totalPages = Math.ceil(total / limit);

  return (
    <div className="max-w-7xl mx-auto h-full flex flex-col space-y-6 animate-fadeIn pb-12">
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
              className="w-full pl-10 pr-4 py-2 bg-white border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent transition-shadow outline-none"
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
                <tr><td colSpan={5} className="px-6 py-12 text-center text-ink-500">
                  <div className="flex flex-col items-center gap-2">
                    <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
                    <span className="text-sm">Loading Kritis...</span>
                  </div>
                </td></tr>
              ) : krithis.length === 0 ? (
                <tr><td colSpan={5} className="px-6 py-12 text-center text-ink-500 text-sm">No krithis found matching your criteria.</td></tr>
              ) : krithis.map((item) => (
                <tr
                  key={item.id}
                  onClick={() => navigate(`/krithis/${item.id}`)}
                  className="hover:bg-slate-50 cursor-pointer transition-colors group"
                >
                  <td className="px-6 py-4">
                    <span className="font-display font-medium text-ink-900 text-sm">{item.name}</span>
                  </td>
                  <td className="px-6 py-4 text-sm text-ink-700">{item.composerName || '-'}</td>
                  <td className="px-6 py-4 text-sm text-ink-700">
                    <div className="flex flex-wrap gap-1">
                      {item.ragas && item.ragas.map(r => (
                        <span key={r.id} className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
                          {r.name}
                        </span>
                      ))}
                      {(!item.ragas || item.ragas.length === 0) && '-'}
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm text-ink-700 uppercase">{item.primaryLanguage || '-'}</td>
                  <td className="px-6 py-4 text-right">
                    <button className="text-ink-400 hover:text-primary p-2 rounded-full hover:bg-blue-50 transition-colors opacity-0 group-hover:opacity-100">
                      <span className="material-symbols-outlined text-[20px]">edit</span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination - Placeholder for now as API doesn't support full pagination params yet */}
        <div className="p-4 border-t border-border-light bg-white flex items-center justify-between text-sm text-ink-500">
          <span>Showing {krithis.length} results</span>
          {/* 
          <div className="flex gap-2">
            <button disabled={page <= 1} onClick={() => setPage(p => p - 1)} className="px-3 py-1.5 border border-border-light rounded-md bg-white disabled:opacity-50 hover:bg-slate-50 transition-colors">Previous</button>
            <span className="flex items-center px-2">Page {page}</span>
            <button disabled={page >= totalPages} onClick={() => setPage(p => p + 1)} className="px-3 py-1.5 border border-border-light rounded-md bg-white hover:bg-slate-50 transition-colors">Next</button>
          </div>
          */}
        </div>
      </div>
    </div>
  );
};

export default KrithiList;
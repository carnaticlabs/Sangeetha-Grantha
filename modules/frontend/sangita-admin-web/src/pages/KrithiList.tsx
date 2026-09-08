import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchKrithis, searchDiscoveryKrithis, getRagas, getComposers } from '../api/client';
import { KrithiSummary, Raga, Composer, SemanticSearchResultItem } from '../types';

const PAGE_SIZE = 25;
type SearchMode = 'standard' | 'hybrid' | 'semantic';

const KrithiList: React.FC = () => {
  const navigate = useNavigate();

  // Search & filter state
  const [searchTerm, setSearchTerm] = useState('');
  const [searchMode, setSearchMode] = useState<SearchMode>('hybrid');
  const [selectedRagaId, setSelectedRagaId] = useState('');
  const [selectedComposerId, setSelectedComposerId] = useState('');
  const [selectedLanguage, setSelectedLanguage] = useState('');
  const [showFilters, setShowFilters] = useState(false);

  // Data state
  const [krithis, setKrithis] = useState<KrithiSummary[]>([]);
  const [semanticResults, setSemanticResults] = useState<SemanticSearchResultItem[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0); // 0-indexed for backend
  const searchRequestId = useRef(0);
  const isBrowseMode = searchMode === 'standard' || !searchTerm.trim();
  const listPage = isBrowseMode ? page : 0;

  // Reference data for filter dropdowns
  const [ragas, setRagas] = useState<Raga[]>([]);
  const [composers, setComposers] = useState<Composer[]>([]);

  // Load reference data once
  useEffect(() => {
    getRagas().then(r => setRagas(r.sort((a, b) => a.name.localeCompare(b.name)))).catch(() => {});
    getComposers().then(c => setComposers(c.sort((a, b) => a.name.localeCompare(b.name)))).catch(() => {});
  }, []);

  // Reset to page 0 when filters/search change
  useEffect(() => {
    setPage(0);
  }, [searchTerm, searchMode, selectedRagaId, selectedComposerId, selectedLanguage]);

  // Load krithis
  const loadKrithis = useCallback(async () => {
    const requestId = ++searchRequestId.current;
    setLoading(true);
    setLoadError(null);
    try {
      if (isBrowseMode) {
        const data = await searchKrithis({
          query: searchTerm || undefined,
          ragaId: selectedRagaId || undefined,
          composerId: selectedComposerId || undefined,
          language: selectedLanguage || undefined,
          page: listPage,
          pageSize: PAGE_SIZE,
        });
        if (requestId !== searchRequestId.current) return;
        setKrithis(data.items || []);
        setSemanticResults([]);
        setTotal(data.total || 0);
      } else {
        const data = await searchDiscoveryKrithis(searchMode, {
          query: searchTerm.trim(),
          ragaId: selectedRagaId || undefined,
          composerId: selectedComposerId || undefined,
          limit: 30,
        });
        if (requestId !== searchRequestId.current) return;
        setSemanticResults(data.items || []);
        setKrithis([]);
        setTotal(data.totalMatches || 0);
      }
    } catch (e: any) {
      if (requestId !== searchRequestId.current) return;
      console.error('Failed to load krithis', e);
      setKrithis([]);
      setSemanticResults([]);
      setTotal(0);
      const msg = e?.response?.data?.message || e?.message || 'Failed to search compositions. Please try again.';
      setLoadError(msg);
    } finally {
      if (requestId === searchRequestId.current) {
        setLoading(false);
      }
    }
  }, [searchTerm, searchMode, selectedRagaId, selectedComposerId, selectedLanguage, listPage, isBrowseMode]);

  useEffect(() => {
    const delay = searchTerm === '' ? 0 : 400;
    const timer = setTimeout(loadKrithis, delay);
    return () => clearTimeout(timer);
  }, [loadKrithis]);

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const activeFilterCount = [selectedRagaId, selectedComposerId, selectedLanguage].filter(Boolean).length;

  const clearFilters = () => {
    setSelectedRagaId('');
    setSelectedComposerId('');
    setSelectedLanguage('');
  };

  return (
    <div className="max-w-7xl mx-auto h-full flex flex-col space-y-6 animate-fadeIn pb-12">
      {/* Header */}
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
        <div className="p-4 border-b border-border-light flex flex-col md:flex-row gap-4 bg-slate-50/50 justify-between items-stretch md:items-center">
          <div className="relative flex-1 max-w-xl">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 text-[20px]">
              {searchMode === 'standard' ? 'search' : searchMode === 'hybrid' ? 'auto_awesome' : 'psychology'}
            </span>
            <input
              type="text"
              placeholder={
                searchMode === 'standard'
                  ? 'Search title, raga, or composer...'
                  : searchMode === 'hybrid'
                  ? 'Hybrid search (e.g. Dikshitar in Hamsadhvani praising Ganesha)...'
                  : 'Semantic search (e.g. compositions dedicated to Goddess Kamakshi)...'
              }
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-white border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent transition-shadow outline-none"
            />
          </div>

          <div className="flex items-center gap-3">
            {/* Search Mode Segmented Pill */}
            <div className="flex items-center bg-slate-200/70 p-1 rounded-lg text-xs font-medium border border-border-light/60">
              <button
                type="button"
                onClick={() => setSearchMode('standard')}
                className={`px-3 py-1.5 rounded-md transition-all ${
                  searchMode === 'standard'
                    ? 'bg-white text-ink-900 shadow-sm font-semibold'
                    : 'text-ink-600 hover:text-ink-900'
                }`}
              >
                Lexical
              </button>
              <button
                type="button"
                onClick={() => setSearchMode('hybrid')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md transition-all ${
                  searchMode === 'hybrid'
                    ? 'bg-white text-blue-600 shadow-sm font-semibold'
                    : 'text-ink-600 hover:text-ink-900'
                }`}
              >
                <span className="material-symbols-outlined text-[15px]">auto_awesome</span>
                Hybrid
              </button>
              <button
                type="button"
                onClick={() => setSearchMode('semantic')}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md transition-all ${
                  searchMode === 'semantic'
                    ? 'bg-white text-purple-600 shadow-sm font-semibold'
                    : 'text-ink-600 hover:text-ink-900'
                }`}
              >
                <span className="material-symbols-outlined text-[15px]">psychology</span>
                Semantic
              </button>
            </div>

            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`flex items-center gap-2 px-4 py-2 border rounded-lg text-sm font-medium transition-colors ${
                showFilters || activeFilterCount > 0
                  ? 'bg-primary/10 border-primary/30 text-primary'
                  : 'bg-white border-border-light text-ink-700 hover:bg-slate-50'
              }`}
            >
              <span className="material-symbols-outlined text-[20px]">filter_list</span>
              Filter
              {activeFilterCount > 0 && (
                <span className="inline-flex items-center justify-center w-5 h-5 rounded-full bg-primary text-white text-xs font-bold">
                  {activeFilterCount}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* Filter Panel */}
        {showFilters && (
          <div className="p-4 border-b border-border-light bg-slate-50/30 space-y-3">
            <div className="flex flex-wrap gap-4">
              <div className="flex flex-col gap-1 min-w-[200px]">
                <label className="text-xs font-semibold uppercase tracking-wider text-ink-400">Raga</label>
                <select
                  value={selectedRagaId}
                  onChange={(e) => setSelectedRagaId(e.target.value)}
                  className="px-3 py-2 bg-white border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
                >
                  <option value="">All Ragas</option>
                  {ragas.map(r => (
                    <option key={r.id} value={r.id}>{r.name}</option>
                  ))}
                </select>
              </div>
              <div className="flex flex-col gap-1 min-w-[200px]">
                <label className="text-xs font-semibold uppercase tracking-wider text-ink-400">Composer</label>
                <select
                  value={selectedComposerId}
                  onChange={(e) => setSelectedComposerId(e.target.value)}
                  className="px-3 py-2 bg-white border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
                >
                  <option value="">All Composers</option>
                  {composers.map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              {searchMode === 'standard' && (
                <div className="flex flex-col gap-1 min-w-[140px]">
                  <label className="text-xs font-semibold uppercase tracking-wider text-ink-400">Language</label>
                  <select
                    value={selectedLanguage}
                    onChange={(e) => setSelectedLanguage(e.target.value)}
                    className="px-3 py-2 bg-white border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
                  >
                    <option value="">All Languages</option>
                    <option value="SA">Sanskrit</option>
                    <option value="TE">Telugu</option>
                    <option value="TA">Tamil</option>
                    <option value="KN">Kannada</option>
                  </select>
                </div>
              )}
            </div>
            {activeFilterCount > 0 && (
              <button
                onClick={clearFilters}
                className="text-xs text-primary hover:text-primary-dark font-medium flex items-center gap-1"
              >
                <span className="material-symbols-outlined text-[16px]">close</span>
                Clear all filters
              </button>
            )}
          </div>
        )}

        {loadError && (
          <div className="px-4 py-3 border-b border-red-200 bg-red-50 text-sm text-red-800">
            {loadError}
          </div>
        )}

        {/* Table / Results */}
        <div className="overflow-x-auto">
          {isBrowseMode ? (
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
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50/50 border-b border-border-light text-xs font-semibold uppercase tracking-wider text-ink-500">
                  <th className="px-6 py-4">Title & Melody</th>
                  <th className="px-6 py-4">Matched Content Snippet</th>
                  <th className="px-6 py-4">Kind</th>
                  <th className="px-6 py-4 text-right">Relevance Score</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {loading ? (
                  <tr><td colSpan={4} className="px-6 py-12 text-center text-ink-500">
                    <div className="flex flex-col items-center gap-2">
                      <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
                      <span className="text-sm">
                        {searchMode === 'hybrid'
                          ? 'Searching with Hybrid RRF (Dense + Lexical)...'
                          : 'Searching with Gemini Embedding 2...'}
                      </span>
                    </div>
                  </td></tr>
                ) : loadError ? (
                  <tr><td colSpan={4} className="px-6 py-12 text-center text-red-600 text-sm">
                    <div className="max-w-md mx-auto flex flex-col items-center gap-2">
                      <span className="material-symbols-outlined text-red-400 text-3xl">error</span>
                      <p className="font-semibold text-red-800">Search Error</p>
                      <p className="text-xs text-red-600">{loadError}</p>
                    </div>
                  </td></tr>
                ) : semanticResults.length === 0 ? (
                  <tr><td colSpan={4} className="px-6 py-12 text-center text-ink-500 text-sm">No matches found for &ldquo;{searchTerm}&rdquo;.</td></tr>
                ) : semanticResults.map((item) => (
                  <tr
                    key={item.krithiId}
                    onClick={() => navigate(`/krithis/${item.krithiId}`)}
                    className="hover:bg-blue-50/40 cursor-pointer transition-colors group"
                  >
                    <td className="px-6 py-4">
                      <div className="flex flex-col">
                        <span className="font-display font-semibold text-ink-900 text-sm group-hover:text-primary transition-colors">
                          {item.title}
                        </span>
                        <div className="flex items-center gap-2 text-xs text-ink-500 mt-1">
                          <span className="font-medium text-ink-700">{item.composerName}</span>
                          {item.ragaName && (
                            <>
                              <span>&bull;</span>
                              <span className="px-1.5 py-0.5 rounded bg-amber-50 text-amber-700 border border-amber-200/60 font-medium">
                                {item.ragaName}
                              </span>
                            </>
                          )}
                          {item.talaName && (
                            <>
                              <span>&bull;</span>
                              <span>{item.talaName}</span>
                            </>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 max-w-md text-xs text-ink-700">
                      <p className="line-clamp-2 font-serif italic text-ink-600 bg-slate-50 p-2 rounded border border-slate-100">
                        {item.matchedContent}
                      </p>
                    </td>
                    <td className="px-6 py-4 text-xs">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold tracking-wide uppercase ${
                        item.documentKind === 'SECTION_PASSAGE'
                          ? 'bg-purple-50 text-purple-700 border border-purple-200'
                          : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                      }`}>
                        {item.documentKind === 'SECTION_PASSAGE' ? 'Passage' : 'Overview'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex flex-col items-end gap-1">
                        {searchMode === 'hybrid' && item.rrfScore != null ? (
                          <>
                            {item.similarityScore > 0 && (item.lexicalScore ?? 0) > 0 ? (
                              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                                <span className="material-symbols-outlined text-[13px]">auto_awesome</span>
                                Hybrid Match
                              </span>
                            ) : item.similarityScore > 0 ? (
                              <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold bg-blue-100 text-blue-800 border border-blue-200">
                                {Math.round(item.similarityScore * 100)}% Semantic
                              </span>
                            ) : (
                              <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200">
                                {Math.round((item.lexicalScore ?? 0) * 100)}% Lexical
                              </span>
                            )}
                            <div className="flex items-center gap-1.5 text-[10px] text-ink-400 font-mono">
                              <span>RRF: {item.rrfScore.toFixed(4)}</span>
                              {item.similarityScore > 0 && (
                                <span>&bull; Sem: {(item.similarityScore * 100).toFixed(0)}%</span>
                              )}
                              {(item.lexicalScore ?? 0) > 0 && (
                                <span>&bull; Lex: {((item.lexicalScore ?? 0) * 100).toFixed(0)}%</span>
                              )}
                            </div>
                          </>
                        ) : (
                          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold bg-blue-100 text-blue-800 border border-blue-200">
                            {Math.round(item.similarityScore * 100)}% Match
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination */}
        <div className="p-4 border-t border-border-light bg-white flex items-center justify-between text-sm text-ink-500">
          {isBrowseMode ? (
            <>
              <span>
                {total > 0
                  ? `Showing ${page * PAGE_SIZE + 1}\u2013${Math.min((page + 1) * PAGE_SIZE, total)} of ${total} results`
                  : 'No results'}
              </span>
              {totalPages > 1 && (
                <div className="flex items-center gap-2">
                  <button
                    disabled={page <= 0}
                    onClick={() => setPage(p => p - 1)}
                    className="px-3 py-1.5 border border-border-light rounded-md bg-white disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50 transition-colors text-sm"
                  >
                    Previous
                  </button>
                  <div className="flex items-center gap-1">
                    {Array.from({ length: totalPages }, (_, i) => i).map(i => {
                      if (i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1) {
                        return (
                          <button
                            key={i}
                            onClick={() => setPage(i)}
                            className={`w-8 h-8 flex items-center justify-center rounded-md text-sm transition-colors ${
                              i === page
                                ? 'bg-primary text-white font-semibold'
                                : 'hover:bg-slate-100 text-ink-600'
                            }`}
                          >
                            {i + 1}
                          </button>
                        );
                      }
                      if (i === 1 && page > 2) return <span key={i} className="px-1 text-ink-400">&hellip;</span>;
                      if (i === totalPages - 2 && page < totalPages - 3) return <span key={i} className="px-1 text-ink-400">&hellip;</span>;
                      return null;
                    })}
                  </div>
                  <button
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage(p => p + 1)}
                    className="px-3 py-1.5 border border-border-light rounded-md bg-white disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50 transition-colors text-sm"
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          ) : (
            <div className="flex items-center justify-between w-full">
              <span>
                {semanticResults.length > 0
                  ? `Displaying ${semanticResults.length} compositions ranked by ${searchMode === 'hybrid' ? 'Reciprocal Rank Fusion (Hybrid)' : 'Dense Semantic Similarity'}`
                  : '0 results'}
              </span>
              <span className="text-xs text-ink-400 font-medium">
                Top discovery results
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default KrithiList;

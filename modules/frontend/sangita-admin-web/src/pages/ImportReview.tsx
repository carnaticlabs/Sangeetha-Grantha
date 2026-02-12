import React, { useEffect, useState } from 'react';
import { ImportedKrithi, ImportReviewRequest, ResolutionResult } from '../types';
import { getImports, reviewImport } from '../api/client';
import { useToast, ToastContainer } from '../components/Toast';
import { useSourceDetail } from '../hooks/useSourcingQueries';
import { TierBadge } from '../components/sourcing/shared';
import { AuthorityWarning } from '../components/import-review/AuthorityWarning';

const ImportReviewPage: React.FC = () => {
  const { toasts, success, error, removeToast } = useToast();
  const [imports, setImports] = useState<ImportedKrithi[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [selectedImportIds, setSelectedImportIds] = useState<Set<string>>(new Set());

  // Form State
  const [overrideTitle, setOverrideTitle] = useState('');
  const [overrideComposer, setOverrideComposer] = useState('');
  const [overrideRaga, setOverrideRaga] = useState('');
  const [overrideTala, setOverrideTala] = useState('');
  const [overrideLanguage, setOverrideLanguage] = useState('');
  const [overrideDeity, setOverrideDeity] = useState('');
  const [overrideTemple, setOverrideTemple] = useState('');
  const [overrideLyrics, setOverrideLyrics] = useState('');

  const [warningDismissed, setWarningDismissed] = useState(false);

  // Fetch source detail to get the tier badge for the current import's source
  const selectedItem = imports.find(i => i.id === selectedId);
  const { data: sourceDetail } = useSourceDetail(selectedItem?.importSourceId ?? '');

  useEffect(() => {
    loadImports();
  }, []);

  const loadImports = async () => {
    setLoading(true);
    try {
      const data = await getImports('PENDING');
      setImports(data);
      if (data.length > 0 && !selectedId) {
        selectImport(data[0]);
      }
    } catch (e) {
      error('Failed to load pending imports');
    } finally {
      setLoading(false);
    }
  };

  const selectImport = (item: ImportedKrithi) => {
    setSelectedId(item.id);
    setWarningDismissed(false);
    setOverrideTitle(item.rawTitle || '');
    setOverrideComposer(item.rawComposer || '');
    setOverrideRaga(item.rawRaga || '');
    setOverrideTala(item.rawTala || '');
    setOverrideLanguage(item.rawLanguage || '');
    setOverrideDeity(item.rawDeity || '');
    setOverrideTemple(item.rawTemple || '');
    setOverrideLyrics(item.rawLyrics || '');
  };

  const handleApprove = async () => {
    if (!selectedId) return;
    setProcessing(true);
    try {
      const overrides = {
        title: overrideTitle,
        composer: overrideComposer,
        raga: overrideRaga,
        tala: overrideTala,
        language: overrideLanguage,
        deity: overrideDeity,
        temple: overrideTemple,
        lyrics: overrideLyrics
      };

      await reviewImport(selectedId, {
        status: 'APPROVED', // Use APPROVED to trigger creation
        overrides
      });

      success('Import approved and entity created');
      await loadImports(); // Refresh list
      setSelectedId(null);
    } catch (e) {
      error('Failed to approve import');
    } finally {
      setProcessing(false);
    }
  };

  // ... [handleReject, handleBulkApprove, handleBulkReject, toggleSelectImport, toggleSelectAll remain unchanged] ...

  const handleReject = async () => {
    if (!selectedId) return;
    if (!confirm('Are you sure you want to reject this import?')) return;

    setProcessing(true);
    try {
      await reviewImport(selectedId, { status: 'REJECTED' });
      success('Import rejected');
      await loadImports();
      setSelectedId(null);
    } catch (e) {
      error('Failed to reject import');
    } finally {
      setProcessing(false);
    }
  };

  const handleBulkApprove = async () => {
    if (selectedImportIds.size === 0) {
      error('No imports selected');
      return;
    }
    if (!confirm(`Approve ${selectedImportIds.size} selected imports?`)) return;

    setProcessing(true);
    try {
      const promises = Array.from(selectedImportIds).map(id =>
        reviewImport(id, { status: 'APPROVED' })
      );
      await Promise.all(promises);
      success(`${selectedImportIds.size} imports approved`);
      setSelectedImportIds(new Set());
      await loadImports();
    } catch (e) {
      error('Failed to bulk approve imports');
    } finally {
      setProcessing(false);
    }
  };

  const handleBulkReject = async () => {
    if (selectedImportIds.size === 0) {
      error('No imports selected');
      return;
    }
    if (!confirm(`Reject ${selectedImportIds.size} selected imports?`)) return;

    setProcessing(true);
    try {
      const promises = Array.from(selectedImportIds).map(id =>
        reviewImport(id, { status: 'REJECTED' })
      );
      await Promise.all(promises);
      success(`${selectedImportIds.size} imports rejected`);
      setSelectedImportIds(new Set());
      await loadImports();
    } catch (e) {
      error('Failed to bulk reject imports');
    } finally {
      setProcessing(false);
    }
  };

  const toggleSelectImport = (id: string) => {
    const newSet = new Set(selectedImportIds);
    if (newSet.has(id)) {
      newSet.delete(id);
    } else {
      newSet.add(id);
    }
    setSelectedImportIds(newSet);
  };

  const toggleSelectAll = () => {
    if (selectedImportIds.size === imports.length) {
      setSelectedImportIds(new Set());
    } else {
      setSelectedImportIds(new Set(imports.map(i => i.id)));
    }
  };


  const renderResolutionPanel = () => {
    if (!selectedItem?.resolutionData) return null;
    let resolution: ResolutionResult;
    try {
      resolution = JSON.parse(selectedItem.resolutionData);
    } catch (e) {
      return <div className="text-xs text-rose-500">Invalid resolution data</div>;
    }

    return (
      <div className="mt-4 p-3 bg-indigo-50 border border-indigo-100 rounded-lg">
        <h4 className="text-xs font-bold text-indigo-900 mb-2 uppercase tracking-wide">AI Resolution Candidates</h4>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <div className="text-[10px] font-semibold text-indigo-500 mb-1">COMPOSER</div>
            {!resolution.composerCandidates || resolution.composerCandidates.length === 0 ? <span className="text-xs italic text-gray-400">No matches</span> : (
              <div className="flex flex-col gap-1">
                {resolution.composerCandidates.map((c, i) => (
                  <button
                    key={i}
                    onClick={() => setOverrideComposer(c.entity.name)}
                    className="text-left text-xs px-2 py-1 bg-white border border-indigo-100 rounded hover:border-indigo-300 flex justify-between items-center group"
                  >
                    <span>{c.entity.name}</span>
                    <span className={`text-[9px] font-bold ${c.confidence === 'HIGH' ? 'text-green-600' : 'text-amber-600'}`}>{c.score}%</span>
                  </button>
                ))}
              </div>
            )}
          </div>
          <div>
            <div className="text-[10px] font-semibold text-indigo-500 mb-1">RAGA</div>
            {!resolution.ragaCandidates || resolution.ragaCandidates.length === 0 ? <span className="text-xs italic text-gray-400">No matches</span> : (
              <div className="flex flex-col gap-1">
                {resolution.ragaCandidates.map((c, i) => (
                  <button
                    key={i}
                    onClick={() => setOverrideRaga(c.entity.name)}
                    className="text-left text-xs px-2 py-1 bg-white border border-indigo-100 rounded hover:border-indigo-300 flex justify-between items-center group"
                  >
                    <span>{c.entity.name}</span>
                    <span className={`text-[9px] font-bold ${c.confidence === 'HIGH' ? 'text-green-600' : 'text-amber-600'}`}>{c.score}%</span>
                  </button>
                ))}
              </div>
            )}
          </div>
          <div>
            <div className="text-[10px] font-semibold text-indigo-500 mb-1">DEITY</div>
            {!resolution.deityCandidates || resolution.deityCandidates.length === 0 ? <span className="text-xs italic text-gray-400">No matches</span> : (
              <div className="flex flex-col gap-1">
                {resolution.deityCandidates.map((c, i) => (
                  <button
                    key={i}
                    onClick={() => setOverrideDeity(c.entity.name)}
                    className="text-left text-xs px-2 py-1 bg-white border border-indigo-100 rounded hover:border-indigo-300 flex justify-between items-center group"
                  >
                    <span>{c.entity.name}</span>
                    <span className={`text-[9px] font-bold ${c.confidence === 'HIGH' ? 'text-green-600' : 'text-amber-600'}`}>{c.score}%</span>
                  </button>
                ))}
              </div>
            )}
          </div>
          <div>
            <div className="text-[10px] font-semibold text-indigo-500 mb-1">TEMPLE</div>
            {!resolution.templeCandidates || resolution.templeCandidates.length === 0 ? <span className="text-xs italic text-gray-400">No matches</span> : (
              <div className="flex flex-col gap-1">
                {resolution.templeCandidates.map((c, i) => (
                  <button
                    key={i}
                    onClick={() => setOverrideTemple(c.entity.name)}
                    className="text-left text-xs px-2 py-1 bg-white border border-indigo-100 rounded hover:border-indigo-300 flex justify-between items-center group"
                  >
                    <span>{c.entity.name}</span>
                    <span className={`text-[9px] font-bold ${c.confidence === 'HIGH' ? 'text-green-600' : 'text-amber-600'}`}>{c.score}%</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="max-w-7xl mx-auto h-[calc(100vh-140px)] flex flex-col">
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      <div className="mb-4 flex justify-between items-start">
        <div>
          <h1 className="text-2xl font-display font-bold text-ink-900">Review Queue</h1>
          <p className="text-sm text-ink-500">Review pending imports and resolve entity conflicts.</p>
        </div>
        {selectedImportIds.size > 0 && (
          <div className="flex gap-2">
            <span className="px-3 py-2 text-sm font-semibold text-ink-700 bg-slate-100 rounded-lg">
              {selectedImportIds.size} selected
            </span>
            <button
              onClick={handleBulkApprove}
              disabled={processing}
              className="px-4 py-2 text-sm font-semibold text-white bg-primary rounded-lg hover:bg-primary-dark disabled:opacity-50"
            >
              Approve Selected
            </button>
            <button
              onClick={handleBulkReject}
              disabled={processing}
              className="px-4 py-2 text-sm font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded-lg hover:bg-rose-100 disabled:opacity-50"
            >
              Reject Selected
            </button>
          </div>
        )}
      </div>

      <div className="flex-1 flex gap-6 overflow-hidden border border-border-light rounded-xl bg-white shadow-sm">
        {/* Sidebar List */}
        <div className="w-1/3 border-r border-border-light flex flex-col">
          <div className="p-3 border-b border-border-light bg-slate-50 flex justify-between items-center">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={imports.length > 0 && selectedImportIds.size === imports.length}
                onChange={toggleSelectAll}
                className="w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
              />
              <span className="text-xs font-bold text-ink-500 uppercase">Pending ({imports.length})</span>
            </div>
            <button onClick={loadImports} className="text-primary hover:text-primary-dark"><span className="material-symbols-outlined text-sm">refresh</span></button>
          </div>
          <div className="flex-1 overflow-y-auto">
            {loading ? (
              <div className="p-4 text-center text-sm text-gray-400">Loading...</div>
            ) : imports.length === 0 ? (
              <div className="p-4 text-center text-sm text-gray-400">Queue is empty.</div>
            ) : (
              imports.map(item => (
                <div
                  key={item.id}
                  className={`p-3 border-b border-border-light hover:bg-slate-50 ${selectedId === item.id ? 'bg-primary-50 border-l-4 border-l-primary' : 'border-l-4 border-l-transparent'}`}
                >
                  <div className="flex items-start gap-2">
                    <input
                      type="checkbox"
                      checked={selectedImportIds.has(item.id)}
                      onChange={(e) => {
                        e.stopPropagation();
                        toggleSelectImport(item.id);
                      }}
                      className="mt-1 w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer"
                    />
                    <div className="flex-1 cursor-pointer" onClick={() => selectImport(item)}>
                      <div className="font-semibold text-sm text-ink-900 truncate">{item.rawTitle || 'Untitled'}</div>
                      <div className="text-xs text-ink-500 truncate">{item.rawComposer} • {item.rawRaga}</div>
                      <div className="text-[10px] text-ink-400 mt-1 truncate">{item.sourceKey}</div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Detail View */}
        <div className="flex-1 flex flex-col overflow-hidden">
          {selectedItem ? (
            <>
              <div className="p-4 border-b border-border-light bg-slate-50 flex justify-between items-center">
                <div>
                  <div className="text-xs text-ink-400 uppercase font-bold tracking-wider flex items-center gap-2">
                    Source
                    {sourceDetail?.sourceTier != null && (
                      <TierBadge tier={sourceDetail.sourceTier} />
                    )}
                  </div>
                  <a href={selectedItem.sourceKey || '#'} target="_blank" rel="noreferrer" className="text-sm text-primary hover:underline truncate block max-w-md">
                    {selectedItem.sourceKey}
                  </a>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleReject}
                    disabled={processing}
                    className="px-3 py-1.5 text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded hover:bg-rose-100 disabled:opacity-50"
                  >
                    Reject
                  </button>
                  <button
                    onClick={handleApprove}
                    disabled={processing}
                    className="px-3 py-1.5 text-xs font-semibold text-white bg-primary border border-primary rounded hover:bg-primary-dark disabled:opacity-50"
                  >
                    {processing ? 'Processing...' : 'Approve & Create'}
                  </button>
                </div>
              </div>

              <div className="flex-1 overflow-y-auto p-6 space-y-6">
                {renderResolutionPanel()}

                {/* Authority Validation Warning (TRACK-052 §3.4) */}
                {sourceDetail && sourceDetail.sourceTier != null && sourceDetail.sourceTier > 2 && (
                  <AuthorityWarning
                    currentTier={sourceDetail.sourceTier}
                    conflicts={[]}
                    onDismiss={(reason) => {
                      setWarningDismissed(true);
                      console.log('Authority warning dismissed:', reason);
                      // TODO: Log dismissal to audit_log via API
                    }}
                    dismissed={warningDismissed}
                  />
                )}

                <div className="grid grid-cols-2 gap-6">
                  <div className="space-y-4">
                    <h3 className="text-sm font-bold text-ink-900 border-b pb-2">Primary Metadata</h3>

                    <div>
                      <label className="block text-xs font-semibold text-ink-600 mb-1">Title</label>
                      <input
                        value={overrideTitle}
                        onChange={e => setOverrideTitle(e.target.value)}
                        className="w-full px-3 py-2 text-sm border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-ink-600 mb-1">Composer</label>
                      <input
                        value={overrideComposer}
                        onChange={e => setOverrideComposer(e.target.value)}
                        className="w-full px-3 py-2 text-sm border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-ink-600 mb-1">Raga</label>
                      <input
                        value={overrideRaga}
                        onChange={e => setOverrideRaga(e.target.value)}
                        className="w-full px-3 py-2 text-sm border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-ink-600 mb-1">Tala</label>
                      <input
                        value={overrideTala}
                        onChange={e => setOverrideTala(e.target.value)}
                        className="w-full px-3 py-2 text-sm border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                      />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-semibold text-ink-600 mb-1">Deity</label>
                        <input
                          value={overrideDeity}
                          onChange={e => setOverrideDeity(e.target.value)}
                          className="w-full px-3 py-2 text-sm border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold text-ink-600 mb-1">Temple</label>
                        <input
                          value={overrideTemple}
                          onChange={e => setOverrideTemple(e.target.value)}
                          className="w-full px-3 py-2 text-sm border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-ink-600 mb-1">Language</label>
                      <input
                        value={overrideLanguage}
                        onChange={e => setOverrideLanguage(e.target.value)}
                        className="w-full px-3 py-2 text-sm border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                      />
                    </div>
                  </div>

                  <div className="space-y-4">
                    <h3 className="text-sm font-bold text-ink-900 border-b pb-2">Lyrics & Content</h3>
                    <div>
                      <label className="block text-xs font-semibold text-ink-600 mb-1">Lyrics Preview</label>
                      <textarea
                        value={overrideLyrics}
                        onChange={e => setOverrideLyrics(e.target.value)}
                        rows={20}
                        className="w-full px-3 py-2 text-sm font-mono text-xs border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-ink-400 text-sm">
              Select an import to review.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ImportReviewPage;

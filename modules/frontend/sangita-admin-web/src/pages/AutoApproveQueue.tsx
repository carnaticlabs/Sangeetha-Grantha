import React, { useEffect, useState } from 'react';
import { ImportedKrithi, ImportBatch } from '../types';
import { getAutoApproveQueue, bulkReviewImports, listBulkImportBatches } from '../api/client';
import { useToast, ToastContainer } from '../components/Toast';

const AutoApproveQueue: React.FC = () => {
  const { toasts, success, error, removeToast } = useToast();
  const [imports, setImports] = useState<ImportedKrithi[]>([]);
  const [batches, setBatches] = useState<ImportBatch[]>([]);
  const [loading, setLoading] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [selectedBatchId, setSelectedBatchId] = useState<string | null>(null);
  const [selectedQualityTier, setSelectedQualityTier] = useState<string | null>(null);
  const [confidenceMin, setConfidenceMin] = useState<number>(0.9);

  useEffect(() => {
    loadBatches();
    loadQueue();
  }, []);

  useEffect(() => {
    loadQueue();
  }, [selectedBatchId, selectedQualityTier, confidenceMin]);

  const loadBatches = async () => {
    try {
      const data = await listBulkImportBatches(undefined, 100, 0);
      setBatches(data);
    } catch (e) {
      console.error('Failed to load batches', e);
    }
  };

  const loadQueue = async () => {
    setLoading(true);
    try {
      const params: any = {};
      if (selectedBatchId) params.batchId = selectedBatchId;
      if (selectedQualityTier) params.qualityTier = selectedQualityTier;
      if (confidenceMin !== undefined) params.confidenceMin = confidenceMin;

      const data = await getAutoApproveQueue(params);
      setImports(data);
    } catch (e) {
      error('Failed to load auto-approve queue');
    } finally {
      setLoading(false);
    }
  };

  const handleBulkApprove = async () => {
    if (imports.length === 0) {
      error('No imports in queue');
      return;
    }
    if (!confirm(`Auto-approve all ${imports.length} imports in this queue?`)) return;

    setProcessing(true);
    try {
      const importIds = imports.map(i => i.id);
      const result = await bulkReviewImports(importIds, 'APPROVE');
      success(`Approved ${result.succeeded} imports. ${result.failed} failed.`);
      await loadQueue();
    } catch (e) {
      error('Failed to bulk approve imports');
    } finally {
      setProcessing(false);
    }
  };

  const getQualityTierColor = (tier: string | null) => {
    switch (tier) {
      case 'EXCELLENT': return 'bg-green-50 text-green-700 border-green-200';
      case 'GOOD': return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'FAIR': return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'POOR': return 'bg-rose-50 text-rose-700 border-rose-200';
      default: return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-2xl font-display font-bold text-ink-900">Auto-Approve Queue</h1>
          <p className="text-sm text-ink-500">Review and approve high-confidence imports automatically</p>
        </div>
        {imports.length > 0 && (
          <button
            onClick={handleBulkApprove}
            disabled={processing}
            className="px-6 py-3 text-sm font-semibold text-white bg-primary rounded-lg hover:bg-primary-dark disabled:opacity-50 shadow-sm"
          >
            {processing ? 'Processing...' : `Approve All (${imports.length})`}
          </button>
        )}
      </div>

      {/* Filters */}
      <div className="bg-white border border-border-light rounded-xl shadow-sm p-6 space-y-4">
        <h3 className="text-sm font-bold text-ink-900">Filters</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-semibold text-ink-600 mb-1">Batch</label>
            <select
              value={selectedBatchId || ''}
              onChange={(e) => setSelectedBatchId(e.target.value || null)}
              className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            >
              <option value="">All Batches</option>
              {batches.map(batch => (
                <option key={batch.id} value={batch.id}>
                  {batch.sourceManifest}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-ink-600 mb-1">Quality Tier</label>
            <select
              value={selectedQualityTier || ''}
              onChange={(e) => setSelectedQualityTier(e.target.value || null)}
              className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            >
              <option value="">All Tiers</option>
              <option value="EXCELLENT">EXCELLENT</option>
              <option value="GOOD">GOOD</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-ink-600 mb-1">Min Confidence Score</label>
            <select
              value={confidenceMin}
              onChange={(e) => setConfidenceMin(Number(e.target.value))}
              className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            >
              <option value="0.95">95% (Very High)</option>
              <option value="0.90">90% (High)</option>
              <option value="0.85">85% (Good)</option>
              <option value="0.80">80% (Moderate)</option>
            </select>
          </div>
        </div>
      </div>

      {/* Queue List */}
      <div className="bg-white border border-border-light rounded-xl shadow-sm">
        <div className="px-6 py-4 border-b border-border-light">
          <h3 className="text-sm font-bold text-ink-900">Eligible for Auto-Approval ({imports.length})</h3>
          <p className="text-xs text-ink-500 mt-1">All items have high confidence scores and no duplicate conflicts</p>
        </div>

        {loading ? (
          <div className="py-12 text-center text-ink-400">Loading queue...</div>
        ) : imports.length === 0 ? (
          <div className="py-12 text-center text-ink-400">No imports eligible for auto-approval</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-ink-500 border-b border-border-light">
                <tr>
                  <th className="px-4 py-3 text-left">Title</th>
                  <th className="px-4 py-3 text-left">Composer</th>
                  <th className="px-4 py-3 text-left">Raga</th>
                  <th className="px-4 py-3 text-left">Quality</th>
                  <th className="px-4 py-3 text-left">Source</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-light">
                {imports.map(item => (
                  <tr key={item.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="font-semibold text-ink-900">{item.rawTitle || 'Untitled'}</div>
                    </td>
                    <td className="px-4 py-3 text-ink-700">{item.rawComposer || '—'}</td>
                    <td className="px-4 py-3 text-ink-700">{item.rawRaga || '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-col gap-1">
                        {item.qualityTier && (
                          <span className={`px-2 py-1 rounded-full text-[10px] font-bold border ${getQualityTierColor(item.qualityTier)}`}>
                            {item.qualityTier}
                          </span>
                        )}
                        {item.qualityScore !== null && (
                          <span className="text-xs text-ink-500">Score: {(item.qualityScore * 100).toFixed(0)}%</span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <a
                        href={item.sourceKey || '#'}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs text-primary hover:underline truncate block max-w-xs"
                      >
                        {item.sourceKey}
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default AutoApproveQueue;

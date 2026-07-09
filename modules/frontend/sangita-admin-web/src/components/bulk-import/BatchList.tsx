import React from 'react';
import { ImportBatch } from '../../types';
import { basename, batchStatusLabel, formatDate, statusChip } from '../../utils/bulk-import-format';

export type BatchRowAction = 'retry' | 'pause' | 'resume' | 'delete';

/** Batch table with per-row retry/pause-resume/delete actions. */
export const BatchList: React.FC<{
  batches: ImportBatch[];
  loading: boolean;
  selectedBatchId: string | null;
  onSelect: (batchId: string) => void;
  onRefresh: () => void;
  onAction: (action: BatchRowAction, batchId: string) => void;
}> = ({ batches, loading, selectedBatchId, onSelect, onRefresh, onAction }) => (
  <div className="xl:col-span-2 bg-white border border-border-light rounded-xl shadow-sm min-w-0">
    <div className="flex items-center justify-between px-4 py-3 border-b border-border-light">
      <div>
        <h3 className="text-sm font-bold text-ink-900">Batches</h3>
        <p className="text-xs text-ink-500">Recent uploads and their progress.</p>
      </div>
      <button
        onClick={onRefresh}
        className="text-sm text-primary font-semibold hover:text-primary-dark flex items-center gap-1"
      >
        <span className="material-symbols-outlined text-base">refresh</span>
        Refresh
      </button>
    </div>
    {loading ? (
      <div className="py-10 text-center text-ink-400">Loading batches…</div>
    ) : batches.length === 0 ? (
      <div className="py-10 text-center text-ink-400">No batches found.</div>
    ) : (
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-ink-500 border-b border-border-light">
            <tr>
              <th className="px-4 py-3 text-left">Manifest</th>
              <th className="px-4 py-3 text-left">Status</th>
              <th className="px-4 py-3 text-left">Progress</th>
              <th className="px-4 py-3 text-left">Created</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {batches.map(batch => {
              const progress =
                batch.totalTasks > 0 ? Math.round((batch.processedTasks / batch.totalTasks) * 100) : 0;
              return (
                <tr
                  key={batch.id}
                  className={`border-b border-border-light hover:bg-slate-50 cursor-pointer ${batch.id === selectedBatchId ? 'bg-slate-50' : ''
                    }`}
                  onClick={() => onSelect(batch.id)}
                >
                  <td className="px-4 py-3 max-w-[260px]">
                    <div className="font-semibold text-ink-900 truncate" title={batch.sourceManifest}>
                      {basename(batch.sourceManifest || '') || batch.sourceManifest}
                    </div>
                    <div className="text-[11px] text-ink-500">{formatDate(batch.createdAt)?.split(',')[0]}</div>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded-full text-[11px] font-semibold ${statusChip[batch.status]}`}>
                      {batchStatusLabel[batch.status]}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <div className="h-2 w-28 bg-slate-100 rounded-full overflow-hidden">
                        <div className="h-full bg-primary" style={{ width: `${progress}%` }}></div>
                      </div>
                      <span className="text-xs text-ink-600 tabular-nums">
                        {batch.processedTasks}/{batch.totalTasks || 0}
                      </span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-xs text-ink-500">{formatDate(batch.createdAt)}</td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={e => {
                          e.stopPropagation();
                          onAction('retry', batch.id);
                        }}
                        className="px-2 py-1 text-xs rounded-md border border-border-light hover:border-primary text-primary font-semibold"
                      >
                        Retry
                      </button>
                      {batch.status === 'RUNNING' ? (
                        <button
                          onClick={e => {
                            e.stopPropagation();
                            onAction('pause', batch.id);
                          }}
                          className="px-2 py-1 text-xs rounded-md border border-amber-200 text-amber-700 bg-amber-50"
                        >
                          Pause
                        </button>
                      ) : (
                        <button
                          onClick={e => {
                            e.stopPropagation();
                            onAction('resume', batch.id);
                          }}
                          className="px-2 py-1 text-xs rounded-md border border-primary text-primary bg-primary-light"
                        >
                          Resume
                        </button>
                      )}
                      <button
                        onClick={e => {
                          e.stopPropagation();
                          onAction('delete', batch.id);
                        }}
                        className="px-2 py-1 text-xs rounded-md border border-slate-200 text-slate-700 bg-slate-50 hover:bg-slate-100"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    )}
  </div>
);

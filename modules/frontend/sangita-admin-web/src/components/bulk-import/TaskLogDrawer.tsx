import React from 'react';
import { ImportTaskRun } from '../../types';
import { formatDuration, taskChip, taskStatusLabel } from '../../utils/bulk-import-format';

/** Slide-in drawer showing one import task's status, timing, and error detail. */
export const TaskLogDrawer: React.FC<{
  task: ImportTaskRun;
  onClose: () => void;
  onCopied: () => void;
}> = ({ task, onClose, onCopied }) => (
  <div className="fixed inset-0 bg-black/50 flex items-end justify-end z-50" onClick={onClose}>
    <div
      className="bg-white h-full w-full md:w-2/3 lg:w-1/2 shadow-2xl overflow-hidden flex flex-col animate-slide-in-right"
      onClick={(e) => e.stopPropagation()}
    >
      <div className="px-6 py-4 border-b border-border-light bg-slate-50 flex justify-between items-center">
        <div>
          <h3 className="text-lg font-bold text-ink-900">Composition Details</h3>
          <p className="text-xs text-ink-500 truncate max-w-md">{task.sourceUrl || task.krithiKey}</p>
        </div>
        <button
          onClick={onClose}
          className="text-ink-400 hover:text-ink-600"
        >
          <span className="material-symbols-outlined">close</span>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {/* Task Overview */}
        <div className="bg-slate-50 rounded-lg p-4 space-y-3">
          <h4 className="text-sm font-bold text-ink-900">Overview</h4>
          <div className="grid grid-cols-2 gap-4 text-xs">
            <div>
              <div className="text-ink-500 font-semibold mb-1">Status</div>
              <span className={`px-2 py-1 rounded-full text-[10px] font-bold ${taskChip[task.status]}`}>
                {taskStatusLabel[task.status]}
              </span>
            </div>
            <div>
              <div className="text-ink-500 font-semibold mb-1">Try #</div>
              <div className="text-ink-700">{task.attempt}</div>
            </div>
            <div>
              <div className="text-ink-500 font-semibold mb-1">Time Taken</div>
              <div className="text-ink-700">{formatDuration(task.durationMs)}</div>
            </div>
            <div className="col-span-2">
              <div className="text-ink-500 font-semibold mb-1">Source URL</div>
              <a
                href={task.sourceUrl || '#'}
                target="_blank"
                rel="noreferrer"
                className="text-primary hover:underline text-[11px] break-all"
              >
                {task.sourceUrl || 'N/A'}
              </a>
            </div>
          </div>
        </div>

        {/* Error Details */}
        {task.error && (
          <div className="bg-rose-50 border border-rose-200 rounded-lg p-4 space-y-2">
            <h4 className="text-sm font-bold text-rose-900 flex items-center gap-2">
              <span className="material-symbols-outlined text-base">error</span>
              Error Details
            </h4>
            <pre className="text-xs font-mono text-rose-700 whitespace-pre-wrap break-words bg-white p-3 rounded border border-rose-100 overflow-x-auto">
              {task.error}
            </pre>
          </div>
        )}

        {/* Task Payload (if available) */}
        {task.krithiKey && (
          <div className="border border-border-light rounded-lg p-4 space-y-2">
            <h4 className="text-sm font-bold text-ink-900">Task Data</h4>
            <div className="text-xs space-y-1">
              <div className="flex justify-between">
                <span className="text-ink-500 font-semibold">Krithi Key:</span>
                <span className="text-ink-700 font-mono">{task.krithiKey}</span>
              </div>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-3">
          <button
            onClick={() => {
              navigator.clipboard.writeText(JSON.stringify(task, null, 2));
              onCopied();
            }}
            className="flex-1 px-4 py-2 text-sm font-medium text-primary border border-primary rounded-lg hover:bg-primary-light"
          >
            Copy Details
          </button>
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-ink-600 border border-border-light rounded-lg hover:bg-slate-50"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  </div>
);

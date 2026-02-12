import React from 'react';
import type { TimelineEvent } from '../../../types/sourcing';

interface TimelineCardProps {
  events: TimelineEvent[];
}

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60_000) return `${(ms / 1000).toFixed(1)}s`;
  if (ms < 3_600_000) return `${Math.floor(ms / 60_000)}m ${Math.floor((ms % 60_000) / 1000)}s`;
  return `${Math.floor(ms / 3_600_000)}h ${Math.floor((ms % 3_600_000) / 60_000)}m`;
}

function formatTimestamp(iso: string): string {
  try {
    return new Date(iso).toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  } catch {
    return iso;
  }
}

const TimelineCard: React.FC<TimelineCardProps> = ({ events }) => {
  if (events.length === 0) {
    return (
      <div className="text-sm text-ink-400 italic">No timeline events</div>
    );
  }

  return (
    <div className="relative" role="list" aria-label="Processing timeline">
      {events.map((event, i) => {
        const isLast = i === events.length - 1;
        return (
          <div key={i} className="relative flex gap-4 pb-6 last:pb-0" role="listitem">
            {/* Vertical line */}
            {!isLast && (
              <div className="absolute left-[11px] top-6 w-0.5 h-[calc(100%-12px)] bg-slate-200" />
            )}

            {/* Dot */}
            <div className="relative z-10 flex-shrink-0">
              <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center ${
                isLast
                  ? 'border-primary bg-primary/10'
                  : 'border-slate-300 bg-white'
              }`}>
                <div className={`w-2 h-2 rounded-full ${isLast ? 'bg-primary' : 'bg-slate-300'}`} />
              </div>
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
              <div className="flex items-baseline gap-2">
                <span className="text-sm font-semibold text-ink-800">{event.state}</span>
                {event.duration != null && (
                  <span className="text-xs text-ink-400 tabular-nums">{formatDuration(event.duration)}</span>
                )}
              </div>
              <div className="text-xs text-ink-500 mt-0.5">{formatTimestamp(event.timestamp)}</div>
              {event.actor && (
                <div className="text-xs text-ink-400 mt-0.5">
                  <span className="material-symbols-outlined text-xs align-text-bottom mr-0.5">person</span>
                  {event.actor}
                </div>
              )}
              {event.detail && (
                <div className="text-xs text-ink-500 mt-1 bg-slate-50 rounded px-2 py-1">{event.detail}</div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default TimelineCard;

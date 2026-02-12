import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useExtractionDetail, useRetryExtraction, useCancelExtraction } from '../../hooks/useSourcingQueries';
import { StatusChip, TierBadge, FormatPill, ConfidenceBar, MetricCard } from '../../components/sourcing/shared';
import VariantMatchReportComponent from '../../components/sourcing/VariantMatchReport';

type DetailTab = 'overview' | 'variants';

const ExtractionDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { data: detail, isLoading, error } = useExtractionDetail(id ?? '', 5000);
  const retryMutation = useRetryExtraction();
  const cancelMutation = useCancelExtraction();
  const [activeTab, setActiveTab] = useState<DetailTab>('overview');

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="bg-rose-50 border border-rose-200 rounded-lg p-6 text-center">
        <span className="material-symbols-outlined text-rose-500 text-3xl mb-2">error</span>
        <p className="text-sm text-rose-700">Failed to load extraction detail.</p>
        <Link to="/admin/sourcing/extractions" className="text-sm text-primary hover:underline mt-2 inline-block">
          Back to Extraction Queue
        </Link>
      </div>
    );
  }

  const canRetry = detail.status === 'FAILED' || detail.status === 'CANCELLED';
  const canCancel = detail.status === 'PENDING' || detail.status === 'PROCESSING';
  const isEnrich = detail.extractionIntent === 'ENRICH';
  const showVariantTab = isEnrich && (detail.status === 'DONE' || detail.status === 'INGESTED');

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Link to="/admin/sourcing/extractions" className="text-ink-400 hover:text-primary transition-colors">
            <span className="material-symbols-outlined text-xl">arrow_back</span>
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-display font-bold text-ink-900">Extraction Detail</h1>
              {isEnrich && (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-bold bg-amber-100 text-amber-800 rounded-full">
                  <span className="material-symbols-outlined text-sm">enhanced_encryption</span>
                  ENRICH
                </span>
              )}
            </div>
            <p className="text-sm text-ink-500 mt-0.5 font-mono">Task ID: {id?.slice(0, 12)}...</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {canRetry && (
            <button
              onClick={() => id && retryMutation.mutate(id)}
              disabled={retryMutation.isPending}
              className="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-primary border border-primary/30 rounded-lg hover:bg-primary-light"
            >
              <span className="material-symbols-outlined text-base">replay</span>
              Retry
            </button>
          )}
          {canCancel && (
            <button
              onClick={() => id && cancelMutation.mutate(id)}
              disabled={cancelMutation.isPending}
              className="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-rose-600 border border-rose-200 rounded-lg hover:bg-rose-50"
            >
              <span className="material-symbols-outlined text-base">cancel</span>
              Cancel
            </button>
          )}
        </div>
      </div>

      {/* Status Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <MetricCard label="Status" value={detail.status} />
        <MetricCard label="Attempts" value={`${detail.attempts} / ${detail.maxAttempts}`} />
        <MetricCard label="Confidence" value={detail.confidence != null ? `${(detail.confidence * 100).toFixed(1)}%` : '—'} />
        <MetricCard label="Duration" value={detail.durationMs != null ? `${(detail.durationMs / 1000).toFixed(1)}s` : '—'} />
      </div>

      {/* Tabs (only show if enrichment) */}
      {showVariantTab && (
        <div className="flex items-center gap-1 border-b border-border-light mb-6">
          <button
            onClick={() => setActiveTab('overview')}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${activeTab === 'overview'
                ? 'border-primary text-primary'
                : 'border-transparent text-ink-500 hover:text-ink-700'
              }`}
          >
            <span className="material-symbols-outlined text-sm align-middle mr-1">info</span>
            Overview
          </button>
          <button
            onClick={() => setActiveTab('variants')}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${activeTab === 'variants'
                ? 'border-amber-500 text-amber-700'
                : 'border-transparent text-ink-500 hover:text-ink-700'
              }`}
          >
            <span className="material-symbols-outlined text-sm align-middle mr-1">compare</span>
            Variant Matches
          </button>
        </div>
      )}

      {/* Tab Content */}
      {activeTab === 'variants' && showVariantTab && id ? (
        <VariantMatchReportComponent extractionId={id} />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Request Parameters */}
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white rounded-xl border border-border-light p-5">
              <h2 className="text-lg font-semibold text-ink-800 mb-4">Request Parameters</h2>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-xs font-semibold text-ink-500 block mb-1">Source</span>
                  <div className="flex items-center gap-2">
                    {detail.sourceName && (
                      <span className="text-ink-700 font-medium">{detail.sourceName}</span>
                    )}
                    {detail.sourceTier != null && <TierBadge tier={detail.sourceTier} />}
                  </div>
                </div>
                <div>
                  <span className="text-xs font-semibold text-ink-500 block mb-1">Format</span>
                  <FormatPill format={detail.sourceFormat} />
                </div>
                <div>
                  <span className="text-xs font-semibold text-ink-500 block mb-1">Status</span>
                  <StatusChip status={detail.status} />
                </div>
                <div>
                  <span className="text-xs font-semibold text-ink-500 block mb-1">Confidence</span>
                  {detail.confidence != null ? (
                    <div className="w-32"><ConfidenceBar value={detail.confidence} showLabel /></div>
                  ) : <span className="text-ink-400">—</span>}
                </div>
                {/* Language & Intent (TRACK-057) */}
                {detail.contentLanguage && (
                  <div>
                    <span className="text-xs font-semibold text-ink-500 block mb-1">Language</span>
                    <span className="text-sm text-ink-700 font-medium">{detail.contentLanguage.toUpperCase()}</span>
                  </div>
                )}
                {detail.extractionIntent && (
                  <div>
                    <span className="text-xs font-semibold text-ink-500 block mb-1">Intent</span>
                    <span className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-bold rounded-full ${detail.extractionIntent === 'ENRICH'
                        ? 'bg-amber-100 text-amber-800'
                        : 'bg-blue-100 text-blue-800'
                      }`}>
                      {detail.extractionIntent}
                    </span>
                  </div>
                )}
                <div className="col-span-2">
                  <span className="text-xs font-semibold text-ink-500 block mb-1">Source URL</span>
                  {detail.sourceUrl ? (
                    <a href={detail.sourceUrl} target="_blank" rel="noreferrer" className="text-primary text-xs hover:underline break-all">
                      {detail.sourceUrl}
                    </a>
                  ) : <span className="text-ink-400">—</span>}
                </div>
              </div>
            </div>

            {/* Error Detail */}
            {detail.errorDetail && (
              <div className="bg-rose-50 border border-rose-200 rounded-xl p-5">
                <h2 className="text-lg font-semibold text-rose-800 mb-2 flex items-center gap-2">
                  <span className="material-symbols-outlined">error</span>
                  Error Detail
                </h2>
                <pre className="text-xs font-mono text-rose-700 whitespace-pre-wrap break-words bg-white p-3 rounded border border-rose-100 overflow-x-auto">
                  {JSON.stringify(detail.errorDetail, null, 2)}
                </pre>
              </div>
            )}

            {/* Result Payload */}
            {detail.resultPayload && (
              <div className="bg-white rounded-xl border border-border-light p-5">
                <h2 className="text-lg font-semibold text-ink-800 mb-4">Result Payload</h2>
                <pre className="text-xs font-mono text-ink-600 whitespace-pre-wrap break-words bg-slate-50 p-3 rounded border border-border-light overflow-x-auto max-h-96 overflow-y-auto">
                  {JSON.stringify(detail.resultPayload, null, 2)}
                </pre>
              </div>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            <div className="bg-white rounded-xl border border-border-light p-5">
              <h2 className="text-lg font-semibold text-ink-800 mb-4">Timeline</h2>
              <div className="space-y-3">
                <div className="flex items-start gap-3">
                  <div className="w-2 h-2 bg-slate-300 rounded-full mt-1.5 flex-shrink-0" />
                  <div>
                    <div className="text-xs font-semibold text-ink-700">Created</div>
                    <div className="text-[11px] text-ink-400">{new Date(detail.createdAt).toLocaleString()}</div>
                  </div>
                </div>
                {detail.claimedAt && (
                  <div className="flex items-start gap-3">
                    <div className="w-2 h-2 bg-blue-400 rounded-full mt-1.5 flex-shrink-0" />
                    <div>
                      <div className="text-xs font-semibold text-ink-700">Processing Started</div>
                      <div className="text-[11px] text-ink-400">{new Date(detail.claimedAt).toLocaleString()}</div>
                    </div>
                  </div>
                )}
                <div className="flex items-start gap-3">
                  <div className="w-2 h-2 bg-slate-200 rounded-full mt-1.5 flex-shrink-0" />
                  <div>
                    <div className="text-xs font-semibold text-ink-700">Last Updated</div>
                    <div className="text-[11px] text-ink-400">{new Date(detail.updatedAt).toLocaleString()}</div>
                  </div>
                </div>
              </div>
            </div>

            {/* Quick Links */}
            <div className="bg-white rounded-xl border border-border-light p-5">
              <h2 className="text-sm font-semibold text-ink-800 mb-3">Related</h2>
              <div className="space-y-2">
                {detail.importSourceId && (
                  <Link
                    to={`/admin/sourcing/sources/${detail.importSourceId}`}
                    className="flex items-center gap-2 text-xs text-primary hover:underline"
                  >
                    <span className="material-symbols-outlined text-sm">source</span>
                    View Source Detail
                  </Link>
                )}
                {detail.relatedExtractionId && (
                  <Link
                    to={`/admin/sourcing/extractions/${detail.relatedExtractionId}`}
                    className="flex items-center gap-2 text-xs text-amber-600 hover:underline"
                  >
                    <span className="material-symbols-outlined text-sm">link</span>
                    View Related (Primary) Extraction
                  </Link>
                )}
                <Link
                  to="/admin/sourcing/evidence"
                  className="flex items-center gap-2 text-xs text-primary hover:underline"
                >
                  <span className="material-symbols-outlined text-sm">compare</span>
                  Source Evidence Browser
                </Link>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ExtractionDetailPage;

// =============================================================================
// Sourcing & Extraction Monitoring — API Client
// Reference: application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md §5
// =============================================================================

import type {
  SourceListResponse,
  SourceDetail,
  CreateSourceRequest,
  UpdateSourceRequest,
  ImportSource,
  ExtractionListResponse,
  ExtractionDetail,
  CreateExtractionRequest,
  ExtractionStatsResponse,
  EvidenceListResponse,
  KrithiEvidenceResponse,
  FieldComparisonResponse,
  VotingListResponse,
  VotingDetailResponse,
  ManualOverrideRequest,
  VotingStatsResponse,
  QualitySummary,
  QualityDistribution,
  CoverageData,
  GapAnalysis,
  AuditResultResponse,
  SourceFilterParams,
  ExtractionFilterParams,
  EvidenceFilterParams,
  VotingFilterParams,
  VariantMatchListResponse,
  VariantMatchReport,
  VariantMatchReviewRequest,
} from '../types/sourcing';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/v1';

const getAuthToken = () => localStorage.getItem('authToken');

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers as Record<string, string> || {}),
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`API Error ${response.status}: ${errorBody || response.statusText}`);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

function buildParams(params: Record<string, unknown>): string {
  const searchParams = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null) continue;
    if (Array.isArray(value)) {
      value.forEach((v) => searchParams.append(key, String(v)));
    } else {
      searchParams.append(key, String(value));
    }
  }
  const qs = searchParams.toString();
  return qs ? `?${qs}` : '';
}

// =============================================================================
// §5.1 — Source Registry API
// =============================================================================

export const listSources = (params?: SourceFilterParams) =>
  request<SourceListResponse>(`/admin/sourcing/sources${buildParams(params ?? {})}`);

export const getSource = (id: string) =>
  request<SourceDetail>(`/admin/sourcing/sources/${id}`);

export const createSource = (payload: CreateSourceRequest) =>
  request<ImportSource>('/admin/sourcing/sources', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const updateSource = (id: string, payload: UpdateSourceRequest) =>
  request<ImportSource>(`/admin/sourcing/sources/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });

export const deactivateSource = (id: string) =>
  request<void>(`/admin/sourcing/sources/${id}`, {
    method: 'DELETE',
  });

// =============================================================================
// §5.2 — Extraction Queue API
// =============================================================================

export const listExtractions = (params?: ExtractionFilterParams) =>
  request<ExtractionListResponse>(`/admin/sourcing/extractions${buildParams(params ?? {})}`);

export const getExtraction = (id: string) =>
  request<ExtractionDetail>(`/admin/sourcing/extractions/${id}`);

export const createExtraction = (payload: CreateExtractionRequest) =>
  request<ExtractionDetail>('/admin/sourcing/extractions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const retryExtraction = (id: string) =>
  request<void>(`/admin/sourcing/extractions/${id}/retry`, { method: 'POST' });

export const cancelExtraction = (id: string) =>
  request<void>(`/admin/sourcing/extractions/${id}/cancel`, { method: 'POST' });

export const retryAllFailedExtractions = () =>
  request<{ retriedCount: number }>('/admin/sourcing/extractions/retry-all-failed', { method: 'POST' });

export const getExtractionStats = () =>
  request<ExtractionStatsResponse>('/admin/sourcing/extractions/stats');

export const processExtractionQueue = (batchSize: number = 50) =>
  request<{ totalTasks: number; processedTasks: number }>(`/admin/quality/extraction/process?batchSize=${batchSize}`, { method: 'POST' });

// =============================================================================
// §5.3 — Source Evidence API
// =============================================================================

export const listEvidence = (params?: EvidenceFilterParams) =>
  request<EvidenceListResponse>(`/admin/sourcing/evidence${buildParams(params ?? {})}`);

export const getKrithiEvidence = (krithiId: string) =>
  request<KrithiEvidenceResponse>(`/admin/sourcing/evidence/krithi/${krithiId}`);

export const getFieldComparison = (krithiId: string) =>
  request<FieldComparisonResponse>(`/admin/sourcing/evidence/compare/${krithiId}`);

// =============================================================================
// §5.4 — Structural Voting API
// =============================================================================

export const listVotingDecisions = (params?: VotingFilterParams) =>
  request<VotingListResponse>(`/admin/sourcing/voting${buildParams(params ?? {})}`);

export const getVotingDetail = (id: string) =>
  request<VotingDetailResponse>(`/admin/sourcing/voting/${id}`);

export const submitOverride = (id: string, payload: ManualOverrideRequest) =>
  request<void>(`/admin/sourcing/voting/${id}/override`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const getVotingStats = () =>
  request<VotingStatsResponse>('/admin/sourcing/voting/stats');

// =============================================================================
// §5.6 — Variant Matching API (TRACK-056)
// =============================================================================

export const listVariantMatches = (extractionId: string, params?: { status?: string[]; page?: number; pageSize?: number }) =>
  request<VariantMatchListResponse>(`/admin/sourcing/variants/extraction/${extractionId}${buildParams(params ?? {})}`);

export const listPendingVariantMatches = (params?: { page?: number; pageSize?: number }) =>
  request<VariantMatchListResponse>(`/admin/sourcing/variants/pending${buildParams(params ?? {})}`);

export const getVariantMatchReport = (extractionId: string) =>
  request<VariantMatchReport>(`/admin/sourcing/variants/extraction/${extractionId}/report`);

export const reviewVariantMatch = (matchId: string, payload: VariantMatchReviewRequest) =>
  request<{ status: string }>(`/admin/sourcing/variants/${matchId}/review`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

// =============================================================================
// §5.5 — Quality Dashboard API
// =============================================================================

export const getQualitySummary = () =>
  request<QualitySummary>('/admin/sourcing/quality/summary');

export const getQualityDistribution = () =>
  request<QualityDistribution>('/admin/sourcing/quality/distribution');

export const getQualityCoverage = () =>
  request<CoverageData>('/admin/sourcing/quality/coverage');

export const getQualityGaps = () =>
  request<GapAnalysis>('/admin/sourcing/quality/gaps');

export const getAuditResults = () =>
  request<AuditResultResponse>('/admin/sourcing/quality/audit');

export const runAudit = () =>
  request<AuditResultResponse>('/admin/sourcing/quality/audit/run', { method: 'POST' });

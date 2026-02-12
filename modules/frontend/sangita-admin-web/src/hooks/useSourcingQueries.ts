// =============================================================================
// Sourcing & Extraction Monitoring — TanStack Query Hooks
// =============================================================================

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  SourceFilterParams,
  ExtractionFilterParams,
  EvidenceFilterParams,
  VotingFilterParams,
  CreateSourceRequest,
  UpdateSourceRequest,
  CreateExtractionRequest,
  ManualOverrideRequest,
  VariantMatchReviewRequest,
} from '../types/sourcing';
import * as api from '../api/sourcingApi';

// --- Query Key Factory ---

export const sourcingKeys = {
  all: ['sourcing'] as const,

  // Sources
  sources: () => [...sourcingKeys.all, 'sources'] as const,
  sourceList: (filters?: SourceFilterParams) => [...sourcingKeys.sources(), 'list', filters] as const,
  sourceDetail: (id: string) => [...sourcingKeys.sources(), 'detail', id] as const,

  // Extractions
  extractions: () => [...sourcingKeys.all, 'extractions'] as const,
  extractionList: (filters?: ExtractionFilterParams) => [...sourcingKeys.extractions(), 'list', filters] as const,
  extractionDetail: (id: string) => [...sourcingKeys.extractions(), 'detail', id] as const,
  extractionStats: () => [...sourcingKeys.extractions(), 'stats'] as const,

  // Evidence
  evidence: () => [...sourcingKeys.all, 'evidence'] as const,
  evidenceList: (filters?: EvidenceFilterParams) => [...sourcingKeys.evidence(), 'list', filters] as const,
  krithiEvidence: (krithiId: string) => [...sourcingKeys.evidence(), 'krithi', krithiId] as const,
  fieldComparison: (krithiId: string) => [...sourcingKeys.evidence(), 'compare', krithiId] as const,

  // Voting
  voting: () => [...sourcingKeys.all, 'voting'] as const,
  votingList: (filters?: VotingFilterParams) => [...sourcingKeys.voting(), 'list', filters] as const,
  votingDetail: (id: string) => [...sourcingKeys.voting(), 'detail', id] as const,
  votingStats: () => [...sourcingKeys.voting(), 'stats'] as const,

  // Variant Matching
  variants: () => [...sourcingKeys.all, 'variants'] as const,
  variantMatches: (extractionId: string, status?: string[]) => [...sourcingKeys.variants(), 'matches', extractionId, status] as const,
  variantMatchReport: (extractionId: string) => [...sourcingKeys.variants(), 'report', extractionId] as const,
  pendingVariantMatches: () => [...sourcingKeys.variants(), 'pending'] as const,

  // Quality
  quality: () => [...sourcingKeys.all, 'quality'] as const,
  qualitySummary: () => [...sourcingKeys.quality(), 'summary'] as const,
  qualityDistribution: () => [...sourcingKeys.quality(), 'distribution'] as const,
  qualityCoverage: () => [...sourcingKeys.quality(), 'coverage'] as const,
  qualityGaps: () => [...sourcingKeys.quality(), 'gaps'] as const,
  auditResults: () => [...sourcingKeys.quality(), 'audit'] as const,
};

// =============================================================================
// Source Registry Hooks
// =============================================================================

export const useSourceList = (filters?: SourceFilterParams) =>
  useQuery({
    queryKey: sourcingKeys.sourceList(filters),
    queryFn: () => api.listSources(filters),
  });

export const useSourceDetail = (id: string) =>
  useQuery({
    queryKey: sourcingKeys.sourceDetail(id),
    queryFn: () => api.getSource(id),
    enabled: !!id,
  });

export const useCreateSource = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateSourceRequest) => api.createSource(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.sources() });
    },
  });
};

export const useUpdateSource = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateSourceRequest }) =>
      api.updateSource(id, payload),
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.sources() });
      queryClient.invalidateQueries({ queryKey: sourcingKeys.sourceDetail(id) });
    },
  });
};

export const useDeactivateSource = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.deactivateSource(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.sources() });
    },
  });
};

// =============================================================================
// Extraction Queue Hooks
// =============================================================================

export const useExtractionList = (filters?: ExtractionFilterParams, refetchInterval?: number) =>
  useQuery({
    queryKey: sourcingKeys.extractionList(filters),
    queryFn: () => api.listExtractions(filters),
    refetchInterval,
  });

export const useExtractionDetail = (id: string, refetchInterval?: number) =>
  useQuery({
    queryKey: sourcingKeys.extractionDetail(id),
    queryFn: () => api.getExtraction(id),
    enabled: !!id,
    refetchInterval,
  });

export const useExtractionStats = (refetchInterval?: number) =>
  useQuery({
    queryKey: sourcingKeys.extractionStats(),
    queryFn: () => api.getExtractionStats(),
    refetchInterval,
  });

export const useCreateExtraction = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateExtractionRequest) => api.createExtraction(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.extractions() });
    },
  });
};

export const useRetryExtraction = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.retryExtraction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.extractions() });
    },
  });
};

export const useCancelExtraction = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.cancelExtraction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.extractions() });
    },
  });
};

export const useRetryAllFailedExtractions = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.retryAllFailedExtractions(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.extractions() });
    },
  });
};

export const useProcessExtractionQueue = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (batchSize?: number) => api.processExtractionQueue(batchSize),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.extractions() });
      queryClient.invalidateQueries({ queryKey: sourcingKeys.qualitySummary() });
    },
  });
};

// =============================================================================
// Source Evidence Hooks
// =============================================================================

export const useEvidenceList = (filters?: EvidenceFilterParams) =>
  useQuery({
    queryKey: sourcingKeys.evidenceList(filters),
    queryFn: () => api.listEvidence(filters),
  });

export const useKrithiEvidence = (krithiId: string) =>
  useQuery({
    queryKey: sourcingKeys.krithiEvidence(krithiId),
    queryFn: () => api.getKrithiEvidence(krithiId),
    enabled: !!krithiId,
  });

export const useFieldComparison = (krithiId: string, enabled = true) =>
  useQuery({
    queryKey: sourcingKeys.fieldComparison(krithiId),
    queryFn: () => api.getFieldComparison(krithiId),
    enabled: !!krithiId && enabled,
  });

// =============================================================================
// Structural Voting Hooks
// =============================================================================

export const useVotingList = (filters?: VotingFilterParams) =>
  useQuery({
    queryKey: sourcingKeys.votingList(filters),
    queryFn: () => api.listVotingDecisions(filters),
  });

export const useVotingDetail = (id: string) =>
  useQuery({
    queryKey: sourcingKeys.votingDetail(id),
    queryFn: () => api.getVotingDetail(id),
    enabled: !!id,
  });

export const useVotingStats = () =>
  useQuery({
    queryKey: sourcingKeys.votingStats(),
    queryFn: () => api.getVotingStats(),
  });

export const useSubmitOverride = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: ManualOverrideRequest }) =>
      api.submitOverride(id, payload),
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.voting() });
      queryClient.invalidateQueries({ queryKey: sourcingKeys.votingDetail(id) });
    },
  });
};

// =============================================================================
// Variant Matching Hooks (TRACK-056)
// =============================================================================

export const useVariantMatches = (extractionId: string, status?: string[]) =>
  useQuery({
    queryKey: sourcingKeys.variantMatches(extractionId, status),
    queryFn: () => api.listVariantMatches(extractionId, { status }),
    enabled: !!extractionId,
  });

export const useVariantMatchReport = (extractionId: string) =>
  useQuery({
    queryKey: sourcingKeys.variantMatchReport(extractionId),
    queryFn: () => api.getVariantMatchReport(extractionId),
    enabled: !!extractionId,
  });

export const usePendingVariantMatches = () =>
  useQuery({
    queryKey: sourcingKeys.pendingVariantMatches(),
    queryFn: () => api.listPendingVariantMatches(),
  });

export const useReviewVariantMatch = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ matchId, payload }: { matchId: string; payload: VariantMatchReviewRequest }) =>
      api.reviewVariantMatch(matchId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.variants() });
    },
  });
};

// =============================================================================
// Quality Dashboard Hooks
// =============================================================================

export const useQualitySummary = (refetchInterval?: number) =>
  useQuery({
    queryKey: sourcingKeys.qualitySummary(),
    queryFn: () => api.getQualitySummary(),
    refetchInterval,
  });

export const useQualityDistribution = () =>
  useQuery({
    queryKey: sourcingKeys.qualityDistribution(),
    queryFn: () => api.getQualityDistribution(),
  });

export const useQualityCoverage = () =>
  useQuery({
    queryKey: sourcingKeys.qualityCoverage(),
    queryFn: () => api.getQualityCoverage(),
  });

export const useQualityGaps = () =>
  useQuery({
    queryKey: sourcingKeys.qualityGaps(),
    queryFn: () => api.getQualityGaps(),
  });

export const useAuditResults = () =>
  useQuery({
    queryKey: sourcingKeys.auditResults(),
    queryFn: () => api.getAuditResults(),
  });

export const useRunAudit = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.runAudit(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: sourcingKeys.auditResults() });
      queryClient.invalidateQueries({ queryKey: sourcingKeys.qualitySummary() });
    },
  });
};

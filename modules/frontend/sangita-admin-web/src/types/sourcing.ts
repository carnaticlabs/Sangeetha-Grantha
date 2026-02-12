// =============================================================================
// Sourcing & Extraction Monitoring — Domain Types
// Reference: application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md
// =============================================================================

// --- Enums & Literals ---

export type SourceTier = 1 | 2 | 3 | 4 | 5;

export type SourceFormat = 'PDF' | 'HTML' | 'DOCX' | 'API' | 'MANUAL' | 'IMAGE';

export type ExtractionStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'INGESTED' | 'FAILED' | 'CANCELLED';

export type ExtractionIntent = 'PRIMARY' | 'ENRICH';

export type VariantMatchStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'AUTO_APPROVED';

export type ConsensusType = 'UNANIMOUS' | 'MAJORITY' | 'AUTHORITY_OVERRIDE' | 'SINGLE_SOURCE' | 'MANUAL';

export type ConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW';

export type ContributedField = 'title' | 'raga' | 'tala' | 'composer' | 'sections' | 'lyrics' | 'deity' | 'temple' | 'notation';

// --- Source Registry ---

export interface ImportSource {
  id: string;
  name: string;
  baseUrl: string;
  description?: string | null;
  sourceTier: SourceTier;
  supportedFormats: SourceFormat[];
  composerAffinity: Record<string, number>; // composerName → weight (0.0–1.0)
  lastHarvestedAt?: string | null;
  isActive: boolean;
  krithiCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface SourceDetail extends ImportSource {
  contributionStats: {
    totalKrithis: number;
    fieldBreakdown: Record<ContributedField, number>;
    avgConfidence: number;
    extractionSuccessRate: number;
  };
}

export interface CreateSourceRequest {
  name: string;
  baseUrl: string;
  description?: string | null;
  sourceTier: SourceTier;
  supportedFormats: SourceFormat[];
  composerAffinity?: Record<string, number>;
  isActive?: boolean;
}

export interface UpdateSourceRequest {
  name?: string;
  baseUrl?: string;
  description?: string | null;
  sourceTier?: SourceTier;
  supportedFormats?: SourceFormat[];
  composerAffinity?: Record<string, number>;
  isActive?: boolean;
}

export interface SourceListResponse {
  items: ImportSource[];
  total: number;
  page: number;
  pageSize: number;
}

// --- Extraction Queue ---

export interface ExtractionTask {
  id: string;
  sourceUrl: string;
  sourceFormat: SourceFormat;
  sourceName?: string | null;
  sourceTier?: SourceTier | null;
  importSourceId?: string | null;
  importBatchId?: string | null;
  status: ExtractionStatus;
  resultCount?: number | null;
  confidence?: number | null;
  extractionMethod?: string | null;
  extractorVersion?: string | null;
  durationMs?: number | null;
  attempts: number;
  maxAttempts: number;
  claimedBy?: string | null;
  claimedAt?: string | null;
  pageRange?: string | null;
  lastErrorAt?: string | null;
  // TRACK-056: Variant support
  contentLanguage?: string | null;
  extractionIntent: ExtractionIntent;
  relatedExtractionId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExtractionDetail extends ExtractionTask {
  requestPayload?: Record<string, unknown> | null;
  resultPayload?: Record<string, unknown> | null;
  errorDetail?: Record<string, unknown> | null;
  sourceChecksum?: string | null;
  cachedArtifactPath?: string | null;
}

export interface CreateExtractionRequest {
  sourceUrl: string;
  sourceFormat: SourceFormat;
  importSourceId?: string | null;
  importBatchId?: string | null;
  pageRange?: string | null;
  composerHint?: string | null;
  expectedKrithiCount?: number | null;
  maxAttempts?: number;
  requestPayload?: Record<string, unknown> | null;
  // TRACK-056: Variant support
  contentLanguage?: string | null;
  extractionIntent?: ExtractionIntent;
  relatedExtractionId?: string | null;
}

export interface ExtractionListResponse {
  items: ExtractionTask[];
  total: number;
  page: number;
  pageSize: number;
}

export interface ExtractionStatsResponse {
  pending: number;
  processing: number;
  done: number;
  failed: number;
  cancelled: number;
  total: number;
  throughputPerHour: number; // over last 24h
}

// --- Source Evidence ---

export interface SourceEvidence {
  krithiId: string;
  krithiTitle: string;
  krithiRaga?: string | null;
  krithiTala?: string | null;
  krithiComposer?: string | null;
  sourceCount: number;
  topSourceName: string;
  topSourceTier: SourceTier;
  contributedFields: ContributedField[];
  avgConfidence: number;
  votingStatus?: ConsensusType | null;
}

export interface EvidenceListResponse {
  items: SourceEvidence[];
  total: number;
  page: number;
  pageSize: number;
}

export interface KrithiEvidenceSource {
  importSourceId: string;
  sourceName: string;
  sourceTier: SourceTier;
  sourceFormat: SourceFormat;
  sourceUrl: string;
  extractionMethod?: string | null;
  confidence: number;
  contributedFields: ContributedField[];
  fieldValues: Record<string, string>;
  extractedAt: string;
  rawExtraction?: Record<string, unknown> | null;
}

export interface KrithiEvidenceResponse {
  krithiId: string;
  krithiTitle: string;
  krithiRaga?: string | null;
  krithiTala?: string | null;
  krithiComposer?: string | null;
  workflowState?: string | null;
  sources: KrithiEvidenceSource[];
}

export interface FieldComparisonCell {
  sourceId: string;
  sourceName: string;
  sourceTier: SourceTier;
  value: string;
}

export interface FieldComparisonRow {
  field: string;
  agreement: 'agree' | 'variation' | 'conflict';
  cells: FieldComparisonCell[];
  votingDecisionId?: string | null;
}

export interface FieldComparisonResponse {
  krithiId: string;
  krithiTitle: string;
  fields: FieldComparisonRow[];
}

// --- Structural Voting ---

export interface VotingDecision {
  id: string;
  krithiId: string;
  krithiTitle: string;
  votedAt: string;
  sourceCount: number;
  consensusType: ConsensusType;
  consensusStructure: SectionSummary[];
  confidence: ConfidenceLevel;
  dissentCount: number;
  reviewerId?: string | null;
  reviewerName?: string | null;
  notes?: string | null;
}

export interface VotingListResponse {
  items: VotingDecision[];
  total: number;
  page: number;
  pageSize: number;
}

export interface VotingParticipant {
  sourceId: string;
  sourceName: string;
  sourceTier: SourceTier;
  agrees: boolean;
  proposedStructure: SectionSummary[];
  sourceUrl: string;
  extractionMethod?: string | null;
}

export interface VotingDetailResponse {
  id: string;
  krithiId: string;
  krithiTitle: string;
  votedAt: string;
  consensusType: ConsensusType;
  confidence: ConfidenceLevel;
  consensusStructure: SectionSummary[];
  participants: VotingParticipant[];
  dissentingSources: VotingParticipant[];
  notes?: string | null;
  reviewerId?: string | null;
  reviewerName?: string | null;
}

export interface ManualOverrideRequest {
  structure: SectionSummary[];
  notes: string; // required for overrides
}

export interface VotingStatsResponse {
  total: number;
  unanimous: number;
  majority: number;
  authorityOverride: number;
  singleSource: number;
  manual: number;
  confidenceDistribution: {
    high: number;
    medium: number;
    low: number;
  };
}

// --- Quality Dashboard ---

export interface QualitySummary {
  totalKrithis: number;
  multiSourceCount: number;
  multiSourcePercent: number;
  consensusCount: number;
  consensusPercent: number;
  avgQualityScore: number;
  enrichmentCoveragePercent: number;
}

export interface QualityDistribution {
  buckets: {
    label: string;
    min: number;
    max: number;
    count: number;
  }[];
}

export interface CoverageData {
  tierCoverage: {
    tier: SourceTier;
    count: number;
  }[];
  composerFieldMatrix: {
    composers: string[];
    fields: string[];
    data: number[][]; // percentages 0–100
  };
  phaseProgress: PhaseProgress[];
}

export interface PhaseProgress {
  phase: number;
  label: string;
  target: number;
  completed: number;
  inProgress: number;
}

export interface GapAnalysis {
  gaps: {
    label: string;
    description: string;
    count: number;
    filterLink?: string;
  }[];
}

export interface AuditResult {
  queryName: string;
  violationCount: number;
  trend: 'up' | 'down' | 'flat';
  lastRunAt: string;
  details?: Record<string, unknown>[];
}

export interface AuditResultResponse {
  results: AuditResult[];
  lastRunAt?: string | null;
}

// --- Variant Matching (TRACK-056) ---

export interface VariantMatch {
  id: string;
  extractionId: string;
  krithiId: string;
  krithiTitle: string;
  confidence: number;
  confidenceTier: ConfidenceLevel;
  matchSignals: string; // JSON
  matchStatus: VariantMatchStatus;
  isAnomaly: boolean;
  structureMismatch: boolean;
  reviewerNotes?: string | null;
  createdAt: string;
}

export interface VariantMatchListResponse {
  items: VariantMatch[];
  total: number;
  page: number;
  pageSize: number;
}

export interface VariantMatchReport {
  extractionId: string;
  totalMatches: number;
  highConfidence: number;
  mediumConfidence: number;
  lowConfidence: number;
  anomalies: number;
  autoApproved: number;
}

export interface VariantMatchReviewRequest {
  action: 'approve' | 'reject' | 'skip';
  notes?: string | null;
}

// --- Shared Component Types ---

export interface SectionSummary {
  sectionType: string; // 'PALLAVI' | 'ANUPALLAVI' | 'CHARANAM' | 'MADHYAMA_KALA_SAHITYA' | 'CHITTASWARAM'
  orderIndex: number;
  label?: string | null;
}

export interface TimelineEvent {
  state: string;
  timestamp: string;
  duration?: number | null; // ms
  actor?: string | null;
  detail?: string | null;
}

export type ColorScale = 'green' | 'blue' | 'heat';

// --- Filter & Pagination ---

export interface SourcingPaginationParams {
  page?: number;
  pageSize?: number;
}

export interface SourceFilterParams extends SourcingPaginationParams {
  tier?: SourceTier[];
  format?: SourceFormat[];
  active?: boolean;
  search?: string;
}

export interface ExtractionFilterParams extends SourcingPaginationParams {
  status?: ExtractionStatus[];
  format?: SourceFormat[];
  sourceId?: string;
  dateFrom?: string;
  dateTo?: string;
  batchId?: string;
}

export interface EvidenceFilterParams extends SourcingPaginationParams {
  minSourceCount?: number;
  tier?: SourceTier[];
  field?: ContributedField[];
  extractionMethod?: string;
  search?: string;
}

export interface VotingFilterParams extends SourcingPaginationParams {
  consensusType?: ConsensusType[];
  confidence?: ConfidenceLevel[];
  dateFrom?: string;
  dateTo?: string;
  hasDissents?: boolean;
  pendingReview?: boolean;
}

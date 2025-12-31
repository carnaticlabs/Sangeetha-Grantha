export enum ViewState {
  DASHBOARD = 'DASHBOARD',
  KRITHIS = 'KRITHIS',
  KRITHI_DETAIL = 'KRITHI_DETAIL',
  REFERENCE = 'REFERENCE',
  IMPORTS = 'IMPORTS',
  TAGS = 'TAGS',
}

export enum MusicalForm {
  KRITHI = 'KRITHI',
  VARNAM = 'VARNAM',
  SWARAJATHI = 'SWARAJATHI',
}

export enum NotationType {
  SWARA = 'SWARA',
  JATHI = 'JATHI',
}

export interface ReferenceDataStats {
  composerCount: number;
  ragaCount: number;
  talaCount: number;
  deityCount: number;
  templeCount: number;
  tagCount: number;
  sampradayaCount: number;
  musicalFormCount: number;
  languageCount: number;
}

export interface KrithiSearchResult {
  items: KrithiSummary[];
  total: number;
  page: number;
  pageSize: number;
}

export interface DashboardStats {
  totalKrithis: number;
  totalComposers: number;
  totalRagas: number;
  pendingReview: number;
}

export interface Krithi {
  id: string;
  title: string;
  composer: string;
  raga: string;
  tala: string;
  musicalForm: MusicalForm;
  status: 'Draft' | 'Review' | 'Published' | 'Archived';
  lastModified: string;
}

export interface LyricVariant {
  id: string;
  language: string;
  script: string;
  isPrimary: boolean;
  label: string;
  sampradaya?: string;
  source: string;
  pallavi: string;
  anupallavi: string;
  charanams: string[];
}

export interface TagCategory {
  category: string;
  tags: { label: string; confidence?: 'Low' | 'Medium' | 'High'; source?: string }[];
}

export interface AuditEvent {
  id: string;
  timestamp: string;
  user: string;
  action: 'Create' | 'Update' | 'Workflow';
  changes?: { field: string; before: string; after: string }[];
}

export interface StatCardProps {
  label: string;
  value: string;
  trend?: string;
  trendUp?: boolean;
}

export interface NotationVariant {
  id: string;
  krithiId: string;
  notationType: NotationType;
  talaId: string | null;
  kalai: number;
  eduppuOffsetBeats: number | null;
  variantLabel: string | null;
  sourceReference: string | null;
  isPrimary: boolean;
}

// --- OpenAPI Types ---

export interface KrithiSummary {
  id: string;
  name: string;
  composerName: string;
  primaryLanguage: string;
  ragas: { id: string; name: string; orderIndex: number }[];
}

export interface Composer {
  id: string;
  name: string;
  nameNormalized: string;
  birthYear?: number;
  deathYear?: number;
  place?: string;
  notes?: string;
  createdAt: string; // ISO 8601 timestamp
  updatedAt: string; // ISO 8601 timestamp
}

export interface Raga {
  id: string;
  name: string;
  nameNormalized: string;
  melakartaNumber?: number;
  parentRagaId?: string;
  arohanam?: string;
  avarohanam?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  orderIndex?: number; // For UI sorting
}

export interface Tala {
  id: string;
  name: string;
  nameNormalized: string;
  angaStructure?: string;
  beatCount?: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Deity {
  id: string;
  name: string;
  nameNormalized: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Tag { id: string; category: string; displayName: string; slug?: string; }
export interface Sampradaya { id: string; name: string; type: string; }

export interface Temple {
  id: string;
  name: string; // canonical name
  nameNormalized: string;
  city?: string;
  state?: string;
  country?: string;
  primaryDeityId?: string;
  latitude?: number;
  longitude?: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  // For UI compatibility
  canonicalName?: string;
  names?: { language: string; script: string; name: string; isPrimary: boolean }[];
}

export interface KrithiSection {
  id: string;
  sectionType: 'PALLAVI' | 'ANUPALLAVI' | 'CHARANAM' | 'CHITTASWARAM' | 'OTHER';
  orderIndex: number;
}

export interface KrithiLyricSection {
  sectionId: string;
  text: string;
}

export interface KrithiLyricVariant {
  id: string;
  language: string;
  script?: string;
  transliterationScheme?: string;
  sampradaya?: Sampradaya;
  sections: KrithiLyricSection[];
}

export interface KrithiDetail {
  id: string;
  title: string;
  incipit?: string;
  composer: Composer;
  primaryLanguage: string;
  tala: Tala;
  ragas: Raga[];
  deity?: Deity;
  temple?: Temple;
  sections: KrithiSection[];
  lyricVariants: KrithiLyricVariant[];
  tags: Tag[];
  status: 'DRAFT' | 'IN_REVIEW' | 'PUBLISHED' | 'ARCHIVED';
  musicalForm: MusicalForm;
  isRagamalika?: boolean;
  sahityaSummary?: string;
  notes?: string;
}

export interface AuditLog {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  actor: string;
  timestamp: string;
  diff?: Record<string, { before?: any; after?: any }> | null;
}

export interface NotationRow {
  id: string;
  notationVariantId: string;
  sectionId: string;
  orderIndex: number;
  swaraText: string;
  sahityaText: string | null;
  talaMarkers: string | null;
}

export interface NotationResponse {
  krithiId: string;
  musicalForm: MusicalForm;
  sections: Array<{ id: string; sectionType: string; orderIndex: number; label?: string | null }>;
  variants: Array<{
    variant: NotationVariant;
    rowsBySectionId: Record<string, NotationRow[]>;
  }>;
}

export interface ImportedKrithi {
  id: string;
  sourceKey: string | null;
  rawTitle: string | null;
  rawComposer: string | null;
  rawRaga: string | null;
  rawTala: string | null;
  importStatus: 'PENDING' | 'IMPORTED' | 'REJECTED';
  createdAt: string;
}
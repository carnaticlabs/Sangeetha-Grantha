/**
 * Wire types for the Mobile Preview (TRACK-158). They mirror the Kotlin DTOs in
 * `modules/shared/domain/.../model/catalogue/` that the mobile app consumes, field for field:
 * UUIDs are strings, optional Kotlin fields are `?` or `| null`, and enums are string unions.
 * When a Kotlin DTO changes, change it here in the same commit.
 */
import type { SemanticSearchResultItem } from '../types';

export type LanguageCode = 'SA' | 'TA' | 'TE' | 'KN' | 'ML' | 'HI' | 'EN';
export type ScriptCode = 'DEVANAGARI' | 'TAMIL' | 'TELUGU' | 'KANNADA' | 'MALAYALAM' | 'LATIN';
export type MusicalFormWire = 'KRITHI' | 'VARNAM' | 'SWARAJATHI' | 'UNESTABLISHED';
export type RagaSection =
    | 'PALLAVI' | 'ANUPALLAVI' | 'CHARANAM' | 'SAMASHTI_CHARANAM' | 'CHITTASWARAM' | 'SWARA_SAHITYA'
    | 'MADHYAMA_KALA' | 'SOLKATTU_SWARA' | 'ANUBANDHA' | 'MUKTAYI_SWARA' | 'ETTUGADA_SWARA'
    | 'ETTUGADA_SAHITYA' | 'VILOMA_CHITTASWARAM' | 'OTHER';
export type Completeness = 'UNKNOWN' | 'PARTIAL' | 'COMPLETE';
export type OrderBasis = 'SOURCE' | 'PERFORMANCE_CONVENTION' | 'UNSPECIFIED';
export type CollectionRole = 'DHYANA' | 'CORE' | 'OPTIONAL' | 'MANGALAM' | 'DISPUTED_CONJECTURE';
export type FacetKey = 'vibhakti' | 'collection' | 'tune' | 'mandalam' | 'element';
export type SearchHitKind = 'KRITHI' | 'COLLECTION' | 'KSHETRA';

export interface PagedResponse<T> {
    items: T[];
    total: number;
    page: number;
    pageSize: number;
}

export interface ComposerRef { id: string; name: string }
export interface TalaRef { id: string; name: string }
export interface ReferenceName { name: string }

export interface RagaRef {
    id: string;
    name: string;
    orderIndex: number;
    section?: RagaSection | null;
    sectionId?: string | null;
}

export interface KrithiSummary {
    id: string;
    title: string;
    incipit?: string | null;
    composer: ComposerRef;
    ragas: RagaRef[];
    tala?: TalaRef | null;
    musicalForm: MusicalFormWire;
    isRagamalika: boolean;
}

export interface VariantRef {
    id: string;
    language: LanguageCode;
    script: ScriptCode;
    transliterationScheme?: string | null;
    isPrimary: boolean;
    label?: string | null;
    sourceReference?: string | null;
}

export interface FacetChip { key: FacetKey; value: string; label?: string | null }

export interface CollectionNeighbour { krithiId: string; title: string; ragaName?: string | null }

export interface CollectionRef {
    collectionId: string;
    slug: string;
    name: string;
    role: CollectionRole;
    sequenceOrder: number;
    position?: number | null;
    coreCount: number;
    axisValue?: string | null;
    orderBasis: OrderBasis;
    orderConvention?: string | null;
    attributes: Record<string, string>;
    previous?: CollectionNeighbour | null;
    next?: CollectionNeighbour | null;
}

export interface CycleContext {
    collections: CollectionRef[];
    vibhaktiCase?: string | null;
    occasionNote?: string | null;
}

export interface KrithiReader {
    id: string;
    title: string;
    incipit?: string | null;
    composer: ComposerRef;
    ragas: RagaRef[];
    tala?: TalaRef | null;
    musicalForm: MusicalFormWire;
    originalLanguage: LanguageCode;
    isRagamalika: boolean;
    defaultVariantId?: string | null;
    variants: VariantRef[];
    completeness: Completeness;
    deity?: ReferenceName | null;
    temple?: ReferenceName | null;
    cycleContext?: CycleContext | null;
    facetChips?: FacetChip[];
}

export interface LyricSection {
    sectionId: string;
    sectionType: string;
    label?: string | null;
    orderIndex: number;
    text: string;
}

export interface Lyrics {
    variantId: string;
    krithiId: string;
    language: LanguageCode;
    script: ScriptCode;
    transliterationScheme?: string | null;
    isPrimary: boolean;
    label?: string | null;
    sourceReference?: string | null;
    unsegmentedText?: string | null;
    sections: LyricSection[];
    completeness: Completeness;
}

export interface RagaSummary {
    id: string;
    name: string;
    matchingAliases: string[];
    publishedCompositionCount: number;
    melakartaNumber?: number | null;
    parentRagaName?: string | null;
    parentMelakartaNumber?: number | null;
}

export interface RagaDetail {
    id: string;
    name: string;
    aliases: string[];
    publishedCompositionCount: number;
    melakartaNumber?: number | null;
    parentRagaId?: string | null;
    parentRagaName?: string | null;
    arohanam?: string | null;
    avarohanam?: string | null;
    nomenclatureLinks: { relatedRagaId: string; relatedRagaName: string; relationLabel: string }[];
    parentMelakartaNumber?: number | null;
}

export interface ComposerSummary {
    id: string;
    name: string;
    matchingAliases: string[];
    publishedCompositionCount: number;
}

export interface ComposerDetail {
    id: string;
    name: string;
    aliases: string[];
    publishedCompositionCount: number;
    birthYear?: number | null;
    deathYear?: number | null;
    place?: string | null;
}

export interface DiscoveryFeature {
    selection: 'CATALOGUE_ORDER' | 'EDITORIAL';
    heading: string;
    summary: string;
    krithi: KrithiSummary;
}

export interface Discovery {
    feature?: DiscoveryFeature | null;
    editorialRevision?: string | null;
}

export interface CollectionSummary {
    id: string;
    slug: string;
    name: string;
    composer?: ComposerRef | null;
    memberCount: number;
    orderBasis: OrderBasis;
    hasOverview: boolean;
}

export interface CollectionMember {
    krithi: KrithiSummary;
    role: CollectionRole;
    sequenceOrder: number;
    position?: number | null;
    axisValue?: string | null;
    label?: string | null;
    attributes: Record<string, string>;
}

export interface CollectionDetail {
    id: string;
    slug: string;
    name: string;
    composer?: ComposerRef | null;
    overview?: string | null;
    orderBasis: OrderBasis;
    orderConvention?: string | null;
    isComplete: boolean;
    coreCount: number;
    members: CollectionMember[];
}

export interface TempleSummary {
    id: string;
    name: string;
    city?: string | null;
    state?: string | null;
    placeKind: 'LOCALITY' | 'COMPLEX' | 'SANNIDHI' | 'MANDAPAM';
    mandalam?: string | null;
    element?: string | null;
    publishedKrithiCount: number;
    childCount: number;
    recordedGap: boolean;
}

export interface TempleDetail extends TempleSummary {
    country?: string | null;
    sthalaVriksha?: string | null;
    sthalaTirtha?: string | null;
    nadiTirtha?: string | null;
    deityPosture?: 'SAYANA' | 'STHANAKA' | 'ASINA' | 'TANDAVA' | null;
    overview?: string | null;
    parent?: ReferenceName | null;
    children: TempleSummary[];
    krithis: KrithiSummary[];
    place?: { googlePlaceId: string; precision: 'TEMPLE' | 'PARENT_COMPLEX' } | null;
}

export interface FacetValue { value: string; label?: string | null; count?: number | null }
export interface Facet { key: FacetKey; values: FacetValue[] }
export interface FacetList { facets: Facet[] }

export interface MacroHit {
    kind: Exclude<SearchHitKind, 'KRITHI'>;
    title: string;
    subtitle?: string | null;
    snippet: string;
    collectionSlug?: string | null;
    templeId?: string | null;
    memberCount?: number | null;
    publishedKrithiCount?: number | null;
    recordedGap: boolean;
    similarityScore: number;
    lexicalScore?: number | null;
    rrfScore?: number | null;
}

export interface SearchResponse {
    query: string;
    totalMatches: number;
    macroHits: MacroHit[];
    items: SemanticSearchResultItem[];
    exactTitleMatchId?: string | null;
}

/** Where the phone frame is. Mirrors `RasikaDestination` (kotlin: navigation/RasikaDestination.kt). */
export type PreviewTab = 'Home' | 'Explore' | 'Library' | 'Settings';

export type PreviewDestination =
    | { kind: 'Home' }
    | { kind: 'Explore' }
    | { kind: 'Library' }
    | { kind: 'Settings' }
    | { kind: 'Browse' }
    | { kind: 'Collections' }
    | { kind: 'CollectionDetail'; slug: string }
    | { kind: 'Kshetras' }
    | { kind: 'KshetraDetail'; templeId: string }
    | { kind: 'RagaDetail'; ragaId: string }
    | { kind: 'ComposerDetail'; composerId: string }
    | { kind: 'KrithiReader'; krithiId: string };

/** Preview-local reading preferences. Never sent to the server (TRACK-158 R4). */
export interface PreviewPreferences {
    appearance: 'light' | 'dark';
    textSize: 'small' | 'medium' | 'large';
    preferredScript: ScriptCode | null;
}

// -- Preview search (TRACK-158 B1): wrappers around the unchanged V2 search response --------------------------

export type PreviewScope = 'PUBLISHED_ONLY' | 'INCLUDE_UNPUBLISHED';
export type SearchMode = 'Hybrid' | 'Semantic' | 'Lexical';
export type ExploreCategory = 'Krithis' | 'Ragas' | 'Composers';

export interface PreviewFloor { minSimilarity?: number | null; minLexical?: number | null; minGap: number }

export interface PreviewMacroCandidate {
    kind: Exclude<SearchHitKind, 'KRITHI'>;
    title: string;
    similarity?: number | null;
    gap?: number | null;
    lexicalScore?: number | null;
    cleared: boolean;
    reason?: string | null;
}

export interface PreviewSearchExplain {
    mode: string;
    profileModel?: string | null;
    profileDimensions?: number | null;
    floorCalibrated: boolean;
    floor?: PreviewFloor | null;
    macroCandidates: PreviewMacroCandidate[];
}

export interface PreviewSearchResponse {
    response: SearchResponse;
    explain?: PreviewSearchExplain | null;
    /** Result krithis that are not published; empty for PUBLISHED_ONLY. */
    unpublishedKrithiIds?: string[];
}

export type PreviewIndexVerdict = 'NOT_INDEXED' | 'MAY_BE_STALE' | 'CURRENT';

export interface PreviewIndexStatus {
    krithiId: string;
    verdict: PreviewIndexVerdict;
    workflowState: string;
    documentCount: number;
    publishedFlagMatches: boolean;
    embeddedForActiveProfile: boolean;
    indexedAt?: string | null;
    sourceUpdatedAt?: string | null;
    activeProfileModel?: string | null;
    activeProfileDimensions?: number | null;
    reasons: string[];
}

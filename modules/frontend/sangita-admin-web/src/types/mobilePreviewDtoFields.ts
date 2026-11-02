/**
 * The field names of each Kotlin catalogue DTO as mirrored by `mobilePreview.types.ts`. Two guards keep them honest:
 * the `Exhaustive` type makes this file fail to compile when a TypeScript interface gains or loses a field that is
 * not listed here, and `mobilePreviewDtoDrift.test.ts` fails when a Kotlin DTO's fields differ from the list.
 * Change a Kotlin DTO, then the interface, then this list, in one commit.
 */
import type {
    CollectionDetail, CollectionRef, CollectionSummary, ComposerDetail, ComposerSummary, KrithiReader, KrithiSummary,
    Lyrics, MacroHit, PagedResponse, PreviewIndexStatus, PreviewSearchExplain, PreviewSearchResponse, RagaDetail,
    RagaRef, RagaSummary, SearchResponse, TempleDetail, TempleSummary, VariantRef,
} from './mobilePreview.types';

type Exhaustive<T, L extends readonly (keyof T)[]> = Exclude<keyof T, L[number]> extends never ? L : never;
const fields = <T,>() => <const L extends readonly (keyof T)[]>(list: Exhaustive<T, L>): L => list;

export const DTO_FIELDS = {
    CataloguePagedResponse: fields<PagedResponse<unknown>>()(['items', 'total', 'page', 'pageSize']),
    CatalogueRagaRefDto: fields<RagaRef>()(['id', 'name', 'orderIndex', 'section', 'sectionId']),
    CatalogueKrithiSummaryDto: fields<KrithiSummary>()(['id', 'title', 'incipit', 'composer', 'ragas', 'tala', 'musicalForm', 'isRagamalika']),
    CatalogueVariantRefDto: fields<VariantRef>()(['id', 'language', 'script', 'transliterationScheme', 'isPrimary', 'label', 'sourceReference']),
    CatalogueKrithiReaderDto: fields<KrithiReader>()([
        'id', 'title', 'incipit', 'composer', 'ragas', 'tala', 'musicalForm', 'originalLanguage', 'isRagamalika', 'defaultVariantId',
        'variants', 'completeness', 'deity', 'temple', 'cycleContext', 'facetChips',
    ]),
    CatalogueLyricsDto: fields<Lyrics>()([
        'variantId', 'krithiId', 'language', 'script', 'transliterationScheme', 'isPrimary', 'label', 'sourceReference',
        'unsegmentedText', 'sections', 'completeness',
    ]),
    CatalogueRagaSummaryDto: fields<RagaSummary>()(['id', 'name', 'matchingAliases', 'publishedCompositionCount', 'melakartaNumber', 'parentRagaName', 'parentMelakartaNumber']),
    CatalogueRagaDetailDto: fields<RagaDetail>()([
        'id', 'name', 'aliases', 'publishedCompositionCount', 'melakartaNumber', 'parentRagaId', 'parentRagaName', 'arohanam', 'avarohanam',
        'nomenclatureLinks', 'parentMelakartaNumber',
    ]),
    CatalogueComposerSummaryDto: fields<ComposerSummary>()(['id', 'name', 'matchingAliases', 'publishedCompositionCount']),
    CatalogueComposerDetailDto: fields<ComposerDetail>()(['id', 'name', 'aliases', 'publishedCompositionCount', 'birthYear', 'deathYear', 'place']),
    CatalogueCollectionRefDto: fields<CollectionRef>()([
        'collectionId', 'slug', 'name', 'role', 'sequenceOrder', 'position', 'coreCount', 'axisValue', 'orderBasis', 'orderConvention',
        'attributes', 'previous', 'next',
    ]),
    CatalogueCollectionSummaryDto: fields<CollectionSummary>()(['id', 'slug', 'name', 'composer', 'memberCount', 'orderBasis', 'hasOverview']),
    CatalogueCollectionDetailDto: fields<CollectionDetail>()(['id', 'slug', 'name', 'composer', 'overview', 'orderBasis', 'orderConvention', 'isComplete', 'coreCount', 'members']),
    CatalogueTempleSummaryDto: fields<TempleSummary>()(['id', 'name', 'city', 'state', 'placeKind', 'mandalam', 'element', 'publishedKrithiCount', 'childCount', 'recordedGap']),
    CatalogueTempleDetailDto: fields<TempleDetail>()([
        'id', 'name', 'city', 'state', 'placeKind', 'mandalam', 'element', 'publishedKrithiCount', 'childCount', 'recordedGap', 'country',
        'sthalaVriksha', 'sthalaTirtha', 'nadiTirtha', 'deityPosture', 'overview', 'parent', 'children', 'krithis', 'place',
    ]),
    CatalogueMacroHitDto: fields<MacroHit>()([
        'kind', 'title', 'subtitle', 'snippet', 'collectionSlug', 'templeId', 'memberCount', 'publishedKrithiCount', 'recordedGap',
        'similarityScore', 'lexicalScore', 'rrfScore',
    ]),
    CatalogueSearchResponseDto: fields<SearchResponse>()(['query', 'totalMatches', 'macroHits', 'items', 'exactTitleMatchId']),
    PreviewSearchResponse: fields<PreviewSearchResponse>()(['response', 'explain', 'unpublishedKrithiIds']),
    PreviewSearchExplainDto: fields<PreviewSearchExplain>()(['mode', 'profileModel', 'profileDimensions', 'floorCalibrated', 'floor', 'macroCandidates']),
    PreviewIndexStatus: fields<PreviewIndexStatus>()([
        'krithiId', 'verdict', 'workflowState', 'documentCount', 'publishedFlagMatches', 'embeddedForActiveProfile', 'indexedAt',
        'sourceUpdatedAt', 'activeProfileModel', 'activeProfileDimensions', 'reasons',
    ]),
} as const;

/**
 * Search presentation rules ported from `SearchPresenter` (kotlin: search/SearchPresenter.kt). Ranking, floors and
 * the cap on cards are decided by the server; only these client rules exist on mobile, and they are pinned by
 * `fixtures/searchRules.golden.json`.
 */
import type { MacroHit, PreviewDestination, SearchResponse } from '../../types/mobilePreview.types';
import type { SemanticSearchResultItem } from '../../types';

export const DISCOVERY_LIMIT = 30;
export const PAGE_SIZE = 30;
export const MAX_PAGES = 3;
/** The mobile build ships a kshetra screen (TRACK-151), so KSHETRA cards are shown. */
export const KSHETRA_DESTINATION_AVAILABLE = true;

export const MODE_COPY = {
    Hybrid: { label: 'All matches', hint: 'Search titles, lyrics and meaning together.' },
    Semantic: { label: 'Related meanings', hint: 'Explore related themes and ideas.' },
    Lexical: { label: 'Titles & lyrics', hint: 'Find words in the catalogue, with rāga and composer filters.' },
} as const;

export const SEARCH_IDLE = 'Find a known kriti or explore an idea. Try a title, a line of sahitya, or a theme such as Kamalamba Navavarna.';
export const EMPTY_SEARCH = 'Nothing stored under that';
export const EMPTY_SEARCH_BODY = 'No published krithi matches. Try a shorter phrase, or browse by rāga.';
export const DISCOVERY_SAVED_FILTERS = 'Your saved filters apply only to Titles & lyrics.';

/** Kinds this build can open: COLLECTION always, KSHETRA only once its destination exists. Anything else is dropped. */
export const displayableMacroHits = (response: SearchResponse, kshetraAvailable = KSHETRA_DESTINATION_AVAILABLE): MacroHit[] =>
    response.macroHits.filter((h) => (h.kind === 'COLLECTION' ? true : h.kind === 'KSHETRA' ? kshetraAvailable : false));

/** The kriti whose title the query equals renders first; the rest keep server order. Macro cards never reorder kritis. */
export const splitExactTitle = (
    items: SemanticSearchResultItem[],
    exactTitleMatchId?: string | null,
): { exact: SemanticSearchResultItem | null; rest: SemanticSearchResultItem[] } => {
    const exact = exactTitleMatchId ? items.find((i) => i.krithiId === exactTitleMatchId) ?? null : null;
    return { exact, rest: exact ? items.filter((i) => i !== exact) : items };
};

/** Where a collection or kshetra search card opens; null if the hit carries no target. */
export const macroDestination = (hit: MacroHit): PreviewDestination | null => {
    if (hit.kind === 'COLLECTION') return hit.collectionSlug ? { kind: 'CollectionDetail', slug: hit.collectionSlug } : null;
    return hit.templeId ? { kind: 'KshetraDetail', templeId: hit.templeId } : null;
};

export const macroKindLabel = (hit: MacroHit): string => (hit.kind === 'COLLECTION' ? 'Collection' : 'Kshetra');

/** Lexical krithis page 30 at a time and stop after three pages (90); ragas have no cap, composers use three. */
export const hasMorePages = (loaded: number, total: number, pagesLoaded: number, capPages: number | null): boolean =>
    loaded < total && (capPages === null || pagesLoaded < capPages);

export const fmtScore = (n: number | null | undefined): string => (n == null ? '–' : n.toFixed(3));

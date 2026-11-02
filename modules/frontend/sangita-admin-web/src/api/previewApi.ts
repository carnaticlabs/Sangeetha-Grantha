/**
 * Mobile Preview reads (TRACK-158). Every call goes to the admin-gated mount of the V2 catalogue
 * (`/v1/admin/preview/catalogue`), which returns the same DTOs as `/v2/catalogue` and also shows
 * draft and in-review krithis. Read-only; sends no `X-Rasika-*` usage headers on purpose.
 */
import { request } from './client';
import type {
    PreviewIndexStatus, PreviewScope, PreviewSearchResponse,
    CollectionDetail, CollectionSummary, ComposerDetail, ComposerSummary, Discovery, FacetList,
    KrithiReader, KrithiSummary, Lyrics, PagedResponse, RagaDetail, RagaSummary, TempleDetail, TempleSummary,
} from '../types/mobilePreview.types';

/** Reads that include draft and in-review krithis (reader, lists). */
const ROOT = '/admin/preview/catalogue';
/** The unchanged published-only V2 reads: what a mobile user gets. */
const MOBILE_ROOT = '/admin/preview/mobile';

type Query = Record<string, string | number | undefined>;

const qs = (query?: Query): string => {
    const params = new URLSearchParams();
    Object.entries(query ?? {}).forEach(([key, value]) => {
        if (value !== undefined && value !== '') params.append(key, String(value));
    });
    const text = params.toString();
    return text ? `?${text}` : '';
};

const withQuery = (path: string, query?: Query): string => `${ROOT}${path}${qs(query)}`;

export const previewKeys = {
    all: ['mobile-preview'] as const,
    krithi: (id: string) => ['mobile-preview', 'krithi', id] as const,
    lyrics: (id: string, variantId: string) => ['mobile-preview', 'krithi', id, 'lyrics', variantId] as const,
    discovery: ['mobile-preview', 'discovery'] as const,
    list: (name: string, query?: Query) => ['mobile-preview', 'list', name, query ?? {}] as const,
    detail: (name: string, id: string) => ['mobile-preview', 'detail', name, id] as const,
    indexStatus: (id: string) => ['mobile-preview', 'index-status', id] as const,
};

/**
 * Search results are cached under their own prefix, not `previewKeys.all`: a save or a window focus must not
 * re-run a search, because that can cost an embedding (TRACK-158 R13).
 */
export const searchKeys = {
    run: (parts: readonly unknown[]) => ['mobile-preview-search', ...parts] as const,
};

export const getPreviewDiscovery = () => request<Discovery>(withQuery('/discovery'));

export const searchPreviewKrithis = (
    query?: { query?: string; composerId?: string; ragaId?: string; page?: number; pageSize?: number },
    scope: PreviewScope = 'INCLUDE_UNPUBLISHED',
) => request<PagedResponse<KrithiSummary>>(`${scope === 'PUBLISHED_ONLY' ? MOBILE_ROOT : ROOT}/krithis${qs(query)}`);

export const getPreviewKrithi = (id: string) => request<KrithiReader>(withQuery(`/krithis/${id}`));

export const getPreviewLyrics = (krithiId: string, variantId: string) =>
    request<Lyrics>(withQuery(`/krithis/${krithiId}/lyrics/${variantId}`));

export const getPreviewFacets = (query?: Query) => request<FacetList>(withQuery('/facets', query));

export const searchPreviewRagas = (query?: { query?: string; page?: number; pageSize?: number }, scope: PreviewScope = 'INCLUDE_UNPUBLISHED') =>
    request<PagedResponse<RagaSummary>>(`${scope === 'PUBLISHED_ONLY' ? MOBILE_ROOT : ROOT}/ragas${qs(query)}`);

export const getPreviewRaga = (id: string) => request<RagaDetail>(withQuery(`/ragas/${id}`));

export const searchPreviewComposers = (query?: { query?: string; page?: number; pageSize?: number }, scope: PreviewScope = 'INCLUDE_UNPUBLISHED') =>
    request<PagedResponse<ComposerSummary>>(`${scope === 'PUBLISHED_ONLY' ? MOBILE_ROOT : ROOT}/composers${qs(query)}`);

export const getPreviewComposer = (id: string) => request<ComposerDetail>(withQuery(`/composers/${id}`));

export const getPreviewCollections = () => request<{ items: CollectionSummary[] }>(withQuery('/collections'));

export const getPreviewCollection = (slug: string) => request<CollectionDetail>(withQuery(`/collections/${slug}`));

export const getPreviewTemples = () => request<{ items: TempleSummary[] }>(withQuery('/temples'));

export const getPreviewTemple = (id: string) => request<TempleDetail>(withQuery(`/temples/${id}`));

/**
 * Hybrid or Semantic search through the same service mobile uses. Sends what mobile sends (trimmed query, no
 * composer or raga, limit 30). Each call may embed the query, so callers run it only on an explicit submit.
 */
export const previewSearch = (mode: 'hybrid' | 'semantic', query: string, scope: PreviewScope) =>
    request<PreviewSearchResponse>(`/admin/preview/search/${mode}`, {
        method: 'POST',
        body: JSON.stringify({ query: query.trim(), limit: 30, scope, explain: true }),
    });

export const getPreviewIndexStatus = (krithiId: string) =>
    request<PreviewIndexStatus>(`/admin/preview/krithis/${krithiId}/index-status`);

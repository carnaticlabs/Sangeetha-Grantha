import React, { useState, useSyncExternalStore } from 'react';
import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import * as api from '../../api/previewApi';
import { searchKeys, previewKeys } from '../../api/previewApi';
import { LoadStateView } from '../../components/mobile-preview/PreviewParts';
import { usePalette } from '../../components/mobile-preview/previewPalette';
import { lastEditedAt, subscribeEdits } from '../../components/mobile-preview/previewEdits';
import { DiscoveryResults, FilterSheet, SearchControls, type AppliedFilters } from '../../components/mobile-preview/SearchViews';
import * as views from '../../components/mobile-preview/screens';
import * as s from '../../components/mobile-preview/searchLogic';
import type { ExploreCategory, PreviewDestination, PreviewScope, SearchMode } from '../../types/mobilePreview.types';

interface SearchPaneProps {
    open: (d: PreviewDestination) => void;
    /** Prefills and submits the query on mount; the parent remounts the pane (new `key`) for a new seed. */
    seed?: { query: string; nonce: number } | null;
}

/**
 * Explore: the mobile search. Hybrid and Semantic run only on submit and are cached under their own key prefix, so a
 * save, a refocus or a remount never re-runs one (an embedding can cost money). Lexical, Ragas and Composers are
 * plain catalogue reads.
 */
export const SearchPane: React.FC<SearchPaneProps> = ({ open, seed }) => {
    const pal = usePalette();
    const [text, setText] = useState(seed?.query ?? '');
    const [committed, setCommitted] = useState<string | null>(seed?.query ?? null);
    const [mode, setMode] = useState<SearchMode>('Hybrid');
    const [category, setCategory] = useState<ExploreCategory>('Krithis');
    const [optionsOpen, setOptionsOpen] = useState(false);
    const [scope, setScope] = useState<PreviewScope>('PUBLISHED_ONLY');
    const [applied, setApplied] = useState<AppliedFilters>({});
    const [draft, setDraft] = useState<AppliedFilters>({});
    const [sheetOpen, setSheetOpen] = useState(false);
    const [ranAt, setRanAt] = useState<number>(() => Date.now());

    const edited = useSyncExternalStore(subscribeEdits, lastEditedAt);
    const discovery = category === 'Krithis' && mode !== 'Lexical';
    const query = committed ?? '';

    const discoveryQuery = useQuery({
        queryKey: searchKeys.run(['discovery', mode, query, scope]),
        enabled: discovery && committed !== null,
        staleTime: Infinity,
        retry: false,
        refetchOnWindowFocus: false,
        queryFn: () => api.previewSearch(mode === 'Hybrid' ? 'hybrid' : 'semantic', query, scope),
    });

    const lexical = useInfiniteQuery({
        queryKey: searchKeys.run(['lexical', query, scope, applied.ragaId ?? '', applied.composerId ?? '']),
        enabled: category === 'Krithis' && mode === 'Lexical' && committed !== null,
        staleTime: Infinity,
        refetchOnWindowFocus: false,
        initialPageParam: 0,
        queryFn: ({ pageParam }) => api.searchPreviewKrithis({ query, composerId: applied.composerId, ragaId: applied.ragaId, page: pageParam, pageSize: s.PAGE_SIZE }, scope),
        getNextPageParam: (last, all) => {
            const loaded = all.reduce((n, p) => n + p.items.length, 0);
            return s.hasMorePages(loaded, last.total, all.length, s.MAX_PAGES) ? all.length : undefined;
        },
    });

    const ragas = useInfiniteQuery({
        queryKey: searchKeys.run(['ragas', query, scope]),
        enabled: category === 'Ragas',
        staleTime: Infinity,
        refetchOnWindowFocus: false,
        initialPageParam: 0,
        queryFn: ({ pageParam }) => api.searchPreviewRagas({ query, page: pageParam, pageSize: s.PAGE_SIZE }, scope),
        getNextPageParam: (last, all) => {
            const loaded = all.reduce((n, p) => n + p.items.length, 0);
            return s.hasMorePages(loaded, last.total, all.length, null) && last.items.length > 0 ? all.length : undefined;
        },
    });

    const composers = useInfiniteQuery({
        queryKey: searchKeys.run(['composers', query, scope]),
        enabled: category === 'Composers',
        staleTime: Infinity,
        refetchOnWindowFocus: false,
        initialPageParam: 0,
        queryFn: ({ pageParam }) => api.searchPreviewComposers({ query, page: pageParam, pageSize: s.PAGE_SIZE }, scope),
        getNextPageParam: (last, all) => {
            const loaded = all.reduce((n, p) => n + p.items.length, 0);
            return s.hasMorePages(loaded, last.total, all.length, s.MAX_PAGES) ? all.length : undefined;
        },
    });

    // The picker pages 30 at a time and can search, as the mobile sheet does (TRACK-159 D8).
    const [ragaText, setRagaText] = useState('');
    const [ragaQuery, setRagaQuery] = useState('');
    const filterRagas = useInfiniteQuery({
        queryKey: previewKeys.list('filter-ragas', { scope, q: ragaQuery }),
        enabled: sheetOpen,
        initialPageParam: 0,
        queryFn: ({ pageParam }) => api.searchPreviewRagas({ query: ragaQuery || undefined, page: pageParam, pageSize: s.PAGE_SIZE }, scope),
        getNextPageParam: (last, all) => (all.reduce((n, p) => n + p.items.length, 0) < last.total && last.items.length > 0 ? all.length : undefined),
    });
    const filterComposers = useInfiniteQuery({
        queryKey: previewKeys.list('filter-composers', { scope }),
        enabled: sheetOpen,
        initialPageParam: 0,
        queryFn: ({ pageParam }) => api.searchPreviewComposers({ page: pageParam, pageSize: s.PAGE_SIZE }, scope),
        getNextPageParam: (last, all) => (all.reduce((n, p) => n + p.items.length, 0) < last.total && last.items.length > 0 ? all.length : undefined),
    });

    const flat = <T,>(pages: { items: T[] }[] | undefined): T[] => pages?.flatMap((p) => p.items) ?? [];
    const submit = () => { setCommitted(text.trim()); setRanAt(Date.now()); };
    const openKrithi = (id: string) => open({ kind: 'KrithiReader', krithiId: id });

    const active = discovery ? discoveryQuery : category === 'Krithis' ? lexical : category === 'Ragas' ? ragas : composers;
    const loadMore = (q: typeof lexical) => q.hasNextPage ? (
        <button type="button" onClick={() => { void q.fetchNextPage(); }} disabled={q.isFetchingNextPage} className="mx-5 mb-5 rounded border px-3 py-1 text-sm" style={{ borderColor: pal.hairline }}>
            {q.isFetchingNextPage ? 'Loading…' : 'Load more'}
        </button>
    ) : null;

    const idle = category === 'Krithis' && committed === null;
    const lexicalItems = flat(lexical.data?.pages);
    const emptyDiscovery = discoveryQuery.data && discoveryQuery.data.response.items.length === 0 && s.displayableMacroHits(discoveryQuery.data.response).length === 0;
    const emptyLexical = lexical.data && lexicalItems.length === 0;
    const emptyDir = category === 'Ragas' ? ragas.data && flat(ragas.data.pages).length === 0 : category === 'Composers' ? composers.data && flat(composers.data.pages).length === 0 : false;

    return (
        <div>
            <SearchControls
                text={text} onText={setText} onSubmit={submit}
                mode={mode} onMode={(m) => { setMode(m); setOptionsOpen(false); }}
                optionsOpen={optionsOpen} onToggleOptions={() => setOptionsOpen(!optionsOpen)}
                category={category} onCategory={setCategory}
                scope={scope} onScope={setScope}
                applied={applied}
                onOpenFilters={() => { setDraft(applied); setRagaText(''); setRagaQuery(''); setSheetOpen(true); }}
                onRemoveRaga={() => setApplied({ ...applied, ragaId: undefined, ragaLabel: undefined })}
                onRemoveComposer={() => setApplied({ ...applied, composerId: undefined, composerLabel: undefined })}
            />
            {sheetOpen ? (
                <FilterSheet
                    ragas={flat(filterRagas.data?.pages)} ragaTotal={filterRagas.data?.pages[0]?.total ?? 0}
                    ragaText={ragaText} ragaQuery={ragaQuery}
                    composers={flat(filterComposers.data?.pages)} composerTotal={filterComposers.data?.pages[0]?.total ?? 0}
                    draft={draft}
                    onRagaText={setRagaText}
                    onRagaSearch={() => setRagaQuery(ragaText.trim())}
                    onLoadMoreRagas={() => { void filterRagas.fetchNextPage(); }}
                    onLoadMoreComposers={() => { void filterComposers.fetchNextPage(); }}
                    onDraftRaga={(id, label) => setDraft({ ...draft, ragaId: id ?? undefined, ragaLabel: label ?? undefined })}
                    onDraftComposer={(id, label) => setDraft({ ...draft, composerId: id ?? undefined, composerLabel: label ?? undefined })}
                    onApply={() => { setApplied(draft); setSheetOpen(false); }}
                    onCancel={() => { setDraft(applied); setSheetOpen(false); }}
                    onReset={() => setDraft({})}
                />
            ) : null}
            {edited > ranAt && !idle ? (
                <p role="status" className="mx-5 mt-3 rounded p-2 text-xs" style={{ background: pal.creamDeep }}>
                    Edits saved since this search.{' '}
                    <button type="button" className="underline" onClick={submit}>Run it again</button>
                </p>
            ) : null}

            {idle ? <p className="p-5 text-sm" style={{ color: pal.inkSoft }}>{s.SEARCH_IDLE}</p> : (
                <LoadStateView
                    isLoading={active.isLoading && active.fetchStatus !== 'idle'}
                    error={active.error}
                    isEmpty={Boolean(emptyDiscovery || emptyLexical || emptyDir)}
                    emptyTitle={s.EMPTY_SEARCH}
                    emptyBody={s.EMPTY_SEARCH_BODY}
                    onRetry={() => { void active.refetch(); }}
                >
                    {discovery && discoveryQuery.data ? (
                        <DiscoveryResults
                            response={discoveryQuery.data.response}
                            explain={discoveryQuery.data.explain}
                            mode={mode}
                            unpublished={new Set(discoveryQuery.data.unpublishedKrithiIds ?? [])}
                            scope={scope}
                            onOpenKrithi={openKrithi}
                            onOpenMacro={(h) => { const d = s.macroDestination(h); if (d) open(d); }}
                        />
                    ) : null}
                    {category === 'Krithis' && mode === 'Lexical' && lexical.data ? (
                        <>
                            <p className="px-5 pt-3 text-xs">{lexical.data.pages[0].total} in this library · Title order</p>
                            <views.KrithiListView krithis={lexicalItems} onOpen={openKrithi} />
                            {loadMore(lexical)}
                        </>
                    ) : null}
                    {category === 'Ragas' && ragas.data ? (
                        <>
                            <views.RagaDirectoryView ragas={flat(ragas.data.pages)} total={ragas.data.pages[0].total} onOpen={(id) => open({ kind: 'RagaDetail', ragaId: id })} />
                            {loadMore(ragas as unknown as typeof lexical)}
                        </>
                    ) : null}
                    {category === 'Composers' && composers.data ? (
                        <>
                            <views.ComposerDirectoryView composers={flat(composers.data.pages)} onOpen={(id) => open({ kind: 'ComposerDetail', composerId: id })} />
                            {loadMore(composers as unknown as typeof lexical)}
                        </>
                    ) : null}
                </LoadStateView>
            )}
        </div>
    );
};

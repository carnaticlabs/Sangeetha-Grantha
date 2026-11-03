import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import * as api from '../../api/previewApi';
import { previewKeys } from '../../api/previewApi';
import { LoadStateView } from '../../components/mobile-preview/PreviewParts';
import { usePalette } from '../../components/mobile-preview/previewPalette';
import { useScreenHeader } from '../../components/mobile-preview/previewHeader';
import * as views from '../../components/mobile-preview/screens';
import type { FacetKey, PreviewDestination } from '../../types/mobilePreview.types';

export interface PaneProps {
    open: (d: PreviewDestination) => void;
}

export const HomePane: React.FC<PaneProps> = ({ open }) => {
    const q = useQuery({ queryKey: previewKeys.discovery, queryFn: api.getPreviewDiscovery });
    return (
        <LoadStateView isLoading={q.isLoading} error={q.error} isEmpty={false} emptyTitle="" onRetry={() => { void q.refetch(); }}>
            {q.data ? (
                <views.HomeView
                    discovery={q.data}
                    onOpenKrithi={(id) => open({ kind: 'KrithiReader', krithiId: id })}
                    onOpenRagas={() => open({ kind: 'Browse' })}
                    onOpenComposers={() => open({ kind: 'Browse' })}
                    onOpenCollections={() => open({ kind: 'Collections' })}
                    onOpenKshetras={() => open({ kind: 'Kshetras' })}
                />
            ) : null}
        </LoadStateView>
    );
};

export const BrowsePane: React.FC<PaneProps> = ({ open }) => {
    const [selected, setSelected] = useState<FacetKey | null>(null);
    const q = useQuery({ queryKey: previewKeys.list('facets', { counts: 'true' }), queryFn: () => api.getPreviewFacets({ counts: 'true' }) });
    return (
        <>
            <LoadStateView isLoading={q.isLoading} error={q.error} isEmpty={!!q.data && q.data.facets.length === 0} emptyTitle="No facets yet" onRetry={() => { void q.refetch(); }}>
                {q.data ? <views.BrowseFacetsView facets={q.data.facets} selected={selected} onSelect={setSelected} /> : null}
            </LoadStateView>
            <button type="button" className="mx-5 mb-5 text-sm underline" onClick={() => open({ kind: 'Explore' })}>Browse ragas and composers</button>
        </>
    );
};

export const RagaDetailPane: React.FC<PaneProps & { ragaId: string }> = ({ ragaId, open }) => {
    const raga = useQuery({ queryKey: previewKeys.detail('raga', ragaId), queryFn: () => api.getPreviewRaga(ragaId) });
    useScreenHeader(raga.data?.name);
    const works = useQuery({ queryKey: previewKeys.list('raga-works', { ragaId }), queryFn: () => api.searchPreviewKrithis({ ragaId, pageSize: 30 }) });
    return (
        <LoadStateView isLoading={raga.isLoading} error={raga.error} isEmpty={false} emptyTitle="" onRetry={() => { void raga.refetch(); }}>
            {raga.data ? <views.RagaDetailView raga={raga.data} krithis={works.data?.items ?? []} onOpenKrithi={(id) => open({ kind: 'KrithiReader', krithiId: id })} /> : null}
        </LoadStateView>
    );
};

export const ComposerDetailPane: React.FC<PaneProps & { composerId: string }> = ({ composerId, open }) => {
    const composer = useQuery({ queryKey: previewKeys.detail('composer', composerId), queryFn: () => api.getPreviewComposer(composerId) });
    useScreenHeader(composer.data?.name);
    const works = useQuery({ queryKey: previewKeys.list('composer-works', { composerId }), queryFn: () => api.searchPreviewKrithis({ composerId, pageSize: 30 }) });
    return (
        <LoadStateView isLoading={composer.isLoading} error={composer.error} isEmpty={false} emptyTitle="" onRetry={() => { void composer.refetch(); }}>
            {composer.data ? <views.ComposerDetailView composer={composer.data} krithis={works.data?.items ?? []} onOpenKrithi={(id) => open({ kind: 'KrithiReader', krithiId: id })} /> : null}
        </LoadStateView>
    );
};

export const CollectionsPane: React.FC<PaneProps> = ({ open }) => {
    const q = useQuery({ queryKey: previewKeys.list('collections'), queryFn: api.getPreviewCollections });
    return (
        <LoadStateView isLoading={q.isLoading} error={q.error} isEmpty={!!q.data && q.data.items.length === 0} emptyTitle="No collections yet" emptyBody="Reviewed cycles appear here once their kritis are published." onRetry={() => { void q.refetch(); }}>
            {q.data ? <views.CollectionsView collections={q.data.items} onOpen={(slug) => open({ kind: 'CollectionDetail', slug })} /> : null}
        </LoadStateView>
    );
};

export const CollectionDetailPane: React.FC<PaneProps & { slug: string }> = ({ slug, open }) => {
    const q = useQuery({ queryKey: previewKeys.detail('collection', slug), queryFn: () => api.getPreviewCollection(slug) });
    useScreenHeader(q.data?.name);
    return (
        <LoadStateView isLoading={q.isLoading} error={q.error} isEmpty={false} emptyTitle="" onRetry={() => { void q.refetch(); }}>
            {q.data ? <views.CollectionDetailView collection={q.data} onOpenKrithi={(id) => open({ kind: 'KrithiReader', krithiId: id })} /> : null}
        </LoadStateView>
    );
};

export const KshetrasPane: React.FC<PaneProps> = ({ open }) => {
    const q = useQuery({ queryKey: previewKeys.list('temples'), queryFn: api.getPreviewTemples });
    return (
        <LoadStateView isLoading={q.isLoading} error={q.error} isEmpty={!!q.data && q.data.items.length === 0} emptyTitle="No kshetras yet" emptyBody="Places appear here once their kritis or notes are published." onRetry={() => { void q.refetch(); }}>
            {q.data ? <views.KshetrasView temples={q.data.items} onOpen={(id) => open({ kind: 'KshetraDetail', templeId: id })} /> : null}
        </LoadStateView>
    );
};

export const KshetraDetailPane: React.FC<PaneProps & { templeId: string }> = ({ templeId, open }) => {
    const q = useQuery({ queryKey: previewKeys.detail('temple', templeId), queryFn: () => api.getPreviewTemple(templeId) });
    useScreenHeader(q.data?.name);
    return (
        <LoadStateView isLoading={q.isLoading} error={q.error} isEmpty={false} emptyTitle="" onRetry={() => { void q.refetch(); }}>
            {q.data ? <views.KshetraDetailView temple={q.data} onOpenKrithi={(id) => open({ kind: 'KrithiReader', krithiId: id })} onOpenTemple={(id) => open({ kind: 'KshetraDetail', templeId: id })} /> : null}
        </LoadStateView>
    );
};

export const SimplePane: React.FC<{ title: string; body: string }> = ({ title, body }) => {
    const p = usePalette();
    return (
        <div className="p-5 text-sm">
            <p className="font-semibold">{title}</p>
            <p style={{ color: p.inkSoft }}>{body}</p>
        </div>
    );
};

import React, { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getPreviewIndexStatus, previewKeys } from '../../api/previewApi';
import IndexBanner from '../../components/mobile-preview/IndexBanner';
import PhoneFrame from '../../components/mobile-preview/PhoneFrame';
import { useMobilePreview } from '../../hooks/useMobilePreview';
import KrithiReaderView from '../../components/mobile-preview/KrithiReaderView';
import { usePreviewReader } from '../../hooks/usePreviewReader';
import { PaletteContext } from '../../components/mobile-preview/previewPalette';
import { HeaderContext, useScreenHeader, type ScreenHeader } from '../../components/mobile-preview/previewHeader';
import { paletteFor } from '../../components/mobile-preview/previewTokens';
import {
    BrowsePane, CollectionDetailPane, CollectionsPane, ComposerDetailPane, HomePane, KshetraDetailPane,
    KshetrasPane, RagaDetailPane, SimplePane,
} from './PreviewPanes';
import { SearchPane } from './SearchPane';
import type { PreviewDestination, PreviewPreferences } from '../../types/mobilePreview.types';

const TITLES: Record<PreviewDestination['kind'], string> = {
    Home: 'Home', Explore: 'Explore', Library: 'Library', Settings: 'Settings', Browse: 'Browse',
    Collections: 'Collections', CollectionDetail: 'Collection', Kshetras: 'Kshetras', KshetraDetail: 'Kshetra',
    RagaDetail: 'Raga', ComposerDetail: 'Composer', KrithiReader: 'Krithi',
};

interface ReaderPaneProps {
    krithiId: string;
    preferences: PreviewPreferences;
    open: (d: PreviewDestination) => void;
}

const ReaderPane: React.FC<ReaderPaneProps> = ({ krithiId, preferences, open }) => {
    const { reader, status, lyrics, selectedVariantId, selectVariant } = usePreviewReader(krithiId, preferences.preferredScript);
    useScreenHeader(reader.data?.title, reader.data?.composer.name);
    if (reader.isLoading) return <p role="status" className="p-5 text-sm opacity-70">Loading…</p>;
    if (reader.error || !reader.data) {
        return <p role="alert" className="p-5 text-sm">This composition is not available in the preview{reader.error ? `: ${(reader.error as Error).message}` : ''}.</p>;
    }
    return (
        <>
            {status && status !== 'PUBLISHED' ? (
                <p role="note" className="m-3 rounded border border-amber-300 bg-amber-50 p-2 text-xs text-amber-900">
                    Not published ({status.toLowerCase().replace('_', ' ')}): mobile users cannot see this.
                </p>
            ) : null}
            <KrithiReaderView
                reader={reader.data}
                lyrics={lyrics.data}
                lyricsState={lyrics.isFetching && !lyrics.data ? { status: 'loading' } : lyrics.error ? { status: 'error', message: (lyrics.error as Error).message } : { status: 'idle' }}
                selectedVariantId={selectedVariantId}
                preferences={preferences}
                onSelectVariant={selectVariant}
                onRetryLyrics={() => { void lyrics.refetch(); }}
                onOpenCollection={(slug) => open({ kind: 'CollectionDetail', slug })}
                onOpenNeighbour={(id) => open({ kind: 'KrithiReader', krithiId: id })}
                onOpenFacet={() => open({ kind: 'Browse' })}
            />
        </>
    );
};

const Screen: React.FC<{ destination: PreviewDestination; preferences: PreviewPreferences; open: (d: PreviewDestination) => void; seed: { query: string; nonce: number } | null }> = ({ destination: d, preferences, open, seed }) => {
    switch (d.kind) {
        case 'Home': return <HomePane open={open} />;
        case 'Explore': return <SearchPane key={seed?.nonce ?? 0} open={open} seed={seed} />;
        case 'Browse': return <BrowsePane open={open} />;
        case 'KrithiReader': return <ReaderPane krithiId={d.krithiId} preferences={preferences} open={open} />;
        case 'RagaDetail': return <RagaDetailPane ragaId={d.ragaId} open={open} />;
        case 'ComposerDetail': return <ComposerDetailPane composerId={d.composerId} open={open} />;
        case 'Collections': return <CollectionsPane open={open} />;
        case 'CollectionDetail': return <CollectionDetailPane slug={d.slug} open={open} />;
        case 'Kshetras': return <KshetrasPane open={open} />;
        case 'KshetraDetail': return <KshetraDetailPane templeId={d.templeId} open={open} />;
        case 'Library': return <SimplePane title="No favourites yet" body="Favourites are kept on the mobile device and are not part of the preview." />;
        case 'Settings': return <SimplePane title="Settings" body="Appearance and text size for this preview are set beside the phone." />;
    }
};

interface MobilePreviewAppProps {
    /** Opens the reader for this krithi first; used by the editor tab and the `?krithi=` link. */
    krithiId?: string | null;
}

/**
 * The phone frame with its preview-local settings. It refetches everything it shows when it mounts, when the
 * window regains focus, when the editor saves (via `previewKeys.all`), and on the Refresh button, so an edit is
 * visible without a page reload (TRACK-158 R6).
 */
const MobilePreviewApp: React.FC<MobilePreviewAppProps> = ({ krithiId }) => {
    const queryClient = useQueryClient();
    useEffect(() => {
        const refresh = () => { void queryClient.invalidateQueries({ queryKey: previewKeys.all }); };
        refresh();
        window.addEventListener('focus', refresh);
        return () => window.removeEventListener('focus', refresh);
    }, [queryClient]);
    const [header, setHeader] = useState<ScreenHeader | null>(null);
    const [seed, setSeed] = useState<{ query: string; nonce: number } | null>(null);
    const indexStatus = useQuery({
        queryKey: previewKeys.indexStatus(krithiId ?? ''),
        queryFn: () => getPreviewIndexStatus(krithiId as string),
        enabled: Boolean(krithiId),
        // A worker run shows up as "current" without a reload; polling pauses while the tab is hidden.
        refetchInterval: 30_000,
        refetchIntervalInBackground: false,
    });
    const preview = useMobilePreview(krithiId ? { kind: 'KrithiReader', krithiId } : undefined);
    const { destination, preferences } = preview;
    return (
        <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
            <div className="max-w-md">
                <h1 className="text-2xl font-display font-medium">Mobile Preview</h1>
                <p className="mt-2 text-sm text-ink-500">
                    Read-only view of the Rasika mobile app, fed by the same data. Unpublished krithis are included here
                    and are not visible to mobile users.
                </p>
                <fieldset className="mt-4 space-y-2 text-sm">
                    <legend className="font-semibold">Preview settings (this browser only)</legend>
                    <label className="flex items-center gap-2">
                        Appearance
                        <select
                            value={preferences.appearance}
                            onChange={(e) => preview.setPreferences({ appearance: e.target.value as 'light' | 'dark' })}
                            className="rounded border border-border-light bg-cream px-2 py-1"
                        >
                            <option value="light">Light</option>
                            <option value="dark">Dark</option>
                        </select>
                    </label>
                    <label className="flex items-center gap-2">
                        Text size
                        <select
                            value={preferences.textSize}
                            onChange={(e) => preview.setPreferences({ textSize: e.target.value as 'small' | 'medium' | 'large' })}
                            className="rounded border border-border-light bg-cream px-2 py-1"
                        >
                            <option value="small">Small</option>
                            <option value="medium">Medium</option>
                            <option value="large">Large</option>
                        </select>
                    </label>
                </fieldset>
                {indexStatus.data ? (
                    <IndexBanner
                        status={indexStatus.data}
                        onSearchThisKrithi={() => {
                            const title = queryClient.getQueryData<{ title: string }>(previewKeys.krithi(krithiId ?? ''))?.title ?? '';
                            setSeed({ query: title, nonce: (seed?.nonce ?? 0) + 1 });
                            preview.selectTab('Explore');
                            preview.returnToRoot();
                        }}
                    />
                ) : null}
                <button
                    type="button"
                    onClick={() => { void queryClient.invalidateQueries({ queryKey: previewKeys.all }); }}
                    className="mt-4 rounded border border-border-light px-3 py-1 text-sm"
                >
                    Refresh preview
                </button>
            </div>
            <PhoneFrame
                preferences={preferences}
                selectedTab={preview.state.selectedTab}
                onSelectTab={preview.selectTab}
                onBack={preview.canGoBack ? preview.back : null}
                title={header?.title ?? TITLES[destination.kind]}
                subtitle={header?.subtitle}
            >
                <PaletteContext.Provider value={paletteFor(preferences.appearance)}>
                    <HeaderContext.Provider value={setHeader}>
                        <Screen destination={destination} preferences={preferences} open={preview.open} seed={seed} />
                    </HeaderContext.Provider>
                </PaletteContext.Provider>
            </PhoneFrame>
        </div>
    );
};

export default MobilePreviewApp;

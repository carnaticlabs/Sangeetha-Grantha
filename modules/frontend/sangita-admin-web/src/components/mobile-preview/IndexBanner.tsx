import React from 'react';
import type { PreviewIndexStatus } from '../../types/mobilePreview.types';

const WORDING: Record<PreviewIndexStatus['verdict'], { tone: string; text: string }> = {
    NOT_INDEXED: {
        tone: 'border-amber-300 bg-amber-50 text-amber-900',
        text: 'Not in the search index: All matches and Related meanings cannot find this krithi’s text. Titles & lyrics is current.',
    },
    MAY_BE_STALE: {
        tone: 'border-amber-300 bg-amber-50 text-amber-900',
        text: 'Index out of date for this krithi: All matches and Related meanings may show old text. Titles & lyrics is current.',
    },
    CURRENT: {
        tone: 'border-emerald-300 bg-emerald-50 text-emerald-900',
        text: 'Search index looks current: nothing changed after its documents were written.',
    },
};

const fmt = (iso?: string | null): string => (iso ? new Date(iso).toLocaleString() : 'never');

/** Staleness is never silent (TRACK-158 R11). The check compares timestamps, so the wording says "may". */
const IndexBanner: React.FC<{ status: PreviewIndexStatus; onSearchThisKrithi: () => void }> = ({ status, onSearchThisKrithi }) => {
    const w = WORDING[status.verdict];
    return (
        <section aria-label="Search index status" className={`mt-4 rounded border p-3 text-sm ${w.tone}`}>
            <p role="status" className="font-semibold">{w.text}</p>
            {status.reasons.length > 0 ? <ul className="mt-1 list-disc pl-5 text-xs">{status.reasons.map((r) => <li key={r}>{r}</li>)}</ul> : null}
            <p className="mt-1 text-xs">
                Source last changed {fmt(status.sourceUpdatedAt)}; index written {fmt(status.indexedAt)}
                {status.activeProfileModel ? ` · profile ${status.activeProfileModel}/${status.activeProfileDimensions}` : ''}.
            </p>
            <button type="button" onClick={onSearchThisKrithi} className="mt-2 rounded border border-current px-2 py-1 text-xs font-semibold">
                Search for this krithi
            </button>
        </section>
    );
};

export default IndexBanner;

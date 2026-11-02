import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getKrithi } from '../api/client';
import { getPreviewKrithi, getPreviewLyrics, previewKeys } from '../api/previewApi';
import { chooseVariant } from '../components/mobile-preview/readerLogic';
import type { ScriptCode } from '../types/mobilePreview.types';

/**
 * Reader data for one krithi: the reader DTO and lyrics from the preview mount, plus the workflow status from
 * the existing admin read (the V2 reader DTO deliberately carries no workflow state).
 */
export function usePreviewReader(krithiId: string, preferredScript: ScriptCode | null) {
    const reader = useQuery({ queryKey: previewKeys.krithi(krithiId), queryFn: () => getPreviewKrithi(krithiId) });
    const status = useQuery({
        queryKey: [...previewKeys.krithi(krithiId), 'status'],
        queryFn: async (): Promise<string | null> => {
            // The wire field is `workflowState`; the admin `KrithiDetail` type calls it `status`.
            const detail = (await getKrithi(krithiId)) as unknown as { workflowState?: string };
            return detail.workflowState ?? null;
        },
    });
    // The reader's own pick is remembered with the krithi it belongs to, so opening another krithi starts afresh.
    const [pick, setPick] = useState<{ krithiId: string; variantId: string } | null>(null);
    const chosen = pick?.krithiId === krithiId ? pick.variantId : null;

    const selectedVariantId = chosen ?? (reader.data ? chooseVariant(reader.data, preferredScript) : null);
    const lyrics = useQuery({
        queryKey: selectedVariantId ? previewKeys.lyrics(krithiId, selectedVariantId) : ['mobile-preview', 'lyrics', 'none'],
        queryFn: () => getPreviewLyrics(krithiId, selectedVariantId as string),
        enabled: selectedVariantId !== null,
        placeholderData: (previous) => previous,
    });

    return {
        reader,
        status: status.data,
        lyrics,
        selectedVariantId,
        selectVariant: (variantId: string) => setPick({ krithiId, variantId }),
    };
}

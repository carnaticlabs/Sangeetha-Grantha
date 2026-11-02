import { describe, it, expect, vi } from 'vitest';
import { render, screen, within } from '../../test/test-utils';
import KrithiReaderView from './KrithiReaderView';
import type { KrithiReader, Lyrics, PreviewPreferences } from '../../types/mobilePreview.types';

const prefs: PreviewPreferences = { appearance: 'light', textSize: 'medium', preferredScript: null };

const reader = (over: Partial<KrithiReader> = {}): KrithiReader => ({
    id: 'k1', title: 'Test', composer: { id: 'c', name: 'Dikshitar' },
    ragas: [{ id: 'r1', name: 'Todi', orderIndex: 0 }], musicalForm: 'KRITHI', originalLanguage: 'SA', isRagamalika: false,
    defaultVariantId: 'v1', completeness: 'COMPLETE',
    variants: [
        { id: 'v1', language: 'SA', script: 'LATIN', isPrimary: true },
        { id: 'v2', language: 'SA', script: 'TAMIL', isPrimary: false },
    ],
    ...over,
});

const lyrics = (script: 'LATIN' | 'TAMIL', sections: Lyrics['sections']): Lyrics => ({
    variantId: 'v1', krithiId: 'k1', language: 'SA', script, isPrimary: true, sections, completeness: 'COMPLETE',
});

const baseProps = {
    lyricsState: { status: 'idle' as const },
    selectedVariantId: 'v1',
    preferences: prefs,
    onSelectVariant: vi.fn(),
    onRetryLyrics: vi.fn(),
    onOpenCollection: vi.fn(),
    onOpenNeighbour: vi.fn(),
    onOpenFacet: vi.fn(),
};

describe('KrithiReaderView', () => {
    it('orders sections by orderIndex and prefers a stored label', () => {
        render(<KrithiReaderView {...baseProps} reader={reader()} lyrics={lyrics('LATIN', [
            { sectionId: 's2', sectionType: 'CHARANAM', label: 'Charanam 1', orderIndex: 2, text: 'c' },
            { sectionId: 's1', sectionType: 'PALLAVI', orderIndex: 1, text: 'p' },
        ])} />);
        const headings = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent);
        expect(headings).toEqual(['Pallavi', 'Charanam 1']);
    });

    it('shows the varnam charanam caption only for varnams', () => {
        const sections = [{ sectionId: 's', sectionType: 'CHARANAM', orderIndex: 1, text: 'x' }];
        const { rerender } = render(<KrithiReaderView {...baseProps} reader={reader({ musicalForm: 'VARNAM' })} lyrics={lyrics('LATIN', sections)} />);
        expect(screen.getByText('pallavi of the charanam')).toBeInTheDocument();
        rerender(<KrithiReaderView {...baseProps} reader={reader({ musicalForm: 'KRITHI' })} lyrics={lyrics('LATIN', sections)} />);
        expect(screen.queryByText('pallavi of the charanam')).not.toBeInTheDocument();
    });

    it('labels a section with its bound ragas only, and lists the ragamalika sequence in Details', async () => {
        const r = reader({
            isRagamalika: true,
            ragas: [
                { id: 'r2', name: 'Kalyani', orderIndex: 1, sectionId: 's1' },
                { id: 'r1', name: 'Todi', orderIndex: 0, sectionId: null },
            ],
        });
        const { user } = render(<KrithiReaderView {...baseProps} reader={r} lyrics={lyrics('LATIN', [{ sectionId: 's1', sectionType: 'PALLAVI', orderIndex: 1, text: 'p' }])} />);
        const section = screen.getByRole('region', { name: 'Pallavi' });
        expect(within(section).getByText('Kalyani')).toBeInTheDocument();
        expect(within(section).queryByText('Todi')).not.toBeInTheDocument();
        await user.click(screen.getByRole('tab', { name: 'Details' }));
        const sequence = screen.getByRole('list', { name: 'Raga sequence' });
        expect(within(sequence).getAllByRole('listitem').map((li) => li.textContent)).toEqual(['Todi', 'Kalyani']);
    });

    it('shows Tamil digit notation as superscripts only for Tamil script', () => {
        const sections = [{ sectionId: 's', sectionType: 'PALLAVI', orderIndex: 1, text: 'ப் 3 ரஹ்மேந்த் 3 ராதி 3' }];
        const { rerender } = render(<KrithiReaderView {...baseProps} selectedVariantId="v2" reader={reader()} lyrics={lyrics('TAMIL', sections)} />);
        expect(screen.getByText('ப்³ரஹ்மேந்த்³ராதி³')).toBeInTheDocument();
        rerender(<KrithiReaderView {...baseProps} selectedVariantId="v1" reader={reader()} lyrics={lyrics('LATIN', sections)} />);
        expect(screen.getByText('ப் 3 ரஹ்மேந்த் 3 ராதி 3')).toBeInTheDocument();
    });

    it('offers a script switch when readings differ in script and selects the lowest id of that script', async () => {
        const onSelectVariant = vi.fn();
        const { user } = render(<KrithiReaderView {...baseProps} onSelectVariant={onSelectVariant} reader={reader()} lyrics={lyrics('LATIN', [])} />);
        await user.click(screen.getByRole('button', { name: 'Tamil' }));
        expect(onSelectVariant).toHaveBeenCalledWith('v2');
    });

    it('shows an error with a retry for a failed reading', async () => {
        const onRetryLyrics = vi.fn();
        const { user } = render(<KrithiReaderView {...baseProps} onRetryLyrics={onRetryLyrics} lyricsState={{ status: 'error' }} reader={reader()} lyrics={undefined} />);
        await user.click(screen.getByRole('button', { name: 'Retry' }));
        expect(onRetryLyrics).toHaveBeenCalled();
    });
});

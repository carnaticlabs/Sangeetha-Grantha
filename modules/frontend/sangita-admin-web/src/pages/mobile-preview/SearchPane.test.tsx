import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, render, screen, waitFor, within } from '../../test/test-utils';
import { SearchPane } from './SearchPane';
import { markEdited } from '../../components/mobile-preview/previewEdits';
import type { PreviewSearchResponse } from '../../types/mobilePreview.types';
import type { SemanticSearchResultItem } from '../../types';

vi.mock('../../api/previewApi', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../../api/previewApi')>()),
    previewSearch: vi.fn(),
    searchPreviewKrithis: vi.fn(),
    searchPreviewRagas: vi.fn(),
    searchPreviewComposers: vi.fn(),
}));

import { previewSearch, searchPreviewKrithis, searchPreviewRagas, searchPreviewComposers } from '../../api/previewApi';

const item = (id: string, title: string): SemanticSearchResultItem => ({
    krithiId: id, title, composerName: 'Dikshitar', ragaName: 'Todi', talaName: null,
    documentKind: 'SECTION_PASSAGE', matchedContent: 'matched lyric passage', similarityScore: 0.7, lexicalScore: 0.4, rrfScore: 0.03,
});

const reply = (over: Partial<PreviewSearchResponse['response']> = {}, explain = true): PreviewSearchResponse => ({
    response: { query: 'q', totalMatches: 3, macroHits: [], items: [item('a', 'Alpha'), item('b', 'Beta')], exactTitleMatchId: null, ...over },
    explain: explain ? {
        mode: 'HYBRID', profileModel: 'gemini-embedding-2', profileDimensions: 768, floorCalibrated: true,
        floor: { minSimilarity: 0.81, minLexical: 0.8, minGap: 0.03 },
        macroCandidates: [{ kind: 'COLLECTION', title: 'Weak cycle', similarity: 0.7, gap: 0.01, cleared: false, reason: 'similarity 0.700 is below 0.810' }],
    } : null,
});

const searchFor = async (user: ReturnType<typeof render>['user'], text: string) => {
    await user.type(screen.getByLabelText('Search'), text);
    await user.click(screen.getByRole('button', { name: 'Search' }));
};

describe('SearchPane', () => {
    beforeEach(() => {
        vi.mocked(previewSearch).mockReset();
        vi.mocked(searchPreviewKrithis).mockReset();
        vi.mocked(searchPreviewRagas).mockReset();
        vi.mocked(searchPreviewComposers).mockReset();
    });

    it('is idle until a query is submitted and never searches on typing', async () => {
        const { user } = render(<SearchPane open={vi.fn()} />);
        expect(screen.getByText(/Find a known kriti/)).toBeInTheDocument();
        await user.type(screen.getByLabelText('Search'), 'kamalamba');
        expect(previewSearch).not.toHaveBeenCalled();
    });

    it('shows the exact-title kriti first, then cards, then the other kritis, and explains hidden cards', async () => {
        vi.mocked(previewSearch).mockResolvedValue(reply({
            exactTitleMatchId: 'b',
            macroHits: [{ kind: 'COLLECTION', title: 'Kamalamba Navavarnam', snippet: 'nine enclosures', collectionSlug: 'kn', recordedGap: false, similarityScore: 0.9 }],
        }));
        const open = vi.fn();
        const { user } = render(<SearchPane open={open} />);
        await searchFor(user, 'kamalamba');
        await screen.findByText('Kamalamba Navavarnam');
        const cards = screen.getAllByRole('button').filter((b) => /Dikshitar|Collection:/.test(b.getAttribute('aria-label') ?? ''));
        expect(cards.map((c) => c.getAttribute('aria-label'))).toEqual(['Beta, Dikshitar', 'Collection: Kamalamba Navavarnam', 'Alpha, Dikshitar']);
        expect(previewSearch).toHaveBeenCalledWith('hybrid', 'kamalamba', 'PUBLISHED_ONLY');
        expect(previewSearch).toHaveBeenCalledTimes(1);

        const explain = screen.getByRole('region', { name: 'Why cards appeared' });
        expect(within(explain).getByText(/Hidden: similarity 0.700 is below 0.810/)).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Collection: Kamalamba Navavarnam' }));
        expect(open).toHaveBeenCalledWith({ kind: 'CollectionDetail', slug: 'kn' });
    });

    it('shows why a kriti matched on request', async () => {
        vi.mocked(previewSearch).mockResolvedValue(reply());
        const { user } = render(<SearchPane open={vi.fn()} />);
        await searchFor(user, 'x');
        await user.click((await screen.findAllByRole('button', { name: 'Why this matched' }))[0]);
        expect(screen.getByText('a lyric section')).toBeInTheDocument();
        expect(screen.getByText('matched lyric passage')).toBeInTheDocument();
    });

    it('badges only krithis the server reports as unpublished, with a single search call', async () => {
        vi.mocked(previewSearch).mockResolvedValue({
            ...reply({ items: [item('a', 'Alpha'), item('d', 'Draft Delta')] }),
            unpublishedKrithiIds: ['d'],
        });
        const { user } = render(<SearchPane open={vi.fn()} />);
        await user.click(screen.getByLabelText(/Include unpublished/));
        await searchFor(user, 'x');
        const delta = await screen.findByRole('button', { name: 'Draft Delta, Dikshitar' });
        expect(within(delta).getByText('Not visible on mobile')).toBeInTheDocument();
        expect(within(screen.getByRole('button', { name: 'Alpha, Dikshitar' })).queryByText('Not visible on mobile')).not.toBeInTheDocument();
        expect(previewSearch).toHaveBeenCalledTimes(1);
        expect(previewSearch).toHaveBeenCalledWith('hybrid', 'x', 'INCLUDE_UNPUBLISHED');
        expect(screen.getByText(/order can differ slightly/)).toBeInTheDocument();
    });

    it('shows an error with retry instead of an empty list when search fails', async () => {
        vi.mocked(previewSearch).mockRejectedValue(new Error('API Error 503: embedding unavailable'));
        const { user } = render(<SearchPane open={vi.fn()} />);
        await searchFor(user, 'x');
        expect(await screen.findByRole('alert')).toHaveTextContent('embedding unavailable');
        expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
    });

    it('shows the empty state for no matches', async () => {
        vi.mocked(previewSearch).mockResolvedValue(reply({ items: [], totalMatches: 0 }, false));
        const { user } = render(<SearchPane open={vi.fn()} />);
        await searchFor(user, 'zzz');
        expect(await screen.findByText('Nothing stored under that')).toBeInTheDocument();
    });

    it('pages lexical results 30 at a time and stops at three pages', async () => {
        const row = (n: number) => ({ id: `k${n}`, title: `T${n}`, composer: { id: 'c', name: 'C' }, ragas: [], musicalForm: 'KRITHI' as const, isRagamalika: false });
        vi.mocked(searchPreviewKrithis).mockImplementation(async (q) => ({
            items: Array.from({ length: 30 }, (_, i) => row((q?.page ?? 0) * 30 + i)), total: 500, page: q?.page ?? 0, pageSize: 30,
        }));
        const { user } = render(<SearchPane open={vi.fn()} />);
        await user.click(screen.getByRole('button', { name: 'Search options' }));
        await user.click(screen.getByRole('button', { name: 'Titles & lyrics' }));
        await searchFor(user, 'ta');
        await screen.findByText('T0');
        for (let i = 0; i < 2; i++) await user.click(await screen.findByRole('button', { name: 'Load more' }));
        await screen.findByText('T89');
        await waitFor(() => expect(screen.queryByRole('button', { name: 'Load more' })).not.toBeInTheDocument());
        expect(searchPreviewKrithis).toHaveBeenCalledTimes(3);
        expect(searchPreviewKrithis).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2, pageSize: 30 }), 'PUBLISHED_ONLY');
    });

    it('applies the raga picker limit note and tells the curator when results are older than a save', async () => {
        vi.mocked(searchPreviewKrithis).mockResolvedValue({ items: [], total: 0, page: 0, pageSize: 30 });
        vi.mocked(searchPreviewRagas).mockResolvedValue({ items: [{ id: 'r', name: 'Todi', matchingAliases: [], publishedCompositionCount: 1 }], total: 1014, page: 0, pageSize: 30 });
        vi.mocked(searchPreviewComposers).mockResolvedValue({ items: [], total: 0, page: 0, pageSize: 30 });
        const { user } = render(<SearchPane open={vi.fn()} />);
        await user.click(screen.getByRole('button', { name: 'Search options' }));
        await user.click(screen.getByRole('button', { name: 'Titles & lyrics' }));
        await user.click(screen.getByRole('button', { name: '+ Rāga or composer' }));
        expect(await screen.findByText(/only the first 1 of 1014 ragas/)).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Cancel' }));

        await searchFor(user, 'ta');
        await screen.findByText('Nothing stored under that');
        expect(screen.queryByText(/Edits saved since this search/)).not.toBeInTheDocument();
        act(() => { markEdited(Date.now() + 1000); });
        expect(await screen.findByText(/Edits saved since this search/)).toBeInTheDocument();
    });
});

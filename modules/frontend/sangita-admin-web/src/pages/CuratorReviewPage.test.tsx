import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '../test/test-utils';
import CuratorReviewPage from './CuratorReviewPage';
import { ImportedKrithi } from '../types';

vi.mock('../api/client', () => ({
    getCuratorStats: vi.fn(),
    getCuratorSectionIssues: vi.fn(),
    getImports: vi.fn(),
    reviewImport: vi.fn(),
    searchKrithis: vi.fn(),
}));

// The page only consumes useSourceDetail from this module; returning no data
// keeps TierBadge/AuthorityWarning out of the render (tier-specific UI).
vi.mock('../hooks/useSourcingQueries', () => ({
    useSourceDetail: vi.fn(() => ({ data: undefined })),
}));

import {
    getCuratorStats,
    getCuratorSectionIssues,
    getImports,
    reviewImport,
} from '../api/client';

const importFixture = (over: Partial<ImportedKrithi> = {}): ImportedKrithi => ({
    id: 'imp-1',
    importSourceId: 'src-1',
    sourceKey: 'https://example.org/krithi/1',
    rawTitle: 'Vatapi Ganapatim',
    rawLyrics: 'vātāpi gaṇapatiṃ bhajē',
    rawComposer: 'Muthuswami Dikshitar',
    rawRaga: 'Hamsadhwani',
    rawTala: 'Adi',
    rawDeity: 'Ganesha',
    rawTemple: null,
    rawLanguage: 'SANSKRIT',
    parsedPayload: null,
    resolutionData: null,
    importStatus: 'IN_REVIEW',
    mappedKrithiId: null,
    reviewerUserId: null,
    reviewerNotes: null,
    reviewedAt: null,
    createdAt: '2026-07-01T00:00:00Z',
    ...over,
});

const IMPORT_A = importFixture();
const IMPORT_B = importFixture({ id: 'imp-2', rawTitle: 'Nagumomu', rawComposer: 'Thyagaraja', rawRaga: 'Abheri' });

const STATS = { totalKrithis: 500, totalPending: 12, totalApproved: 300, totalRejected: 8, sectionIssuesCount: 3 };

/**
 * Detail-panel action buttons and modal confirm buttons share labels
 * ("Approve & Create", "Reject"); the modal's is rendered last.
 */
const lastButton = (name: string | RegExp) => {
    const all = screen.getAllByRole('button', { name });
    return all[all.length - 1];
};

beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getCuratorStats).mockResolvedValue(STATS);
    vi.mocked(getImports).mockResolvedValue([IMPORT_A, IMPORT_B]);
    vi.mocked(reviewImport).mockResolvedValue(IMPORT_A);
});

describe('CuratorReviewPage — queue', () => {
    it('renders stats, the pending queue, and auto-selects the first import into the form', async () => {
        render(<CuratorReviewPage />);

        expect(await screen.findByText('Nagumomu')).toBeInTheDocument();
        // Stats cards
        expect(screen.getByText('Total Krithis')).toBeInTheDocument();
        expect(await screen.findByText('500')).toBeInTheDocument();
        // First import auto-selected: override form populated from raw fields
        expect(screen.getByLabelText('Title')).toHaveValue('Vatapi Ganapatim');
        expect(screen.getByLabelText('Composer')).toHaveValue('Muthuswami Dikshitar');
        expect(screen.getByLabelText('Raga')).toHaveValue('Hamsadhwani');
        expect(screen.getByLabelText('Lyrics Preview')).toHaveValue('vātāpi gaṇapatiṃ bhajē');
    });

    it('shows the empty state when the queue has no imports', async () => {
        vi.mocked(getImports).mockResolvedValue([]);
        render(<CuratorReviewPage />);
        expect(await screen.findByText('Queue is empty.')).toBeInTheDocument();
    });

    it('refetches page 0 with the new status when the filter changes', async () => {
        const { user } = render(<CuratorReviewPage />);
        await screen.findByText('Nagumomu');

        await user.selectOptions(screen.getByRole('combobox'), 'APPROVED');

        await waitFor(() => expect(getImports).toHaveBeenCalledWith('APPROVED', 51, 0));
    });
});

describe('CuratorReviewPage — review actions', () => {
    it('approve: confirms via modal and sends APPROVED with the override values', async () => {
        const { user } = render(<CuratorReviewPage />);
        await screen.findByText('Nagumomu');

        await user.click(screen.getByRole('button', { name: 'Approve & Create' }));
        expect(screen.getByText('Approve Import')).toBeInTheDocument();
        await user.click(lastButton('Approve & Create'));

        await waitFor(() =>
            expect(reviewImport).toHaveBeenCalledWith('imp-1', {
                status: 'APPROVED',
                reviewerNotes: null,
                overrides: {
                    title: 'Vatapi Ganapatim',
                    composer: 'Muthuswami Dikshitar',
                    raga: 'Hamsadhwani',
                    tala: 'Adi',
                    language: 'SANSKRIT',
                    deity: 'Ganesha',
                    temple: '',
                    lyrics: 'vātāpi gaṇapatiṃ bhajē',
                },
            }),
        );
        expect(await screen.findByText('Import approved')).toBeInTheDocument();
    });

    it('approve: edited override fields are sent, not the raw values', async () => {
        const { user } = render(<CuratorReviewPage />);
        await screen.findByText('Nagumomu');

        const raga = screen.getByLabelText('Raga');
        await user.clear(raga);
        await user.type(raga, 'Hamsadhvani');

        await user.click(screen.getByRole('button', { name: 'Approve & Create' }));
        await user.click(lastButton('Approve & Create'));

        await waitFor(() =>
            expect(reviewImport).toHaveBeenCalledWith(
                'imp-1',
                expect.objectContaining({ overrides: expect.objectContaining({ raga: 'Hamsadhvani' }) }),
            ),
        );
    });

    it('reject: requires notes before the modal confirm enables, then sends REJECTED', async () => {
        const { user } = render(<CuratorReviewPage />);
        await screen.findByText('Nagumomu');

        await user.click(screen.getByRole('button', { name: 'Reject' }));
        expect(screen.getByText('Reject Import')).toBeInTheDocument();

        const confirm = lastButton('Reject');
        expect(confirm).toBeDisabled();
        await user.type(screen.getByPlaceholderText('Reason for rejection (required)...'), 'Duplicate source');
        expect(confirm).toBeEnabled();
        await user.click(confirm);

        await waitFor(() =>
            expect(reviewImport).toHaveBeenCalledWith('imp-1', {
                status: 'REJECTED',
                reviewerNotes: 'Duplicate source',
            }),
        );
        expect(await screen.findByText('Import rejected')).toBeInTheDocument();
    });

    it('approve failure: surfaces an error toast', async () => {
        vi.mocked(reviewImport).mockRejectedValue(new Error('API Error 500'));
        const { user } = render(<CuratorReviewPage />);
        await screen.findByText('Nagumomu');

        await user.click(screen.getByRole('button', { name: 'Approve & Create' }));
        await user.click(lastButton('Approve & Create'));

        expect(await screen.findByText('Failed to approve import')).toBeInTheDocument();
    });

    it('keyboard: pressing "a" opens the approve modal for the selected import', async () => {
        const { user } = render(<CuratorReviewPage />);
        await screen.findByText('Nagumomu');

        await user.keyboard('a');

        expect(await screen.findByText('Approve Import')).toBeInTheDocument();
        expect(screen.getByText('Approve "Vatapi Ganapatim" and create a new krithi?')).toBeInTheDocument();
    });
});

describe('CuratorReviewPage — bulk selection', () => {
    it('select-all surfaces the bulk bar and bulk approve reviews every selected import', async () => {
        const { user } = render(<CuratorReviewPage />);
        await screen.findByText('Nagumomu');

        const [selectAll] = screen.getAllByRole('checkbox');
        await user.click(selectAll);
        expect(screen.getByText('2 selected')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Approve Selected' }));
        expect(screen.getByText('Bulk Approve')).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Approve All' }));

        await waitFor(() => expect(reviewImport).toHaveBeenCalledTimes(2));
        expect(reviewImport).toHaveBeenCalledWith('imp-1', { status: 'APPROVED', reviewerNotes: null });
        expect(reviewImport).toHaveBeenCalledWith('imp-2', { status: 'APPROVED', reviewerNotes: null });
        expect(await screen.findByText('2 imports approved')).toBeInTheDocument();
    });
});

describe('CuratorReviewPage — section issues tab', () => {
    it('only queries section issues once the tab is opened, then renders them', async () => {
        vi.mocked(getCuratorSectionIssues).mockResolvedValue({
            items: [{ krithiId: 'k-9', title: 'Endaro Mahanubhavulu', language: 'TELUGU', expectedSections: 3, actualSections: 0, issueType: 'missing sections' }],
            total: 1, page: 0, size: 50,
        });
        const { user } = render(<CuratorReviewPage />);
        await screen.findByText('Nagumomu');
        expect(getCuratorSectionIssues).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', { name: /Section Issues/ }));

        expect(await screen.findByText('Endaro Mahanubhavulu')).toBeInTheDocument();
        expect(getCuratorSectionIssues).toHaveBeenCalledWith(0, 50);
    });
});

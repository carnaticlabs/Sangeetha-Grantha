import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '../../test/test-utils';
import { UnresolvedRagasTab } from './UnresolvedRagasTab';

vi.mock('../../api/client', () => ({
    getUnresolvedRagas: vi.fn(),
    getRagas: vi.fn(),
    attachRagaAlias: vi.fn(),
    confirmNewRaga: vi.fn(),
    disambiguateRaga: vi.fn(),
    scanRagaScaleCollisions: vi.fn(),
}));

import {
    getUnresolvedRagas,
    getRagas,
    attachRagaAlias,
    disambiguateRaga,
} from '../../api/client';

const QUEUE_UNKNOWN = {
    id: 'q-1',
    rawName: 'Dhanyasi-alt',
    matchKey: 'dhanyasi',
    kind: 'unknown',
    context: null,
    proposedLakshana: null,
    status: 'pending',
    resolvedRagaId: null,
    createdAt: '2026-08-30T00:00:00Z',
    resolvedAt: null,
};

const QUEUE_AMBIGUOUS = {
    id: 'q-2',
    rawName: 'Kalāvati',
    matchKey: 'kalavati',
    kind: 'ambiguous',
    context: null,
    proposedLakshana: JSON.stringify({ candidateRagaIds: ['r-31', 'r-16'] }),
    status: 'pending',
    resolvedRagaId: null,
    createdAt: '2026-08-30T00:00:00Z',
    resolvedAt: null,
};

beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getRagas).mockResolvedValue([
        { id: 'r-31', name: 'Kalāvathi', nameNormalized: 'kalavathi', melakartaNumber: 31, createdAt: '', updatedAt: '' },
        { id: 'r-16', name: 'Kalāvati', nameNormalized: 'kalavati', melakartaNumber: 16, createdAt: '', updatedAt: '' },
        { id: 'r-abh', name: 'Abheri', nameNormalized: 'abheri', createdAt: '', updatedAt: '' },
    ]);
});

describe('UnresolvedRagasTab', () => {
    it('lists pending unknown items and exposes attach + confirm-new', async () => {
        vi.mocked(getUnresolvedRagas).mockResolvedValue({
            items: [QUEUE_UNKNOWN], total: 1, page: 0, size: 50,
        });
        render(
            <UnresolvedRagasTab page={0} pageSize={50} onPageChange={vi.fn()} onError={vi.fn()} onSuccess={vi.fn()} />,
        );

        expect(await screen.findByText('Dhanyasi-alt')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Attach alias' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Confirm new' })).toBeInTheDocument();
    });

    it('attach-alias posts the selected raga', async () => {
        vi.mocked(getUnresolvedRagas).mockResolvedValue({
            items: [QUEUE_UNKNOWN], total: 1, page: 0, size: 50,
        });
        vi.mocked(attachRagaAlias).mockResolvedValue({
            id: 'r-abh', name: 'Abheri', nameNormalized: 'abheri', createdAt: '', updatedAt: '',
        });
        const onSuccess = vi.fn();
        const { user } = render(
            <UnresolvedRagasTab page={0} pageSize={50} onPageChange={vi.fn()} onError={vi.fn()} onSuccess={onSuccess} />,
        );
        await screen.findByText('Dhanyasi-alt');
        await user.selectOptions(screen.getAllByRole('combobox')[0], 'r-abh');
        await user.click(screen.getByRole('button', { name: 'Attach alias' }));
        await waitFor(() => expect(attachRagaAlias).toHaveBeenCalledWith('q-1', 'r-abh'));
        expect(onSuccess).toHaveBeenCalled();
    });

    it('ambiguous items offer a pick per candidate', async () => {
        vi.mocked(getUnresolvedRagas).mockResolvedValue({
            items: [QUEUE_AMBIGUOUS], total: 1, page: 0, size: 50,
        });
        vi.mocked(disambiguateRaga).mockResolvedValue({
            id: 'r-31', name: 'Kalāvathi', nameNormalized: 'kalavathi', createdAt: '', updatedAt: '',
        });
        const { user } = render(
            <UnresolvedRagasTab page={0} pageSize={50} onPageChange={vi.fn()} onError={vi.fn()} onSuccess={vi.fn()} />,
        );
        expect(await screen.findByText(/Disambiguate/)).toBeInTheDocument();
        const picks = screen.getAllByRole('button', { name: 'Pick' });
        expect(picks).toHaveLength(2);
        await user.click(picks[0]);
        await waitFor(() => expect(disambiguateRaga).toHaveBeenCalledWith('q-2', 'r-31'));
    });
});

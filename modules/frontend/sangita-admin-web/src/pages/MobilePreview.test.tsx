import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '../test/test-utils';
import MobilePreview from './MobilePreview';

vi.mock('../api/previewApi', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../api/previewApi')>()),
    getPreviewDiscovery: vi.fn(),
    searchPreviewKrithis: vi.fn(),
}));

import { getPreviewDiscovery, searchPreviewKrithis } from '../api/previewApi';

describe('MobilePreview shell', () => {
    beforeEach(() => {
        vi.mocked(getPreviewDiscovery).mockReset();
        localStorage.clear();
    });

    it('shows loading, then the featured krithi from the preview mount', async () => {
        vi.mocked(getPreviewDiscovery).mockResolvedValue({
            feature: {
                selection: 'EDITORIAL', heading: 'Today', summary: 'A featured krithi',
                krithi: {
                    id: 'k1', title: 'Aada Modi Galadae', composer: { id: 'c1', name: 'Tyagaraja' },
                    ragas: [], musicalForm: 'KRITHI', isRagamalika: false,
                },
            },
        });
        render(<MobilePreview />);
        expect(screen.getByRole('status')).toBeInTheDocument();
        expect(await screen.findByRole('heading', { name: 'Aada Modi Galadae' })).toBeInTheDocument();
    });

    it('shows Home without an error panel when the featured composition cannot load', async () => {
        vi.mocked(getPreviewDiscovery).mockRejectedValue(new Error('API Error 403'));
        render(<MobilePreview />);
        expect(await screen.findByText('Explore the tradition')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(screen.queryByText(/Could not reach/)).not.toBeInTheDocument();
    });

    it('shows an empty state when there is nothing to feature', async () => {
        vi.mocked(getPreviewDiscovery).mockResolvedValue({ feature: null });
        render(<MobilePreview />);
        expect(await screen.findByText('Explore the tradition')).toBeInTheDocument();
        expect(screen.queryByText(/could not be loaded/)).not.toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('switches tabs, goes back to Home, and keeps preferences in this browser only', async () => {
        vi.mocked(getPreviewDiscovery).mockResolvedValue({ feature: null });
        vi.mocked(searchPreviewKrithis).mockResolvedValue({ items: [], total: 0, page: 0, pageSize: 30 });
        const { user } = render(<MobilePreview />);
        await user.click(screen.getByRole('button', { name: /Explore/ }));
        expect(screen.getByRole('heading', { name: 'Explore', level: 2 })).toBeInTheDocument();
        await user.selectOptions(screen.getByLabelText('Appearance'), 'dark');
        await waitFor(() => expect(localStorage.getItem('mobilePreview.preferences')).toContain('"dark"'));
    });

    it('refetches what it shows when Refresh is pressed', async () => {
        vi.mocked(getPreviewDiscovery).mockResolvedValue({ feature: null });
        const { user } = render(<MobilePreview />);
        await screen.findByText('Explore the tradition');
        const before = vi.mocked(getPreviewDiscovery).mock.calls.length;
        await user.click(screen.getByRole('button', { name: 'Refresh preview' }));
        await waitFor(() => expect(vi.mocked(getPreviewDiscovery).mock.calls.length).toBeGreaterThan(before));
    });
});

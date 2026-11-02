import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '../../test/test-utils';
import IndexBanner from './IndexBanner';
import type { PreviewIndexStatus } from '../../types/mobilePreview.types';

const status = (over: Partial<PreviewIndexStatus>): PreviewIndexStatus => ({
    krithiId: 'k', verdict: 'CURRENT', workflowState: 'published', documentCount: 2, publishedFlagMatches: true,
    embeddedForActiveProfile: true, reasons: [], ...over,
});

describe('IndexBanner', () => {
    it('says the index may be out of date, with reasons, and never silently', () => {
        render(<IndexBanner onSearchThisKrithi={vi.fn()} status={status({ verdict: 'MAY_BE_STALE', reasons: ['The krithi changed after its search documents were written.'] })} />);
        expect(screen.getByRole('status')).toHaveTextContent('Index out of date for this krithi');
        expect(screen.getByText(/Titles & lyrics is current/)).toBeInTheDocument();
        expect(screen.getByText(/changed after its search documents/)).toBeInTheDocument();
    });

    it('says a krithi is not in the index at all', () => {
        render(<IndexBanner onSearchThisKrithi={vi.fn()} status={status({ verdict: 'NOT_INDEXED', documentCount: 0 })} />);
        expect(screen.getByRole('status')).toHaveTextContent('Not in the search index');
    });

    it('reports current and offers the search shortcut', async () => {
        const onSearch = vi.fn();
        const { user } = render(<IndexBanner onSearchThisKrithi={onSearch} status={status({})} />);
        expect(screen.getByRole('status')).toHaveTextContent('Search index looks current');
        await user.click(screen.getByRole('button', { name: 'Search for this krithi' }));
        expect(onSearch).toHaveBeenCalled();
    });
});

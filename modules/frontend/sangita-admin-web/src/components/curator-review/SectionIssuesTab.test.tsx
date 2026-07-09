import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '../../test/test-utils';
import { SectionIssuesTab } from './SectionIssuesTab';
import { type SectionIssuesPage } from '../../api/client';

const issue = (over: Partial<SectionIssuesPage['items'][number]> = {}) => ({
    krithiId: 'k-1',
    title: 'Vatapi Ganapatim',
    language: 'SANSKRIT',
    expectedSections: 3,
    actualSections: 1,
    issueType: 'count mismatch',
    ...over,
});

const pageOf = (items: SectionIssuesPage['items'], total = items.length): SectionIssuesPage => ({
    items, total, page: 0, size: 50,
});

describe('SectionIssuesTab', () => {
    it('shows the empty state when there are no issues', () => {
        render(<SectionIssuesTab data={pageOf([])} loading={false} page={0} onPageChange={() => {}} pageSize={50} />);
        expect(screen.getByText('No section issues found.')).toBeInTheDocument();
    });

    it('renders one row per issue with expected/actual counts and issue type', () => {
        render(
            <SectionIssuesTab
                data={pageOf([issue(), issue({ krithiId: 'k-2', title: 'Nagumomu', actualSections: 0, issueType: 'missing sections' })])}
                loading={false} page={0} onPageChange={() => {}} pageSize={50}
            />,
        );
        expect(screen.getByText('Vatapi Ganapatim')).toBeInTheDocument();
        expect(screen.getByText('Nagumomu')).toBeInTheDocument();
        expect(screen.getByText('count mismatch')).toBeInTheDocument();
        expect(screen.getByText('missing sections')).toBeInTheDocument();
    });

    it('hides pagination when everything fits on one page', () => {
        render(<SectionIssuesTab data={pageOf([issue()])} loading={false} page={0} onPageChange={() => {}} pageSize={50} />);
        expect(screen.queryByRole('button', { name: 'Next' })).not.toBeInTheDocument();
    });

    it('paginates: range text, Previous disabled on first page, Next advances', async () => {
        const onPageChange = vi.fn();
        const { user } = render(
            <SectionIssuesTab data={pageOf([issue()], 120)} loading={false} page={0} onPageChange={onPageChange} pageSize={50} />,
        );
        expect(screen.getByText('Showing 1-50 of 120')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
        await user.click(screen.getByRole('button', { name: 'Next' }));
        expect(onPageChange).toHaveBeenCalledWith(1);
    });

    it('disables Next on the last page', () => {
        render(<SectionIssuesTab data={pageOf([issue()], 120)} loading={false} page={2} onPageChange={() => {}} pageSize={50} />);
        expect(screen.getByText('Showing 101-120 of 120')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    });
});

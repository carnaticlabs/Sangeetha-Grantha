import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '../../test/test-utils';
import { TaskLogDrawer } from './TaskLogDrawer';
import { ImportTaskRun } from '../../types';

const task = (over: Partial<ImportTaskRun> = {}): ImportTaskRun => ({
    id: 'task-1',
    jobId: 'job-1',
    krithiKey: 'vatapi-ganapatim',
    status: 'SUCCEEDED',
    attempt: 2,
    sourceUrl: 'https://example.org/krithi/1',
    error: null,
    durationMs: 1500,
    createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-07-01T00:00:05Z',
    ...over,
});

describe('TaskLogDrawer', () => {
    it('shows the task overview: status label, attempt, duration, source link', () => {
        render(<TaskLogDrawer task={task()} onClose={() => {}} onCopied={() => {}} />);
        expect(screen.getByText('Composition Details')).toBeInTheDocument();
        expect(screen.getByText('Completed')).toBeInTheDocument();
        expect(screen.getByText('2')).toBeInTheDocument();
        expect(screen.getByText('1.5s')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'https://example.org/krithi/1' })).toBeInTheDocument();
        expect(screen.getByText('vatapi-ganapatim')).toBeInTheDocument();
    });

    it('renders the error panel only when the task has an error', () => {
        const { rerender } = render(<TaskLogDrawer task={task()} onClose={() => {}} onCopied={() => {}} />);
        expect(screen.queryByText('Error Details')).not.toBeInTheDocument();

        rerender(<TaskLogDrawer task={task({ status: 'FAILED', error: 'HTTP 404 from source' })} onClose={() => {}} onCopied={() => {}} />);
        expect(screen.getByText('Error Details')).toBeInTheDocument();
        expect(screen.getByText('HTTP 404 from source')).toBeInTheDocument();
    });

    it('copies the task JSON to the clipboard and notifies', async () => {
        const onCopied = vi.fn();
        const { user } = render(<TaskLogDrawer task={task()} onClose={() => {}} onCopied={onCopied} />);
        // userEvent.setup() (in the render wrapper) installs a clipboard stub — spy on it.
        const writeText = vi.spyOn(navigator.clipboard, 'writeText');

        await user.click(screen.getByRole('button', { name: 'Copy Details' }));

        expect(writeText).toHaveBeenCalledWith(JSON.stringify(task(), null, 2));
        expect(onCopied).toHaveBeenCalledOnce();
    });

    it('closes via the Close button', async () => {
        const onClose = vi.fn();
        const { user } = render(<TaskLogDrawer task={task()} onClose={onClose} onCopied={() => {}} />);
        await user.click(screen.getByRole('button', { name: 'Close' }));
        expect(onClose).toHaveBeenCalledOnce();
    });
});

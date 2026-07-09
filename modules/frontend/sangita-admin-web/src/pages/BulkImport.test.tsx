import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '../test/test-utils';
import BulkImportPage from './BulkImport';
import { ImportBatch, ImportEvent, ImportJob, ImportTaskRun } from '../types';

vi.mock('../api/client', () => ({
    // page data loads
    listBulkImportBatches: vi.fn(),
    getBulkImportBatch: vi.fn(),
    getBulkImportJobs: vi.fn(),
    getBulkImportTasks: vi.fn(),
    getBulkImportEvents: vi.fn(),
    uploadBulkImportFile: vi.fn(),
    // batch mutations consumed via useBatchActions
    pauseBulkImportBatch: vi.fn(),
    resumeBulkImportBatch: vi.fn(),
    cancelBulkImportBatch: vi.fn(),
    retryBulkImportBatch: vi.fn(),
    deleteBulkImportBatch: vi.fn(),
    approveAllInBulkImportBatch: vi.fn(),
    rejectAllInBulkImportBatch: vi.fn(),
    finalizeBulkImportBatch: vi.fn(),
    exportBulkImportReport: vi.fn(),
}));

import {
    approveAllInBulkImportBatch,
    getBulkImportBatch,
    getBulkImportEvents,
    getBulkImportJobs,
    getBulkImportTasks,
    listBulkImportBatches,
    uploadBulkImportFile,
} from '../api/client';

// SUCCEEDED status keeps the page's 5s live-polling interval switched off.
const BATCH: ImportBatch = {
    id: 'batch-1',
    sourceManifest: '/data/imports/dikshitar-batch.csv',
    status: 'SUCCEEDED',
    totalTasks: 4,
    processedTasks: 4,
    succeededTasks: 3,
    failedTasks: 1,
    blockedTasks: 0,
    startedAt: '2026-07-01T10:00:00Z',
    completedAt: '2026-07-01T10:05:00Z',
    createdAt: '2026-07-01T09:59:00Z',
    updatedAt: '2026-07-01T10:05:00Z',
};

const JOBS: ImportJob[] = [
    { id: 'job-1', batchId: 'batch-1', jobType: 'MANIFEST_INGEST', status: 'SUCCEEDED', retryCount: 0, createdAt: '2026-07-01T10:00:00Z' } as ImportJob,
    { id: 'job-2', batchId: 'batch-1', jobType: 'SCRAPE', status: 'SUCCEEDED', retryCount: 0, createdAt: '2026-07-01T10:01:00Z' } as ImportJob,
];

const TASKS: ImportTaskRun[] = [
    { id: 'task-1', jobId: 'job-2', krithiKey: 'vatapi-ganapatim', status: 'SUCCEEDED', attempt: 1, sourceUrl: 'https://example.org/krithi/1', durationMs: 1500, createdAt: '2026-07-01T10:01:00Z', updatedAt: '2026-07-01T10:01:02Z' },
    { id: 'task-2', jobId: 'job-2', krithiKey: 'nagumomu', status: 'FAILED', attempt: 3, sourceUrl: 'https://example.org/krithi/2', error: '{"message":"HTTP 404"}', durationMs: 800, createdAt: '2026-07-01T10:02:00Z', updatedAt: '2026-07-01T10:02:01Z' },
];

const EVENTS: ImportEvent[] = [
    { id: 'ev-1', refType: 'BATCH', refId: 'batch-1', eventType: 'BATCH_CREATED', data: null, createdAt: '2026-07-01T09:59:00Z' },
    { id: 'ev-2', refType: 'BATCH', refId: 'batch-1', eventType: 'BATCH_COMPLETED', data: '{"succeeded":3}', createdAt: '2026-07-01T10:05:00Z' },
];

beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listBulkImportBatches).mockResolvedValue([BATCH]);
    vi.mocked(getBulkImportBatch).mockResolvedValue(BATCH);
    vi.mocked(getBulkImportJobs).mockResolvedValue(JOBS);
    vi.mocked(getBulkImportTasks).mockResolvedValue(TASKS);
    vi.mocked(getBulkImportEvents).mockResolvedValue(EVENTS);
});

describe('BulkImportPage — list and detail', () => {
    it('renders the batch list, auto-selects the first batch, and loads its detail', async () => {
        render(<BulkImportPage />);

        expect(await screen.findByText('dikshitar-batch.csv')).toBeInTheDocument();
        // Detail panel loads for the auto-selected batch
        expect(await screen.findByText('Batch Detail')).toBeInTheDocument();
        expect(getBulkImportBatch).toHaveBeenCalledWith('batch-1');
        // Progress breakdown from the loaded batch + tasks
        expect(screen.getByText('Completed: 3')).toBeInTheDocument();
        expect(screen.getByText('Failed: 1')).toBeInTheDocument();
        // Humanized event timeline
        expect(screen.getByText('Import created')).toBeInTheDocument();
        expect(screen.getByText('Import completed')).toBeInTheDocument();
    });

    it('shows the empty state when there are no batches', async () => {
        vi.mocked(listBulkImportBatches).mockResolvedValue([]);
        render(<BulkImportPage />);
        expect(await screen.findByText('No batches found.')).toBeInTheDocument();
    });

    it('refetches tasks with the chosen status when the task filter changes', async () => {
        const { user } = render(<BulkImportPage />);
        await screen.findByText('Batch Detail');

        await user.selectOptions(screen.getByRole('combobox'), 'FAILED');

        await waitFor(() =>
            expect(getBulkImportTasks).toHaveBeenCalledWith('batch-1', 'FAILED', 200),
        );
    });

    it('opens the task drawer when a task row is clicked', async () => {
        const { user } = render(<BulkImportPage />);
        await screen.findByText('Batch Detail');

        await user.click(screen.getByText('https://example.org/krithi/2'));

        expect(await screen.findByText('Composition Details')).toBeInTheDocument();
        expect(screen.getByText('Error Details')).toBeInTheDocument();
        expect(screen.getByText('HTTP 404')).toBeInTheDocument();
    });
});

describe('BulkImportPage — upload', () => {
    it('uploads the selected CSV, toasts, and selects the created batch', async () => {
        const created = { ...BATCH, id: 'batch-new', sourceManifest: '/data/new.csv' };
        vi.mocked(uploadBulkImportFile).mockResolvedValue(created);
        const { user } = render(<BulkImportPage />);
        await screen.findByText('Batch Detail');

        const file = new File(['title,raga,url'], 'manifest.csv', { type: 'text/csv' });
        const uploadButton = screen.getByRole('button', { name: 'Upload & Process' });
        expect(uploadButton).toBeDisabled();

        await user.upload(screen.getByLabelText('Upload Composition List'), file);
        expect(uploadButton).toBeEnabled();
        await user.click(uploadButton);

        await waitFor(() => expect(uploadBulkImportFile).toHaveBeenCalledWith(file));
        expect(await screen.findByText('Batch created from upload')).toBeInTheDocument();
        await waitFor(() => expect(getBulkImportBatch).toHaveBeenCalledWith('batch-new'));
    });

    it('surfaces an error toast when the upload fails', async () => {
        vi.mocked(uploadBulkImportFile).mockRejectedValue(new Error('manifest missing columns'));
        const { user } = render(<BulkImportPage />);
        await screen.findByText('Batch Detail');

        await user.upload(
            screen.getByLabelText('Upload Composition List'),
            new File(['x'], 'bad.csv', { type: 'text/csv' }),
        );
        await user.click(screen.getByRole('button', { name: 'Upload & Process' }));

        expect(await screen.findByText('Failed to create batch: manifest missing columns')).toBeInTheDocument();
    });
});

describe('BulkImportPage — batch actions', () => {
    it('Approve All dispatches the mutation and refreshes the list', async () => {
        vi.stubGlobal('confirm', vi.fn(() => true));
        vi.mocked(approveAllInBulkImportBatch).mockResolvedValue(undefined as never);
        const { user } = render(<BulkImportPage />);
        await screen.findByText('Batch Detail');
        const listCallsBefore = vi.mocked(listBulkImportBatches).mock.calls.length;

        await user.click(screen.getByRole('button', { name: 'Approve All' }));

        await waitFor(() => expect(approveAllInBulkImportBatch).toHaveBeenCalledWith('batch-1'));
        await waitFor(() =>
            expect(vi.mocked(listBulkImportBatches).mock.calls.length).toBeGreaterThan(listCallsBefore),
        );
        vi.unstubAllGlobals();
    });
});

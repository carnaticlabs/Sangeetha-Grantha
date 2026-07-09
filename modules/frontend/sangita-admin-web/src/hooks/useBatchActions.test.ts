import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useBatchActions } from './useBatchActions';

const toast = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }));

vi.mock('../components/Toast', () => ({
    useToast: () => toast,
}));

vi.mock('../api/client', () => ({
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
    pauseBulkImportBatch,
    retryBulkImportBatch,
    deleteBulkImportBatch,
    finalizeBulkImportBatch,
    exportBulkImportReport,
} from '../api/client';

const BATCH_ID = 'batch-123';

describe('useBatchActions', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        vi.unstubAllGlobals();
    });

    it('pause: calls the client, toasts success, refreshes, and clears loading', async () => {
        vi.mocked(pauseBulkImportBatch).mockResolvedValue(undefined as never);
        const onRefresh = vi.fn();
        const { result } = renderHook(() => useBatchActions(onRefresh));

        await act(() => result.current.triggerAction('pause', BATCH_ID));

        expect(pauseBulkImportBatch).toHaveBeenCalledWith(BATCH_ID);
        expect(toast.success).toHaveBeenCalledWith('Batch paused');
        expect(onRefresh).toHaveBeenCalledOnce();
        expect(result.current.actionLoading).toBeNull();
    });

    it('exposes the acting batch id in actionLoading while the request is in flight', async () => {
        let resolve!: () => void;
        vi.mocked(pauseBulkImportBatch).mockReturnValue(
            new Promise<void>((r) => { resolve = r; }) as never,
        );
        const { result } = renderHook(() => useBatchActions());

        let pending!: Promise<void>;
        act(() => { pending = result.current.triggerAction('pause', BATCH_ID); });
        expect(result.current.actionLoading).toBe(BATCH_ID);

        resolve();
        await act(() => pending);
        expect(result.current.actionLoading).toBeNull();
    });

    it('retry: reports the number of requeued tasks', async () => {
        vi.mocked(retryBulkImportBatch).mockResolvedValue({ requeuedTasks: 7 } as never);
        const { result } = renderHook(() => useBatchActions());

        await act(() => result.current.triggerAction('retry', BATCH_ID));

        expect(toast.success).toHaveBeenCalledWith('Retrying batch (7 tasks requeued)');
    });

    it('delete: does nothing when the user declines the confirm dialog', async () => {
        vi.stubGlobal('confirm', vi.fn(() => false));
        const onRefresh = vi.fn();
        const { result } = renderHook(() => useBatchActions(onRefresh));

        await act(() => result.current.triggerAction('delete', BATCH_ID));

        expect(deleteBulkImportBatch).not.toHaveBeenCalled();
        expect(onRefresh).not.toHaveBeenCalled();
        expect(result.current.actionLoading).toBeNull();
    });

    it('delete: proceeds when the user confirms', async () => {
        vi.stubGlobal('confirm', vi.fn(() => true));
        vi.mocked(deleteBulkImportBatch).mockResolvedValue(undefined as never);
        const { result } = renderHook(() => useBatchActions());

        await act(() => result.current.triggerAction('delete', BATCH_ID));

        expect(deleteBulkImportBatch).toHaveBeenCalledWith(BATCH_ID);
        expect(toast.success).toHaveBeenCalledWith('Batch deleted');
    });

    it('finalize: toasts error (not success) when the batch cannot be finalized yet', async () => {
        vi.mocked(finalizeBulkImportBatch).mockResolvedValue(
            { canFinalize: false, message: '3 items still pending' } as never,
        );
        const { result } = renderHook(() => useBatchActions());

        await act(() => result.current.triggerAction('finalize', BATCH_ID));

        expect(toast.error).toHaveBeenCalledWith('3 items still pending');
        expect(toast.success).not.toHaveBeenCalled();
    });

    it('export: downloads the report blob and toasts success', async () => {
        vi.mocked(exportBulkImportReport).mockResolvedValue(new Blob(['{}']) as never);
        const createObjectURL = vi.fn(() => 'blob:fake');
        const revokeObjectURL = vi.fn();
        vi.stubGlobal('URL', Object.assign(Object.create(URL), { createObjectURL, revokeObjectURL }));
        // The hook clicks a synthetic <a download> — jsdom would treat the real
        // click as navigation (unimplemented), so intercept it.
        const anchorClick = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
        const { result } = renderHook(() => useBatchActions());

        await act(() => result.current.triggerAction('export', BATCH_ID));

        expect(exportBulkImportReport).toHaveBeenCalledWith(BATCH_ID, 'json');
        expect(createObjectURL).toHaveBeenCalledOnce();
        expect(anchorClick).toHaveBeenCalledOnce();
        expect(revokeObjectURL).toHaveBeenCalledWith('blob:fake');
        expect(toast.success).toHaveBeenCalledWith('Report exported');
        anchorClick.mockRestore();
    });

    it('failure: toasts the error message, skips refresh, and clears loading', async () => {
        vi.mocked(pauseBulkImportBatch).mockRejectedValue(new Error('API Error 500: nope'));
        vi.spyOn(console, 'error').mockImplementation(() => {});
        const onRefresh = vi.fn();
        const { result } = renderHook(() => useBatchActions(onRefresh));

        await act(() => result.current.triggerAction('pause', BATCH_ID));

        await waitFor(() =>
            expect(toast.error).toHaveBeenCalledWith('Action failed: API Error 500: nope'),
        );
        expect(onRefresh).not.toHaveBeenCalled();
        expect(result.current.actionLoading).toBeNull();
    });
});

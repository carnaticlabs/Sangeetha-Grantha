import { useState, useCallback } from 'react';
import {
    pauseBulkImportBatch,
    resumeBulkImportBatch,
    cancelBulkImportBatch,
    retryBulkImportBatch,
    deleteBulkImportBatch,
    approveAllInBulkImportBatch,
    rejectAllInBulkImportBatch,
    finalizeBulkImportBatch,
    exportBulkImportReport
} from '../api/client';
import { useToast } from '../components/Toast';

export const useBatchActions = (onRefresh?: () => void) => {
    const { success, error } = useToast();
    const toast = { success, error };
    const [actionLoading, setActionLoading] = useState<string | null>(null);

    const triggerAction = useCallback(async (action: 'pause' | 'resume' | 'cancel' | 'retry' | 'delete' | 'approveAll' | 'rejectAll' | 'finalize' | 'export', batchId: string) => {
        setActionLoading(batchId);
        try {
            switch (action) {
                case 'pause':
                    await pauseBulkImportBatch(batchId);
                    toast.success('Batch paused');
                    break;
                case 'resume':
                    await resumeBulkImportBatch(batchId);
                    toast.success('Batch resumed');
                    break;
                case 'cancel':
                    await cancelBulkImportBatch(batchId);
                    toast.success('Batch cancelled');
                    break;
                case 'retry':
                    const result = await retryBulkImportBatch(batchId);
                    toast.success(`Retrying batch (${result.requeuedTasks} tasks requeued)`);
                    break;
                case 'delete':
                    if (confirm('Are you sure you want to delete this batch history? This cannot be undone.')) {
                        await deleteBulkImportBatch(batchId);
                        toast.success('Batch deleted');
                        // Special case: delete implies we might need to clear selection or navigate
                        // handled by caller if needed
                    } else {
                        setActionLoading(null);
                        return; // User cancelled
                    }
                    break;
                case 'approveAll':
                    if (confirm('Are you sure you want to approve ALL pending items?')) {
                        await approveAllInBulkImportBatch(batchId);
                        toast.success('All pending items approved');
                    } else {
                        setActionLoading(null);
                        return;
                    }
                    break;
                case 'rejectAll':
                    if (confirm('Are you sure you want to reject ALL pending items?')) {
                        await rejectAllInBulkImportBatch(batchId);
                        toast.success('All pending items rejected');
                    } else {
                        setActionLoading(null);
                        return;
                    }
                    break;
                case 'finalize':
                    const finalizeRes = await finalizeBulkImportBatch(batchId);
                    if (finalizeRes.canFinalize) {
                        toast.success(finalizeRes.message || 'Batch finalized');
                    } else {
                        toast.error(finalizeRes.message || 'Cannot finalize batch yet');
                    }
                    break;
                case 'export':
                    try {
                        const blob = await exportBulkImportReport(batchId, 'json');
                        const url = window.URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = `qa-report-${batchId}.json`;
                        document.body.appendChild(a);
                        a.click();
                        window.URL.revokeObjectURL(url);
                        document.body.removeChild(a);
                        toast.success('Report exported');
                    } catch (e) {
                        throw e;
                    }
                    break;
            }
            if (onRefresh) onRefresh();
        } catch (e: any) {
            console.error(`Action ${action} failed:`, e);
            toast.error(`Action failed: ${e.message || 'Unknown error'}`);
        } finally {
            setActionLoading(null);
        }
    }, [onRefresh, success, error]);

    return {
        actionLoading,
        triggerAction
    };
};

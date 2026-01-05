import React, { useState, useEffect } from 'react';
import { ImportedKrithi, ImportReviewRequest } from '../types';

interface ReviewImportModalProps {
    isOpen: boolean;
    onClose: () => void;
    importItem: ImportedKrithi | null;
    onReview: (id: string, request: ImportReviewRequest) => Promise<void>;
}

export const ReviewImportModal: React.FC<ReviewImportModalProps> = ({
    isOpen,
    onClose,
    importItem,
    onReview
}) => {
    const [status, setStatus] = useState<'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'MAPPED' | 'REJECTED' | 'DISCARDED'>('IN_REVIEW');
    const [mappedKrithiId, setMappedKrithiId] = useState<string>('');
    const [reviewerNotes, setReviewerNotes] = useState<string>('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    // Reset form when modal opens/closes
    useEffect(() => {
        if (isOpen && importItem) {
            setStatus('IN_REVIEW');
            setMappedKrithiId('');
            setReviewerNotes('');
            setIsSubmitting(false);
        }
    }, [isOpen, importItem]);

    if (!isOpen || !importItem) return null;

    const handleSubmit = async (e?: React.FormEvent) => {
        e?.preventDefault();
        if (!importItem) return;

        setIsSubmitting(true);
        try {
            await onReview(importItem.id, {
                status,
                mappedKrithiId: mappedKrithiId.trim() || null,
                reviewerNotes: reviewerNotes.trim() || null,
            });
            // Reset form
            setStatus('IN_REVIEW');
            setMappedKrithiId('');
            setReviewerNotes('');
            onClose();
        } catch (error) {
            console.error('Review failed:', error);
            // Error is already handled by the parent component's toast
            throw error; // Re-throw so parent can handle it
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleClose = () => {
        if (!isSubmitting) {
            setStatus('IN_REVIEW');
            setMappedKrithiId('');
            setReviewerNotes('');
            onClose();
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
            <div className="bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden animate-fadeIn">
                {/* Header */}
                <div className="p-6 border-b border-border-light flex justify-between items-center bg-slate-50">
                    <div className="flex items-center gap-3">
                        <span className="material-symbols-outlined text-primary">rate_review</span>
                        <div>
                            <h2 className="text-xl font-bold text-ink-900">Review Import</h2>
                            <p className="text-xs text-ink-500">Review and process imported krithi</p>
                        </div>
                    </div>
                    <button
                        onClick={handleClose}
                        disabled={isSubmitting}
                        className="text-ink-400 hover:text-ink-600 transition-colors disabled:opacity-50"
                    >
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6 space-y-6">
                    {/* Import Details */}
                    <div className="bg-slate-50 rounded-lg p-4 space-y-3 border border-border-light">
                        <h3 className="text-sm font-bold text-ink-900 uppercase tracking-wide">Import Details</h3>
                        <div className="grid grid-cols-2 gap-4 text-sm">
                            <div>
                                <span className="text-ink-500">Title:</span>
                                <p className="font-medium text-ink-900 mt-1">{importItem.rawTitle || '-'}</p>
                            </div>
                            <div>
                                <span className="text-ink-500">Composer:</span>
                                <p className="font-medium text-ink-900 mt-1">{importItem.rawComposer || '-'}</p>
                            </div>
                            <div>
                                <span className="text-ink-500">Raga:</span>
                                <p className="font-medium text-ink-900 mt-1">{importItem.rawRaga || '-'}</p>
                            </div>
                            <div>
                                <span className="text-ink-500">Tala:</span>
                                <p className="font-medium text-ink-900 mt-1">{importItem.rawTala || '-'}</p>
                            </div>
                            {importItem.rawDeity && (
                                <div>
                                    <span className="text-ink-500">Deity:</span>
                                    <p className="font-medium text-ink-900 mt-1">{importItem.rawDeity}</p>
                                </div>
                            )}
                            {importItem.rawTemple && (
                                <div>
                                    <span className="text-ink-500">Temple:</span>
                                    <p className="font-medium text-ink-900 mt-1">{importItem.rawTemple}</p>
                                </div>
                            )}
                        </div>
                        {importItem.rawLyrics && (
                            <div className="mt-3 pt-3 border-t border-border-light">
                                <span className="text-ink-500 text-sm">Lyrics:</span>
                                <p className="text-sm text-ink-700 mt-2 whitespace-pre-wrap max-h-32 overflow-y-auto bg-white p-2 rounded border border-border-light">
                                    {importItem.rawLyrics}
                                </p>
                            </div>
                        )}
                        {importItem.sourceKey && (
                            <div className="mt-2">
                                <span className="text-ink-500 text-xs">Source Key:</span>
                                <p className="text-xs text-ink-400 font-mono mt-1 truncate">{importItem.sourceKey}</p>
                            </div>
                        )}
                    </div>

                    {/* Review Form */}
                    <form onSubmit={handleSubmit} id="review-form" className="space-y-4">
                        <div>
                            <label className="block text-sm font-bold text-ink-900 mb-2">
                                Review Status <span className="text-red-500">*</span>
                            </label>
                            <select
                                value={status}
                                onChange={(e) => setStatus(e.target.value as typeof status)}
                                required
                                className="w-full px-4 py-2 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                            >
                                <option value="PENDING">Pending</option>
                                <option value="IN_REVIEW">In Review</option>
                                <option value="APPROVED">Approved - Create New Krithi</option>
                                <option value="MAPPED">Mapped to Existing Krithi</option>
                                <option value="REJECTED">Rejected</option>
                                <option value="DISCARDED">Discarded</option>
                            </select>
                            <p className="text-xs text-ink-500 mt-1">
                                {status === 'APPROVED' && 'Approve this import to automatically create a new krithi. The krithi will be created in DRAFT status and can be edited in the Krithi Editor.'}
                                {status === 'MAPPED' && 'Map this import to an existing krithi in the system'}
                                {status === 'REJECTED' && 'Reject this import (e.g., incorrect data, duplicate)'}
                                {status === 'DISCARDED' && 'Discard this import (e.g., low quality, not relevant)'}
                            </p>
                        </div>

                        {status === 'MAPPED' && (
                            <div>
                                <label className="block text-sm font-bold text-ink-900 mb-2">
                                    Mapped Krithi ID
                                </label>
                                <input
                                    type="text"
                                    value={mappedKrithiId}
                                    onChange={(e) => setMappedKrithiId(e.target.value)}
                                    placeholder="Enter UUID of existing krithi"
                                    className="w-full px-4 py-2 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm font-mono"
                                />
                                <p className="text-xs text-ink-500 mt-1">
                                    Enter the UUID of the krithi this import should be mapped to
                                </p>
                            </div>
                        )}

                        <div>
                            <label className="block text-sm font-bold text-ink-900 mb-2">
                                Reviewer Notes
                            </label>
                            <textarea
                                value={reviewerNotes}
                                onChange={(e) => setReviewerNotes(e.target.value)}
                                placeholder="Add any notes about this review..."
                                rows={4}
                                className="w-full px-4 py-2 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm resize-none"
                            />
                            <p className="text-xs text-ink-500 mt-1">
                                Optional notes about the review decision
                            </p>
                        </div>
                    </form>
                </div>

                {/* Footer */}
                <div className="p-6 border-t border-border-light flex justify-end gap-3 bg-slate-50">
                    <button
                        type="button"
                        onClick={handleClose}
                        disabled={isSubmitting}
                        className="px-4 py-2 text-sm font-medium text-ink-600 hover:text-ink-900 transition-colors disabled:opacity-50"
                    >
                        Cancel
                    </button>
                    <button
                        type="submit"
                        form="review-form"
                        disabled={isSubmitting}
                        className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark shadow-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {isSubmitting ? (
                            <>
                                <span className="material-symbols-outlined text-[18px] animate-spin">autorenew</span>
                                Submitting...
                            </>
                        ) : (
                            <>
                                <span className="material-symbols-outlined text-[18px]">check</span>
                                Submit Review
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};


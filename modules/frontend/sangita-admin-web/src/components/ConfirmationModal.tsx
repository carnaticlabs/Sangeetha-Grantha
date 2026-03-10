import React, { useState, useEffect, useRef } from 'react';

interface ConfirmationModalProps {
    isOpen: boolean;
    title: string;
    message: string;
    confirmLabel?: string;
    confirmColor?: 'green' | 'red' | 'blue';
    showNotes?: boolean;
    notesRequired?: boolean;
    notesPlaceholder?: string;
    onConfirm: (notes?: string) => void;
    onCancel: () => void;
}

const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
    isOpen,
    title,
    message,
    confirmLabel = 'Confirm',
    confirmColor = 'blue',
    showNotes = false,
    notesRequired = false,
    notesPlaceholder = 'Add a note (optional)...',
    onConfirm,
    onCancel,
}) => {
    const [notes, setNotes] = useState('');
    const overlayRef = useRef<HTMLDivElement>(null);
    const confirmRef = useRef<HTMLButtonElement>(null);

    useEffect(() => {
        if (isOpen) {
            setNotes('');
            // Focus confirm button on open
            setTimeout(() => confirmRef.current?.focus(), 100);
        }
    }, [isOpen]);

    useEffect(() => {
        if (!isOpen) return;
        const handler = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onCancel();
        };
        window.addEventListener('keydown', handler);
        return () => window.removeEventListener('keydown', handler);
    }, [isOpen, onCancel]);

    if (!isOpen) return null;

    const colorMap = {
        green: 'bg-green-600 hover:bg-green-700 focus:ring-green-500',
        red: 'bg-red-600 hover:bg-red-700 focus:ring-red-500',
        blue: 'bg-primary hover:bg-primary-dark focus:ring-primary',
    };

    const canConfirm = !notesRequired || notes.trim().length > 0;

    return (
        <div
            ref={overlayRef}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm"
            onClick={(e) => { if (e.target === overlayRef.current) onCancel(); }}
        >
            <div className="bg-white rounded-xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
                <div className="p-6 space-y-4">
                    <h3 className="text-lg font-display font-bold text-ink-900">{title}</h3>
                    <p className="text-sm text-ink-600">{message}</p>

                    {showNotes && (
                        <div>
                            <label className="block text-xs font-semibold text-ink-500 mb-1">
                                Notes {notesRequired && <span className="text-red-500">*</span>}
                            </label>
                            <textarea
                                value={notes}
                                onChange={(e) => setNotes(e.target.value)}
                                placeholder={notesPlaceholder}
                                rows={3}
                                className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent resize-none"
                            />
                        </div>
                    )}
                </div>

                <div className="px-6 py-4 bg-slate-50 border-t border-border-light flex justify-end gap-3">
                    <button
                        onClick={onCancel}
                        className="px-4 py-2 text-sm font-medium text-ink-600 bg-white border border-border-light rounded-lg hover:bg-slate-50"
                    >
                        Cancel
                    </button>
                    <button
                        ref={confirmRef}
                        onClick={() => onConfirm(showNotes ? notes : undefined)}
                        disabled={!canConfirm}
                        className={`px-4 py-2 text-sm font-semibold text-white rounded-lg focus:ring-2 focus:ring-offset-2 disabled:opacity-50 ${colorMap[confirmColor]}`}
                    >
                        {confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmationModal;

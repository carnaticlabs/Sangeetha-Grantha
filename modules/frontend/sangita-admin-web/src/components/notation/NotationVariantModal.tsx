import React, { useState, useEffect } from 'react';
import { NotationVariant, NotationType } from '../../types';

interface NotationVariantModalProps {
    krithiId: string;
    existingVariant?: NotationVariant;
    onClose: () => void;
    onSave: (payload: Partial<NotationVariant>) => void;
}

const NotationVariantModal: React.FC<NotationVariantModalProps> = ({
    krithiId,
    existingVariant,
    onClose,
    onSave
}) => {
    const [formData, setFormData] = useState<Partial<NotationVariant>>({
        notationType: NotationType.SWARA,
        kalai: 1,
        variantLabel: '',
        sourceReference: '',
        isPrimary: false,
    });

    useEffect(() => {
        if (existingVariant) {
            setFormData(existingVariant);
        }
    }, [existingVariant]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSave(formData);
    };

    return (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-xl shadow-xl w-full max-w-md overflow-hidden animate-fadeIn">
                <div className="p-4 border-b border-border-light flex justify-between items-center bg-slate-50">
                    <h3 className="font-display text-lg font-bold text-ink-900">
                        {existingVariant ? 'Edit Variant' : 'New Variant'}
                    </h3>
                    <button onClick={onClose} className="text-ink-400 hover:text-ink-900">
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    <div>
                        <label className="block text-sm font-semibold text-ink-900 mb-1">Label</label>
                        <input
                            type="text"
                            required
                            value={formData.variantLabel || ''}
                            onChange={(e) => setFormData({ ...formData, variantLabel: e.target.value })}
                            className="w-full px-3 py-2 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                            placeholder="e.g., Standard Pathanthara"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-semibold text-ink-900 mb-1">Type</label>
                            <select
                                value={formData.notationType}
                                onChange={(e) => setFormData({ ...formData, notationType: e.target.value as NotationType })}
                                className="w-full px-3 py-2 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                            >
                                <option value={NotationType.SWARA}>Swara</option>
                                <option value={NotationType.JATHI}>Jathi</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-semibold text-ink-900 mb-1">Kalai</label>
                            <input
                                type="number"
                                min={1}
                                max={8}
                                value={formData.kalai}
                                onChange={(e) => setFormData({ ...formData, kalai: parseInt(e.target.value) })}
                                className="w-full px-3 py-2 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-semibold text-ink-900 mb-1">Source Reference</label>
                        <input
                            type="text"
                            value={formData.sourceReference || ''}
                            onChange={(e) => setFormData({ ...formData, sourceReference: e.target.value })}
                            className="w-full px-3 py-2 border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                            placeholder="e.g., SSP Vol 2, Page 45"
                        />
                    </div>

                    <div className="flex items-center gap-2 pt-2">
                        <input
                            type="checkbox"
                            id="isPrimaryModal"
                            checked={formData.isPrimary}
                            onChange={(e) => setFormData({ ...formData, isPrimary: e.target.checked })}
                            className="rounded border-gray-300 text-primary focus:ring-primary"
                        />
                        <label htmlFor="isPrimaryModal" className="text-sm text-ink-700 font-medium">Mark as Primary</label>
                    </div>

                    <div className="flex items-center gap-3 pt-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 px-4 py-2 border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            className="flex-1 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors"
                        >
                            Save
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default NotationVariantModal;

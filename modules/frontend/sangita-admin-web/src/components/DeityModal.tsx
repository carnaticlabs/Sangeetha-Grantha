import React, { useState, useEffect } from 'react';
import { Deity } from '../types';
import { createDeity, updateDeity } from '../api/client';
import { ToastService } from './Toast';

interface DeityModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: (deity: Deity) => void;
    existingDeity?: Deity | null;
    toast: ToastService;
}

export const DeityModal: React.FC<DeityModalProps> = ({
    isOpen,
    onClose,
    onSave,
    existingDeity,
    toast
}) => {
    const [formData, setFormData] = useState({
        name: '',
        nameNormalized: '',
        description: ''
    });
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (isOpen) {
            if (existingDeity) {
                setFormData({
                    name: existingDeity.name || '',
                    nameNormalized: existingDeity.nameNormalized || '',
                    description: existingDeity.description || ''
                });
            } else {
                setFormData({
                    name: '',
                    nameNormalized: '',
                    description: ''
                });
            }
        }
    }, [isOpen, existingDeity]);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);

        try {
            const payload = {
                name: formData.name.trim(),
                nameNormalized: formData.nameNormalized.trim() || undefined,
                description: formData.description.trim() || undefined
            };

            let savedDeity: Deity;
            if (existingDeity) {
                savedDeity = await updateDeity(existingDeity.id, payload);
                toast.success('Deity updated successfully');
            } else {
                savedDeity = await createDeity(payload);
                toast.success('Deity created successfully');
            }

            onSave(savedDeity);
            onClose();
        } catch (error: any) {
            toast.error(error.message || 'Failed to save deity');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
            <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden animate-fadeIn">
                <div className="p-6 border-b border-border-light flex justify-between items-center bg-slate-50">
                    <div className="flex items-center gap-3">
                        <span className="material-symbols-outlined text-primary">temple_buddhist</span>
                        <div>
                            <h2 className="text-xl font-bold text-ink-900">
                                {existingDeity ? 'Edit Deity' : 'Create Deity'}
                            </h2>
                            <p className="text-xs text-ink-500">Manage deity information</p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="text-ink-400 hover:text-ink-600 transition-colors"
                    >
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
                    <div className="space-y-2">
                        <label className="block text-sm font-semibold text-ink-900">
                            Name <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            value={formData.name}
                            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                            required
                            className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                            placeholder="e.g., Venkateswara"
                        />
                    </div>

                    <div className="space-y-2">
                        <label className="block text-sm font-semibold text-ink-900">
                            Normalized Name
                        </label>
                        <input
                            type="text"
                            value={formData.nameNormalized}
                            onChange={(e) => setFormData({ ...formData, nameNormalized: e.target.value })}
                            className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                            placeholder="e.g., venkateswara (IAST format)"
                        />
                        <p className="text-xs text-ink-500">Leave empty to auto-generate from name</p>
                    </div>

                    <div className="space-y-2">
                        <label className="block text-sm font-semibold text-ink-900">
                            Description
                        </label>
                        <textarea
                            value={formData.description}
                            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                            rows={4}
                            className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent resize-none"
                            placeholder="Optional description or notes about the deity..."
                        />
                    </div>

                    <div className="pt-6 border-t border-border-light flex flex-col-reverse sm:flex-row items-center justify-end gap-4">
                        <button
                            type="button"
                            onClick={onClose}
                            className="w-full sm:w-auto px-6 py-2.5 rounded-lg border border-border-light text-ink-700 bg-white hover:bg-slate-50 text-sm font-medium transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={saving || !formData.name.trim()}
                            className="w-full sm:w-auto px-6 py-2.5 rounded-lg bg-primary hover:bg-primary-dark text-white text-sm font-medium shadow-md shadow-blue-500/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            <span className="material-symbols-outlined text-lg">save</span>
                            {saving ? 'Saving...' : existingDeity ? 'Update Deity' : 'Create Deity'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

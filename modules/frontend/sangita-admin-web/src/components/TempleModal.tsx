import React, { useState, useEffect } from 'react';
import { Temple, Deity } from '../types';
import { createTemple, updateTemple, getDeities } from '../api/client';
import { ToastService } from './Toast';

interface TempleModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSave: (temple: Temple) => void;
    existingTemple?: Temple | null;
    toast: ToastService;
}

export const TempleModal: React.FC<TempleModalProps> = ({
    isOpen,
    onClose,
    onSave,
    existingTemple,
    toast
}) => {
    const [formData, setFormData] = useState({
        name: '',
        nameNormalized: '',
        city: '',
        state: '',
        country: '',
        primaryDeityId: '',
        latitude: '',
        longitude: '',
        notes: ''
    });
    const [deities, setDeities] = useState<Deity[]>([]);
    const [saving, setSaving] = useState(false);
    const [loadingDeities, setLoadingDeities] = useState(false);

    useEffect(() => {
        if (isOpen) {
            loadDeities();
            if (existingTemple) {
                setFormData({
                    name: existingTemple.name || '',
                    nameNormalized: existingTemple.nameNormalized || '',
                    city: existingTemple.city || '',
                    state: existingTemple.state || '',
                    country: existingTemple.country || '',
                    primaryDeityId: existingTemple.primaryDeityId || '',
                    latitude: existingTemple.latitude?.toString() || '',
                    longitude: existingTemple.longitude?.toString() || '',
                    notes: existingTemple.notes || ''
                });
            } else {
                setFormData({
                    name: '',
                    nameNormalized: '',
                    city: '',
                    state: '',
                    country: '',
                    primaryDeityId: '',
                    latitude: '',
                    longitude: '',
                    notes: ''
                });
            }
        }
    }, [isOpen, existingTemple]);

    const loadDeities = async () => {
        setLoadingDeities(true);
        try {
            const data = await getDeities();
            setDeities(data);
        } catch (error) {
            console.error('Failed to load deities:', error);
        } finally {
            setLoadingDeities(false);
        }
    };

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);

        try {
            const payload = {
                name: formData.name.trim(),
                nameNormalized: formData.nameNormalized.trim() || undefined,
                city: formData.city.trim() || undefined,
                state: formData.state.trim() || undefined,
                country: formData.country.trim() || undefined,
                primaryDeityId: formData.primaryDeityId || undefined,
                latitude: formData.latitude ? parseFloat(formData.latitude) : undefined,
                longitude: formData.longitude ? parseFloat(formData.longitude) : undefined,
                notes: formData.notes.trim() || undefined
            };

            let savedTemple: Temple;
            if (existingTemple) {
                savedTemple = await updateTemple(existingTemple.id, payload);
                toast.success('Temple updated successfully');
            } else {
                savedTemple = await createTemple(payload);
                toast.success('Temple created successfully');
            }

            onSave(savedTemple);
            onClose();
        } catch (error: any) {
            toast.error(error.message || 'Failed to save temple');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
            <div className="bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden animate-fadeIn">
                <div className="p-6 border-b border-border-light flex justify-between items-center bg-slate-50">
                    <div className="flex items-center gap-3">
                        <span className="material-symbols-outlined text-primary">temple_hindu</span>
                        <div>
                            <h2 className="text-xl font-bold text-ink-900">
                                {existingTemple ? 'Edit Temple' : 'Create Temple'}
                            </h2>
                            <p className="text-xs text-ink-500">Manage temple information</p>
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
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
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
                                placeholder="e.g., Tirumala Venkateswara Temple"
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
                                placeholder="e.g., tirumala venkateswara temple"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="space-y-2">
                            <label className="block text-sm font-semibold text-ink-900">City</label>
                            <input
                                type="text"
                                value={formData.city}
                                onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                                className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                placeholder="e.g., Tirupati"
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="block text-sm font-semibold text-ink-900">State</label>
                            <input
                                type="text"
                                value={formData.state}
                                onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                                className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                placeholder="e.g., Andhra Pradesh"
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="block text-sm font-semibold text-ink-900">Country</label>
                            <input
                                type="text"
                                value={formData.country}
                                onChange={(e) => setFormData({ ...formData, country: e.target.value })}
                                className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                placeholder="e.g., India"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <label className="block text-sm font-semibold text-ink-900">Primary Deity</label>
                            <select
                                value={formData.primaryDeityId}
                                onChange={(e) => setFormData({ ...formData, primaryDeityId: e.target.value })}
                                disabled={loadingDeities}
                                className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                            >
                                <option value="">Select Deity...</option>
                                {deities.map(deity => (
                                    <option key={deity.id} value={deity.id}>{deity.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <label className="block text-sm font-semibold text-ink-900">Latitude</label>
                            <input
                                type="number"
                                step="any"
                                value={formData.latitude}
                                onChange={(e) => setFormData({ ...formData, latitude: e.target.value })}
                                className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                placeholder="e.g., 13.6500"
                            />
                        </div>

                        <div className="space-y-2">
                            <label className="block text-sm font-semibold text-ink-900">Longitude</label>
                            <input
                                type="number"
                                step="any"
                                value={formData.longitude}
                                onChange={(e) => setFormData({ ...formData, longitude: e.target.value })}
                                className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                placeholder="e.g., 79.4200"
                            />
                        </div>
                    </div>

                    <div className="space-y-2">
                        <label className="block text-sm font-semibold text-ink-900">Notes</label>
                        <textarea
                            value={formData.notes}
                            onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                            rows={3}
                            className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent resize-none"
                            placeholder="Optional notes about the temple..."
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
                            {saving ? 'Saving...' : existingTemple ? 'Update Temple' : 'Create Temple'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

import React, { useState } from 'react';
import { EntityFormProps } from '../types';
import { FormInput, FormTextarea } from '../../form';

export const TalaForm: React.FC<EntityFormProps> = ({ initialData, onSave, onCancel, saving }) => {
    const [formData, setFormData] = useState({
        name: initialData?.name || '',
        normalizedName: initialData?.normalizedName || '',
        beatCount: initialData?.beatCount || '',
        angaStructure: initialData?.angaStructure || '',
        notes: initialData?.notes || '',
    });

    const handleChange = (field: string, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!formData.name) return;

        await onSave({
            name: formData.name,
            nameNormalized: formData.normalizedName || null,
            beatCount: formData.beatCount && String(formData.beatCount).trim() ? parseInt(String(formData.beatCount)) : null,
            angaStructure: formData.angaStructure || null,
            notes: formData.notes || null,
        });
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                <FormInput name="name" label="Name" placeholder="e.g. Adi Tala" value={formData.name} onChange={(e: any) => handleChange('name', e.target.value)} required />
                <div className="space-y-2">
                    <label className="block text-sm font-semibold text-ink-900">Normalized Name</label>
                    <input className="w-full h-12 px-4 rounded-lg bg-slate-100 border border-border-light text-ink-500 focus:outline-none cursor-not-allowed" readOnly placeholder="e.g. adi_tala" value={formData.normalizedName || ''} />
                </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-12 gap-6 md:gap-8">
                <FormInput name="beatCount" label="Beat Count (Aksharas)" placeholder="e.g. 8" type="number" value={formData.beatCount} onChange={(e: any) => handleChange('beatCount', e.target.value)} className="md:col-span-4" />
                <FormInput name="angaStructure" label="Anga Structure" placeholder="e.g. I4 O O" value={formData.angaStructure} onChange={(e: any) => handleChange('angaStructure', e.target.value)} className="md:col-span-8" />
            </div>
            <FormTextarea name="notes" label="Notes" placeholder="Add any additional context..." value={formData.notes} onChange={(e: any) => handleChange('notes', e.target.value)} />

            {/* Footer Actions */}
            <div className="pt-6 border-t border-border-light flex flex-col-reverse sm:flex-row items-center justify-end gap-4">
                <button
                    type="button"
                    onClick={onCancel}
                    className="w-full sm:w-auto px-6 py-2.5 rounded-lg border border-border-light text-ink-700 bg-white hover:bg-slate-50 text-sm font-medium transition-colors"
                >
                    Cancel
                </button>
                <button
                    type="submit"
                    disabled={saving}
                    className="w-full sm:w-auto px-6 py-2.5 rounded-lg bg-primary hover:bg-primary-dark text-white text-sm font-medium shadow-md shadow-blue-500/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    <span className="material-symbols-outlined text-lg">save</span>
                    {saving ? 'Saving...' : 'Save Changes'}
                </button>
            </div>
        </form>
    );
};

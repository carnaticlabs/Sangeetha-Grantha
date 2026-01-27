import React, { useState } from 'react';
import { EntityFormProps } from '../types';
import { FormInput, FormTextarea } from '../../form';

export const DeityForm: React.FC<EntityFormProps> = ({ initialData, onSave, onCancel, saving }) => {
    const [formData, setFormData] = useState({
        name: initialData?.name || '',
        normalizedName: initialData?.normalizedName || '',
        description: initialData?.description || '',
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
            description: formData.description || null,
        });
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                <FormInput name="name" label="Name" placeholder="e.g. Venkateswara" value={formData.name} onChange={(e: any) => handleChange('name', e.target.value)} required />
                <FormInput name="normalizedName" label="Normalized Name" placeholder="e.g. venkateswara" value={formData.normalizedName} onChange={(e: any) => handleChange('normalizedName', e.target.value)} />
            </div>
            <FormTextarea name="description" label="Description" placeholder="Description..." value={formData.description} onChange={(e: any) => handleChange('description', e.target.value)} />

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

import React, { useState } from 'react';
import { EntityFormProps } from '../types';
import { FormInput, FormTextarea } from '../../form';

export const ComposerForm: React.FC<EntityFormProps> = ({ initialData, onSave, onCancel, saving }) => {
    const [formData, setFormData] = useState({
        name: initialData?.name || '',
        normalizedName: initialData?.normalizedName || '',
        birthYear: initialData?.birthYear || '',
        deathYear: initialData?.deathYear || '',
        place: initialData?.place || '',
        notes: initialData?.notes || '',
    });

    const handleChange = (field: string, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        // Basic validation handled by FormInput required prop or here
        if (!formData.name) return;

        await onSave({
            name: formData.name,
            nameNormalized: formData.normalizedName || null,
            birthYear: formData.birthYear && formData.birthYear.trim() ? parseInt(formData.birthYear) : null,
            deathYear: formData.deathYear && formData.deathYear.trim() ? parseInt(formData.deathYear) : null,
            place: formData.place || null,
            notes: formData.notes || null,
        });
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                <FormInput
                    name="name"
                    label="Name"
                    placeholder="e.g. Tyagaraja"
                    value={formData.name}
                    onChange={(e: any) => handleChange('name', e.target.value)}
                    required
                />
                <FormInput
                    name="normalizedName"
                    label="Normalized Name"
                    placeholder="e.g. Tyāgarāja"
                    value={formData.normalizedName}
                    onChange={(e: any) => handleChange('normalizedName', e.target.value)}
                />
                <FormInput
                    name="birthYear"
                    label="Birth Year"
                    placeholder="YYYY"
                    value={formData.birthYear}
                    onChange={(e: any) => handleChange('birthYear', e.target.value)}
                />
                <FormInput
                    name="deathYear"
                    label="Death Year"
                    placeholder="YYYY"
                    value={formData.deathYear}
                    onChange={(e: any) => handleChange('deathYear', e.target.value)}
                />
                <FormInput
                    name="place"
                    label="Place of Origin"
                    placeholder="City/Village"
                    value={formData.place}
                    onChange={(e: any) => handleChange('place', e.target.value)}
                    className="md:col-span-2"
                />
            </div>
            <FormTextarea
                name="notes"
                label="Biographical Notes"
                value={formData.notes}
                onChange={(e: any) => handleChange('notes', e.target.value)}
            />

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

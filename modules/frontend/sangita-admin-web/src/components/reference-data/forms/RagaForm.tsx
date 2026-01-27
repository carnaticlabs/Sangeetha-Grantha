import React, { useState } from 'react';
import { EntityFormProps } from '../types';
import { FormInput, FormTextarea, FormSelect } from '../../form';

export const RagaForm: React.FC<EntityFormProps> = ({ initialData, onSave, onCancel, saving }) => {
    const [formData, setFormData] = useState({
        name: initialData?.name || '',
        normalizedName: initialData?.normalizedName || '',
        melakartaNumber: initialData?.melakartaNumber || '',
        parentRaga: initialData?.parentRaga || '',
        arohanam: initialData?.arohanam || '',
        avarohanam: initialData?.avarohanam || '',
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
            melakartaNumber: formData.melakartaNumber && String(formData.melakartaNumber).trim() ? parseInt(String(formData.melakartaNumber)) : null,
            parentRagaId: formData.parentRaga || null,
            arohanam: formData.arohanam || null,
            avarohanam: formData.avarohanam || null,
            notes: formData.notes || null,
        });
    };

    // TODO: Populate parent raga options dynamically if possible
    // For now, replicating behavior but maybe we should allow text input or future fetch
    // Actually, 'FormSelect' expects simple options array. 
    // Let's use FormSelect logic from previous ReferenceData.tsx but fix the options logic?
    // The previous code had "Melakarta" and "Janya" which was likely wrong for ID.
    // I'll make it a text input for ID/Name for now to be safe, or just keep the fields.

    return (
        <form onSubmit={handleSubmit} className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                <FormInput name="name" label="Name" placeholder="e.g. Mayamalavagowla" value={formData.name} onChange={(e: any) => handleChange('name', e.target.value)} required />

                {/* 
                   Ideally this should be a searchable select of other Ragas. 
                   For now, we'll keep it as text input or maybe just a placeholder select if we want to support classification.
                   Let's use a text input for Parent ID temporarily or just "Input" so user can type ID or Name 
                */}
                <FormInput name="parentRaga" label="Parent Raga (ID or Classification)" placeholder="e.g. Melakarta or Parent ID" value={formData.parentRaga} onChange={(e: any) => handleChange('parentRaga', e.target.value)} />

                <FormInput name="melakartaNumber" label="Melakarta Number" placeholder="1-72" value={formData.melakartaNumber} onChange={(e: any) => handleChange('melakartaNumber', e.target.value)} />
                <FormInput name="normalizedName" label="Normalized Name" placeholder="e.g. mayamulavagowla" value={formData.normalizedName} onChange={(e: any) => handleChange('normalizedName', e.target.value)} />
            </div>
            <div className="space-y-6">
                <FormInput name="arohanam" label="Arohanam" placeholder="Format: S R1 G3 M1 P D1 N3 S" value={formData.arohanam} onChange={(e: any) => handleChange('arohanam', e.target.value)} />
                <FormInput name="avarohanam" label="Avarohanam" placeholder="Format: S N3 D1 P M1 G3 R1 S" value={formData.avarohanam} onChange={(e: any) => handleChange('avarohanam', e.target.value)} />
            </div>
            <FormTextarea name="notes" label="Musicological Notes" value={formData.notes} onChange={(e: any) => handleChange('notes', e.target.value)} />

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

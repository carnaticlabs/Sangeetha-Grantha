import React, { useState, useEffect } from 'react';
import { EntityFormProps } from '../types';
import { FormInput, FormTextarea, FormSelect } from '../../form';
import { getDeities } from '../../../api/client';
import { Deity } from '../../../types';

export const TempleForm: React.FC<EntityFormProps> = ({ initialData, onSave, onCancel, saving }) => {
    const locationParts = initialData?.location?.split(',') || [];
    const coordParts = initialData?.coordinates?.split(',') || [];

    // We need to parse initialData back to form fields if it was flattened
    // But initialData here comes from the hook/list which calls mapTempleToItem

    // We should be careful about state/city separation.
    // The previous implementation stored flattened location string in local interface
    // but the API expects city, state, etc.
    // So we'll try to use the raw fields if we have them, or parse.
    // However, the `initialData` prop passed here will be the `ReferenceItem` type.
    // Let's rely on `initialData` having the fields from `Temple` interface if we cast it.
    // But `mapTempleToItem` might have flattened them.
    // Let's assume we can pass the raw object or we need to respect the list item shape.
    // Wait, `EntityList` passes the item from `filteredData`. 
    // `useEntityCrud` returns the raw API response usually.
    // `ReferenceData.tsx` mapped them locally.
    // `useEntityCrud` in `ReferenceData.tsx` refactor returns raw objects from API!
    // So `initialData` here is likely the raw `Temple` object from API (plus whatever map if we add one).
    // Let's assume raw `Temple` object for `useEntityCrud`.

    const [formData, setFormData] = useState({
        name: initialData?.name || '',
        normalizedName: initialData?.normalizedName || '', // normalizedName might be in raw or mapped
        city: initialData?.city || '',
        state: initialData?.state || '',
        country: initialData?.country || '',
        primaryDeity: initialData?.primaryDeityId || initialData?.primaryDeity || '',
        latitude: initialData?.latitude || '',
        longitude: initialData?.longitude || '',
        notes: initialData?.notes || '',
    });

    const [deities, setDeities] = useState<Deity[]>([]);

    useEffect(() => {
        getDeities().then(setDeities).catch(console.error);
    }, []);

    const handleChange = (field: string, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!formData.name) return;

        await onSave({
            name: formData.name,
            nameNormalized: formData.normalizedName || null,
            city: formData.city || null,
            state: formData.state || null,
            country: formData.country || null,
            primaryDeityId: formData.primaryDeity || null,
            latitude: formData.latitude && String(formData.latitude).trim() ? parseFloat(String(formData.latitude)) : null,
            longitude: formData.longitude && String(formData.longitude).trim() ? parseFloat(String(formData.longitude)) : null,
            notes: formData.notes || null,
        });
    };

    const deityOptions = deities.map(d => ({ value: d.id, label: d.name }));

    return (
        <form onSubmit={handleSubmit} className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                <FormInput name="name" label="Name" placeholder="e.g. Srirangam" value={formData.name} onChange={(e: any) => handleChange('name', e.target.value)} required />
                <div className="space-y-2">
                    <label className="block text-sm font-semibold text-ink-900">Normalized Name</label>
                    <input className="w-full h-12 px-4 rounded-lg bg-slate-100 border border-border-light text-ink-500 focus:outline-none cursor-not-allowed" readOnly placeholder="e.g. srirangam" value={formData.normalizedName || ''} />
                </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 md:gap-8">
                <FormInput name="city" label="City" placeholder="e.g. Trichy" value={formData.city} onChange={(e: any) => handleChange('city', e.target.value)} />
                <FormInput name="state" label="State" placeholder="e.g. Tamil Nadu" value={formData.state} onChange={(e: any) => handleChange('state', e.target.value)} />
                <FormInput name="country" label="Country" placeholder="e.g. India" value={formData.country} onChange={(e: any) => handleChange('country', e.target.value)} />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                <FormSelect
                    name="primaryDeity"
                    label="Primary Deity"
                    value={formData.primaryDeity}
                    options={deityOptions}
                    onChange={(e: any) => handleChange('primaryDeity', e.target.value)}
                    placeholder="Select Deity..."
                />
                <div className="grid grid-cols-2 gap-4">
                    <FormInput name="latitude" label="Latitude" placeholder="e.g. 10.8635" type="number" step="any" value={formData.latitude} onChange={(e: any) => handleChange('latitude', e.target.value)} />
                    <FormInput name="longitude" label="Longitude" placeholder="e.g. 78.6864" type="number" step="any" value={formData.longitude} onChange={(e: any) => handleChange('longitude', e.target.value)} />
                </div>
            </div>
            <FormTextarea name="notes" label="Notes" placeholder="Context..." value={formData.notes} onChange={(e: any) => handleChange('notes', e.target.value)} />

            {initialData?.names && initialData.names.length > 0 && (
                <div className="space-y-2">
                    <label className="block text-sm font-semibold text-ink-900">Temple Names (Aliases)</label>
                    <div className="space-y-2">
                        {initialData.names.map((alias: any, idx: number) => (
                            <div key={idx} className="p-3 bg-slate-50 border border-border-light rounded-lg text-sm">
                                <span className="font-medium">{alias.name}</span> ({alias.language || alias.lang}, {alias.script})
                            </div>
                        ))}
                    </div>
                    <p className="text-xs text-ink-500">Temple name aliases can be managed separately after creation.</p>
                </div>
            )}

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

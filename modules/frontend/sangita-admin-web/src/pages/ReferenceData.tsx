import React, { useState, useEffect } from 'react';

import { ReferenceDataStats, Deity, Composer, Raga, Tala, Temple } from '../types';
import { 
    getReferenceStats, 
    getDeities, 
    getComposers, 
    getRagas, 
    getTalas, 
    getTemples,
    createComposer,
    updateComposer,
    deleteComposer,
    createRaga,
    updateRaga,
    deleteRaga,
    createTala,
    updateTala,
    deleteTala,
    createTemple,
    updateTemple,
    deleteTemple
} from '../api/client';
import { useToast } from '../components/Toast';

// --- Types & Mocks ---

type EntityType = 'Ragas' | 'Talas' | 'Composers' | 'Temples' | null;
type ViewMode = 'HOME' | 'LIST' | 'FORM';

interface BaseEntity {
    id: string;
    name: string;
    normalizedName: string;
    updatedAt: string;
    updatedBy: string;
}

interface Composer extends BaseEntity {
    type: 'Composers';
    birthYear?: string;
    deathYear?: string;
    place?: string;
    notes?: string;
}

interface Raga extends BaseEntity {
    type: 'Ragas';
    melakartaNumber?: string;
    parentRaga?: string;
    arohanam: string;
    avarohanam: string;
    notes?: string;
}

interface Tala extends BaseEntity {
    type: 'Talas';
    angaStructure: string;
    beatCount: number;
    notes?: string;
}

interface Temple extends BaseEntity {
    type: 'Temples';
    location: string;
    primaryDeity: string;
    coordinates?: string;
    aliases?: { lang: string; script: string; name: string }[];
    notes?: string;
}

// Union type for selected item
type ReferenceItem = Composer | Raga | Tala | Temple;

// Helper function to map API types to local ReferenceItem types
// API returns camelCase fields matching the DTOs
const mapComposerToItem = (c: Composer): Composer => ({
    ...c,
    type: 'Composers' as const,
    normalizedName: c.nameNormalized,
    birthYear: c.birthYear?.toString(),
    deathYear: c.deathYear?.toString(),
    updatedAt: c.updatedAt ? new Date(c.updatedAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
    updatedBy: 'System' // Not in API DTO
});

const mapRagaToItem = (r: Raga): Raga => ({
    ...r,
    type: 'Ragas' as const,
    normalizedName: r.nameNormalized,
    melakartaNumber: r.melakartaNumber?.toString(),
    parentRaga: r.parentRagaId, // Will need to resolve to name if needed
    arohanam: r.arohanam || '',
    avarohanam: r.avarohanam || '',
    updatedAt: r.updatedAt ? new Date(r.updatedAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
    updatedBy: 'System'
});

const mapTalaToItem = (t: Tala): Tala => ({
    ...t,
    type: 'Talas' as const,
    normalizedName: t.nameNormalized,
    angaStructure: t.angaStructure || '',
    beatCount: t.beatCount || 0,
    updatedAt: t.updatedAt ? new Date(t.updatedAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
    updatedBy: 'System'
});

const mapTempleToItem = (t: Temple): Temple => ({
    ...t,
    type: 'Temples' as const,
    normalizedName: t.nameNormalized,
    canonicalName: t.name, // For UI compatibility
    location: `${t.city || ''}${t.city && t.state ? ', ' : ''}${t.state || ''}${(t.city || t.state) && t.country ? ', ' : ''}${t.country || ''}`.trim() || 'Unknown',
    primaryDeity: t.primaryDeityId || '', // Will need to resolve to name if needed
    coordinates: t.latitude && t.longitude ? `${t.latitude},${t.longitude}` : undefined,
    aliases: t.names || [],
    updatedAt: t.updatedAt ? new Date(t.updatedAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
    updatedBy: 'System'
});

// --- Sub-Components ---

// Reusable styled input components matching the reference design
const FormInput = ({ label, placeholder, defaultValue, value, onChange, required = false, type = "text", help, className = "", name }: any) => {
    const inputValue = value !== undefined ? value : defaultValue;
    return (
        <div className={`space-y-2 ${className}`}>
            <label className="block text-sm font-semibold text-ink-900">
                {label} {required && <span className="text-red-500">*</span>}
            </label>
            <div className="relative">
                <input
                    type={type}
                    name={name}
                    value={inputValue || ''}
                    onChange={onChange}
                    placeholder={placeholder}
                    className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 placeholder-ink-400 focus:ring-2 focus:ring-primary focus:border-transparent transition-all"
                />
                {type === 'number' && (
                    <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-ink-400">
                        <span className="material-symbols-outlined text-lg">tag</span>
                    </div>
                )}
            </div>
            {help && <p className="text-xs text-ink-500">{help}</p>}
        </div>
    );
};

const FormTextarea = ({ label, placeholder, defaultValue, value, onChange, rows = 4, className = "", name }: any) => {
    const textareaValue = value !== undefined ? value : defaultValue;
    return (
        <div className={`space-y-2 ${className}`}>
            <label className="block text-sm font-semibold text-ink-900">{label}</label>
            <textarea
                name={name}
                rows={rows}
                value={textareaValue || ''}
                onChange={onChange}
                placeholder={placeholder}
                className="w-full p-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 placeholder-ink-400 focus:ring-2 focus:ring-primary focus:border-transparent transition-all resize-none"
            ></textarea>
        </div>
    );
};

const FormSelect = ({ label, options, defaultValue, className = "" }: any) => (
    <div className={`space-y-2 ${className}`}>
        <label className="block text-sm font-semibold text-ink-900">{label}</label>
        <div className="relative">
            <select
                defaultValue={defaultValue}
                className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent appearance-none"
            >
                {options.map((opt: string) => <option key={opt}>{opt}</option>)}
            </select>
            <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-ink-500 pointer-events-none">expand_more</span>
        </div>
    </div>
);

const EntityForm: React.FC<{
    entityType: EntityType;
    initialData?: ReferenceItem | null;
    onSave: () => void;
    onCancel: () => void;
}> = ({ entityType, initialData, onSave, onCancel }) => {
    const { toast } = useToast();
    const [deities, setDeities] = useState<Deity[]>([]);
    const [formData, setFormData] = useState<Record<string, any>>({});
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (entityType === 'Temples') {
            getDeities().then(setDeities).catch(console.error);
        }
        
        // Initialize form data from initialData or set defaults for new entities
        if (initialData) {
            if (entityType === 'Composers') {
                const c = initialData as Composer;
                setFormData({
                    name: c.name || '',
                    normalizedName: c.normalizedName || '',
                    birthYear: c.birthYear || '',
                    deathYear: c.deathYear || '',
                    place: c.place || '',
                    notes: c.notes || '',
                });
            } else if (entityType === 'Ragas') {
                const r = initialData as Raga;
                setFormData({
                    name: r.name || '',
                    normalizedName: r.normalizedName || '',
                    melakartaNumber: r.melakartaNumber || '',
                    parentRaga: r.parentRaga || '',
                    arohanam: r.arohanam || '',
                    avarohanam: r.avarohanam || '',
                    notes: r.notes || '',
                });
            } else if (entityType === 'Talas') {
                const t = initialData as Tala;
                setFormData({
                    name: t.name || '',
                    normalizedName: t.normalizedName || '',
                    beatCount: t.beatCount || '',
                    angaStructure: t.angaStructure || '',
                    notes: t.notes || '',
                });
            } else if (entityType === 'Temples') {
                const tm = initialData as Temple;
                const locationParts = tm.location?.split(',') || [];
                const coordParts = tm.coordinates?.split(',') || [];
                setFormData({
                    name: tm.name || '',
                    normalizedName: tm.normalizedName || '',
                    city: locationParts[0] || '',
                    state: locationParts[1] || '',
                    country: locationParts[2] || '',
                    primaryDeity: tm.primaryDeity || '',
                    latitude: coordParts[0] || '',
                    longitude: coordParts[1] || '',
                    notes: tm.notes || '',
                });
            }
        } else {
            // Initialize empty form for new entities
            if (entityType === 'Composers') {
                setFormData({
                    name: '',
                    normalizedName: '',
                    birthYear: '',
                    deathYear: '',
                    place: '',
                    notes: '',
                });
            } else if (entityType === 'Ragas') {
                setFormData({
                    name: '',
                    normalizedName: '',
                    melakartaNumber: '',
                    parentRaga: '',
                    arohanam: '',
                    avarohanam: '',
                    notes: '',
                });
            } else if (entityType === 'Talas') {
                setFormData({
                    name: '',
                    normalizedName: '',
                    beatCount: '',
                    angaStructure: '',
                    notes: '',
                });
            } else if (entityType === 'Temples') {
                setFormData({
                    name: '',
                    normalizedName: '',
                    city: '',
                    state: '',
                    country: '',
                    primaryDeity: '',
                    latitude: '',
                    longitude: '',
                    notes: '',
                });
            }
        }
    }, [entityType, initialData]);

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSave = async (e?: React.MouseEvent) => {
        e?.preventDefault();
        e?.stopPropagation();
        
        setSaving(true);
        try {
            // Validate required fields
            if (entityType === 'Composers' && !formData.name) {
                toast.error('Name is required');
                setSaving(false);
                return;
            }
            if (entityType === 'Ragas' && !formData.name) {
                toast.error('Name is required');
                setSaving(false);
                return;
            }
            if (entityType === 'Talas' && !formData.name) {
                toast.error('Name is required');
                setSaving(false);
                return;
            }
            if (entityType === 'Temples' && !formData.name) {
                toast.error('Name is required');
                setSaving(false);
                return;
            }

            if (initialData) {
                // Update existing entity
                if (entityType === 'Composers') {
                    await updateComposer(initialData.id, {
                        name: formData.name || null,
                        nameNormalized: formData.normalizedName || null,
                        birthYear: formData.birthYear && formData.birthYear.trim() ? parseInt(formData.birthYear) : null,
                        deathYear: formData.deathYear && formData.deathYear.trim() ? parseInt(formData.deathYear) : null,
                        place: formData.place || null,
                        notes: formData.notes || null,
                    });
                } else if (entityType === 'Ragas') {
                    await updateRaga(initialData.id, {
                        name: formData.name || null,
                        nameNormalized: formData.normalizedName || null,
                        melakartaNumber: formData.melakartaNumber && formData.melakartaNumber.trim() ? parseInt(formData.melakartaNumber) : null,
                        parentRagaId: formData.parentRaga || null,
                        arohanam: formData.arohanam || null,
                        avarohanam: formData.avarohanam || null,
                        notes: formData.notes || null,
                    });
                } else if (entityType === 'Talas') {
                    await updateTala(initialData.id, {
                        name: formData.name || null,
                        nameNormalized: formData.normalizedName || null,
                        beatCount: formData.beatCount && formData.beatCount.trim() ? parseInt(formData.beatCount) : null,
                        angaStructure: formData.angaStructure || null,
                        notes: formData.notes || null,
                    });
                } else if (entityType === 'Temples') {
                    await updateTemple(initialData.id, {
                        name: formData.name || null,
                        nameNormalized: formData.normalizedName || null,
                        city: formData.city || null,
                        state: formData.state || null,
                        country: formData.country || null,
                        primaryDeityId: formData.primaryDeity || null,
                        latitude: formData.latitude && formData.latitude.trim() ? parseFloat(formData.latitude) : null,
                        longitude: formData.longitude && formData.longitude.trim() ? parseFloat(formData.longitude) : null,
                        notes: formData.notes || null,
                    });
                }
                toast.success('Entity updated successfully');
            } else {
                // Create new entity
                if (entityType === 'Composers') {
                    await createComposer({
                        name: formData.name!,
                        nameNormalized: formData.normalizedName || null,
                        birthYear: formData.birthYear && formData.birthYear.trim() ? parseInt(formData.birthYear) : null,
                        deathYear: formData.deathYear && formData.deathYear.trim() ? parseInt(formData.deathYear) : null,
                        place: formData.place || null,
                        notes: formData.notes || null,
                    });
                } else if (entityType === 'Ragas') {
                    await createRaga({
                        name: formData.name!,
                        nameNormalized: formData.normalizedName || null,
                        melakartaNumber: formData.melakartaNumber && formData.melakartaNumber.trim() ? parseInt(formData.melakartaNumber) : null,
                        parentRagaId: formData.parentRaga || null,
                        arohanam: formData.arohanam || null,
                        avarohanam: formData.avarohanam || null,
                        notes: formData.notes || null,
                    });
                } else if (entityType === 'Talas') {
                    await createTala({
                        name: formData.name!,
                        nameNormalized: formData.normalizedName || null,
                        beatCount: formData.beatCount && formData.beatCount.trim() ? parseInt(formData.beatCount) : null,
                        angaStructure: formData.angaStructure || null,
                        notes: formData.notes || null,
                    });
                } else if (entityType === 'Temples') {
                    await createTemple({
                        name: formData.name!,
                        nameNormalized: formData.normalizedName || null,
                        city: formData.city || null,
                        state: formData.state || null,
                        country: formData.country || null,
                        primaryDeityId: formData.primaryDeity || null,
                        latitude: formData.latitude && formData.latitude.trim() ? parseFloat(formData.latitude) : null,
                        longitude: formData.longitude && formData.longitude.trim() ? parseFloat(formData.longitude) : null,
                        notes: formData.notes || null,
                    });
                }
                toast.success('Entity created successfully');
            }
            
            onSave(); // Navigate back to list
        } catch (err: any) {
            console.error('Failed to save:', err);
            toast.error('Failed to save: ' + (err.message || 'Unknown error'));
        } finally {
            setSaving(false);
        }
    };

    // Layout Structure
    return (
        <div className="max-w-5xl mx-auto space-y-8 animate-fadeIn">
            {/* Header */}
            <div className="space-y-4">
                <nav className="flex items-center gap-2 text-sm text-ink-500">
                    <span className="hover:text-primary transition-colors cursor-pointer" onClick={onCancel}>Reference Data</span>
                    <span className="material-symbols-outlined text-base">chevron_right</span>
                    <span className="hover:text-primary transition-colors cursor-pointer" onClick={onCancel}>{entityType}</span>
                    <span className="material-symbols-outlined text-base">chevron_right</span>
                    <span className="text-ink-900 font-medium">{initialData ? 'Edit' : 'Create'} {entityType?.slice(0, -1)}</span>
                </nav>
                <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
                    <div>
                        <h2 className="text-3xl md:text-4xl font-display font-bold text-ink-900 tracking-tight">
                            {initialData ? 'Edit' : 'Create'} {entityType?.slice(0, -1)}
                        </h2>
                        <p className="text-ink-500 mt-2 text-base">Define canonical details for this entity.</p>
                    </div>
                </div>
            </div>

            {/* Form Card */}
            <div className="bg-surface-light rounded-xl shadow-sm border border-border-light overflow-hidden">
                <div className="p-6 md:p-8 space-y-8">
                    {/* Render Form Fields based on Type */}
                    {entityType === 'Talas' && (
                        <div className="space-y-8">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                                <FormInput name="name" label="Name" placeholder="e.g. Adi Tala" value={formData.name} onChange={handleInputChange} required />
                                <div className="space-y-2">
                                    <div className="flex items-center justify-between">
                                        <label className="block text-sm font-semibold text-ink-900">Normalized Name</label>
                                        <span className="text-xs text-ink-400">Auto-generated</span>
                                    </div>
                                    <input className="w-full h-12 px-4 rounded-lg bg-slate-100 border border-border-light text-ink-500 focus:outline-none cursor-not-allowed" readOnly placeholder="e.g. adi_tala" value={formData.normalizedName || ''} />
                                </div>
                            </div>
                            <div className="grid grid-cols-1 md:grid-cols-12 gap-6 md:gap-8">
                                <FormInput name="beatCount" label="Beat Count (Aksharas)" placeholder="e.g. 8" type="number" value={formData.beatCount} onChange={handleInputChange} className="md:col-span-4" />
                                <FormInput name="angaStructure" label="Anga Structure" placeholder="e.g. I4 O O" help="Example: I4 O O for Adi Tala" value={formData.angaStructure} onChange={handleInputChange} className="md:col-span-8" />
                            </div>
                            <FormTextarea name="notes" label="Notes" placeholder="Add any additional context, historical details, or usage notes..." value={formData.notes} onChange={handleInputChange} />
                        </div>
                    )}

                    {entityType === 'Ragas' && (
                        <div className="space-y-8">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                                <FormInput name="name" label="Name" placeholder="e.g. Mayamalavagowla" value={formData.name} onChange={handleInputChange} required />
                                <div className="space-y-2">
                                    <label className="block text-sm font-semibold text-ink-900">Parent Raga</label>
                                    <select name="parentRaga" value={formData.parentRaga || ''} onChange={handleInputChange} className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent">
                                        <option value="">Select Parent...</option>
                                        <option value="Melakarta">Melakarta</option>
                                        <option value="Janya">Janya</option>
                                    </select>
                                </div>
                                <FormInput name="melakartaNumber" label="Melakarta Number" placeholder="1-72" value={formData.melakartaNumber} onChange={handleInputChange} />
                                <FormInput name="normalizedName" label="Normalized Name" placeholder="e.g. mayamulavagowla" value={formData.normalizedName} onChange={handleInputChange} />
                            </div>
                            <div className="space-y-6">
                                <FormInput name="arohanam" label="Arohanam" placeholder="Format: S R1 G3 M1 P D1 N3 S" value={formData.arohanam} onChange={handleInputChange} />
                                <FormInput name="avarohanam" label="Avarohanam" placeholder="Format: S N3 D1 P M1 G3 R1 S" value={formData.avarohanam} onChange={handleInputChange} />
                            </div>
                            <FormTextarea name="notes" label="Musicological Notes" value={formData.notes} onChange={handleInputChange} />
                        </div>
                    )}

                    {entityType === 'Composers' && (
                        <div className="space-y-8">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                                <FormInput name="name" label="Name" placeholder="e.g. Tyagaraja" value={formData.name} onChange={handleInputChange} required />
                                <FormInput name="normalizedName" label="Normalized Name" placeholder="e.g. Tyāgarāja" value={formData.normalizedName} onChange={handleInputChange} />
                                <FormInput name="birthYear" label="Birth Year" placeholder="YYYY" value={formData.birthYear} onChange={handleInputChange} />
                                <FormInput name="deathYear" label="Death Year" placeholder="YYYY" value={formData.deathYear} onChange={handleInputChange} />
                                <FormInput name="place" label="Place of Origin" placeholder="City/Village" value={formData.place} onChange={handleInputChange} className="md:col-span-2" />
                            </div>
                            <FormTextarea name="notes" label="Biographical Notes" value={formData.notes} onChange={handleInputChange} />
                        </div>
                    )}

                    {entityType === 'Temples' && (
                        <div className="space-y-8">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                                <FormInput name="name" label="Name" placeholder="e.g. Srirangam" value={formData.name} onChange={handleInputChange} required />
                                <div className="space-y-2">
                                    <div className="flex items-center justify-between">
                                        <label className="block text-sm font-semibold text-ink-900">Normalized Name</label>
                                        <span className="text-xs text-ink-400">Auto-generated</span>
                                    </div>
                                    <input className="w-full h-12 px-4 rounded-lg bg-slate-100 border border-border-light text-ink-500 focus:outline-none cursor-not-allowed" readOnly placeholder="e.g. srirangam" value={formData.normalizedName || ''} />
                                </div>
                            </div>
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 md:gap-8">
                                <FormInput name="city" label="City" placeholder="e.g. Trichy" value={formData.city} onChange={handleInputChange} />
                                <FormInput name="state" label="State" placeholder="e.g. Tamil Nadu" value={formData.state} onChange={handleInputChange} />
                                <FormInput name="country" label="Country" placeholder="e.g. India" value={formData.country} onChange={handleInputChange} />
                            </div>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
                                <div className="space-y-2">
                                    <label className="block text-sm font-semibold text-ink-900">Primary Deity</label>
                                    <select
                                        name="primaryDeity"
                                        value={formData.primaryDeity || ''}
                                        onChange={handleInputChange}
                                        className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent appearance-none"
                                    >
                                        <option value="">Select Deity...</option>
                                        {deities.map(deity => (
                                            <option key={deity.id} value={deity.id}>{deity.name}</option>
                                        ))}
                                    </select>
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <FormInput name="latitude" label="Latitude" placeholder="e.g. 10.8635" type="number" step="any" value={formData.latitude} onChange={handleInputChange} />
                                    <FormInput name="longitude" label="Longitude" placeholder="e.g. 78.6864" type="number" step="any" value={formData.longitude} onChange={handleInputChange} />
                                </div>
                            </div>
                            <FormTextarea name="notes" label="Notes" placeholder="Add any additional context, historical details, or usage notes..." value={formData.notes} onChange={handleInputChange} />
                            {(initialData as Temple)?.aliases && (initialData as Temple)!.aliases!.length > 0 && (
                                <div className="space-y-2">
                                    <label className="block text-sm font-semibold text-ink-900">Temple Names (Aliases)</label>
                                    <div className="space-y-2">
                                        {(initialData as Temple)!.aliases!.map((alias, idx) => (
                                            <div key={idx} className="p-3 bg-slate-50 border border-border-light rounded-lg text-sm">
                                                <span className="font-medium">{alias.name}</span> ({alias.lang}, {alias.script})
                                            </div>
                                        ))}
                                    </div>
                                    <p className="text-xs text-ink-500">Temple name aliases can be managed separately after creation.</p>
                                </div>
                            )}
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
                            type="button"
                            onClick={handleSave} 
                            disabled={saving}
                            className="w-full sm:w-auto px-6 py-2.5 rounded-lg bg-primary hover:bg-primary-dark text-white text-sm font-medium shadow-md shadow-blue-500/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            <span className="material-symbols-outlined text-lg">save</span>
                            {saving ? 'Saving...' : 'Save Changes'}
                        </button>
                    </div>
                </div>
            </div>

            {/* Helper Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-4 rounded-lg border border-blue-100 bg-blue-50/50 flex gap-3">
                    <span className="material-symbols-outlined text-blue-600 shrink-0">info</span>
                    <div className="text-sm">
                        <p className="font-medium text-blue-900">Tip for Curators</p>
                        <p className="text-blue-700 mt-1">Ensure the normalized name follows the IAST standard for proper indexing.</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

// --- Main Page Component ---

const ReferenceData: React.FC = () => {
    const [viewMode, setViewMode] = useState<ViewMode>('HOME');
    const [activeEntity, setActiveEntity] = useState<EntityType>(null);
    const [selectedItem, setSelectedItem] = useState<ReferenceItem | null>(null);
    
    // API Data State
    const [composers, setComposers] = useState<Composer[]>([]);
    const [ragas, setRagas] = useState<Raga[]>([]);
    const [talas, setTalas] = useState<Tala[]>([]);
    const [temples, setTemples] = useState<Temple[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [stats, setStats] = useState<ReferenceDataStats | null>(null);

    const handleEntityClick = (type: EntityType) => {
        setActiveEntity(type);
        setViewMode('LIST');
        setSelectedItem(null);
    };

    const handleBack = () => {
        if (viewMode === 'FORM') {
            setViewMode('LIST');
        } else {
            setViewMode('HOME');
            setActiveEntity(null);
            setSelectedItem(null);
        }
    };

    const handleCreate = () => {
        setSelectedItem(null);
        setViewMode('FORM');
    };

    const handleEdit = (item: ReferenceItem) => {
        setSelectedItem(item);
        setViewMode('FORM');
    };

    const handleSave = async () => {
        // Reload the list data after save
        if (activeEntity) {
            setLoading(true);
            try {
                switch (activeEntity) {
                    case 'Composers':
                        const composersData = await getComposers();
                        setComposers(composersData);
                        break;
                    case 'Ragas':
                        const ragasData = await getRagas();
                        setRagas(ragasData);
                        break;
                    case 'Talas':
                        const talasData = await getTalas();
                        setTalas(talasData);
                        break;
                    case 'Temples':
                        const templesData = await getTemples();
                        setTemples(templesData);
                        break;
                }
                // Also reload stats
                const statsData = await getReferenceStats();
                setStats(statsData);
            } catch (err: any) {
                console.error('Failed to reload data after save:', err);
            } finally {
                setLoading(false);
            }
        }
        setViewMode('LIST');
        setSelectedItem(null);
    };

    // Load reference stats on mount
    useEffect(() => {
        const loadStats = async () => {
            try {
                const data = await getReferenceStats();
                setStats(data);
            } catch (e) {
                console.error("Failed to load reference stats", e);
            }
        };
        loadStats();
    }, []);

    // Load reference data when entity type changes
    useEffect(() => {
        if (activeEntity && viewMode === 'LIST') {
            setLoading(true);
            setError(null);
            
            const loadData = async () => {
                try {
                    switch (activeEntity) {
                        case 'Composers':
                            const composersData = await getComposers();
                            setComposers(composersData);
                            break;
                        case 'Ragas':
                            const ragasData = await getRagas();
                            setRagas(ragasData);
                            break;
                        case 'Talas':
                            const talasData = await getTalas();
                            setTalas(talasData);
                            break;
                        case 'Temples':
                            const templesData = await getTemples();
                            setTemples(templesData);
                            break;
                    }
                } catch (err: any) {
                    console.error(`Failed to load ${activeEntity}:`, err);
                    setError(err.message || 'Failed to load data');
                } finally {
                    setLoading(false);
                }
            };
            
            loadData();
        }
    }, [activeEntity, viewMode]);

    // --- RENDERERS ---

    const renderCard = (title: string, count: number, description: string, colorClass: string, type: EntityType) => (
        <div
            onClick={() => handleEntityClick(type)}
            className="bg-surface-light border border-border-light rounded-xl p-6 hover:shadow-md transition-all cursor-pointer group relative overflow-hidden hover:-translate-y-1"
        >
            <div className={`absolute top-0 left-0 w-1 h-full ${colorClass}`}></div>
            <div className="flex justify-between items-start mb-4">
                <h3 className="font-display text-xl font-bold text-ink-900">{title}</h3>
                <span className="bg-slate-100 text-ink-600 text-xs font-bold px-2.5 py-1 rounded-full">{count}</span>
            </div>
            <p className="text-sm text-ink-500 mb-6 leading-relaxed">{description}</p>
            <div className="flex items-center text-sm font-medium text-ink-700 group-hover:text-primary transition-colors">
                Manage
                <span className="material-symbols-outlined text-[18px] ml-1 group-hover:translate-x-1 transition-transform">arrow_forward</span>
            </div>
        </div>
    );

    const renderList = () => {
        let data: ReferenceItem[] = [];
        if (activeEntity === 'Composers') {
            data = composers.map(mapComposerToItem);
        } else if (activeEntity === 'Ragas') {
            data = ragas.map(mapRagaToItem);
        } else if (activeEntity === 'Talas') {
            data = talas.map(mapTalaToItem);
        } else if (activeEntity === 'Temples') {
            data = temples.map(mapTempleToItem);
        }

        return (
            <div className="h-full flex flex-col max-w-7xl mx-auto space-y-6">
                {/* List Header */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <nav className="flex items-center gap-2 text-sm text-ink-500 mb-1">
                        <span className="hover:text-primary transition-colors cursor-pointer" onClick={handleBack}>Reference Data</span>
                        <span className="material-symbols-outlined text-base">chevron_right</span>
                        <span className="text-ink-900 font-medium">{activeEntity}</span>
                    </nav>
                </div>

                <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
                    <div>
                        <h1 className="font-display text-3xl md:text-4xl font-bold text-ink-900 tracking-tight">{activeEntity}</h1>
                        <p className="text-ink-500 mt-2">Manage the collection of {activeEntity?.toLowerCase()}.</p>
                    </div>
                    <button
                        onClick={handleCreate}
                        className="flex items-center gap-2 px-6 py-2.5 bg-primary hover:bg-primary-dark text-white rounded-lg text-sm font-medium shadow-md shadow-blue-500/20 transition-all"
                    >
                        <span className="material-symbols-outlined text-[20px]">add</span>
                        Create New
                    </button>
                </div>

                {/* Filters & Table Container */}
                <div className="bg-surface-light border border-border-light rounded-xl shadow-sm overflow-hidden flex flex-col">
                    {/* Toolbar */}
                    <div className="p-4 border-b border-border-light bg-slate-50/50 flex gap-4">
                        <div className="relative flex-1 max-w-md">
                            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 text-[20px]">search</span>
                            <input
                                type="text"
                                placeholder={`Search ${activeEntity}...`}
                                className="w-full pl-10 pr-4 py-2 bg-white border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent transition-shadow"
                            />
                        </div>
                    </div>

                    {loading && (
                        <div className="p-8 text-center text-ink-500">
                            <span className="material-symbols-outlined animate-spin text-4xl mb-2">sync</span>
                            <p>Loading {activeEntity}...</p>
                        </div>
                    )}
                    {error && (
                        <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-red-800 text-sm">
                            <span className="material-symbols-outlined text-[18px] align-middle mr-1">error</span>
                            Error: {error}
                        </div>
                    )}
                    {!loading && !error && (
                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead className="bg-slate-50/50 border-b border-border-light text-xs font-semibold uppercase tracking-wider text-ink-500">
                                    <tr>
                                        <th className="px-6 py-4">Name</th>
                                        <th className="px-6 py-4">Normalized</th>
                                        {activeEntity === 'Composers' && <th className="px-6 py-4">Period</th>}
                                        {activeEntity === 'Ragas' && <th className="px-6 py-4">Structure</th>}
                                        {activeEntity === 'Talas' && <th className="px-6 py-4">Beats</th>}
                                        <th className="px-6 py-4 text-right">Updated</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-100 bg-white">
                                    {data.length === 0 ? (
                                        <tr>
                                            <td colSpan={5} className="px-6 py-8 text-center text-ink-500">
                                                No {activeEntity?.toLowerCase()} found.
                                            </td>
                                        </tr>
                                    ) : (
                                        data.map(item => (
                                    <tr
                                        key={item.id}
                                        onClick={() => handleEdit(item)}
                                        className="cursor-pointer hover:bg-slate-50 transition-colors group"
                                    >
                                        <td className="px-6 py-4">
                                            <span className="font-display font-medium text-ink-900">{item.name}</span>
                                        </td>
                                        <td className="px-6 py-4 text-sm text-ink-500 font-mono">{item.normalizedName}</td>

                                        {item.type === 'Composers' && <td className="px-6 py-4 text-sm text-ink-500">{(item as Composer).birthYear} – {(item as Composer).deathYear}</td>}
                                        {item.type === 'Ragas' && <td className="px-6 py-4 text-sm text-ink-500">{(item as Raga).arohanam?.substring(0, 15)}...</td>}
                                        {item.type === 'Talas' && <td className="px-6 py-4 text-sm text-ink-500">{(item as Tala).beatCount}</td>}
                                        {item.type === 'Temples' && <td className="px-6 py-4 text-sm text-ink-500">{(item as Temple).location}</td>}
                                            <td className="px-6 py-4 text-right text-xs text-ink-500">
                                                {item.updatedAt}
                                            </td>
                                        </tr>
                                    ))
                                    )}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>
        );
    };

    // --- MAIN RENDER ---

    if (viewMode === 'FORM' && activeEntity) {
        return <EntityForm entityType={activeEntity} initialData={selectedItem} onSave={handleSave} onCancel={handleBack} />;
    }

    if (viewMode === 'LIST') {
        return renderList();
    }

    // Default: Dashboard View
    return (
        <div className="max-w-7xl mx-auto space-y-8 animate-fadeIn">
            <div>
                <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">Reference Data</h1>
                <p className="text-ink-500 mt-2 text-lg">Controlled vocabularies and taxonomies used across the archive.</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {renderCard('Ragas', stats?.ragaCount || 0, 'Melakarta and Janya ragas with Arohana/Avarohana scales.', 'bg-blue-500', 'Ragas')}
                {renderCard('Talas', stats?.talaCount || 0, 'Suladi Sapta talas, Chapu talas, and rhythmic cycles.', 'bg-teal-500', 'Talas')}
                {renderCard('Composers', stats?.composerCount || 0, 'Biographical data, periods, and lineages of vaggeyakaras.', 'bg-purple-500', 'Composers')}
                {renderCard('Temples', stats?.templeCount || 0, 'Geographical locations associated with specific compositions.', 'bg-orange-500', 'Temples')}
            </div>

            <div className="p-6 rounded-xl border border-blue-100 bg-blue-50/50 flex gap-4">
                <span className="material-symbols-outlined text-3xl text-blue-600">sync</span>
                <div>
                    <h3 className="font-bold text-blue-900">External Sync</h3>
                    <p className="text-sm text-blue-700 mt-1">Last synced with Musicological Ontology Service on Oct 24, 2023.</p>
                </div>
            </div>
        </div>
    );
};

export default ReferenceData;

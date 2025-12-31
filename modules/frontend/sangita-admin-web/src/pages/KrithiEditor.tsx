import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { GoogleGenAI } from "@google/genai";
import {
    KrithiDetail,
    MusicalForm,
    Composer,
    Raga,
    Tala,
    Deity,
    Temple,
    Tag,
    KrithiCreateRequest,
    KrithiUpdateRequest,
    AuditLog,
    Sampradaya,
    KrithiLyricVariant,
    KrithiSection,
    KrithiLyricSection
} from '../types';
import NotationTab from '../components/notation/NotationTab';
import { formatLanguageCode, LANGUAGE_CODE_OPTIONS, formatScriptCode, SCRIPT_CODE_OPTIONS, formatSectionType, formatWorkflowState, getWorkflowStateColor } from '../utils/enums';
import {
    getKrithi,
    createKrithi,
    updateKrithi,
    getComposers,
    getRagas,
    getTalas,
    getDeities,
    getTemples,
    getTags,
    getKrithiAuditLogs,
    getSampradayas,
    saveKrithiSections,
    createLyricVariant,
    updateLyricVariant,
    saveVariantSections
} from '../api/client';
import { useToast, ToastContainer } from '../components/Toast';

interface KrithiEditorProps { }

const SectionHeader: React.FC<{ title: string; action?: React.ReactNode }> = ({ title, action }) => (
    <div className="flex items-center justify-between mb-6 pb-2 border-b border-border-light">
        <h3 className="font-display text-lg font-bold text-ink-900">{title}</h3>
        {action}
    </div>
);

const InputField = ({ label, value, onChange, placeholder = '', highlight = false }: any) => (
    <div>
        <label className="block text-sm font-semibold text-ink-900 mb-2 flex justify-between">
            {label}
            {highlight && (
                <span className="text-[10px] text-purple-600 font-bold flex items-center gap-1 animate-pulse">
                    <span className="material-symbols-outlined text-[12px]">auto_awesome</span>
                    UPDATED
                </span>
            )}
        </label>
        <div className="relative">
            <input
                type="text"
                value={value}
                onChange={(e) => onChange(e.target.value)}
                className={`w-full h-12 px-4 border rounded-lg text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent transition-all ${highlight ? 'border-purple-400 bg-purple-50' : 'border-border-light bg-slate-50'
                    }`}
                placeholder={placeholder}
            />
        </div>
    </div>
);

const SelectField = ({ label, value, onChange, options, placeholder = 'Select...' }: any) => (
    <div>
        <label className="block text-sm font-semibold text-ink-900 mb-2">{label}</label>
        <select
            value={value}
            onChange={(e) => onChange(e.target.value)}
            className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
        >
            <option value="">{placeholder}</option>
            {options.map((opt: any) => (
                <option key={opt.id} value={opt.id}>{opt.name || opt.canonicalName || opt.displayName}</option>
            ))}
        </select>
    </div>
);

const TextareaField = ({ label, value, onChange, placeholder = '', rows = 4 }: any) => (
    <div>
        <label className="block text-sm font-semibold text-ink-900 mb-2">{label}</label>
        <textarea
            value={value || ''}
            onChange={(e) => onChange(e.target.value)}
            rows={rows}
            className="w-full px-4 py-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent transition-all resize-none"
            placeholder={placeholder}
        />
    </div>
);

const CheckboxField = ({ label, checked, onChange }: any) => (
    <div className="flex items-center gap-3">
        <input
            type="checkbox"
            checked={checked || false}
            onChange={(e) => onChange(e.target.checked)}
            className="w-5 h-5 rounded border-border-light text-primary focus:ring-2 focus:ring-primary"
        />
        <label className="text-sm font-semibold text-ink-900 cursor-pointer">{label}</label>
    </div>
);


const KrithiEditor: React.FC<KrithiEditorProps> = () => {
    const { id: paramKrithiId } = useParams();
    const isNew = paramKrithiId === 'new';
    const krithiId = isNew ? undefined : paramKrithiId;

    const navigate = useNavigate();
    const onBack = () => navigate('/krithis');
    const toast = useToast();
    const [activeTab, setActiveTab] = useState<'Metadata' | 'Lyrics' | 'Notation' | 'Tags' | 'Audit'>('Metadata');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    // Data State
    const [krithi, setKrithi] = useState<Partial<KrithiDetail>>({
        title: '',
        status: 'DRAFT',
        primaryLanguage: 'te', // Default
        musicalForm: MusicalForm.KRITHI,
        ragas: [],
        sections: [],
        lyricVariants: [],
        tags: []
    });

    // Reference Data State
    const [composers, setComposers] = useState<Composer[]>([]);
    const [ragas, setRagas] = useState<Raga[]>([]);
    const [talas, setTalas] = useState<Tala[]>([]);
    const [deities, setDeities] = useState<Deity[]>([]);
    const [temples, setTemples] = useState<Temple[]>([]);
    const [allTags, setAllTags] = useState<Tag[]>([]);
    const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
    const [sampradayas, setSampradayas] = useState<Sampradaya[]>([]);
    const [editingVariantId, setEditingVariantId] = useState<string | null>(null);

    // Load initial data
    useEffect(() => {
        const loadRefs = async () => {
            try {
                const [c, r, t, d, tm, tg, s] = await Promise.all([
                    getComposers(), getRagas(), getTalas(), getDeities(), getTemples(), getTags(), getSampradayas()
                ]);
                setComposers(c);
                setRagas(r);
                setTalas(t);
                setDeities(d);
                setTemples(tm);
                setAllTags(tg);
                setSampradayas(s);
            } catch (e) {
                console.error("Failed to load reference data", e);
            }
        };
        loadRefs();
    }, []);

    useEffect(() => {
        if (!isNew && krithiId) {
            setLoading(true);
            getKrithi(krithiId)
                .then(setKrithi)
                .catch(err => alert("Failed to load krithi: " + err.message))
                .finally(() => setLoading(false));
            
            // Load audit logs for this krithi
            getKrithiAuditLogs(krithiId)
                .then(setAuditLogs)
                .catch(err => console.error("Failed to load audit logs:", err));
        }
    }, [krithiId, isNew]);

    // Helpers to handle ID-based selection for object fields
    const handleComposerChange = (id: string) => {
        const obj = composers.find(c => c.id === id);
        setKrithi(prev => ({ ...prev, composer: obj }));
    };
    const handleTalaChange = (id: string) => {
        const obj = talas.find(t => t.id === id);
        setKrithi(prev => ({ ...prev, tala: obj }));
    };
    const handleRagaChange = (id: string) => {
        const obj = ragas.find(r => r.id === id);
        if (krithi.isRagamalika) {
            // For ragamalika, add to array if not already present
            if (obj && !krithi.ragas?.some(r => r.id === obj.id)) {
                setKrithi(prev => ({ ...prev, ragas: [...(prev.ragas || []), obj] }));
            }
        } else {
            // Single raga selection
            setKrithi(prev => ({ ...prev, ragas: obj ? [obj] : [] }));
        }
    };

    const handleRemoveRaga = (ragaId: string) => {
        setKrithi(prev => ({
            ...prev,
            ragas: prev.ragas?.filter(r => r.id !== ragaId) || []
        }));
    };
    const handleDeityChange = (id: string) => {
        const obj = deities.find(d => d.id === id);
        setKrithi(prev => ({ ...prev, deity: obj }));
    };
    const handleTempleChange = (id: string) => {
        const obj = temples.find(t => t.id === id);
        setKrithi(prev => ({ ...prev, temple: obj }));
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            // Map types to Request Schema
            const payload: any = {
                title: krithi.title,
                incipit: krithi.incipit,
                composerId: krithi.composer?.id,
                talaId: krithi.tala?.id,
                primaryLanguage: krithi.primaryLanguage,
                ragaIds: krithi.ragas?.map(r => r.id) || [],
                deityId: krithi.deity?.id,
                templeId: krithi.temple?.id,
                musicalForm: krithi.musicalForm,
                isRagamalika: krithi.isRagamalika || false,
                sahityaSummary: krithi.sahityaSummary,
                notes: krithi.notes,
                tagIds: krithi.tags?.map(t => t.id) || [],
                workflowState: krithi.status
            };

            let savedKrithiId = krithiId;

            // Create or update krithi
            if (isNew) {
                const res = await createKrithi(payload);
                savedKrithiId = res.id;
                navigate(`/krithis/${res.id}`, { replace: true });
            } else if (krithiId) {
                await updateKrithi(krithiId, payload);
            }

            // Save sections if krithi ID is available
            if (savedKrithiId && krithi.sections && krithi.sections.length > 0) {
                try {
                    // Map all sections to the API format (ignore temp IDs, API will assign new ones)
                    const sectionsToSave = krithi.sections.map(s => ({
                        sectionType: s.sectionType,
                        orderIndex: s.orderIndex,
                        label: null
                    }));

                    await saveKrithiSections(savedKrithiId, sectionsToSave);
                } catch (err: any) {
                    console.error('Failed to save sections:', err);
                    toast.warning('Krithi saved, but sections may not have been updated');
                }
            }

            // Save lyric variants if krithi ID is available
            if (savedKrithiId && krithi.lyricVariants && krithi.lyricVariants.length > 0) {
                try {
                    for (const variant of krithi.lyricVariants) {
                        if (variant.id.startsWith('temp-')) {
                            // Create new variant
                            const variantPayload = {
                                language: variant.language,
                                script: variant.script,
                                transliterationScheme: variant.transliterationScheme,
                                sampradayaId: variant.sampradaya?.id,
                                isPrimary: false
                            };
                            const createdVariant = await createLyricVariant(savedKrithiId, variantPayload);
                            
                            // Save sections for this variant
                            if (variant.sections && variant.sections.length > 0) {
                                await saveVariantSections(createdVariant.id, variant.sections);
                            }
                        } else {
                            // Update existing variant
                            const variantPayload = {
                                language: variant.language,
                                script: variant.script,
                                transliterationScheme: variant.transliterationScheme,
                                sampradayaId: variant.sampradaya?.id,
                                isPrimary: false
                            };
                            await updateLyricVariant(variant.id, variantPayload);
                            
                            // Save sections for this variant
                            if (variant.sections && variant.sections.length > 0) {
                                await saveVariantSections(variant.id, variant.sections);
                            }
                        }
                    }
                } catch (err: any) {
                    console.error('Failed to save lyric variants:', err);
                    toast.warning('Krithi saved, but lyric variants may not have been updated');
                }
            }

            toast.success('Changes saved successfully');
        } catch (e: any) {
            console.error(e);
            toast.error('Save failed: ' + (e.message || 'Unknown error'));
        } finally {
            setSaving(false);
        }
    };


    const renderWorkflowPill = (status: string = 'draft') => {
        return (
            <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border tracking-wide ${getWorkflowStateColor(status)}`}>
                {formatWorkflowState(status)}
            </span>
        );
    };

    return (
        <>
            <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />
            <div className="max-w-7xl mx-auto animate-fadeIn pb-12 relative">
            {loading ? (
                <div className="p-12 text-center">
                    <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary mb-4"></div>
                    <p className="text-ink-500">Loading Editor...</p>
                </div>
            ) : (
                <>
            {/* 1. Top Header */}
            <div className="mb-6">
                <nav className="flex items-center gap-2 text-sm text-ink-500 mb-4">
                    <button onClick={onBack} className="hover:text-primary transition-colors">Kritis</button>
                    <span className="material-symbols-outlined text-[14px]">chevron_right</span>
                    <span className="text-ink-900 font-medium">{isNew ? 'New Composition' : 'Edit Composition'}</span>
                </nav>

                <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-6">
                    <div>
                        <div className="flex items-center gap-3 mb-2 flex-wrap">
                            <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">{krithi.title || 'Untitled Krithi'}</h1>
                            {renderWorkflowPill(krithi.status)}
                        </div>
                        <p className="text-ink-500 font-medium text-sm flex items-center gap-2">
                            {krithi.composer?.name || 'Unknown Composer'} <span className="text-ink-300">•</span>
                            {krithi.ragas?.[0]?.name || 'Unknown Raga'} <span className="text-ink-300">•</span>
                            {krithi.tala?.name || 'Unknown Tala'} <span className="text-ink-300">•</span>
                            {formatLanguageCode(krithi.primaryLanguage || 'te')}
                        </p>
                    </div>

                    <div className="flex gap-2 flex-wrap">
                        <button
                            disabled={saving}
                            onClick={handleSave}
                            className="px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors shadow-sm shadow-blue-500/20 disabled:opacity-50"
                        >
                            {saving ? 'Saving...' : 'Save Changes'}
                        </button>
                    </div>
                </div>
            </div>

            {/* Tabs */}
            <div className="border-b border-border-light mb-8">
                <nav className="flex gap-8">
                    {['Metadata', 'Lyrics', 'Tags', 'Audit'].map((tab) => {
                        if (tab === 'Notation' && krithi.musicalForm !== MusicalForm.VARNAM && krithi.musicalForm !== MusicalForm.SWARAJATHI) return null;
                        return (
                            <button
                                key={tab}
                                onClick={() => setActiveTab(tab as any)}
                                className={`pb-4 text-sm font-bold border-b-2 transition-colors ${activeTab === tab
                                    ? 'border-primary text-primary'
                                    : 'border-transparent text-ink-500 hover:text-ink-900 hover:border-slate-300'
                                    }`}
                            >
                                {tab}
                            </button>
                        );
                    })}
                    {/* Dynamic Notation Tab */}
                    {(krithi.musicalForm === MusicalForm.VARNAM || krithi.musicalForm === MusicalForm.SWARAJATHI) && (
                        <button
                            onClick={() => setActiveTab('Notation')}
                            className={`pb-4 text-sm font-bold border-b-2 transition-colors ${activeTab === 'Notation'
                                ? 'border-primary text-primary'
                                : 'border-transparent text-ink-500 hover:text-ink-900 hover:border-slate-300'
                                }`}
                        >
                            Notation
                        </button>
                    )}
                </nav>
            </div>

            {/* --- TAB CONTENT --- */}

            {/* 1. METADATA TAB */}
            {activeTab === 'Metadata' && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    <div className="lg:col-span-2 space-y-8">
                        {/* Identity */}
                        <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6 relative overflow-hidden">
                            <SectionHeader title="Identity" />
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div className="md:col-span-2">
                                    <InputField
                                        label="Name (Transliterated)"
                                        value={krithi.title}
                                        onChange={(v: string) => setKrithi({ ...krithi, title: v })}
                                    />
                                </div>
                                <div className="md:col-span-2">
                                    <InputField
                                        label="Incipit"
                                        value={krithi.incipit || ''}
                                        onChange={(v: string) => setKrithi({ ...krithi, incipit: v })}
                                        placeholder="First line or popular handle"
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Canonical Links */}
                        <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6">
                            <SectionHeader title="Canonical Links" />
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <SelectField
                                    label="Composer"
                                    options={composers}
                                    value={krithi.composer?.id || ''}
                                    onChange={handleComposerChange}
                                />
                                <div>
                                    <label className="block text-sm font-semibold text-ink-900 mb-2">
                                        {krithi.isRagamalika ? 'Ragas (Ragamalika)' : 'Raga'}
                                    </label>
                                    {krithi.isRagamalika && krithi.ragas && krithi.ragas.length > 0 && (
                                        <div className="flex flex-wrap gap-2 mb-2">
                                            {krithi.ragas.map((raga, idx) => (
                                                <span
                                                    key={raga.id}
                                                    className="inline-flex items-center gap-1 px-3 py-1 bg-primary-light text-primary rounded-full text-sm border border-primary/20"
                                                >
                                                    {raga.name}
                                                    <button
                                                        type="button"
                                                        onClick={() => handleRemoveRaga(raga.id)}
                                                        className="ml-1 hover:text-primary-dark"
                                                    >
                                                        <span className="material-symbols-outlined text-[16px]">close</span>
                                                    </button>
                                                </span>
                                            ))}
                                        </div>
                                    )}
                                    <select
                                        value=""
                                        onChange={(e) => {
                                            if (e.target.value) {
                                                handleRagaChange(e.target.value);
                                                e.target.value = ''; // Reset after selection
                                            }
                                        }}
                                        className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                    >
                                        <option value="">{krithi.isRagamalika ? 'Add Raga...' : 'Select Raga...'}</option>
                                        {ragas
                                            .filter(r => !krithi.isRagamalika || !krithi.ragas?.some(selected => selected.id === r.id))
                                            .map(opt => (
                                                <option key={opt.id} value={opt.id}>{opt.name}</option>
                                            ))}
                                    </select>
                                    {!krithi.isRagamalika && (
                                        <span className="text-xs text-ink-500 mt-1 block">
                                            {krithi.ragas?.[0]?.name || 'No raga selected'}
                                        </span>
                                    )}
                                </div>
                                <SelectField
                                    label="Tala"
                                    options={talas}
                                    value={krithi.tala?.id || ''}
                                    onChange={handleTalaChange}
                                />
                                <SelectField
                                    label="Deity"
                                    options={deities}
                                    value={krithi.deity?.id || ''}
                                    onChange={handleDeityChange}
                                />
                                <SelectField
                                    label="Temple"
                                    options={temples}
                                    value={krithi.temple?.id || ''}
                                    onChange={handleTempleChange}
                                />
                            </div>
                        </div>

                        {/* Language & Form */}
                        <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6">
                            <SectionHeader title="Language & Form" />
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div>
                                    <label className="block text-sm font-semibold text-ink-900 mb-2">Primary Language</label>
                                    <select
                                        value={krithi.primaryLanguage}
                                        onChange={(e) => setKrithi({ ...krithi, primaryLanguage: e.target.value })}
                                        className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                    >
                                        {LANGUAGE_CODE_OPTIONS.map(opt => (
                                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                                        ))}
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-semibold text-ink-900 mb-2">Musical Form</label>
                                    <select
                                        value={krithi.musicalForm}
                                        onChange={(e) => setKrithi({ ...krithi, musicalForm: e.target.value as MusicalForm })}
                                        className="w-full h-12 px-4 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                    >
                                        <option value={MusicalForm.KRITHI}>Krithi</option>
                                        <option value={MusicalForm.VARNAM}>Varnam</option>
                                        <option value={MusicalForm.SWARAJATHI}>Swarajathi</option>
                                    </select>
                                    {(krithi.musicalForm === MusicalForm.VARNAM || krithi.musicalForm === MusicalForm.SWARAJATHI) && (
                                        <p className="text-xs text-blue-600 mt-1 flex items-center gap-1">
                                            <span className="material-symbols-outlined text-[14px]">info</span>
                                            Enables Notation Editor
                                        </p>
                                    )}
                                </div>
                                <div className="md:col-span-2">
                                    <CheckboxField
                                        label="Ragamalika (Multiple Ragas)"
                                        checked={krithi.isRagamalika}
                                        onChange={(checked: boolean) => setKrithi({ ...krithi, isRagamalika: checked })}
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Sections Management */}
                        <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6">
                            <div className="flex items-center justify-between mb-6 pb-2 border-b border-border-light">
                                <h3 className="font-display text-lg font-bold text-ink-900">Sections</h3>
                                <button
                                    onClick={() => {
                                        const newSection: KrithiSection = {
                                            id: `temp-section-${Date.now()}`,
                                            sectionType: 'PALLAVI',
                                            orderIndex: (krithi.sections?.length || 0) + 1
                                        };
                                        setKrithi(prev => ({
                                            ...prev,
                                            sections: [...(prev.sections || []), newSection]
                                        }));
                                    }}
                                    className="flex items-center gap-2 px-3 py-1.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors"
                                >
                                    <span className="material-symbols-outlined text-[16px]">add</span>
                                    Add Section
                                </button>
                            </div>
                            <div className="space-y-3">
                                {krithi.sections && krithi.sections.length > 0 ? (
                                    krithi.sections
                                        .sort((a, b) => a.orderIndex - b.orderIndex)
                                        .map((section, idx) => (
                                            <div key={section.id} className="border border-border-light rounded-lg p-4 bg-white">
                                                <div className="flex items-start justify-between mb-3">
                                                    <div className="flex-1">
                                                        <div className="flex items-center gap-3 mb-2">
                                                            <span className="text-sm font-semibold text-ink-900">
                                                                Section {idx + 1}
                                                            </span>
                                                            <span className="px-2 py-0.5 bg-primary-light text-primary rounded text-xs font-medium">
                                                                {formatSectionType(section.sectionType)}
                                                            </span>
                                                        </div>
                                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                                            <div>
                                                                <label className="block text-xs font-semibold text-ink-700 mb-1">Section Type</label>
                                                                <select
                                                                    value={section.sectionType}
                                                                    onChange={(e) => {
                                                                        const updated = krithi.sections?.map(s =>
                                                                            s.id === section.id ? { ...s, sectionType: e.target.value as any } : s
                                                                        ) || [];
                                                                        setKrithi(prev => ({ ...prev, sections: updated }));
                                                                    }}
                                                                    className="w-full h-10 px-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary text-sm"
                                                                >
                                                                    <option value="PALLAVI">Pallavi</option>
                                                                    <option value="ANUPALLAVI">Anupallavi</option>
                                                                    <option value="CHARANAM">Charanam</option>
                                                                    <option value="CHITTASWARAM">Chittaswaram</option>
                                                                    <option value="OTHER">Other</option>
                                                                </select>
                                                            </div>
                                                            <div>
                                                                <label className="block text-xs font-semibold text-ink-700 mb-1">Order Index</label>
                                                                <input
                                                                    type="number"
                                                                    value={section.orderIndex}
                                                                    onChange={(e) => {
                                                                        const order = parseInt(e.target.value) || 1;
                                                                        const updated = krithi.sections?.map(s =>
                                                                            s.id === section.id ? { ...s, orderIndex: order } : s
                                                                        ) || [];
                                                                        setKrithi(prev => ({ ...prev, sections: updated }));
                                                                    }}
                                                                    min="1"
                                                                    className="w-full h-10 px-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary text-sm"
                                                                />
                                                            </div>
                                                        </div>
                                                    </div>
                                                    <button
                                                        onClick={() => {
                                                            setKrithi(prev => ({
                                                                ...prev,
                                                                sections: prev.sections?.filter(s => s.id !== section.id) || []
                                                            }));
                                                        }}
                                                        className="ml-4 p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                                        title="Remove section"
                                                    >
                                                        <span className="material-symbols-outlined text-[20px]">delete</span>
                                                    </button>
                                                </div>
                                            </div>
                                        ))
                                ) : (
                                    <div className="p-4 text-center text-ink-500 text-sm bg-slate-50 rounded-lg border border-border-light">
                                        <p>No sections defined yet. Add sections to organize the lyrics.</p>
                                    </div>
                                )}
                            </div>
                            {krithi.sections && krithi.sections.length > 0 && (
                                <p className="text-xs text-ink-500 mt-4">
                                    <span className="material-symbols-outlined text-[14px] align-middle mr-1">info</span>
                                    Sections define the structure of the composition. Add lyrics for each section in the Lyrics tab.
                                </p>
                            )}
                        </div>

                        {/* Additional Metadata */}
                        <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6">
                            <SectionHeader title="Additional Information" />
                            <div className="space-y-6">
                                <TextareaField
                                    label="Sahitya Summary"
                                    value={krithi.sahityaSummary || ''}
                                    onChange={(v: string) => setKrithi({ ...krithi, sahityaSummary: v })}
                                    placeholder="Short prose summary or meaning of the composition"
                                    rows={4}
                                />
                                <TextareaField
                                    label="Notes"
                                    value={krithi.notes || ''}
                                    onChange={(v: string) => setKrithi({ ...krithi, notes: v })}
                                    placeholder="Additional notes, context, or metadata"
                                    rows={3}
                                />
                            </div>
                        </div>
                    </div>

                    {/* Right Column */}
                    <div className="space-y-6">
                        <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-5">
                            <h4 className="text-xs font-bold text-ink-500 uppercase tracking-wider mb-4">Status</h4>
                            <select
                                value={krithi.status}
                                onChange={e => setKrithi({ ...krithi, status: e.target.value as any })}
                                className="w-full p-2 border rounded"
                            >
                                <option value="draft">Draft</option>
                                <option value="in_review">In Review</option>
                                <option value="published">Published</option>
                                <option value="archived">Archived</option>
                            </select>
                            <div className="mt-2">
                                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border ${getWorkflowStateColor(krithi.status || 'draft')}`}>
                                    {formatWorkflowState(krithi.status || 'draft')}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {activeTab === 'Lyrics' && (
                <div className="space-y-6">
                    <div className="bg-surface-light border rounded-xl p-6">
                        <div className="flex items-center justify-between mb-6">
                            <SectionHeader title="Lyric Variants" />
                            <button
                                onClick={() => {
                                    const newVariant: KrithiLyricVariant = {
                                        id: `temp-${Date.now()}`,
                                        language: krithi.primaryLanguage || 'te',
                                        script: 'devanagari',
                                        sections: []
                                    };
                                    setKrithi(prev => ({
                                        ...prev,
                                        lyricVariants: [...(prev.lyricVariants || []), newVariant]
                                    }));
                                    setEditingVariantId(newVariant.id);
                                }}
                                className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors"
                            >
                                <span className="material-symbols-outlined text-[18px]">add</span>
                                Add Variant
                            </button>
                        </div>

                        {!krithiId && (
                            <div className="mb-4 p-4 bg-amber-50 border border-amber-200 rounded-lg">
                                <p className="text-sm text-amber-800">
                                    <span className="material-symbols-outlined text-[18px] align-middle mr-1">info</span>
                                    Save the krithi first to persist lyric variants.
                                </p>
                            </div>
                        )}

                        {krithi.lyricVariants && krithi.lyricVariants.length > 0 ? (
                            <div className="space-y-4">
                                {krithi.lyricVariants.map((variant, idx) => (
                                    <div key={variant.id} className="border border-border-light rounded-lg p-4 bg-white">
                                        <div className="flex items-start justify-between mb-4">
                                            <div className="flex-1">
                                                <div className="flex items-center gap-3 mb-2">
                                                    <span className="font-semibold text-ink-900">Variant {idx + 1}</span>
                                                    <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs">
                                                        {formatLanguageCode(variant.language)}
                                                    </span>
                                                    {variant.script && (
                                                    <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs">
                                                        {formatScriptCode(variant.script)}
                                                    </span>
                                                    )}
                                                    {variant.transliterationScheme && (
                                                        <span className="px-2 py-0.5 bg-purple-100 text-purple-600 rounded text-xs" title="Transliteration Scheme">
                                                            {variant.transliterationScheme}
                                                        </span>
                                                    )}
                                                    {variant.sampradaya && (
                                                        <span className="px-2 py-0.5 bg-blue-100 text-blue-600 rounded text-xs">
                                                            {variant.sampradaya.name}
                                                        </span>
                                                    )}
                                                </div>
                                                {editingVariantId === variant.id ? (
                                                    <div className="space-y-4 mt-4">
                                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                                            <div>
                                                                <label className="block text-sm font-semibold text-ink-900 mb-2">Language</label>
                                                                <select
                                                                    value={variant.language}
                                                                    onChange={(e) => {
                                                                        const updated = krithi.lyricVariants?.map(v =>
                                                                            v.id === variant.id ? { ...v, language: e.target.value } : v
                                                                        ) || [];
                                                                        setKrithi(prev => ({ ...prev, lyricVariants: updated }));
                                                                    }}
                                                                    className="w-full h-10 px-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary"
                                                                >
                                                                    {LANGUAGE_CODE_OPTIONS.map(opt => (
                                                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                                                    ))}
                                                                </select>
                                                            </div>
                                                            <div>
                                                                <label className="block text-sm font-semibold text-ink-900 mb-2">Script</label>
                                                                <select
                                                                    value={variant.script || 'devanagari'}
                                                                    onChange={(e) => {
                                                                        const updated = krithi.lyricVariants?.map(v =>
                                                                            v.id === variant.id ? { ...v, script: e.target.value } : v
                                                                        ) || [];
                                                                        setKrithi(prev => ({ ...prev, lyricVariants: updated }));
                                                                    }}
                                                                    className="w-full h-10 px-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary"
                                                                >
                                                                    {SCRIPT_CODE_OPTIONS.map(opt => (
                                                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                                                    ))}
                                                                </select>
                                                            </div>
                                                        </div>
                                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                                            <div>
                                                                <label className="block text-sm font-semibold text-ink-900 mb-2">Transliteration Scheme</label>
                                                                <select
                                                                    value={variant.transliterationScheme || ''}
                                                                    onChange={(e) => {
                                                                        const updated = krithi.lyricVariants?.map(v =>
                                                                            v.id === variant.id ? { ...v, transliterationScheme: e.target.value || undefined } : v
                                                                        ) || [];
                                                                        setKrithi(prev => ({ ...prev, lyricVariants: updated }));
                                                                    }}
                                                                    className="w-full h-10 px-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary"
                                                                >
                                                                    <option value="">None (Native Script)</option>
                                                                    <option value="IAST">IAST (International Alphabet of Sanskrit Transliteration)</option>
                                                                    <option value="ISO-15919">ISO-15919 (ISO Standard)</option>
                                                                    <option value="ITRANS">ITRANS</option>
                                                                    <option value="Harvard-Kyoto">Harvard-Kyoto</option>
                                                                    <option value="Velthuis">Velthuis</option>
                                                                    <option value="SLP1">SLP1 (Sanskrit Library Phonetic)</option>
                                                                    <option value="WX">WX Notation</option>
                                                                </select>
                                                                <p className="text-xs text-ink-500 mt-1">Select the transliteration scheme used for this variant (if applicable)</p>
                                                            </div>
                                                            <div>
                                                                <label className="block text-sm font-semibold text-ink-900 mb-2">Sampradaya</label>
                                                                <select
                                                                    value={variant.sampradaya?.id || ''}
                                                                    onChange={(e) => {
                                                                        const sampradaya = sampradayas.find(s => s.id === e.target.value);
                                                                        const updated = krithi.lyricVariants?.map(v =>
                                                                            v.id === variant.id ? { ...v, sampradaya } : v
                                                                        ) || [];
                                                                        setKrithi(prev => ({ ...prev, lyricVariants: updated }));
                                                                    }}
                                                                    className="w-full h-10 px-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary"
                                                                >
                                                                    <option value="">None</option>
                                                                    {sampradayas.map(s => (
                                                                        <option key={s.id} value={s.id}>{s.name}</option>
                                                                    ))}
                                                                </select>
                                                            </div>
                                                        </div>

                                                        {/* Sections Editor */}
                                                        <div className="space-y-3">
                                                            <label className="block text-sm font-semibold text-ink-900">Sections</label>
                                                            {krithi.sections && krithi.sections.length > 0 ? (
                                                                krithi.sections.map(section => {
                                                                    const lyricSection = variant.sections.find(s => s.sectionId === section.id);
                                                                    return (
                                                                        <div key={section.id} className="p-3 bg-slate-50 rounded-lg border border-border-light">
                                                                            <div className="flex items-center justify-between mb-2">
                                                                                <span className="text-sm font-semibold text-ink-900">
                                                                                    {formatSectionType(section.sectionType)}
                                                                                </span>
                                                                                {section.label && (
                                                                                    <span className="text-xs text-ink-500">{section.label}</span>
                                                                                )}
                                                                            </div>
                                                                            <textarea
                                                                                value={lyricSection?.text || ''}
                                                                                onChange={(e) => {
                                                                                    const text = e.target.value;
                                                                                    const updatedSections = variant.sections.filter(s => s.sectionId !== section.id);
                                                                                    if (text.trim()) {
                                                                                        updatedSections.push({ sectionId: section.id, text });
                                                                                    }
                                                                                    const updatedVariants = krithi.lyricVariants?.map(v =>
                                                                                        v.id === variant.id ? { ...v, sections: updatedSections } : v
                                                                                    ) || [];
                                                                                    setKrithi(prev => ({ ...prev, lyricVariants: updatedVariants }));
                                                                                }}
                                                                                placeholder={`Enter ${formatSectionType(section.sectionType).toLowerCase()} text...`}
                                                                                rows={6}
                                                                                className="w-full px-4 py-3 rounded-lg bg-white border-2 border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-primary transition-all resize-y font-mono text-sm leading-relaxed"
                                                                            />
                                                                        </div>
                                                                    );
                                                                })
                                                            ) : (
                                                                <p className="text-sm text-ink-500 italic">
                                                                    No sections defined. Add sections in the Metadata tab first.
                                                                </p>
                                                            )}
                                                        </div>

                                                        <div className="flex gap-2">
                                                            <button
                                                                onClick={() => setEditingVariantId(null)}
                                                                className="px-4 py-2 bg-slate-100 text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-200 transition-colors"
                                                            >
                                                                Done
                                                            </button>
                                                            <button
                                                                onClick={() => {
                                                                    setKrithi(prev => ({
                                                                        ...prev,
                                                                        lyricVariants: prev.lyricVariants?.filter(v => v.id !== variant.id) || []
                                                                    }));
                                                                    setEditingVariantId(null);
                                                                }}
                                                                className="px-4 py-2 bg-red-50 text-red-700 rounded-lg text-sm font-medium hover:bg-red-100 transition-colors"
                                                            >
                                                                Remove
                                                            </button>
                                                        </div>
                                                    </div>
                                                ) : (
                                                    <div>
                                                        {variant.sections.length > 0 ? (
                                                            <div className="space-y-2 mt-2">
                                                                {variant.sections.map((lyricSection, secIdx) => {
                                                                    const section = krithi.sections?.find(s => s.id === lyricSection.sectionId);
                                                                    return (
                                                                        <div key={secIdx} className="p-2 bg-slate-50 rounded text-sm">
                                                                            <span className="font-semibold text-ink-700">
                                                                                {section ? formatSectionType(section.sectionType) : 'Section'}:
                                                                            </span>
                                                                            <p className="text-ink-600 mt-1 whitespace-pre-wrap">{lyricSection.text}</p>
                                                                        </div>
                                                                    );
                                                                })}
                                                            </div>
                                                        ) : (
                                                            <p className="text-sm text-ink-400 italic mt-2">No sections added yet.</p>
                                                        )}
                                                        <button
                                                            onClick={() => setEditingVariantId(variant.id)}
                                                            className="mt-3 text-sm text-primary hover:text-primary-dark font-medium"
                                                        >
                                                            Edit Variant
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className="p-8 text-center text-ink-500">
                                <p>No lyric variants added yet. Click "Add Variant" to get started.</p>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {activeTab === 'Tags' && (
                <div className="space-y-6">
                    <div className="bg-surface-light border rounded-xl p-6">
                        <SectionHeader title="Assigned Tags" />
                        <div className="flex flex-wrap gap-2 mb-6">
                            {krithi.tags && krithi.tags.length > 0 ? (
                                krithi.tags.map(t => (
                                    <span
                                        key={t.id}
                                        className="inline-flex items-center gap-2 px-3 py-1.5 bg-primary-light text-primary rounded-full text-sm border border-primary/20"
                                    >
                                        <span>{t.displayName}</span>
                                        <span className="text-xs text-ink-500">({t.category})</span>
                                        <button
                                            type="button"
                                            onClick={() => {
                                                setKrithi(prev => ({
                                                    ...prev,
                                                    tags: prev.tags?.filter(tag => tag.id !== t.id) || []
                                                }));
                                            }}
                                            className="ml-1 hover:text-primary-dark transition-colors"
                                        >
                                            <span className="material-symbols-outlined text-[16px]">close</span>
                                        </button>
                                    </span>
                                ))
                            ) : (
                                <p className="text-ink-400 text-sm">No tags assigned.</p>
                            )}
                        </div>

                        {/* Add Tag Section */}
                        <div className="space-y-4">
                            <label className="block text-sm font-semibold text-ink-900">Add Tag</label>
                            <div className="relative">
                                <input
                                    type="text"
                                    placeholder="Search tags by name or category..."
                                    className="w-full h-12 px-4 pr-10 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                    onFocus={(e) => {
                                        // Show dropdown on focus
                                        const dropdown = e.target.nextElementSibling as HTMLElement;
                                        if (dropdown) dropdown.classList.remove('hidden');
                                    }}
                                    onBlur={(e) => {
                                        // Hide dropdown after a delay to allow clicking
                                        setTimeout(() => {
                                            const dropdown = e.target.nextElementSibling as HTMLElement;
                                            if (dropdown) dropdown.classList.add('hidden');
                                        }, 200);
                                    }}
                                    onChange={(e) => {
                                        const searchTerm = e.target.value.toLowerCase();
                                        const dropdown = e.target.nextElementSibling as HTMLElement;
                                        if (dropdown) {
                                            const items = dropdown.querySelectorAll('[data-tag-id]');
                                            items.forEach(item => {
                                                const text = item.textContent?.toLowerCase() || '';
                                                const parent = item.parentElement;
                                                if (parent) {
                                                    if (text.includes(searchTerm) || searchTerm === '') {
                                                        parent.classList.remove('hidden');
                                                    } else {
                                                        parent.classList.add('hidden');
                                                    }
                                                }
                                            });
                                        }
                                    }}
                                />
                                <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-ink-400 text-[20px]">search</span>
                                
                                {/* Tag Dropdown */}
                                <div className="absolute z-10 w-full mt-1 bg-white border border-border-light rounded-lg shadow-lg max-h-60 overflow-y-auto hidden">
                                    {allTags
                                        .filter(tag => !krithi.tags?.some(assigned => assigned.id === tag.id))
                                        .map(tag => (
                                            <button
                                                key={tag.id}
                                                type="button"
                                                data-tag-id={tag.id}
                                                onClick={() => {
                                                    setKrithi(prev => ({
                                                        ...prev,
                                                        tags: [...(prev.tags || []), tag]
                                                    }));
                                                    // Clear search and hide dropdown
                                                    const input = document.activeElement as HTMLInputElement;
                                                    if (input) {
                                                        input.value = '';
                                                        input.blur();
                                                    }
                                                }}
                                                className="w-full text-left px-4 py-2 hover:bg-slate-50 transition-colors border-b border-border-light last:border-0"
                                            >
                                                <div className="flex items-center justify-between">
                                                    <div className="flex items-center gap-2">
                                                        <span className="font-medium text-ink-900">{tag.displayName}</span>
                                                        {tag.slug && (
                                                            <span className="text-xs text-ink-400 font-mono">#{tag.slug}</span>
                                                        )}
                                                    </div>
                                                    <span className="text-xs text-ink-500 bg-slate-100 px-2 py-0.5 rounded">{tag.category}</span>
                                                </div>
                                            </button>
                                        ))}
                                    {allTags.filter(tag => !krithi.tags?.some(assigned => assigned.id === tag.id)).length === 0 && (
                                        <div className="px-4 py-2 text-sm text-ink-500">No available tags to add</div>
                                    )}
                                </div>
                            </div>
                            <p className="text-xs text-ink-500">Tags are from a controlled vocabulary. Search and select to add.</p>
                        </div>
                    </div>
                </div>
            )}

            {/* 2.5 NOTATION TAB */}
            {activeTab === 'Notation' && krithiId && (
                <NotationTab krithiId={krithiId} musicalForm={krithi.musicalForm as MusicalForm} />
            )}

            {activeTab === 'Audit' && (
                <div className="space-y-6">
                    <div className="bg-surface-light border rounded-xl p-6">
                        <SectionHeader title="Audit History" />
                        {!krithiId ? (
                            <div className="p-8 text-center text-ink-500">
                                <p>Save the krithi first to view audit logs.</p>
                            </div>
                        ) : auditLogs.length === 0 ? (
                            <div className="p-8 text-center text-ink-500">
                                <p>No audit logs found for this krithi.</p>
                            </div>
                        ) : (
                            <div className="space-y-4">
                                {auditLogs.map((log) => (
                                    <div
                                        key={log.id}
                                        className="p-4 bg-white border border-border-light rounded-lg hover:shadow-sm transition-shadow"
                                    >
                                        <div className="flex items-start justify-between mb-2">
                                            <div className="flex items-center gap-3">
                                                <div className="w-10 h-10 rounded-full bg-primary-light flex items-center justify-center">
                                                    <span className="material-symbols-outlined text-primary text-[20px]">
                                                        {log.action === 'CREATE' ? 'add_circle' :
                                                         log.action === 'UPDATE' ? 'edit' :
                                                         log.action === 'DELETE' ? 'delete' : 'history'}
                                                    </span>
                                                </div>
                                                <div>
                                                    <div className="flex items-center gap-2">
                                                        <span className="font-semibold text-ink-900">{log.action}</span>
                                                        <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs font-medium">
                                                            {log.entityType}
                                                        </span>
                                                    </div>
                                                    <p className="text-sm text-ink-500 mt-0.5">
                                                        by {log.actor || 'Unknown'}
                                                    </p>
                                                </div>
                                            </div>
                                            <span className="text-xs text-ink-500 font-mono">
                                                {new Date(log.timestamp).toLocaleString()}
                                            </span>
                                        </div>
                                        {log.diff && Object.keys(log.diff).length > 0 && (
                                            <div className="mt-3 pt-3 border-t border-border-light">
                                                <p className="text-xs font-semibold text-ink-500 mb-2 uppercase tracking-wide">Field Changes:</p>
                                                <div className="space-y-2">
                                                    {Object.entries(log.diff).map(([field, change]) => (
                                                        <div key={field} className="text-xs">
                                                            <span className="font-semibold text-ink-700">{field}:</span>
                                                            <div className="ml-4 mt-1 space-y-1">
                                                                {change.before !== undefined && (
                                                                    <div className="text-red-600">
                                                                        <span className="font-medium">Before:</span> {String(change.before || '(empty)')}
                                                                    </div>
                                                                )}
                                                                {change.after !== undefined && (
                                                                    <div className="text-green-600">
                                                                        <span className="font-medium">After:</span> {String(change.after || '(empty)')}
                                                                    </div>
                                                                )}
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                        {log.entityId && (
                                            <div className="mt-2 text-xs text-ink-400">
                                                Entity ID: <span className="font-mono">{log.entityId}</span>
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            )}
                </>
            )}
        </div>
        </>
    );
};

export default KrithiEditor;

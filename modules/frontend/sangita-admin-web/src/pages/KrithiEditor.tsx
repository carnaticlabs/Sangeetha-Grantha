import React, { useState, useEffect, useRef } from 'react';
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
    getKrithiSections,
    saveKrithiSections,
    getKrithiLyricVariants,
    getKrithiTags,
    createLyricVariant,
    updateLyricVariant,
    saveVariantSections
} from '../api/client';
import { useToast, ToastContainer } from '../components/Toast';
import { TransliterationModal } from '../components/TransliterationModal';
import { transliterateContent } from '../api/client';

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
    const [activeTab, setActiveTab] = useState<'Metadata' | 'Structure' | 'Lyrics' | 'Notation' | 'Tags' | 'Audit'>('Metadata');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [sectionsLoading, setSectionsLoading] = useState(false);
    const [lyricVariantsLoading, setLyricVariantsLoading] = useState(false);
    const [tagsLoading, setTagsLoading] = useState(false);
    const [tagSearchTerm, setTagSearchTerm] = useState('');
    const [showTagDropdown, setShowTagDropdown] = useState(false);
    const sectionsLoadedRef = useRef(false);

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

    // AI Modal State
    const [isTransliterationModalOpen, setIsTransliterationModalOpen] = useState(false);
    const [transliterationInitialContent, setTransliterationInitialContent] = useState('');

    // Load initial reference data (tags will be loaded when Tags tab is clicked)
    useEffect(() => {
        const loadRefs = async () => {
            try {
                const [c, r, t, d, tm, s] = await Promise.all([
                    getComposers(), getRagas(), getTalas(), getDeities(), getTemples(), getSampradayas()
                ]);
                setComposers(c);
                setRagas(r);
                setTalas(t);
                setDeities(d);
                setTemples(tm);
                setSampradayas(s);
                // Tags will be loaded when Tags tab is clicked
            } catch (e) {
                console.error("Failed to load reference data", e);
            }
        };
        loadRefs();
    }, []);

    // Helper to map backend DTO (with IDs) to frontend format (with objects)
    const mapKrithiDtoToDetail = (dto: any): Partial<KrithiDetail> => {
        // Handle workflowState: backend uses DRAFT/IN_REVIEW/PUBLISHED/ARCHIVED (uppercase)
        // Frontend expects same format but ensure it's uppercase
        let workflowState = 'DRAFT';
        if (dto.workflowState) {
            workflowState = typeof dto.workflowState === 'string' 
                ? dto.workflowState.toUpperCase().replace(/-/g, '_')
                : dto.workflowState;
        }

        // Map ragas - backend may have primaryRagaId or ragaIds array
        let ragaList: Raga[] = [];
        if (dto.ragas && Array.isArray(dto.ragas)) {
            // If backend returns full raga objects
            ragaList = dto.ragas;
        } else if (dto.ragaIds && Array.isArray(dto.ragaIds)) {
            // If backend returns raga IDs array
            ragaList = dto.ragaIds.map((id: string) => ragas.find(r => r.id === id)).filter(Boolean) as Raga[];
        } else if (dto.primaryRagaId) {
            // Fallback to primaryRagaId
            const primaryRaga = ragas.find(r => r.id === dto.primaryRagaId);
            if (primaryRaga) ragaList = [primaryRaga];
        }

        return {
            id: dto.id,
            title: dto.title || '',
            incipit: dto.incipit,
            composer: dto.composer ? dto.composer : (dto.composerId ? composers.find(c => c.id === dto.composerId) : undefined),
            tala: dto.tala ? dto.tala : (dto.talaId ? talas.find(t => t.id === dto.talaId) : undefined),
            ragas: ragaList,
            deity: dto.deity ? dto.deity : (dto.deityId ? deities.find(d => d.id === dto.deityId) : undefined),
            temple: dto.temple ? dto.temple : (dto.templeId ? temples.find(t => t.id === dto.templeId) : undefined),
            primaryLanguage: dto.primaryLanguage?.toLowerCase() || dto.primaryLanguage || 'te',
            musicalForm: dto.musicalForm || MusicalForm.KRITHI,
            isRagamalika: dto.isRagamalika || false,
            status: workflowState as any,
            sahityaSummary: dto.sahityaSummary,
            notes: dto.notes,
            sections: dto.sections || [],
            lyricVariants: dto.lyricVariants || [],
            tags: dto.tags || []
        };
    };

    // Store the raw DTO to remap when reference data loads
    const [rawKrithiDto, setRawKrithiDto] = useState<any>(null);

    useEffect(() => {
        if (!isNew && krithiId) {
            setLoading(true);
            getKrithi(krithiId)
                .then((dto) => {
                    setRawKrithiDto(dto);
                    // Map immediately if reference data is available
                    if (composers.length > 0 && ragas.length > 0 && talas.length > 0) {
                        const mapped = mapKrithiDtoToDetail(dto);
                        // Sections will be loaded when Structure tab is clicked
                        setKrithi(mapped);
                    }
                })
                .catch(err => alert("Failed to load krithi: " + err.message))
                .finally(() => setLoading(false));

            // Load audit logs for this krithi
            getKrithiAuditLogs(krithiId)
                .then(setAuditLogs)
                .catch(err => console.error("Failed to load audit logs:", err));
        }
    }, [krithiId, isNew]);

    // Remap krithi when reference data becomes available
    // Preserve sections and lyricVariants if they've already been loaded (these come from separate endpoints)
    useEffect(() => {
        if (rawKrithiDto && composers.length > 0 && ragas.length > 0 && talas.length > 0) {
            const mapped = mapKrithiDtoToDetail(rawKrithiDto);
            setKrithi(prev => ({
                ...mapped,
                // Preserve sections if they've already been loaded from the sections endpoint
                // (either they have content, or the ref flag indicates they were loaded - even if empty)
                sections: sectionsLoadedRef.current || (prev.sections && prev.sections.length > 0) ? prev.sections : mapped.sections,
                // Preserve lyricVariants if they've already been loaded from the variants endpoint
                lyricVariants: prev.lyricVariants && prev.lyricVariants.length > 0 ? prev.lyricVariants : mapped.lyricVariants
            }));
        }
    }, [rawKrithiDto, composers, ragas, talas, deities, temples]);

    // Load sections automatically when krithi is loaded (if not a new krithi)
    useEffect(() => {
        if (!isNew && krithiId && !sectionsLoadedRef.current && !sectionsLoading) {
            // Load sections after a short delay to ensure krithi state is set
            const timer = setTimeout(async () => {
                // Check again in case sections were loaded during the delay
                if (sectionsLoadedRef.current) return;
                
                setSectionsLoading(true);
                try {
                    const sections = await getKrithiSections(krithiId);
                    sectionsLoadedRef.current = true;
                    setKrithi(prev => ({
                        ...prev,
                        sections: sections.map(s => ({
                            id: s.id,
                            sectionType: s.sectionType as any,
                            orderIndex: s.orderIndex,
                            label: s.label || undefined
                        }))
                    }));
                } catch (err: any) {
                    console.error('Failed to load sections:', err);
                    // Don't show error toast on initial load - sections might not exist yet
                } finally {
                    setSectionsLoading(false);
                }
            }, 100);
            return () => clearTimeout(timer);
        }
    }, [krithiId, isNew]);

    // Reset sections loaded flag when krithiId changes
    useEffect(() => {
        sectionsLoadedRef.current = false;
    }, [krithiId]);

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
            const payload: any = {};
            
            // Only include fields that have values
            if (krithi.title) payload.title = krithi.title;
            if (krithi.incipit !== undefined) payload.incipit = krithi.incipit || null;
            if (krithi.composer?.id) payload.composerId = krithi.composer.id;
            if (krithi.tala?.id) payload.talaId = krithi.tala.id;
            if (krithi.primaryLanguage) {
                // Convert to uppercase enum value (e.g., 'te' -> 'TE')
                payload.primaryLanguage = krithi.primaryLanguage.toUpperCase();
            }
            if (krithi.ragas && krithi.ragas.length > 0) {
                payload.ragaIds = krithi.ragas.map(r => r.id);
            }
            if (krithi.deity?.id) payload.deityId = krithi.deity.id;
            if (krithi.temple?.id) payload.templeId = krithi.temple.id;
            if (krithi.musicalForm) {
                // Ensure enum value is uppercase
                payload.musicalForm = typeof krithi.musicalForm === 'string' 
                    ? krithi.musicalForm.toUpperCase() 
                    : krithi.musicalForm;
            }
            if (krithi.isRagamalika !== undefined) payload.isRagamalika = krithi.isRagamalika;
            if (krithi.sahityaSummary) payload.sahityaSummary = krithi.sahityaSummary;
            if (krithi.notes) payload.notes = krithi.notes;
            if (krithi.status) {
                // Convert status to uppercase enum value (e.g., 'DRAFT', 'IN_REVIEW')
                const statusValue = typeof krithi.status === 'string' 
                    ? krithi.status.toUpperCase().replace(/-/g, '_') 
                    : krithi.status;
                payload.workflowState = statusValue;
            }
            // Include tagIds if tags array exists (even if empty, to clear tags)
            if (krithi.tags !== undefined) {
                payload.tagIds = krithi.tags.map(t => t.id);
            }

            let savedKrithiId = krithiId;

            // Create or update krithi
            if (isNew) {
                // For create, ensure required fields are present
                if (!payload.title || !payload.composerId || !payload.primaryLanguage) {
                    toast.error('Title, Composer, and Primary Language are required');
                    setSaving(false);
                    return;
                }
                // Create request requires non-null values
                const createPayload = {
                    title: payload.title,
                    incipit: payload.incipit || null,
                    composerId: payload.composerId,
                    primaryLanguage: payload.primaryLanguage,
                    musicalForm: payload.musicalForm || 'KRITHI',
                    talaId: payload.talaId || null,
                    primaryRagaId: payload.ragaIds?.[0] || null,
                    deityId: payload.deityId || null,
                    templeId: payload.templeId || null,
                    isRagamalika: payload.isRagamalika || false,
                    ragaIds: payload.ragaIds || [],
                    sahityaSummary: payload.sahityaSummary || null,
                    notes: payload.notes || null,
                };
                const res = await createKrithi(createPayload);
                savedKrithiId = res.id;
                navigate(`/krithis/${res.id}`, { replace: true });
            } else if (krithiId) {
                await updateKrithi(krithiId, payload);
            }

            // Save sections if krithi ID is available (even if empty, to clear existing sections)
            if (savedKrithiId && krithi.sections !== undefined) {
                try {
                    // Map all sections to the API format (ignore temp IDs, API will assign new ones)
                    const sectionsToSave = (krithi.sections || []).map(s => ({
                        sectionType: s.sectionType,
                        orderIndex: s.orderIndex,
                        label: null
                    }));

                    await saveKrithiSections(savedKrithiId, sectionsToSave);
                } catch (err: any) {
                    console.error('Failed to save sections:', err);
                    toast.warning('Krithi saved, but sections may not have been updated: ' + (err.message || 'Unknown error'));
                }
            }

            // Reload the krithi to get updated data with full objects
            let reloadedSections: Array<{ id: string; sectionType: string; orderIndex: number }> = [];
            if (savedKrithiId) {
                try {
                    const reloaded = await getKrithi(savedKrithiId);
                    const mapped = mapKrithiDtoToDetail(reloaded);
                    // Load sections from the dedicated endpoint
                    try {
                        const sections = await getKrithiSections(savedKrithiId);
                        reloadedSections = sections.map(s => ({
                            id: s.id,
                            sectionType: s.sectionType,
                            orderIndex: s.orderIndex
                        }));
                        mapped.sections = reloadedSections.map(s => ({
                            id: s.id,
                            sectionType: s.sectionType as any,
                            orderIndex: s.orderIndex
                        }));
                    } catch (err: any) {
                        console.warn('Failed to reload sections after save:', err);
                        // Keep existing sections if reload fails
                        if (krithi.sections && krithi.sections.length > 0) {
                            reloadedSections = krithi.sections.map(s => ({
                                id: s.id,
                                sectionType: s.sectionType,
                                orderIndex: s.orderIndex
                            }));
                            mapped.sections = krithi.sections;
                        }
                    }
                    // Load tags from the dedicated endpoint
                    try {
                        const tags = await getKrithiTags(savedKrithiId);
                        mapped.tags = tags;
                    } catch (err: any) {
                        console.warn('Failed to reload tags after save:', err);
                        // Keep existing tags if reload fails
                        if (krithi.tags && krithi.tags.length > 0) {
                            mapped.tags = krithi.tags;
                        }
                    }
                    // Load lyric variants from the dedicated endpoint
                    try {
                        const variantsData = await getKrithiLyricVariants(savedKrithiId);
                        mapped.lyricVariants = variantsData.map(v => ({
                            id: v.variant.id,
                            language: v.variant.language.toLowerCase(),
                            script: v.variant.script.toLowerCase(),
                            transliterationScheme: v.variant.transliterationScheme,
                            sampradaya: v.variant.sampradayaId ? sampradayas.find(s => s.id === v.variant.sampradayaId) : undefined,
                            sections: v.sections.map(s => ({
                                sectionId: s.sectionId,
                                text: s.text
                            }))
                        }));
                    } catch (err: any) {
                        console.warn('Failed to reload lyric variants after save:', err);
                        // Keep existing variants if reload fails
                        if (krithi.lyricVariants && krithi.lyricVariants.length > 0) {
                            mapped.lyricVariants = krithi.lyricVariants;
                        }
                    }
                    setKrithi(mapped);
                } catch (err: any) {
                    console.error('Failed to reload krithi after save:', err);
                    // Fallback to existing sections if reload fails
                    if (krithi.sections && krithi.sections.length > 0) {
                        reloadedSections = krithi.sections.map(s => ({
                            id: s.id,
                            sectionType: s.sectionType,
                            orderIndex: s.orderIndex
                        }));
                    }
                }
            }

            // Save lyric variants if krithi ID is available
            if (savedKrithiId && krithi.lyricVariants && krithi.lyricVariants.length > 0) {
                try {
                    // Create a mapping from old section IDs to new section IDs
                    // Map by sectionType and orderIndex since those are stable identifiers
                    const sectionIdMap = new Map<string, string>();
                    if (krithi.sections && reloadedSections.length > 0) {
                        for (const oldSection of krithi.sections) {
                            const newSection = reloadedSections.find(
                                s => s.sectionType === oldSection.sectionType && s.orderIndex === oldSection.orderIndex
                            );
                            if (newSection) {
                                sectionIdMap.set(oldSection.id, newSection.id);
                            }
                        }
                    }
                    
                    // Get valid section IDs from the reloaded sections (sections that were just saved)
                    const validSectionIds = new Set(
                        reloadedSections.map(s => s.id).filter(id => !id.startsWith('temp-'))
                    );

                    for (const variant of krithi.lyricVariants) {
                        if (variant.id.startsWith('temp-')) {
                            // Create new variant
                            // Convert language and script to uppercase enum values
                            const languageEnum = typeof variant.language === 'string' 
                                ? variant.language.toUpperCase() 
                                : variant.language;
                            const scriptEnum = typeof variant.script === 'string' 
                                ? variant.script.toUpperCase() 
                                : variant.script;
                            
                            const variantPayload = {
                                language: languageEnum,
                                script: scriptEnum,
                                transliterationScheme: variant.transliterationScheme,
                                sampradayaId: variant.sampradaya?.id,
                                isPrimary: false
                            };
                            const createdVariant = await createLyricVariant(savedKrithiId, variantPayload);

                            // Save sections for this variant (filter out empty text and map to new section IDs)
                            if (variant.sections && variant.sections.length > 0) {
                                const validSections = variant.sections
                                    .filter(s => s.text && s.text.trim())
                                    .map(s => {
                                        // Map old section ID to new section ID if needed
                                        const newSectionId = sectionIdMap.get(s.sectionId) || s.sectionId;
                                        return { sectionId: newSectionId, text: s.text };
                                    })
                                    .filter(s => validSectionIds.has(s.sectionId));
                                
                                if (validSections.length > 0) {
                                    console.log('Saving variant sections:', validSections);
                                    await saveVariantSections(createdVariant.id, validSections);
                                } else {
                                    console.warn('No valid sections to save for variant:', variant.id, 'Original sections:', variant.sections, 'Valid IDs:', Array.from(validSectionIds));
                                }
                            }
                        } else {
                            // Update existing variant
                            // Convert language and script to uppercase enum values
                            const languageEnum = typeof variant.language === 'string' 
                                ? variant.language.toUpperCase() 
                                : variant.language;
                            const scriptEnum = typeof variant.script === 'string' 
                                ? variant.script.toUpperCase() 
                                : variant.script;
                            
                            const variantPayload = {
                                language: languageEnum,
                                script: scriptEnum,
                                transliterationScheme: variant.transliterationScheme,
                                sampradayaId: variant.sampradaya?.id,
                                isPrimary: false
                            };
                            await updateLyricVariant(variant.id, variantPayload);

                            // Save sections for this variant (filter out empty text and map to new section IDs)
                            if (variant.sections && variant.sections.length > 0) {
                                const validSections = variant.sections
                                    .filter(s => s.text && s.text.trim())
                                    .map(s => {
                                        // Map old section ID to new section ID if needed
                                        const newSectionId = sectionIdMap.get(s.sectionId) || s.sectionId;
                                        return { sectionId: newSectionId, text: s.text };
                                    })
                                    .filter(s => validSectionIds.has(s.sectionId));
                                
                                if (validSections.length > 0) {
                                    console.log('Saving variant sections:', validSections);
                                    await saveVariantSections(variant.id, validSections);
                                } else {
                                    console.warn('No valid sections to save for variant:', variant.id, 'Original sections:', variant.sections, 'Valid IDs:', Array.from(validSectionIds));
                                }
                            }
                        }
                    }
                } catch (err: any) {
                    console.error('Failed to save lyric variants:', err);
                    const errorMessage = err.message || err.toString();
                    console.error('Error details:', errorMessage);
                    toast.warning(`Krithi saved, but lyric variants may not have been updated: ${errorMessage}`);
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
                                {['Metadata', 'Structure', 'Lyrics', 'Tags', 'Audit'].map((tab) => {
                                    if (tab === 'Notation' && krithi.musicalForm !== MusicalForm.VARNAM && krithi.musicalForm !== MusicalForm.SWARAJATHI) return null;
                                    return (
                                        <button
                                            key={tab}
                                            onClick={async () => {
                                                setActiveTab(tab as any);
                                                // Load sections when Structure tab is clicked (if not already loaded)
                                                if (tab === 'Structure' && krithiId && !sectionsLoadedRef.current && !sectionsLoading) {
                                                    setSectionsLoading(true);
                                                    try {
                                                        const sections = await getKrithiSections(krithiId);
                                                        sectionsLoadedRef.current = true;
                                                        setKrithi(prev => ({
                                                            ...prev,
                                                            sections: sections.map(s => ({
                                                                id: s.id,
                                                                sectionType: s.sectionType as any,
                                                                orderIndex: s.orderIndex,
                                                                label: s.label || undefined
                                                            }))
                                                        }));
                                                    } catch (err: any) {
                                                        console.error('Failed to load sections:', err);
                                                        toast.error('Failed to load sections: ' + (err.message || 'Unknown error'));
                                                    } finally {
                                                        setSectionsLoading(false);
                                                    }
                                                }
                                                // Load lyric variants when Lyrics tab is clicked
                                                if (tab === 'Lyrics' && krithiId && (!krithi.lyricVariants || krithi.lyricVariants.length === 0)) {
                                                    setLyricVariantsLoading(true);
                                                    try {
                                                        const variantsData = await getKrithiLyricVariants(krithiId);
                                                        setKrithi(prev => ({
                                                            ...prev,
                                                            lyricVariants: variantsData.map(v => ({
                                                                id: v.variant.id,
                                                                language: v.variant.language.toLowerCase(),
                                                                script: v.variant.script.toLowerCase(),
                                                                transliterationScheme: v.variant.transliterationScheme,
                                                                sampradaya: v.variant.sampradayaId ? sampradayas.find(s => s.id === v.variant.sampradayaId) : undefined,
                                                                sections: v.sections.map(s => ({
                                                                    sectionId: s.sectionId,
                                                                    text: s.text
                                                                }))
                                                            }))
                                                        }));
                                                    } catch (err: any) {
                                                        console.error('Failed to load lyric variants:', err);
                                                        toast.error('Failed to load lyric variants: ' + (err.message || 'Unknown error'));
                                                    } finally {
                                                        setLyricVariantsLoading(false);
                                                    }
                                                }
                                            }}
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
                                            value={krithi.status || 'DRAFT'}
                                            onChange={e => setKrithi({ ...krithi, status: e.target.value as any })}
                                            className="w-full p-2 border rounded"
                                        >
                                            <option value="DRAFT">Draft</option>
                                            <option value="IN_REVIEW">In Review</option>
                                            <option value="PUBLISHED">Published</option>
                                            <option value="ARCHIVED">Archived</option>
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

                        {/* 2. STRUCTURE TAB */}
                        {activeTab === 'Structure' && (
                            <div className="space-y-6">
                                <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6">
                                    <SectionHeader title="Section Structure" />
                                    <p className="text-sm text-ink-600 mb-6">
                                        Define the structure of your composition by adding sections (Pallavi, Anupallavi, Charanam, etc.). 
                                        Once sections are defined, you can add lyrics for each section in the Lyrics tab.
                                    </p>

                                    {sectionsLoading && (
                                        <div className="p-8 text-center">
                                            <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-primary mb-2"></div>
                                            <p className="text-ink-500 text-sm">Loading sections...</p>
                                        </div>
                                    )}

                                    {!sectionsLoading && (
                                        <>
                                    <div className="flex items-center justify-between mb-6 pb-2 border-b border-border-light">
                                        <h4 className="text-sm font-semibold text-ink-700">Sections</h4>
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
                                            className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors"
                                        >
                                            <span className="material-symbols-outlined text-[18px]">add</span>
                                            Add Section
                                        </button>
                                    </div>

                                    <div className="space-y-3">
                                        {krithi.sections && krithi.sections.length > 0 ? (
                                            krithi.sections
                                                .sort((a, b) => a.orderIndex - b.orderIndex)
                                                .map((section, idx) => (
                                                    <div key={section.id} className="border border-border-light rounded-lg p-4 bg-white hover:shadow-sm transition-shadow">
                                                        <div className="flex items-start justify-between mb-3">
                                                            <div className="flex-1">
                                                                <div className="flex items-center gap-3 mb-3">
                                                                    <span className="text-sm font-semibold text-ink-900">
                                                                        Section {idx + 1}
                                                                    </span>
                                                                    <span className="px-2.5 py-1 bg-primary-light text-primary rounded-full text-xs font-medium">
                                                                        {formatSectionType(section.sectionType)}
                                                                    </span>
                                                                </div>
                                                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                                                    <div>
                                                                        <label className="block text-xs font-semibold text-ink-700 mb-2">Section Type</label>
                                                                        <select
                                                                            value={section.sectionType}
                                                                            onChange={(e) => {
                                                                                const updated = krithi.sections?.map(s =>
                                                                                    s.id === section.id ? { ...s, sectionType: e.target.value as any } : s
                                                                                ) || [];
                                                                                setKrithi(prev => ({ ...prev, sections: updated }));
                                                                            }}
                                                                            className="w-full h-10 px-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
                                                                        >
                                                                            <option value="PALLAVI">Pallavi</option>
                                                                            <option value="ANUPALLAVI">Anupallavi</option>
                                                                            <option value="CHARANAM">Charanam</option>
                                                                            <option value="CHITTASWARAM">Chittaswaram</option>
                                                                            <option value="OTHER">Other</option>
                                                                        </select>
                                                                    </div>
                                                                    <div>
                                                                        <label className="block text-xs font-semibold text-ink-700 mb-2">Order Index</label>
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
                                                                            className="w-full h-10 px-3 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent text-sm"
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
                                            <div className="p-8 text-center bg-slate-50 rounded-lg border-2 border-dashed border-border-light">
                                                <span className="material-symbols-outlined text-[48px] text-ink-300 mb-3 block">article</span>
                                                <p className="text-ink-500 text-sm font-medium mb-1">No sections defined yet</p>
                                                <p className="text-ink-400 text-xs">Add sections to define the structure of your composition</p>
                                            </div>
                                        )}
                                    </div>

                                    {krithi.sections && krithi.sections.length > 0 && (
                                        <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                                            <p className="text-sm text-blue-800 flex items-start gap-2">
                                                <span className="material-symbols-outlined text-[18px] mt-0.5">info</span>
                                                <span>
                                                    <strong>Next step:</strong> Once your section structure is defined, navigate to the <strong>Lyrics</strong> tab to add content for each section.
                                                </span>
                                            </p>
                                        </div>
                                    )}
                                        </>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* 3. LYRICS TAB */}
                        {activeTab === 'Lyrics' && (
                            <div className="space-y-6">
                                <div className="bg-surface-light border rounded-xl p-6">
                                    <div className="flex items-center justify-between mb-6">
                                        <SectionHeader title="Lyric Variants" />
                                        
                                        {lyricVariantsLoading ? (
                                            <div className="flex items-center gap-2 text-sm text-ink-500">
                                                <div className="inline-block animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
                                                <span>Loading variants...</span>
                                            </div>
                                        ) : (
                                        <div className="flex gap-2">
                                            {/* AI Generation Button */}
                                            <button
                                                onClick={() => {
                                                    // Pre-fill content from current editing variant if possible
                                                    let initial = '';
                                                    if (editingVariantId) {
                                                        const v = krithi.lyricVariants?.find(v => v.id === editingVariantId);
                                                        // Join sections text
                                                        if (v && v.sections) {
                                                            initial = v.sections.map(s => s.text).join('\n\n');
                                                        }
                                                    }
                                                    setTransliterationInitialContent(initial);
                                                    setIsTransliterationModalOpen(true);
                                                }}
                                                className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors shadow-sm shadow-purple-200"
                                            >
                                                <span className="material-symbols-outlined text-[18px]">auto_awesome</span>
                                                Generate Variants
                                            </button>

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
                                        )}
                                    </div>

                                    {/* AI Transliteration Modal */}
                                    <TransliterationModal
                                        isOpen={isTransliterationModalOpen}
                                        onClose={() => setIsTransliterationModalOpen(false)}
                                        initialContent={transliterationInitialContent}
                                        onTransliterate={async (content, targetScript) => {
                                            const res = await transliterateContent(krithiId || 'new', content, null, targetScript);
                                            return res.transliterated;
                                        }}
                                        onConfirm={(content, script) => {
                                            // Create new variant
                                            // Ensure we have at least one section
                                            let targetSectionId = krithi.sections?.[0]?.id;

                                            // If no sections, create a default Pallavi section
                                            if (!targetSectionId) {
                                                const newSec: KrithiSection = {
                                                    id: `temp-sec-${Date.now()}`,
                                                    sectionType: 'PALLAVI',
                                                    orderIndex: 1
                                                };
                                                // Update krithi sections
                                                setKrithi(prev => ({
                                                    ...prev,
                                                    sections: [newSec]
                                                }));
                                                targetSectionId = newSec.id;
                                                toast.info('Created default Pallavi section for the new variant.');
                                            }

                                            const newVariant: KrithiLyricVariant = {
                                                id: `temp-ai-${Date.now()}`,
                                                language: krithi.primaryLanguage || 'te',
                                                script: script,
                                                transliterationScheme: 'ISO-15919 (AI Generated)',
                                                sections: [{
                                                    sectionId: targetSectionId,
                                                    text: content
                                                }]
                                            };

                                            setKrithi(prev => ({
                                                ...prev,
                                                lyricVariants: [...(prev.lyricVariants || []), newVariant]
                                            }));
                                            setEditingVariantId(newVariant.id);
                                            setIsTransliterationModalOpen(false);
                                            toast.success('Generated variant added!');
                                        }}
                                    />

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
                                    
                                    {tagsLoading && (
                                        <div className="p-8 text-center">
                                            <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-primary mb-2"></div>
                                            <p className="text-ink-500 text-sm">Loading tags...</p>
                                        </div>
                                    )}

                                    {!tagsLoading && (
                                        <>
                                    <div className="flex flex-wrap gap-2 mb-6">
                                        {krithi.tags && krithi.tags.length > 0 ? (
                                            krithi.tags.map(t => (
                                                <span
                                                    key={t.id}
                                                    className="inline-flex items-center gap-2 px-3 py-1.5 bg-primary-light text-primary rounded-full text-sm border border-primary/20"
                                                >
                                                    <span>{t.displayNameEn || t.displayName}</span>
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
                                                value={tagSearchTerm}
                                                placeholder="Search tags by name or category..."
                                                className="w-full h-12 px-4 pr-10 rounded-lg bg-slate-50 border border-border-light text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                                                onFocus={() => setShowTagDropdown(true)}
                                                onBlur={() => {
                                                    // Delay to allow clicking on dropdown items
                                                    setTimeout(() => setShowTagDropdown(false), 200);
                                                }}
                                                onChange={(e) => setTagSearchTerm(e.target.value)}
                                            />
                                            <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-ink-400 text-[20px]">search</span>

                                            {/* Tag Dropdown */}
                                            {showTagDropdown && (
                                                <div className="absolute z-10 w-full mt-1 bg-white border border-border-light rounded-lg shadow-lg max-h-60 overflow-y-auto">
                                                    {allTags
                                                        .filter(tag => {
                                                            // Filter out already assigned tags
                                                            if (krithi.tags?.some(assigned => assigned.id === tag.id)) {
                                                                return false;
                                                            }
                                                            // Filter by search term
                                                            if (tagSearchTerm) {
                                                                const searchLower = tagSearchTerm.toLowerCase();
                                                                const displayName = tag.displayNameEn || tag.displayName;
                                                                return displayName.toLowerCase().includes(searchLower) ||
                                                                       tag.category.toLowerCase().includes(searchLower) ||
                                                                       (tag.slug && tag.slug.toLowerCase().includes(searchLower));
                                                            }
                                                            return true;
                                                        })
                                                        .map(tag => (
                                                            <button
                                                                key={tag.id}
                                                                type="button"
                                                                onMouseDown={(e) => {
                                                                    // Prevent onBlur from firing before onClick
                                                                    e.preventDefault();
                                                                }}
                                                                onClick={() => {
                                                                    setKrithi(prev => ({
                                                                        ...prev,
                                                                        tags: [...(prev.tags || []), tag]
                                                                    }));
                                                                    setTagSearchTerm('');
                                                                    setShowTagDropdown(false);
                                                                }}
                                                                className="w-full text-left px-4 py-2 hover:bg-slate-50 transition-colors border-b border-border-light last:border-0"
                                                            >
                                                                <div className="flex items-center justify-between">
                                                                    <div className="flex items-center gap-2">
                                                                        <span className="font-medium text-ink-900">{tag.displayNameEn || tag.displayName}</span>
                                                                        {tag.slug && (
                                                                            <span className="text-xs text-ink-400 font-mono">#{tag.slug}</span>
                                                                        )}
                                                                    </div>
                                                                    <span className="text-xs text-ink-500 bg-slate-100 px-2 py-0.5 rounded">{tag.category}</span>
                                                                </div>
                                                            </button>
                                                        ))}
                                                    {allTags.filter(tag => {
                                                        if (krithi.tags?.some(assigned => assigned.id === tag.id)) return false;
                                                        if (tagSearchTerm) {
                                                            const searchLower = tagSearchTerm.toLowerCase();
                                                            const displayName = tag.displayNameEn || tag.displayName;
                                                            return displayName.toLowerCase().includes(searchLower) ||
                                                                   tag.category.toLowerCase().includes(searchLower) ||
                                                                   (tag.slug && tag.slug.toLowerCase().includes(searchLower));
                                                        }
                                                        return true;
                                                    }).length === 0 && (
                                                        <div className="px-4 py-2 text-sm text-ink-500">No available tags to add</div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                        <p className="text-xs text-ink-500">Tags are from a controlled vocabulary. Search and select to add.</p>
                                    </div>
                                        </>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* 4. NOTATION TAB (conditional) */}
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

import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    getKrithi,
    createKrithi,
    updateKrithi,
    saveKrithiSections,
    getKrithiSections,
    getKrithiTags,
    getKrithiLyricVariants,
    createLyricVariant,
    updateLyricVariant,
    saveVariantSections,
    getKrithiAuditLogs
} from '../api/client';
import { KrithiDetail, Composer, Raga, Tala, Deity, Temple } from '../types';
import { mapKrithiDtoToDetail, buildKrithiPayload } from '../utils/krithi-mapper';
import { useToast } from '../components/Toast'; // Assuming this exists or we use our own error handler
import { handleApiError } from '../utils/error-handler';

export const useKrithiData = (
    refs: {
        composers: Composer[];
        ragas: Raga[];
        talas: Tala[];
        deities: Deity[];
        temples: Temple[];
    }
) => {
    const navigate = useNavigate();
    // We can use the global toast or pass it in. For now let's assume global or we wrap it.
    // The original used a custom hook. Let's stick to the pattern but maybe use our new util.
    const toast = useToast();

    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [auditLogs, setAuditLogs] = useState<any[]>([]);

    const loadKrithi = useCallback(async (id: string) => {
        setLoading(true);
        try {
            const dto = await getKrithi(id);

            // If references aren't ready, we might get partial data, but the hook consumer
            // should ensure refs are loaded before calling this or we re-map when refs change.
            // For this hook, we assume refs are passed in current state.

            const mapped = mapKrithiDtoToDetail(dto, refs);

            // Load audit logs in background
            getKrithiAuditLogs(id).then(setAuditLogs).catch(console.error);

            return mapped;
        } catch (error) {
            handleApiError(error, toast);
            return null;
        } finally {
            setLoading(false);
        }
    }, [refs, toast]);

    // Separate loader for sections (lazy loading)
    const loadSections = useCallback(async (id: string) => {
        try {
            const sections = await getKrithiSections(id);
            return sections.map((s: any) => ({
                id: s.id,
                sectionType: s.sectionType,
                orderIndex: s.orderIndex,
                label: s.label || undefined
            }));
        } catch (error) {
            handleApiError(error, toast);
            return [];
        }
    }, [toast]);

    // Separate loader for variants (lazy loading)
    const loadVariants = useCallback(async (id: string, sampradayas: any[]) => {
        try {
            const variantsData = await getKrithiLyricVariants(id);
            return variantsData.map((v: any) => ({
                id: v.variant.id,
                language: v.variant.language.toLowerCase(),
                script: v.variant.script.toLowerCase(),
                transliterationScheme: v.variant.transliterationScheme,
                sampradaya: v.variant.sampradayaId ? sampradayas.find((s: any) => s.id === v.variant.sampradayaId) : undefined,
                sections: v.sections.map((s: any) => ({
                    sectionId: s.sectionId,
                    text: s.text
                }))
            }));
        } catch (error) {
            handleApiError(error, toast);
            return [];
        }
    }, [toast]);


    const saveKrithi = useCallback(async (krithi: Partial<KrithiDetail>, isNew: boolean, krithiId?: string) => {
        setSaving(true);
        try {
            const payload = buildKrithiPayload(krithi);
            let savedId = krithiId;

            if (isNew) {
                if (!payload.title || !payload.composerId || !payload.primaryLanguage) {
                    toast.error('Title, Composer, and Primary Language are required');
                    setSaving(false);
                    return null;
                }

                // Create request requires non-null values
                const createPayload = {
                    title: payload.title,
                    incipit: payload.incipit || null,
                    composerId: payload.composerId,
                    primaryLanguage: payload.primaryLanguage,
                    musicalForm: payload.musicalForm || 'KRITHI',
                    talaId: payload.talaId || null,
                    primaryRagaId: payload.ragaIds?.[0] || null, // Create endpoint usually takes primary
                    deityId: payload.deityId || null,
                    templeId: payload.templeId || null,
                    isRagamalika: payload.isRagamalika || false,
                    ragaIds: payload.ragaIds || [],
                    sahityaSummary: payload.sahityaSummary || null,
                    notes: payload.notes || null,
                };

                const res = await createKrithi(createPayload);
                savedId = res.id;
                // Navigate will happen in component
            } else if (krithiId) {
                await updateKrithi(krithiId, payload);
            }

            if (!savedId) throw new Error("Failed to resolve Krithi ID");

            // Save Sections
            let reloadedSections: any[] = [];
            if (krithi.sections) {
                const sectionsToSave = krithi.sections.map(s => ({
                    sectionType: s.sectionType,
                    orderIndex: s.orderIndex,
                    label: null
                }));
                await saveKrithiSections(savedId, sectionsToSave);

                // Reload sections to get new IDs
                const loaded = await getKrithiSections(savedId);
                reloadedSections = loaded;
            }

            // Save Lyric Variants
            if (krithi.lyricVariants && krithi.lyricVariants.length > 0 && reloadedSections.length > 0) {
                // Map old section IDs to new section IDs
                const sectionIdMap = new Map<string, string>();
                if (krithi.sections) {
                    for (const oldSection of krithi.sections) {
                        const newSection = reloadedSections.find(
                            (s: any) => s.sectionType === oldSection.sectionType && s.orderIndex === oldSection.orderIndex
                        );
                        if (newSection) {
                            sectionIdMap.set(oldSection.id, newSection.id);
                        }
                    }
                }

                const validSectionIds = new Set(reloadedSections.map((s: any) => s.id));

                for (const variant of krithi.lyricVariants) {
                    const variantPayload = {
                        language: variant.language.toUpperCase(),
                        script: variant.script?.toUpperCase(),
                        transliterationScheme: variant.transliterationScheme,
                        sampradayaId: variant.sampradaya?.id,
                        isPrimary: false
                    };

                    let variantId = variant.id;
                    if (variant.id.startsWith('temp-')) {
                        const created = await createLyricVariant(savedId, variantPayload);
                        variantId = created.id;
                    } else {
                        await updateLyricVariant(variant.id, variantPayload);
                    }

                    // Save variant sections
                    if (variant.sections && variant.sections.length > 0) {
                        const validSections = variant.sections
                            .filter(s => s.text && s.text.trim())
                            .map(s => ({
                                sectionId: sectionIdMap.get(s.sectionId) || s.sectionId,
                                text: s.text
                            }))
                            .filter(s => validSectionIds.has(s.sectionId));

                        if (validSections.length > 0) {
                            await saveVariantSections(variantId, validSections);
                        }
                    }
                }
            }

            toast.success('Changes saved successfully');
            return savedId;

        } catch (error) {
            handleApiError(error, toast);
            return null;
        } finally {
            setSaving(false);
        }
    }, [toast, navigate]);

    return {
        loading,
        saving,
        auditLogs,
        loadKrithi,
        saveKrithi,
        loadSections,
        loadVariants
    };
};

import { useState, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
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
    // We can use the global toast or pass it in. For now let's assume global or we wrap it.
    const toast = useToast();

    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);

    const queryClient = useQueryClient();

    // React Query for fetching Krithi
    const useKrithiQuery = (id: string | undefined, enabled: boolean) => {
        return useQuery({
            queryKey: ['krithi', id],
            queryFn: () => getKrithi(id!),
            enabled: !!id && enabled,
            select: (dto) => {
                // Pure mapping
                return mapKrithiDtoToDetail(dto, refs);
            },
            staleTime: 5 * 60 * 1000,
        });
    };

    // React Query for Audit Logs
    const useAuditLogsQuery = (id: string | undefined, enabled: boolean) => {
        return useQuery({
            queryKey: ['krithi-audit', id],
            queryFn: () => getKrithiAuditLogs(id!),
            enabled: !!id && enabled
        });
    };

    // Keep loadKrithi for manual reloads if absolutely needed
    const loadKrithi = useCallback(async (id: string) => {
        setLoading(true);
        try {
            const dto = await getKrithi(id);
            const mapped = mapKrithiDtoToDetail(dto, refs);
            // No side effect here either, let consumer fetch logs
            return mapped;
        } catch (error) {
            handleApiError(error, toast);
            return null;
        } finally {
            setLoading(false);
        }
        // NOTE: We intentionally omit `toast` from deps to keep this callback stable.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [refs]);

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
        // Keep callback stable so effects depending on it don't refire every render.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

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
        // Stable callback; `sampradayas` are passed in as an argument.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);



    // Separate loader for tags (lazy loading)
    const loadKrithiTags = useCallback(async (id: string) => {
        try {
            const tags = await getKrithiTags(id);
            return tags;
        } catch (error) {
            handleApiError(error, toast);
            return [];
        }
        // Stable callback; used by effects and tab-change logic.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

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
                    label: s.label || null
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

            await queryClient.invalidateQueries({ queryKey: ['krithi', savedId] });
            toast.success('Changes saved successfully');
            return savedId;

        } catch (error) {
            handleApiError(error, toast);
            return null;
        } finally {
            setSaving(false);
        }
        // Only `toast` and stable module imports are used; keep deps empty to avoid changing identity.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return {
        loading,
        saving,
        useKrithiQuery,
        useAuditLogsQuery,
        loadKrithi,
        saveKrithi,
        loadSections,
        loadVariants,
        loadKrithiTags
    };
};

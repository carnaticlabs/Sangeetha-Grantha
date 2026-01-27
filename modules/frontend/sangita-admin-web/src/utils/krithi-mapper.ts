import { KrithiDetail, MusicalForm, Raga, Composer, Tala, Deity, Temple } from '../types';

export const mapKrithiDtoToDetail = (
    dto: any,
    refs: {
        composers: Composer[];
        ragas: Raga[];
        talas: Tala[];
        deities: Deity[];
        temples: Temple[];
    }
): Partial<KrithiDetail> => {
    // Handle workflowState: backend uses DRAFT/IN_REVIEW/PUBLISHED/ARCHIVED (uppercase)
    // Frontend expects same format but ensure it's uppercase
    let workflowState = 'DRAFT';
    if (dto.workflowState) {
        workflowState =
            typeof dto.workflowState === 'string'
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
        ragaList = dto.ragaIds
            .map((id: string) => refs.ragas.find((r) => r.id === id))
            .filter(Boolean) as Raga[];
    } else if (dto.primaryRagaId) {
        // Fallback to primaryRagaId
        const primaryRaga = refs.ragas.find((r) => r.id === dto.primaryRagaId);
        if (primaryRaga) ragaList = [primaryRaga];
    }

    return {
        id: dto.id,
        title: dto.title || '',
        incipit: dto.incipit,
        composer: dto.composer
            ? dto.composer
            : dto.composerId
                ? refs.composers.find((c) => c.id === dto.composerId)
                : undefined,
        tala: dto.tala
            ? dto.tala
            : dto.talaId
                ? refs.talas.find((t) => t.id === dto.talaId)
                : undefined,
        ragas: ragaList,
        deity: dto.deity
            ? dto.deity
            : dto.deityId
                ? refs.deities.find((d) => d.id === dto.deityId)
                : undefined,
        temple: dto.temple
            ? dto.temple
            : dto.templeId
                ? refs.temples.find((t) => t.id === dto.templeId)
                : undefined,
        primaryLanguage:
            dto.primaryLanguage?.toLowerCase() || dto.primaryLanguage || 'te',
        musicalForm: dto.musicalForm || MusicalForm.KRITHI,
        isRagamalika: dto.isRagamalika || false,
        status: workflowState as any,
        sahityaSummary: dto.sahityaSummary,
        notes: dto.notes,
        sections: dto.sections || [],
        lyricVariants: dto.lyricVariants || [],
        tags: dto.tags || [],
    };
};

export const buildKrithiPayload = (krithi: Partial<KrithiDetail>) => {
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
        payload.ragaIds = krithi.ragas.map((r) => r.id);
    }
    if (krithi.deity?.id) payload.deityId = krithi.deity.id;
    if (krithi.temple?.id) payload.templeId = krithi.temple.id;
    if (krithi.musicalForm) {
        // Ensure enum value is uppercase
        payload.musicalForm =
            typeof krithi.musicalForm === 'string'
                ? krithi.musicalForm.toUpperCase()
                : krithi.musicalForm;
    }
    if (krithi.isRagamalika !== undefined)
        payload.isRagamalika = krithi.isRagamalika;
    if (krithi.sahityaSummary) payload.sahityaSummary = krithi.sahityaSummary;
    if (krithi.notes) payload.notes = krithi.notes;
    if (krithi.status) {
        // Convert status to uppercase enum value (e.g., 'DRAFT', 'IN_REVIEW')
        const statusValue =
            typeof krithi.status === 'string'
                ? krithi.status.toUpperCase().replace(/-/g, '_')
                : krithi.status;
        payload.workflowState = statusValue;
    }
    // Include tagIds if tags array exists (even if empty, to clear tags)
    if (krithi.tags !== undefined) {
        payload.tagIds = krithi.tags.map((t) => t.id);
    }

    return payload;
};

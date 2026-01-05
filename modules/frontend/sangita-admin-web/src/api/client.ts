import {
    MusicalForm,
    NotationResponse,
    NotationVariant,
    NotationRow,
    KrithiSummary,
    KrithiSearchResult,
    KrithiDetail,
    Composer,
    Raga,
    Tala,
    Deity,
    Temple,
    Tag,
    Sampradaya,
    AuditLog,
    DashboardStats,
    ReferenceDataStats,
    ImportedKrithi
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/v1';

const getAuthToken = () => localStorage.getItem('authToken');

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers,
    });

    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(`API Error ${response.status}: ${errorBody || response.statusText}`);
    }

    if (response.status === 204) {
        return {} as T;
    }

    return response.json();
}

// --- Public / Search ---

export const searchKrithis = (query?: string) => {
    const params = new URLSearchParams();
    if (query && query.trim()) {
        params.append('query', query.trim());
    }
    const queryString = params.toString();
    return request<KrithiSearchResult>(`/krithis/search${queryString ? `?${queryString}` : ''}`);
};

export const getKrithi = (id: string, admin = false) => {
    // If admin context, we might want different fields, but spec currently reuses public GET logic mostly
    // or implies specific Admin GET might exist if authentication is checked logic-side.
    // For now using the public endpoint which provides detail.
    return request<KrithiDetail>(`/krithis/${id}`);
};

// --- Admin Krithi Management ---

export const createKrithi = (payload: any) => {
    return request<KrithiDetail>(`/admin/krithis`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateKrithi = (id: string, payload: any) => {
    return request<KrithiDetail>(`/admin/krithis/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

// --- Sections API ---
export const getKrithiSections = (krithiId: string) => {
    return request<Array<{ id: string; sectionType: string; orderIndex: number; label?: string | null }>>(`/admin/krithis/${krithiId}/sections`);
};

export const saveKrithiSections = (krithiId: string, sections: Array<{ sectionType: string; orderIndex: number; label?: string | null }>) => {
    return request<void>(`/admin/krithis/${krithiId}/sections`, {
        method: 'POST',
        body: JSON.stringify({ sections }),
    });
};

// --- Lyric Variants API ---
export const getKrithiLyricVariants = (krithiId: string) => {
    return request<Array<{
        variant: {
            id: string;
            krithiId: string;
            language: string;
            script: string;
            transliterationScheme?: string | null;
            isPrimary: boolean;
            variantLabel?: string | null;
            sourceReference?: string | null;
            lyrics: string;
            sampradayaId?: string | null;
        };
        sections: Array<{
            id: string;
            lyricVariantId: string;
            sectionId: string;
            text: string;
            normalizedText?: string | null;
        }>;
    }>>(`/admin/krithis/${krithiId}/variants`);
};

// --- Tags API ---
export const getKrithiTags = (krithiId: string) => {
    return request<Tag[]>(`/admin/krithis/${krithiId}/tags`);
};

export const getAllTags = () => {
    return request<Tag[]>('/admin/tags');
};

export const getTag = (id: string) => {
    return request<Tag>(`/admin/tags/${id}`);
};

export const createTag = (payload: {
    category: string;
    slug: string;
    displayNameEn: string;
    descriptionEn?: string | null;
}) => {
    return request<Tag>('/admin/tags', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateTag = (id: string, payload: {
    category?: string;
    slug?: string;
    displayNameEn?: string;
    descriptionEn?: string | null;
}) => {
    return request<Tag>(`/admin/tags/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

export const deleteTag = (id: string) => {
    return request<void>(`/admin/tags/${id}`, {
        method: 'DELETE',
    });
};

// --- Lyric Variants API ---
export const createLyricVariant = (krithiId: string, payload: any) => {
    return request<any>(`/admin/krithis/${krithiId}/variants`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateLyricVariant = (variantId: string, payload: any) => {
    return request<any>(`/admin/variants/${variantId}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

export const saveVariantSections = (variantId: string, sections: Array<{ sectionId: string; text: string }>) => {
    return request<void>(`/admin/variants/${variantId}/sections`, {
        method: 'POST',
        body: JSON.stringify({ sections }),
    });
};

// --- Reference Data ---

export const getComposers = () => request<Composer[]>('/composers');
export const getRagas = () => request<Raga[]>('/ragas');
export const getTalas = () => request<Tala[]>('/talas');
export const getDeities = () => request<Deity[]>('/deities');
export const getTemples = () => request<Temple[]>('/temples');
export const getTags = () => request<Tag[]>('/tags');
export const getSampradayas = () => request<Sampradaya[]>('/sampradayas');

// --- Composers API ---
export const getComposer = (id: string) => {
    return request<Composer>(`/admin/composers/${id}`);
};

export const createComposer = (payload: {
    name: string;
    nameNormalized?: string | null;
    birthYear?: number | null;
    deathYear?: number | null;
    place?: string | null;
    notes?: string | null;
}) => {
    return request<Composer>('/admin/composers', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateComposer = (id: string, payload: {
    name?: string | null;
    nameNormalized?: string | null;
    birthYear?: number | null;
    deathYear?: number | null;
    place?: string | null;
    notes?: string | null;
}) => {
    return request<Composer>(`/admin/composers/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

export const deleteComposer = (id: string) => {
    return request<void>(`/admin/composers/${id}`, {
        method: 'DELETE',
    });
};

// --- Ragas API ---
export const getRaga = (id: string) => {
    return request<Raga>(`/admin/ragas/${id}`);
};

export const createRaga = (payload: {
    name: string;
    nameNormalized?: string | null;
    melakartaNumber?: number | null;
    parentRagaId?: string | null;
    arohanam?: string | null;
    avarohanam?: string | null;
    notes?: string | null;
}) => {
    return request<Raga>('/admin/ragas', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateRaga = (id: string, payload: {
    name?: string | null;
    nameNormalized?: string | null;
    melakartaNumber?: number | null;
    parentRagaId?: string | null;
    arohanam?: string | null;
    avarohanam?: string | null;
    notes?: string | null;
}) => {
    return request<Raga>(`/admin/ragas/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

export const deleteRaga = (id: string) => {
    return request<void>(`/admin/ragas/${id}`, {
        method: 'DELETE',
    });
};

// --- Talas API ---
export const getTala = (id: string) => {
    return request<Tala>(`/admin/talas/${id}`);
};

export const createTala = (payload: {
    name: string;
    nameNormalized?: string | null;
    beatCount?: number | null;
    angaStructure?: string | null;
    notes?: string | null;
}) => {
    return request<Tala>('/admin/talas', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateTala = (id: string, payload: {
    name?: string | null;
    nameNormalized?: string | null;
    beatCount?: number | null;
    angaStructure?: string | null;
    notes?: string | null;
}) => {
    return request<Tala>(`/admin/talas/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

export const deleteTala = (id: string) => {
    return request<void>(`/admin/talas/${id}`, {
        method: 'DELETE',
    });
};

// --- Temples API ---
export const getTemple = (id: string) => {
    return request<Temple>(`/admin/temples/${id}`);
};

export const createTemple = (payload: {
    name: string;
    nameNormalized?: string | null;
    city?: string | null;
    state?: string | null;
    country?: string | null;
    primaryDeityId?: string | null;
    latitude?: number | null;
    longitude?: number | null;
    notes?: string | null;
}) => {
    return request<Temple>('/admin/temples', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateTemple = (id: string, payload: {
    name?: string | null;
    nameNormalized?: string | null;
    city?: string | null;
    state?: string | null;
    country?: string | null;
    primaryDeityId?: string | null;
    latitude?: number | null;
    longitude?: number | null;
    notes?: string | null;
}) => {
    return request<Temple>(`/admin/temples/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

export const deleteTemple = (id: string) => {
    return request<void>(`/admin/temples/${id}`, {
        method: 'DELETE',
    });
};

// --- Audit ---

export const getAuditLogs = () => request<AuditLog[]>('/audit/logs');
export const getKrithiAuditLogs = (krithiId: string) => request<AuditLog[]>(`/audit/logs?entityTable=krithis&entityId=${krithiId}`);
export const getDashboardStats = () => request<DashboardStats>('/admin/dashboard/stats');
export const getReferenceStats = () => request<ReferenceDataStats>('/reference/stats');

export const transliterateContent = (krithiId: string, content: string, sourceScript: string | null, targetScript: string) => {
    return request<{ transliterated: string, targetScript: string }>(`/admin/krithis/${krithiId}/transliterate`, {
        method: 'POST',
        body: JSON.stringify({ content, sourceScript, targetScript }),
    });
};


// --- Imports API ---

export const getImports = (status?: string) => {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    return request<ImportedKrithi[]>(`/admin/imports?${params}`);
};

export const scrapeContent = (url: string) => {
    return request<ImportedKrithi>('/admin/imports/scrape', {
        method: 'POST',
        body: JSON.stringify({ url }),
    });
};

export const reviewImport = (id: string, reviewRequest: { status: string; mappedKrithiId?: string | null; reviewerNotes?: string | null }) => {
    return request<ImportedKrithi>(`/admin/imports/${id}/review`, {
        method: 'POST',
        body: JSON.stringify(reviewRequest),
    });
};


// --- Notation API ---

export const getAdminKrithiNotation = (krithiId: string, form: MusicalForm) => {
    return request<NotationResponse>(`/admin/krithis/${krithiId}/notation?musicalForm=${form}`);
};

export const createNotationVariant = (krithiId: string, payload: Partial<NotationVariant>) => {
    return request<NotationVariant>(`/admin/krithis/${krithiId}/notation/variants`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateNotationVariant = (variantId: string, payload: Partial<NotationVariant>) => {
    return request<NotationVariant>(`/admin/notation/variants/${variantId}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

export const deleteNotationVariant = (variantId: string) => {
    return request<void>(`/admin/notation/variants/${variantId}`, {
        method: 'DELETE',
    });
};

export const createNotationRow = (variantId: string, payload: Partial<NotationRow>) => {
    return request<NotationRow>(`/admin/notation/variants/${variantId}/rows`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateNotationRow = (rowId: string, payload: Partial<NotationRow>) => {
    return request<NotationRow>(`/admin/notation/rows/${rowId}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

export const deleteNotationRow = (rowId: string) => {
    return request<void>(`/admin/notation/rows/${rowId}`, {
        method: 'DELETE',
    });
};

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
    ReferenceDataStats
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
    return request<KrithiDetail>(`/krithis`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateKrithi = (id: string, payload: any) => {
    return request<KrithiDetail>(`/krithis/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

// --- Sections API ---
export const saveKrithiSections = (krithiId: string, sections: Array<{ sectionType: string; orderIndex: number; label?: string | null }>) => {
    return request<void>(`/admin/krithis/${krithiId}/sections`, {
        method: 'POST',
        body: JSON.stringify({ sections }),
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

// --- Audit ---

export const getAuditLogs = () => request<AuditLog[]>('/audit/logs');
export const getKrithiAuditLogs = (krithiId: string) => request<AuditLog[]>(`/audit/logs?entityTable=krithis&entityId=${krithiId}`);
export const getDashboardStats = () => request<DashboardStats>('/admin/dashboard/stats');
export const getReferenceStats = () => request<ReferenceDataStats>('/reference/stats');


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

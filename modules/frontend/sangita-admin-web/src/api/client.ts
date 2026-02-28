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
    ImportedKrithi,
    ImportBatch,
    ImportJob,
    ImportTaskRun,
    ImportEvent,
    BulkBatchStatus,
    BulkTaskStatus
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

/** Login with admin token and either email (preferred) or userId. */
export const login = async (
    adminToken: string,
    credentials: { email?: string; userId?: string }
) => {
    const body: { adminToken: string; email?: string; userId?: string } = { adminToken };
    if (credentials.email?.trim()) body.email = credentials.email.trim();
    else if (credentials.userId?.trim()) body.userId = credentials.userId.trim();
    const response = await fetch(`${API_BASE_URL}/auth/token`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
    });

    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(`Login failed: ${errorBody || response.statusText}`);
    }

    const data = await response.json();
    if (data.token) {
        localStorage.setItem('authToken', data.token);
    }
    return data;
};

// --- Public / Search ---

export const searchKrithis = (opts?: {
    query?: string;
    ragaId?: string;
    composerId?: string;
    language?: string;
    page?: number;
    pageSize?: number;
}) => {
    const params = new URLSearchParams();
    if (opts?.query?.trim()) params.append('query', opts.query.trim());
    if (opts?.ragaId) params.append('ragaId', opts.ragaId);
    if (opts?.composerId) params.append('composerId', opts.composerId);
    if (opts?.language) params.append('primaryLanguage', opts.language);
    if (opts?.page !== undefined) params.append('page', String(opts.page));
    if (opts?.pageSize !== undefined) params.append('pageSize', String(opts.pageSize));
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

// --- Bulk Import Orchestration ---
export const createBulkImportBatch = (sourceManifestPath: string) => {
    return request<ImportBatch>('/admin/bulk-import/batches', {
        method: 'POST',
        body: JSON.stringify({ sourceManifestPath }),
    });
};

export const uploadBulkImportFile = async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);

    const token = getAuthToken();
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const response = await fetch(`${API_BASE_URL}/admin/bulk-import/upload`, {
        method: 'POST',
        headers,
        body: formData,
    });

    if (!response.ok) {
        const errorBody = await response.text();
        throw new Error(`Upload Error ${response.status}: ${errorBody || response.statusText}`);
    }

    return response.json() as Promise<ImportBatch>;
};

export const listBulkImportBatches = (status?: BulkBatchStatus, limit = 20, offset = 0) => {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    params.append('limit', String(limit));
    params.append('offset', String(offset));
    const qs = params.toString();
    return request<ImportBatch[]>(`/admin/bulk-import/batches${qs ? `?${qs}` : ''}`);
};

export const getBulkImportBatch = (id: string) => request<ImportBatch>(`/admin/bulk-import/batches/${id}`);
export const getBulkImportJobs = (id: string) => request<ImportJob[]>(`/admin/bulk-import/batches/${id}/jobs`);
export const getBulkImportTasks = (id: string, status?: BulkTaskStatus, limit = 200, offset = 0) => {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    params.append('limit', String(limit));
    params.append('offset', String(offset));
    const qs = params.toString();
    return request<ImportTaskRun[]>(`/admin/bulk-import/batches/${id}/tasks${qs ? `?${qs}` : ''}`);
};
export const getBulkImportEvents = (id: string, limit = 200) => {
    const params = new URLSearchParams();
    params.append('limit', String(limit));
    const qs = params.toString();
    return request<ImportEvent[]>(`/admin/bulk-import/batches/${id}/events${qs ? `?${qs}` : ''}`);
};

export const pauseBulkImportBatch = (id: string) =>
    request<ImportBatch>(`/admin/bulk-import/batches/${id}/pause`, { method: 'POST' });
export const resumeBulkImportBatch = (id: string) =>
    request<ImportBatch>(`/admin/bulk-import/batches/${id}/resume`, { method: 'POST' });
export const cancelBulkImportBatch = (id: string) =>
    request<ImportBatch>(`/admin/bulk-import/batches/${id}/cancel`, { method: 'POST' });
export const retryBulkImportBatch = (id: String, includeFailed = true) =>
    request<{ requeuedTasks: number }>(`/admin/bulk-import/batches/${id}/retry`, {
        method: 'POST',
        body: JSON.stringify({ includeFailed }),
    });
export const deleteBulkImportBatch = (id: String) =>
    request<void>(`/admin/bulk-import/batches/${id}`, { method: 'DELETE' });
export const approveAllInBulkImportBatch = (id: String) =>
    request<void>(`/admin/bulk-import/batches/${id}/approve-all`, { method: 'POST' });
export const rejectAllInBulkImportBatch = (id: String) =>
    request<void>(`/admin/bulk-import/batches/${id}/reject-all`, { method: 'POST' });

// TRACK-004: Finalize batch
export const finalizeBulkImportBatch = (id: string) =>
    request<{
        batchId: string;
        total: number;
        approved: number;
        rejected: number;
        pending: number;
        canFinalize: boolean;
        avgQualityScore: number;
        qualityTierCounts: Record<string, number>;
        message: string;
    }>(`/admin/bulk-import/batches/${id}/finalize`, { method: 'POST' });

// TRACK-004: Export QA report
export const exportBulkImportReport = async (id: string, format: 'json' | 'csv' = 'json'): Promise<Blob> => {
    const token = getAuthToken();
    const headers: Record<string, string> = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const response = await fetch(`${API_BASE_URL}/admin/bulk-import/batches/${id}/export?format=${format}`, {
        headers
    });

    if (!response.ok) {
        throw new Error(`Export failed: ${response.statusText}`);
    }

    return response.blob();
};

// --- Deities API ---
export const getDeity = (id: string) => {
    return request<Deity>(`/admin/deities/${id}`);
};

export const createDeity = (payload: {
    name: string;
    nameNormalized?: string | null;
    description?: string | null;
}) => {
    return request<Deity>('/admin/deities', {
        method: 'POST',
        body: JSON.stringify(payload),
    });
};

export const updateDeity = (id: string, payload: {
    name?: string | null;
    nameNormalized?: string | null;
    description?: string | null;
}) => {
    return request<Deity>(`/admin/deities/${id}`, {
        method: 'PUT',
        body: JSON.stringify(payload),
    });
};

export const deleteDeity = (id: string) => {
    return request<void>(`/admin/deities/${id}`, {
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

export const reviewImport = (id: string, reviewRequest: { status: string; mappedKrithiId?: string | null; reviewerNotes?: string | null; overrides?: any | null }) => {
    return request<ImportedKrithi>(`/admin/imports/${id}/review`, {
        method: 'POST',
        body: JSON.stringify(reviewRequest),
    });
};

// TRACK-012: Bulk review endpoint
export const bulkReviewImports = (importIds: string[], action: 'APPROVE' | 'REJECT', overrides?: any | null, reason?: string | null) => {
    return request<{
        total: number;
        succeeded: number;
        failed: number;
        results: Array<{ importId: string; status: string; error: string | null }>;
    }>('/admin/imports/bulk-review', {
        method: 'POST',
        body: JSON.stringify({ importIds, action, overrides, reason }),
    });
};

// TRACK-012: Auto-approve queue endpoint
export const getAutoApproveQueue = (params?: {
    batchId?: string;
    qualityTier?: string;
    confidenceMin?: number;
    limit?: number;
    offset?: number;
}) => {
    const searchParams = new URLSearchParams();
    if (params?.batchId) searchParams.append('batchId', params.batchId);
    if (params?.qualityTier) searchParams.append('qualityTier', params.qualityTier);
    if (params?.confidenceMin !== undefined) searchParams.append('confidenceMin', String(params.confidenceMin));
    if (params?.limit) searchParams.append('limit', String(params.limit));
    if (params?.offset) searchParams.append('offset', String(params.offset));

    const qs = searchParams.toString();
    return request<ImportedKrithi[]>(`/admin/imports/auto-approve-queue${qs ? `?${qs}` : ''}`);
};


// --- Notation API ---

export const getAdminKrithiNotation = async (krithiId: string, form: MusicalForm): Promise<NotationResponse> => {
    // Backend returns sections as array, but frontend expects rowsBySectionId as object
    // Transform the response to match frontend expectations
    const response = await request<any>(`/admin/krithis/${krithiId}/notation?musicalForm=${form}`);

    // Transform variants: convert sections array to rowsBySectionId object
    const transformedVariants = response.variants.map((v: any) => {
        const rowsBySectionId: Record<string, NotationRow[]> = {};

        // Backend sends sections as array of { sectionId, rows }
        if (v.sections && Array.isArray(v.sections)) {
            v.sections.forEach((section: any) => {
                if (section.sectionId && section.rows) {
                    rowsBySectionId[section.sectionId] = section.rows;
                }
            });
        }

        return {
            variant: v.variant,
            rowsBySectionId
        };
    });

    return {
        ...response,
        variants: transformedVariants
    };
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

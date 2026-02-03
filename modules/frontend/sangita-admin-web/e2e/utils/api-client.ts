import { apiConfig, testCredentials } from '../fixtures/test-data';

interface TokenResponse {
  token: string;
  expiresInSeconds: number;
}

interface ImportBatchDto {
  id: string;
  sourceManifest: string;
  status: string;
  totalTasks: number;
  processedTasks: number;
  succeededTasks: number;
  failedTasks: number;
  createdAt: string;
}

interface ImportedKrithiDto {
  id: string;
  sourceKey: string;
  rawTitle: string | null;
  rawComposer: string | null;
  rawRaga: string | null;
  rawTala: string | null;
  rawDeity: string | null;
  rawTemple: string | null;
  rawLanguage: string | null;
  rawLyrics: string | null;
  importStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  qualityScore: number | null;
  qualityTier: string | null;
  parsedPayload: string | null;
  resolutionData: string | null;
}

interface KrithiDto {
  id: string;
  title: string;
  composerName: string | null;
  ragaName: string | null;
  talaName: string | null;
  language: string | null;
  status: string;
}

interface SearchResult {
  items: KrithiDto[];
  total: number;
  page: number;
  limit: number;
}

interface BatchSummary {
  total: number;
  approved: number;
  rejected: number;
  pending: number;
  averageQualityScore: number;
  qualityTierDistribution: Record<string, number>;
  canFinalize: boolean;
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export class DirectApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl?: string) {
    this.baseUrl = baseUrl || apiConfig.baseUrl;
  }

  async login(adminToken?: string, email?: string): Promise<string> {
    for (let attempt = 1; attempt <= 3; attempt++) {
      const response = await fetch(`${this.baseUrl}/v1/auth/token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          adminToken: adminToken || testCredentials.adminToken,
          email: email || testCredentials.email,
        }),
      });

      if (response.ok) {
        const data = (await response.json()) as TokenResponse;
        this.token = data.token;
        return this.token;
      }

      if (response.status === 429) {
        console.log(`Login rate limited, waiting ${attempt * 3} seconds...`);
        await sleep(attempt * 3000);
        continue;
      }

      throw new Error(`Login failed: ${response.status} ${response.statusText}`);
    }

    throw new Error('Login failed after 3 attempts');
  }

  private async fetch<T>(path: string, options: RequestInit = {}, maxRetries = 3): Promise<T> {
    if (!this.token) {
      await this.login();
    }

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      const response = await fetch(`${this.baseUrl}${path}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${this.token}`,
          ...options.headers,
        },
      });

      if (response.ok) {
        const contentType = response.headers.get('content-type');
        if (contentType?.includes('application/json')) {
          return response.json() as Promise<T>;
        }
        return response.text() as unknown as T;
      }

      if (response.status === 429) {
        const waitTime = attempt * 5000; // 5s, 10s, 15s
        console.log(`Rate limited on ${path}, waiting ${waitTime / 1000}s (attempt ${attempt}/${maxRetries})...`);
        await sleep(waitTime);
        continue;
      }

      if (response.status === 401) {
        // Token expired, re-login
        await this.login();
        continue;
      }

      const errorText = await response.text();
      throw new Error(`API request failed: ${response.status} ${response.statusText} - ${errorText}`);
    }

    throw new Error(`API request to ${path} failed after ${maxRetries} attempts`);
  }

  async uploadBulkImportFile(filePath: string): Promise<ImportBatchDto> {
    if (!this.token) {
      await this.login();
    }

    const fs = await import('fs');
    const pathModule = await import('path');
    const fileBuffer = fs.readFileSync(filePath);
    const fileName = pathModule.basename(filePath);

    const formData = new FormData();
    formData.append('file', new Blob([fileBuffer]), fileName);

    for (let attempt = 1; attempt <= 3; attempt++) {
      const response = await fetch(`${this.baseUrl}/v1/admin/bulk-import/upload`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${this.token}`,
        },
        body: formData,
      });

      if (response.ok) {
        return response.json() as Promise<ImportBatchDto>;
      }

      if (response.status === 429) {
        console.log(`Upload rate limited, waiting ${attempt * 5} seconds...`);
        await sleep(attempt * 5000);
        continue;
      }

      const errorText = await response.text();
      throw new Error(`Upload failed: ${response.status} ${response.statusText} - ${errorText}`);
    }

    throw new Error('Upload failed after 3 attempts');
  }

  async listBulkImportBatches(status?: string, limit = 25, offset = 0): Promise<ImportBatchDto[]> {
    let path = `/v1/admin/bulk-import/batches?limit=${limit}&offset=${offset}`;
    if (status) {
      path += `&status=${status}`;
    }
    return this.fetch<ImportBatchDto[]>(path);
  }

  async getBulkImportBatch(batchId: string): Promise<ImportBatchDto> {
    return this.fetch<ImportBatchDto>(`/v1/admin/bulk-import/batches/${batchId}`);
  }

  async waitForBatchCompletion(
    batchId: string,
    timeout = 120000,
    pollInterval = 3000
  ): Promise<ImportBatchDto> {
    const endTime = Date.now() + timeout;

    while (Date.now() < endTime) {
      try {
        const batch = await this.getBulkImportBatch(batchId);
        if (['SUCCEEDED', 'FAILED', 'CANCELLED'].includes(batch.status)) {
          return batch;
        }
        console.log(`Batch status: ${batch.status}, progress: ${batch.processedTasks}/${batch.totalTasks}`);
      } catch (error) {
        // On error (including rate limit), just wait and retry
        console.log(`Error checking batch status: ${error}, retrying...`);
      }

      await sleep(pollInterval);
    }

    throw new Error(`Batch ${batchId} did not complete within ${timeout}ms`);
  }

  async pauseBatch(batchId: string): Promise<ImportBatchDto> {
    return this.fetch<ImportBatchDto>(`/v1/admin/bulk-import/batches/${batchId}/pause`, {
      method: 'POST',
    });
  }

  async resumeBatch(batchId: string): Promise<ImportBatchDto> {
    return this.fetch<ImportBatchDto>(`/v1/admin/bulk-import/batches/${batchId}/resume`, {
      method: 'POST',
    });
  }

  async cancelBatch(batchId: string): Promise<ImportBatchDto> {
    return this.fetch<ImportBatchDto>(`/v1/admin/bulk-import/batches/${batchId}/cancel`, {
      method: 'POST',
    });
  }

  async retryBatch(batchId: string, includeFailed = false): Promise<ImportBatchDto> {
    return this.fetch<ImportBatchDto>(
      `/v1/admin/bulk-import/batches/${batchId}/retry?includeFailed=${includeFailed}`,
      { method: 'POST' }
    );
  }

  async deleteBatch(batchId: string): Promise<void> {
    await this.fetch<void>(`/v1/admin/bulk-import/batches/${batchId}`, {
      method: 'DELETE',
    });
  }

  async approveAllInBatch(batchId: string): Promise<void> {
    await this.fetch<void>(`/v1/admin/bulk-import/batches/${batchId}/approve-all`, {
      method: 'POST',
    });
  }

  async rejectAllInBatch(batchId: string): Promise<void> {
    await this.fetch<void>(`/v1/admin/bulk-import/batches/${batchId}/reject-all`, {
      method: 'POST',
    });
  }

  async finalizeBatch(batchId: string): Promise<BatchSummary> {
    return this.fetch<BatchSummary>(`/v1/admin/bulk-import/batches/${batchId}/finalize`, {
      method: 'POST',
    });
  }

  async getImports(status?: 'PENDING' | 'APPROVED' | 'REJECTED'): Promise<ImportedKrithiDto[]> {
    let path = '/v1/admin/imports';
    if (status) {
      path += `?status=${status}`;
    }
    return this.fetch<ImportedKrithiDto[]>(path);
  }

  async reviewImport(
    importId: string,
    request: {
      status: 'APPROVED' | 'REJECTED';
      overrides?: {
        title?: string;
        composer?: string;
        raga?: string;
        tala?: string;
        language?: string;
        deity?: string;
        temple?: string;
        lyrics?: string;
      };
      reason?: string;
    }
  ): Promise<ImportedKrithiDto> {
    return this.fetch<ImportedKrithiDto>(`/v1/admin/imports/${importId}/review`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async searchKrithis(query: string, limit = 25, offset = 0): Promise<SearchResult> {
    return this.fetch<SearchResult>(
      `/v1/krithis?q=${encodeURIComponent(query)}&limit=${limit}&offset=${offset}`
    );
  }

  async getKrithi(krithiId: string): Promise<KrithiDto> {
    return this.fetch<KrithiDto>(`/v1/krithis/${krithiId}`);
  }

  async healthCheck(): Promise<boolean> {
    try {
      const response = await fetch(`${this.baseUrl}/health`);
      return response.ok;
    } catch {
      return false;
    }
  }
}

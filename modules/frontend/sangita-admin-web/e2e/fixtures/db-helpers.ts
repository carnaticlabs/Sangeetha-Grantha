import { Client, QueryResult } from 'pg';
import { dbConfig } from './test-data';

export class DatabaseVerifier {
  private client: Client;
  private connected = false;

  constructor(connectionString?: string) {
    this.client = new Client({
      connectionString: connectionString || dbConfig.connectionString,
    });
  }

  async connect(): Promise<void> {
    if (!this.connected) {
      await this.client.connect();
      this.connected = true;
    }
  }

  async close(): Promise<void> {
    if (this.connected) {
      await this.client.end();
      this.connected = false;
    }
  }

  async query(sql: string, params?: unknown[]): Promise<QueryResult> {
    await this.connect();
    return this.client.query(sql, params);
  }

  async getImportBatch(id: string): Promise<ImportBatchRow | null> {
    const result = await this.query('SELECT * FROM import_batch WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async getImportBatchByManifest(manifestPattern: string): Promise<ImportBatchRow | null> {
    const result = await this.query(
      'SELECT * FROM import_batch WHERE source_manifest ILIKE $1 ORDER BY created_at DESC LIMIT 1',
      [`%${manifestPattern}%`]
    );
    return result.rows[0] || null;
  }

  async getImportedKrithis(batchId: string): Promise<ImportedKrithiRow[]> {
    const result = await this.query(
      'SELECT * FROM imported_krithis WHERE import_batch_id = $1 ORDER BY created_at',
      [batchId]
    );
    return result.rows;
  }

  async getImportedKrithisByStatus(
    batchId: string,
    status: 'pending' | 'approved' | 'rejected' | 'in_review' | 'mapped' | 'discarded'
  ): Promise<ImportedKrithiRow[]> {
    const result = await this.query(
      'SELECT * FROM imported_krithis WHERE import_batch_id = $1 AND import_status = $2 ORDER BY created_at',
      [batchId, status]
    );
    return result.rows;
  }

  async getImportJobs(batchId: string): Promise<ImportJobRow[]> {
    const result = await this.query(
      'SELECT * FROM import_job WHERE batch_id = $1 ORDER BY created_at',
      [batchId]
    );
    return result.rows;
  }

  async getImportTaskRuns(batchId: string): Promise<ImportTaskRunRow[]> {
    const result = await this.query(
      `SELECT itr.* FROM import_task_run itr
       JOIN import_job ij ON itr.job_id = ij.id
       WHERE ij.batch_id = $1
       ORDER BY itr.created_at`,
      [batchId]
    );
    return result.rows;
  }

  async getKrithiByTitle(title: string): Promise<KrithiRow | null> {
    const result = await this.query('SELECT * FROM krithis WHERE title ILIKE $1 LIMIT 1', [
      `%${title}%`,
    ]);
    return result.rows[0] || null;
  }

  async getKrithiWithDetails(krithiId: string): Promise<KrithiWithDetails | null> {
    const krithiResult = await this.query(
      `SELECT k.*, c.name as composer_name
       FROM krithis k
       LEFT JOIN composers c ON k.composer_id = c.id
       WHERE k.id = $1`,
      [krithiId]
    );
    if (krithiResult.rows.length === 0) return null;

    const ragaResult = await this.query(
      `SELECT r.name FROM krithi_ragas kr
       JOIN ragas r ON kr.raga_id = r.id
       WHERE kr.krithi_id = $1`,
      [krithiId]
    );

    return {
      ...krithiResult.rows[0],
      ragas: ragaResult.rows.map((r) => r.name),
    };
  }

  async getAuditLogs(entityTable: string, entityId: string): Promise<AuditLogRow[]> {
    const result = await this.query(
      'SELECT * FROM audit_log WHERE entity_table = $1 AND entity_id = $2 ORDER BY changed_at DESC',
      [entityTable, entityId]
    );
    return result.rows;
  }

  async getRecentAuditLogs(action: string, limit = 10): Promise<AuditLogRow[]> {
    const result = await this.query(
      'SELECT * FROM audit_log WHERE action = $1 ORDER BY changed_at DESC LIMIT $2',
      [action, limit]
    );
    return result.rows;
  }

  async cleanup(prefix: string): Promise<void> {
    await this.connect();

    // Delete in correct order for foreign key constraints
    await this.query(
      `DELETE FROM krithi_lyric_sections WHERE lyric_variant_id IN (
        SELECT id FROM krithi_lyric_variants WHERE krithi_id IN (
          SELECT id FROM krithis WHERE notes LIKE $1
        )
      )`,
      [`%${prefix}%`]
    );

    await this.query(
      `DELETE FROM krithi_lyric_variants WHERE krithi_id IN (
        SELECT id FROM krithis WHERE notes LIKE $1
      )`,
      [`%${prefix}%`]
    );

    await this.query(
      `DELETE FROM krithi_ragas WHERE krithi_id IN (
        SELECT id FROM krithis WHERE notes LIKE $1
      )`,
      [`%${prefix}%`]
    );

    await this.query(`DELETE FROM krithis WHERE notes LIKE $1`, [`%${prefix}%`]);

    await this.query(`DELETE FROM imported_krithis WHERE source_key LIKE $1`, [`%${prefix}%`]);

    await this.query(`DELETE FROM import_batch WHERE source_manifest LIKE $1`, [`%${prefix}%`]);
  }
}

// Type definitions for database rows
export interface ImportBatchRow {
  id: string;
  source_manifest: string;
  created_by_user_id: string;
  status: 'PENDING' | 'RUNNING' | 'PAUSED' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED';
  total_tasks: number;
  processed_tasks: number;
  succeeded_tasks: number;
  failed_tasks: number;
  blocked_tasks: number;
  started_at: string | null;
  completed_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface ImportedKrithiRow {
  id: string;
  import_source_id: string | null;
  import_batch_id: string | null;
  source_key: string;
  raw_title: string | null;
  raw_lyrics: string | null;
  raw_composer: string | null;
  raw_raga: string | null;
  raw_tala: string | null;
  raw_deity: string | null;
  raw_temple: string | null;
  raw_language: string | null;
  parsed_payload: string | null;
  resolution_data: string | null;
  duplicate_candidates: string | null;
  import_status: 'PENDING' | 'APPROVED' | 'REJECTED';
  mapped_krithi_id: string | null;
  reviewer_user_id: string | null;
  reviewer_notes: string | null;
  reviewed_at: string | null;
  quality_score: number | null;
  quality_tier: string | null;
  completeness_score: number | null;
  resolution_confidence: number | null;
  source_quality: number | null;
  validation_score: number | null;
  created_at: string;
}

export interface ImportJobRow {
  id: string;
  batch_id: string;
  job_type: 'MANIFEST_INGEST' | 'SCRAPE' | 'ENTITY_RESOLUTION';
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'RETRYABLE' | 'BLOCKED' | 'CANCELLED';
  retry_count: number;
  payload: string | null;
  result: string | null;
  started_at: string | null;
  completed_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface ImportTaskRunRow {
  id: string;
  job_id: string;
  krithi_key: string | null;
  idempotency_key: string | null;
  source_url: string | null;
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'RETRYABLE' | 'BLOCKED' | 'CANCELLED';
  attempt: number;
  duration_ms: number | null;
  error: string | null;
  checksum: string | null;
  evidence_path: string | null;
  started_at: string | null;
  completed_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface KrithiRow {
  id: string;
  title: string;
  composer_id: string | null;
  tala_id: string | null;
  language: string | null;
  status: string;
  notes: string | null;
  created_at: string;
  updated_at: string;
}

export interface KrithiWithDetails extends KrithiRow {
  composer_name: string | null;
  ragas: string[];
}

export interface AuditLogRow {
  id: string;
  actor_user_id: string;
  actor_ip: string | null;
  action: string;
  entity_table: string;
  entity_id: string;
  changed_at: string;
  diff: unknown | null;
  metadata: unknown | null;
}

// Helper function for quick database state verification
export async function verifyDatabaseState(
  table: string,
  conditions: Record<string, unknown>
): Promise<unknown[]> {
  const db = new DatabaseVerifier();
  try {
    const whereClauses: string[] = [];
    const params: unknown[] = [];
    let paramIndex = 1;

    for (const [key, value] of Object.entries(conditions)) {
      if (typeof value === 'object' && value !== null && 'contains' in value) {
        whereClauses.push(`${key} ILIKE $${paramIndex}`);
        params.push(`%${(value as { contains: string }).contains}%`);
      } else {
        whereClauses.push(`${key} = $${paramIndex}`);
        params.push(value);
      }
      paramIndex++;
    }

    const sql = `SELECT * FROM ${table} WHERE ${whereClauses.join(' AND ')}`;
    const result = await db.query(sql, params);
    return result.rows;
  } finally {
    await db.close();
  }
}

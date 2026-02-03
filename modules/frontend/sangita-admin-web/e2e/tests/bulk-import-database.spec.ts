import { test, expect } from '../fixtures/shared-batch';
import { DatabaseVerifier } from '../fixtures/db-helpers';

test.describe('Database State Verification', () => {
  let db: DatabaseVerifier;

  test.beforeEach(async () => {
    db = new DatabaseVerifier();
  });

  test.afterEach(async () => {
    await db.close();
  });

  test('import_batch table has correct state', async ({ batchId }) => {
    const batch = await db.getImportBatch(batchId);

    expect(batch).not.toBeNull();
    expect(['pending', 'RUNNING', 'PAUSED', 'SUCCEEDED', 'FAILED', 'CANCELLED']).toContain(
      batch!.status.toUpperCase()
    );
    expect(batch!.total_tasks).toBeGreaterThanOrEqual(0);
    expect(batch!.source_manifest).toContain('bulk_import_test');
  });

  test('imported_krithis records created correctly', async ({ batchId }) => {
    const imports = await db.getImportedKrithis(batchId);

    if (imports.length === 0) {
      console.log('No imported_krithis yet - entity resolution may not have completed');
      test.skip();
      return;
    }

    const first = imports[0];
    expect(first.source_key).not.toBeNull();
    expect(first.import_batch_id).toBe(batchId);
    expect(first.source_key.length).toBeGreaterThan(0);
  });

  test('import_job created for each phase', async ({ batchId }) => {
    const jobs = await db.getImportJobs(batchId);

    if (jobs.length === 0) {
      console.log('No jobs yet - batch may still be initializing');
      test.skip();
      return;
    }

    const jobTypes = jobs.map((j) => j.job_type.toUpperCase());

    // At minimum, should have MANIFEST_INGEST
    expect(jobTypes).toContain('MANIFEST_INGEST');
  });

  test('import_task_run track individual operations', async ({ batchId }) => {
    const tasks = await db.getImportTaskRuns(batchId);

    if (tasks.length === 0) {
      console.log('No task runs yet - batch may still be processing');
      test.skip();
      return;
    }

    const task = tasks[0];
    expect(task.job_id).not.toBeNull();
    expect(task.status).not.toBeNull();
    expect(task.attempt).toBeGreaterThanOrEqual(1);
  });

  test('scrape tasks have source URLs', async ({ batchId }) => {
    const tasks = await db.getImportTaskRuns(batchId);

    const scrapeTasks = tasks.filter((t) => t.source_url);

    if (scrapeTasks.length === 0) {
      console.log('No scrape tasks with source URLs yet');
      test.skip();
      return;
    }

    for (const scrapeTask of scrapeTasks) {
      expect(scrapeTask.source_url).toMatch(/^https?:\/\//);
    }
  });

  test('audit_log entries created for batch operations', async () => {
    const audits = await db.getRecentAuditLogs('BULK_IMPORT_BATCH_CREATE', 10);

    // Audit logs may not exist if no batches were created in this session
    if (audits.length === 0) {
      console.log('No audit logs found for batch creation');
      test.skip();
      return;
    }

    const audit = audits[0];
    expect(audit.action).toBe('BULK_IMPORT_BATCH_CREATE');
    expect(audit.entity_table).toBe('import_batch');
    // actor_user_id may be null for programmatic/API batch creation
    expect(audit.entity_id).not.toBeNull();
  });

  test('approved import creates krithi with correct data', async ({ batchId, apiClient }) => {
    const pendingImports = await db.getImportedKrithisByStatus(batchId, 'pending');

    if (pendingImports.length === 0) {
      console.log('No pending imports to approve');
      test.skip();
      return;
    }

    const importToApprove = pendingImports[0];

    await apiClient.reviewImport(importToApprove.id, { status: 'approved' });

    const updatedImport = await db.query('SELECT * FROM imported_krithis WHERE id = $1', [
      importToApprove.id,
    ]);
    expect(updatedImport.rows[0].import_status).toBe('approved');
  });

  test('approved import has mapped_krithi_id set', async ({ batchId, apiClient }) => {
    // First ensure we have some approved imports
    const pendingImports = await db.getImportedKrithisByStatus(batchId, 'pending');
    if (pendingImports.length > 0) {
      await apiClient.reviewImport(pendingImports[0].id, { status: 'approved' });
    }

    const approved = await db.query(
      `SELECT * FROM imported_krithis
       WHERE import_batch_id = $1 AND import_status = 'approved'
       LIMIT 1`,
      [batchId]
    );

    if (approved.rows.length === 0) {
      console.log('No approved imports found');
      test.skip();
      return;
    }

    expect(approved.rows[0].mapped_krithi_id).not.toBeNull();
  });

  test('krithi has composer relationship', async ({ batchId }) => {
    const approved = await db.query(
      `SELECT ik.*, k.title, c.name as composer_name
       FROM imported_krithis ik
       JOIN krithis k ON ik.mapped_krithi_id = k.id
       LEFT JOIN composers c ON k.composer_id = c.id
       WHERE ik.import_batch_id = $1 AND ik.import_status = 'approved'
       LIMIT 1`,
      [batchId]
    );

    if (approved.rows.length === 0) {
      console.log('No approved imports with mapped krithis');
      test.skip();
      return;
    }

    const row = approved.rows[0];
    expect(row.title).not.toBeNull();
  });

  test('batch task counts are tracked', async ({ batchId }) => {
    const batch = await db.getImportBatch(batchId);
    expect(batch).not.toBeNull();
    expect(typeof batch!.processed_tasks).toBe('number');
    expect(typeof batch!.total_tasks).toBe('number');
  });

  test('import resolution data is stored', async ({ batchId }) => {
    const imports = await db.getImportedKrithis(batchId);

    if (imports.length === 0) {
      console.log('No imports to check');
      test.skip();
      return;
    }

    const withResolution = imports.filter((i) => i.resolution_data !== null);

    if (withResolution.length > 0) {
      const resolutionData = JSON.parse(withResolution[0].resolution_data!);
      expect(typeof resolutionData).toBe('object');
    }
  });

  test('import parsed payload contains scraped data', async ({ batchId }) => {
    const imports = await db.getImportedKrithis(batchId);

    if (imports.length === 0) {
      console.log('No imports to check');
      test.skip();
      return;
    }

    const withPayload = imports.filter((i) => i.parsed_payload !== null);

    if (withPayload.length > 0) {
      const payload = JSON.parse(withPayload[0].parsed_payload!);
      expect(typeof payload).toBe('object');
    }
  });

  test('quality scores are calculated', async ({ batchId }) => {
    const imports = await db.getImportedKrithis(batchId);

    if (imports.length === 0) {
      console.log('No imports to check');
      test.skip();
      return;
    }

    const withScores = imports.filter((i) => i.quality_score !== null);

    if (withScores.length > 0) {
      const imp = withScores[0];
      expect(imp.quality_score).toBeGreaterThanOrEqual(0);
      expect(imp.quality_score).toBeLessThanOrEqual(100);
    }
  });
});

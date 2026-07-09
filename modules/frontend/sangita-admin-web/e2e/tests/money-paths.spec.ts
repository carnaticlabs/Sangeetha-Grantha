/**
 * TRACK-113 Phase 2 — the three E2E money paths (D12: nightly + pre-release).
 *
 * Substrate: the compose stack (`make dev` / start-sangita.sh) — frontend :5001,
 * backend :8080, PostgreSQL :5432 — per integration-tests approach §4.4.
 * DB verification points at the compose database (DATABASE_URL).
 *
 * Path 1: login → review → approve (curator queue)
 * Path 2: bulk import — happy row + one deterministic failure row (.invalid TLD)
 * Path 3: krithi edit — raga change reflected in the detail view + junction table
 *
 * Self-contained: each path seeds and cleans up its own data.
 */
import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { DatabaseVerifier } from '../fixtures/db-helpers';
import { testCredentials, apiConfig } from '../fixtures/test-data';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const runId = `e2e-money-${Date.now()}`;

async function apiToken(): Promise<string> {
  const response = await fetch(`${apiConfig.baseUrl}/v1/auth/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ adminToken: testCredentials.adminToken, email: testCredentials.email }),
  });
  if (!response.ok) throw new Error(`auth/token failed: ${response.status}`);
  return (await response.json()).token;
}

test.describe('Money Path 1 — login → review → approve', () => {
  // This path exercises the real login UI: start unauthenticated.
  test.use({ storageState: { cookies: [], origins: [] } });

  const title = `${runId} vAtApi gaNapatim`;
  let importId: string;
  const db = new DatabaseVerifier();

  test.beforeAll(async () => {
    const result = await db.query(
      `INSERT INTO imported_krithis
         (import_source_id, source_key, raw_title, raw_composer, raw_raga, raw_tala,
          raw_language, raw_lyrics, import_status)
       SELECT id, $1, $2, 'Muthuswami Dikshitar', 'Hamsadhwani', 'Adi',
              'SANSKRIT', 'vAtApi gaNapatiM bhajE', 'in_review'
       FROM import_sources LIMIT 1
       RETURNING id`,
      [`https://example.org/${runId}`, title],
    );
    importId = result.rows[0].id;
  });

  test.afterAll(async () => {
    const row = await db.query('SELECT mapped_krithi_id FROM imported_krithis WHERE id = $1', [importId]);
    const krithiId = row.rows[0]?.mapped_krithi_id;
    await db.query('DELETE FROM imported_krithis WHERE id = $1', [importId]);
    if (krithiId) {
      await db.query('DELETE FROM krithi_lyric_sections WHERE lyric_variant_id IN (SELECT id FROM krithi_lyric_variants WHERE krithi_id = $1)', [krithiId]);
      await db.query('DELETE FROM krithi_lyric_variants WHERE krithi_id = $1', [krithiId]);
      await db.query('DELETE FROM krithi_ragas WHERE krithi_id = $1', [krithiId]);
      await db.query('DELETE FROM krithis WHERE id = $1', [krithiId]);
    }
    await db.close();
  });

  test('curator logs in, reviews the pending import, and approves it', async ({ page }) => {
    // — Login through the real UI —
    await page.goto('/login');
    await page.getByLabel('Admin Token').fill(testCredentials.adminToken);
    await page.getByLabel('Email').fill(testCredentials.email);
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).toHaveURL('/');

    // — Review queue —
    await page.goto('/curator-review');
    await page.getByText(title).click();
    await expect(page.getByLabel('Title')).toHaveValue(title);
    await expect(page.getByLabel('Composer')).toHaveValue('Muthuswami Dikshitar');

    // — Approve via the confirmation modal —
    await page.getByRole('button', { name: 'Approve & Create' }).click();
    await expect(page.getByText('Approve Import')).toBeVisible();
    await page.getByRole('button', { name: 'Approve & Create' }).last().click();
    await expect(page.getByText('Import approved')).toBeVisible({ timeout: 15000 });

    // — DB truth —
    await expect
      .poll(async () => (await db.query('SELECT import_status::text AS s, mapped_krithi_id FROM imported_krithis WHERE id = $1', [importId])).rows[0])
      .toMatchObject({ s: 'approved' });
    const approved = (await db.query('SELECT mapped_krithi_id FROM imported_krithis WHERE id = $1', [importId])).rows[0];
    expect(approved.mapped_krithi_id).not.toBeNull();
    const krithi = (await db.query('SELECT title FROM krithis WHERE id = $1', [approved.mapped_krithi_id])).rows[0];
    expect(krithi.title).toBe(title);
  });
});

test.describe('Money Path 2 — bulk import: happy path + one failure row', () => {
  const db = new DatabaseVerifier();
  let batchId: string | undefined;
  let csv: string;

  test.beforeAll(() => {
    // Content-unique manifest per run: the import pipeline dedupes re-uploads
    // by checksum (TRACK-062), so a byte-identical fixture is rejected on rerun.
    const template = fs.readFileSync(path.resolve(__dirname, '../fixtures/money-path-import.csv'), 'utf-8');
    const tmpDir = path.resolve(__dirname, '../.tmp');
    fs.mkdirSync(tmpDir, { recursive: true });
    csv = path.join(tmpDir, `money-path-${runId}.csv`);
    fs.writeFileSync(csv, template.replaceAll('{RUN_ID}', runId));
  });

  test.afterAll(async () => {
    if (batchId) {
      const token = await apiToken();
      await fetch(`${apiConfig.baseUrl}/v1/admin/bulk-import/batches/${batchId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      }).catch(() => undefined);
    }
    if (csv) fs.rmSync(csv, { force: true });
    await db.close();
  });

  test('uploaded manifest processes the good row and fails the unreachable row', async ({ page }) => {
    test.setTimeout(420_000); // scraping the happy row crosses the real network

    await page.goto('/bulk-import');

    // — Upload through the real UI. The upload endpoint is rate-limited
    //   (429), so back-to-back runs need backoff-and-retry. —
    await page.getByLabel('Upload Composition List').setInputFiles(csv);
    let uploaded = false;
    for (let attempt = 1; attempt <= 5 && !uploaded; attempt++) {
      await page.getByRole('button', { name: 'Upload & Process' }).click();
      const outcome = await Promise.race([
        page.getByText('Batch created from upload').waitFor({ timeout: 15000 }).then(() => 'ok' as const),
        page.getByText(/Failed to create batch/).waitFor({ timeout: 15000 }).then(() => 'rejected' as const),
      ]).catch(() => 'timeout' as const);
      if (outcome === 'ok') {
        uploaded = true;
      } else {
        await page.waitForTimeout(attempt * 5000);
      }
    }
    expect(uploaded, 'upload accepted (after rate-limit backoff if needed)').toBeTruthy();

    // — Resolve the created batch in the DB by this run's unique manifest name —
    await expect
      .poll(async () => {
        const row = await db.query(
          'SELECT id FROM import_batch WHERE source_manifest LIKE $1 ORDER BY created_at DESC LIMIT 1',
          [`%money-path-${runId}%`],
        );
        batchId = row.rows[0]?.id;
        return batchId;
      })
      .toBeTruthy();

    // — Wait for a terminal batch state (DB enum values are lowercase) —
    await expect
      .poll(
        async () => (await db.query('SELECT status::text AS s FROM import_batch WHERE id = $1', [batchId])).rows[0]?.s,
        { timeout: 360_000, intervals: [5000] },
      )
      .toMatch(/succeeded|failed/i);

    // — The .invalid row must have failed; the batch must record it —
    // total_tasks is per pipeline stage, not per CSV row — assert failures only.
    const batch = (
      await db.query('SELECT status::text AS s, total_tasks, failed_tasks, succeeded_tasks FROM import_batch WHERE id = $1', [batchId])
    ).rows[0];
    expect(Number(batch.failed_tasks)).toBeGreaterThanOrEqual(1);

    const failedTasks = await db.query(
      `SELECT t.status::text AS s, t.error, t.source_url FROM import_task_run t
       JOIN import_job j ON t.job_id = j.id
       WHERE j.batch_id = $1 AND lower(t.status::text) = 'failed'`,
      [batchId],
    );
    expect(failedTasks.rows.length).toBeGreaterThanOrEqual(1);
    // The deterministic failure row is the .invalid host
    expect(failedTasks.rows.some((r: { source_url?: string }) => r.source_url?.includes('.invalid'))).toBeTruthy();

    // — And the UI reflects it in the batch detail —
    await page.reload();
    await page.getByText(`money-path-${runId}`).first().click();
    await expect(page.getByText('Batch Detail')).toBeVisible();
    await expect(page.getByText(/Failed: [1-9]/)).toBeVisible();
  });
});

test.describe('Money Path 3 — krithi edit: raga change reflected in detail view', () => {
  const db = new DatabaseVerifier();
  const title = `${runId} raga change krithi`;
  let krithiId: string;
  let ragaBefore: { id: string; name: string };
  let ragaAfter: { id: string; name: string };

  test.beforeAll(async () => {
    // Reference data ships with the schema (R__ seeds) — any two distinct
    // single-word ragas will do (single-word keeps text locators exact).
    const ragas = await db.query(
      "SELECT id, name FROM ragas WHERE name !~ '\\s' AND name ~ '^[A-Za-z]+$' ORDER BY name LIMIT 2",
    );
    expect(ragas.rows.length).toBe(2);
    [ragaBefore, ragaAfter] = ragas.rows;

    const token = await apiToken();
    const composers = await db.query('SELECT id FROM composers LIMIT 1');
    const talas = await db.query('SELECT id FROM talas LIMIT 1');
    const response = await fetch(`${apiConfig.baseUrl}/v1/admin/krithis`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({
        title,
        composerId: composers.rows[0].id,
        primaryLanguage: 'SA',
        talaId: talas.rows[0].id,
        primaryRagaId: ragaBefore.id,
        ragaIds: [ragaBefore.id],
      }),
    });
    const bodyText = await response.text();
    expect(response.ok, `create krithi failed: ${response.status} ${bodyText}`).toBeTruthy();
    krithiId = JSON.parse(bodyText).id;
  });

  test.afterAll(async () => {
    if (krithiId) {
      await db.query('DELETE FROM krithi_ragas WHERE krithi_id = $1', [krithiId]);
      await db.query('DELETE FROM krithis WHERE id = $1', [krithiId]);
    }
    await db.close();
  });

  test('changing the raga in the editor updates the detail view and junction table', async ({ page }) => {
    await page.goto(`/krithis/${krithiId}`);
    await expect(page.getByText(ragaBefore.name).first()).toBeVisible();

    // — Open the raga selection modal: the Modify button sits in the same
    //   flex row as the box displaying the current raga name —
    await page
      .locator('div.flex.gap-2')
      .filter({ hasText: ragaBefore.name })
      .filter({ has: page.getByRole('button', { name: /^(Modify|Add)$/ }) })
      .first()
      .getByRole('button', { name: /^(Modify|Add)$/ })
      .click();
    await expect(page.getByText(/Select Raga|Add Raga/)).toBeVisible();
    await page.getByPlaceholder('Search...').fill(ragaAfter.name);
    await page.getByRole('button', { name: new RegExp(`^${ragaAfter.name}`) }).click();

    // — Save; anchor on the PUT round-trip, not UI text (the raga renders in
    //   an input whose value getByText cannot see) —
    const saved = page.waitForResponse(
      (r) => r.url().includes('/admin/krithis/') && r.request().method() === 'PUT' && r.ok(),
    );
    await page.getByRole('button', { name: /^Save/ }).click();
    await saved;

    // — Junction-table truth (not just the FK column): krithi_ragas updated —
    await expect
      .poll(async () =>
        (await db.query('SELECT raga_id FROM krithi_ragas WHERE krithi_id = $1', [krithiId])).rows.map((r) => r.raga_id),
      )
      .toContain(ragaAfter.id);
    // Regression pin (TRACK-113 finding): replacing the raga list must move
    // primary_raga_id with it, not leave it pointing at the removed raga.
    const primary = (await db.query('SELECT primary_raga_id FROM krithis WHERE id = $1', [krithiId])).rows[0];
    expect(primary.primary_raga_id).toBe(ragaAfter.id);

    // — Reload the detail view fresh from the API: the header subtitle
    //   (composer • raga • tala) must show the new raga —
    await page.goto(`/krithis/${krithiId}`);
    await expect(page.getByText(ragaAfter.name).first()).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(ragaBefore.name, { exact: true })).not.toBeVisible();
  });
});

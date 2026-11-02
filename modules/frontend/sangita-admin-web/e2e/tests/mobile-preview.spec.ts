/**
 * TRACK-158 C3 — Mobile Preview end to end: edit → save → the preview reader updates → search says the index is
 * behind. Lexical search only, so the run makes no Gemini call.
 *
 * Substrate: the compose stack (frontend :5001, backend :8080, PostgreSQL :5432). Self-contained: it creates one
 * draft krithi and deletes it afterwards.
 */
import { test, expect } from '@playwright/test';
import { DatabaseVerifier } from '../fixtures/db-helpers';
import { testCredentials, apiConfig } from '../fixtures/test-data';

const runId = `e2e-preview-${Date.now()}`;

async function apiToken(): Promise<string> {
  const response = await fetch(`${apiConfig.baseUrl}/v1/auth/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ adminToken: testCredentials.adminToken, email: testCredentials.email }),
  });
  if (!response.ok) throw new Error(`auth/token failed: ${response.status}`);
  return (await response.json()).token;
}

test.describe('Mobile Preview — edit, save, preview, search freshness', () => {
  const db = new DatabaseVerifier();
  const title = `${runId} pAhi gaNanAtha`;
  const renamed = `${runId} renamed krithi`;
  let krithiId: string;

  test.beforeAll(async () => {
    const token = await apiToken();
    const composers = await db.query('SELECT id FROM composers LIMIT 1');
    const talas = await db.query('SELECT id FROM talas LIMIT 1');
    const ragas = await db.query('SELECT id FROM ragas LIMIT 1');
    const response = await fetch(`${apiConfig.baseUrl}/v1/admin/krithis`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({
        title,
        composerId: composers.rows[0].id,
        primaryLanguage: 'SA',
        talaId: talas.rows[0].id,
        primaryRagaId: ragas.rows[0].id,
        ragaIds: [ragas.rows[0].id],
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

  test('a saved edit shows in the preview, and search reports the index and visibility honestly', async ({ page }) => {
    await page.goto(`/krithis/${krithiId}?tab=Mobile%20Preview`);
    const phone = page.getByTestId('phone-frame');

    // The draft is visible to the curator, labelled, and absent from the search index.
    await expect(phone.getByRole('heading', { name: title })).toBeVisible();
    await expect(phone.getByRole('note')).toContainText('Not published (draft)');
    await expect(page.getByRole('region', { name: 'Search index status' })).toContainText('Not in the search index');

    // Edit the title in the editor and save.
    await page.getByRole('button', { name: 'Metadata', exact: true }).click();
    await page.locator('input[name="title"]').fill(renamed);
    const saved = page.waitForResponse((r) => r.url().includes('/admin/krithis/') && r.request().method() === 'PUT' && r.ok());
    await page.getByRole('button', { name: /^Save/ }).click();
    await saved;

    // The preview shows the new title without a page reload.
    await page.getByRole('button', { name: 'Mobile Preview', exact: true }).click();
    await expect(phone.getByRole('heading', { name: renamed })).toBeVisible();

    // Titles & lyrics reads the live tables: as mobile it hides the draft, with unpublished included it finds it.
    await page.getByRole('button', { name: 'Search for this krithi' }).click();
    await phone.getByRole('button', { name: 'Search options' }).click();
    await phone.getByRole('button', { name: 'Titles & lyrics' }).click();
    await phone.getByRole('button', { name: 'Search', exact: true }).click();
    await expect(phone.getByText('Nothing stored under that')).toBeVisible();
    await phone.getByLabel(/Include unpublished/).check();
    await expect(phone.getByRole('button', { name: new RegExp(renamed) })).toBeVisible();
  });
});

import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export const testCredentials = {
  adminToken: process.env.ADMIN_TOKEN || 'dev-admin-token',
  email: process.env.ADMIN_EMAIL || 'admin@sangitagrantha.org',
};

export const apiConfig = {
  baseUrl: process.env.API_BASE_URL || 'http://localhost:8080',
};

export const dbConfig = {
  connectionString:
    process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/sangita_grantha',
};

export const testData = {
  // Path to the test CSV file (relative to project root)
  csvPath: path.resolve(__dirname, '../../../../../database/for_import/bulk_import_test.csv'),
  // Expected number of krithis in the test CSV
  expectedKrithiCount: 8,
  // First krithi title in the test CSV
  firstKrithiTitle: 'aruNAcala nAthaM',
  // Polling configuration for batch completion
  pollTimeoutMs: 300000, // 5 minutes - scraping can be slow
  pollIntervalMs: 3000,
};

export const logPaths = {
  applicationLog: path.resolve(__dirname, '../../../../../sangita_logs.txt'),
  queryLog: path.resolve(__dirname, '../../../../../exposed_queries.log'),
};

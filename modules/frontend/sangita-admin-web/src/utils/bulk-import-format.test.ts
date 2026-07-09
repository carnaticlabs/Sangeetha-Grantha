import { describe, it, expect } from 'vitest';
import {
    basename,
    batchStatusLabel,
    eventTypeLabel,
    formatDate,
    formatDuration,
    jobTypeLabel,
    parseError,
    statusChip,
    taskChip,
    taskStatusLabel,
} from './bulk-import-format';

describe('formatDuration', () => {
    it('renders an em dash for missing values', () => {
        expect(formatDuration(null)).toBe('—');
        expect(formatDuration(undefined)).toBe('—');
    });

    it('renders sub-second values in milliseconds', () => {
        expect(formatDuration(0)).toBe('0ms');
        expect(formatDuration(999)).toBe('999ms');
    });

    it('renders values from one second up in seconds with one decimal', () => {
        expect(formatDuration(1000)).toBe('1.0s');
        expect(formatDuration(1550)).toBe('1.6s');
        expect(formatDuration(65000)).toBe('65.0s');
    });
});

describe('formatDate', () => {
    it('renders an em dash for missing values', () => {
        expect(formatDate(null)).toBe('—');
        expect(formatDate(undefined)).toBe('—');
        expect(formatDate('')).toBe('—');
    });

    it('renders a locale string for ISO timestamps', () => {
        const iso = '2026-07-09T10:30:00Z';
        expect(formatDate(iso)).toBe(new Date(iso).toLocaleString());
    });
});

describe('basename', () => {
    it('strips POSIX and Windows directory prefixes', () => {
        expect(basename('/data/imports/manifest.csv')).toBe('manifest.csv');
        expect(basename('C:\\imports\\manifest.csv')).toBe('manifest.csv');
    });

    it('returns bare filenames and empty strings unchanged', () => {
        expect(basename('manifest.csv')).toBe('manifest.csv');
        expect(basename('')).toBe('');
    });
});

describe('parseError', () => {
    it('returns empty string for missing values', () => {
        expect(parseError(null)).toBe('');
        expect(parseError(undefined)).toBe('');
        expect(parseError('')).toBe('');
    });

    it('prefers message, then code, from JSON error payloads', () => {
        expect(parseError('{"message":"boom","code":"E42"}')).toBe('boom');
        expect(parseError('{"code":"E42"}')).toBe('E42');
    });

    it('falls back to the raw value for JSON without message/code and non-JSON', () => {
        expect(parseError('{"detail":"other"}')).toBe('{"detail":"other"}');
        expect(parseError('plain failure text')).toBe('plain failure text');
    });
});

describe('presentation maps', () => {
    it('covers every batch status with a chip class and a label', () => {
        const batchStatuses = ['PENDING', 'RUNNING', 'PAUSED', 'SUCCEEDED', 'FAILED', 'CANCELLED'] as const;
        for (const s of batchStatuses) {
            expect(statusChip[s]).toBeTruthy();
            expect(batchStatusLabel[s]).toBeTruthy();
        }
    });

    it('covers every task status with a chip class and a label', () => {
        const taskStatuses = ['PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'RETRYABLE', 'BLOCKED', 'CANCELLED'] as const;
        for (const s of taskStatuses) {
            expect(taskChip[s]).toBeTruthy();
            expect(taskStatusLabel[s]).toBeTruthy();
        }
    });

    it('humanizes job and event types, mirroring the curator-facing terminology', () => {
        expect(jobTypeLabel.MANIFEST_INGEST).toBe('Reading File');
        expect(eventTypeLabel.SCRAPE_SUCCEEDED).toBe('Content fetched successfully');
        expect(eventTypeLabel.ENTITY_RESOLUTION_FAILED).toBe('Matching failed');
    });
});

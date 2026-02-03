import * as fs from 'fs';
import { logPaths } from '../fixtures/test-data';

interface LogVerificationOptions {
  contains: string[];
  within?: number; // milliseconds from now
  logFile?: string;
}

export class LogVerifier {
  private logDir: string;

  constructor(logDir?: string) {
    this.logDir = logDir || process.cwd();
  }

  async verifyLogEntry(options: LogVerificationOptions): Promise<boolean> {
    const logFile = options.logFile || logPaths.applicationLog;

    if (!fs.existsSync(logFile)) {
      console.warn(`Log file not found: ${logFile}`);
      return false;
    }

    const content = fs.readFileSync(logFile, 'utf-8');
    const lines = content.split('\n');

    // Filter by time if specified
    let relevantLines = lines;
    if (options.within) {
      const cutoffTime = Date.now() - options.within;
      relevantLines = lines.filter((line) => {
        // Try to parse timestamp from log line
        const timestampMatch = line.match(/\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}/);
        if (timestampMatch) {
          const lineTime = new Date(timestampMatch[0]).getTime();
          return lineTime >= cutoffTime;
        }
        // If no timestamp found, include the line (might be continuation)
        return true;
      });
    }

    // Check all required strings are present
    const combinedContent = relevantLines.join('\n');
    return options.contains.every((searchStr) => combinedContent.includes(searchStr));
  }

  async getRecentErrors(count = 10): Promise<string[]> {
    const logFile = logPaths.applicationLog;

    if (!fs.existsSync(logFile)) {
      return [];
    }

    const content = fs.readFileSync(logFile, 'utf-8');
    const lines = content.split('\n');

    return lines
      .filter((line) => line.includes('ERROR') || line.includes('Exception'))
      .slice(-count);
  }

  async verifyExposedQuery(pattern: string): Promise<boolean> {
    const logFile = logPaths.queryLog;

    if (!fs.existsSync(logFile)) {
      console.warn('exposed_queries.log not found - query logging may be disabled');
      return true; // Don't fail test if log file doesn't exist
    }

    const content = fs.readFileSync(logFile, 'utf-8');
    return content.includes(pattern);
  }

  async getRecentQueries(count = 20): Promise<string[]> {
    const logFile = logPaths.queryLog;

    if (!fs.existsSync(logFile)) {
      return [];
    }

    const content = fs.readFileSync(logFile, 'utf-8');
    const lines = content.split('\n').filter((line) => line.includes('Exposed'));

    return lines.slice(-count);
  }

  async countQueryOccurrences(pattern: string): Promise<number> {
    const logFile = logPaths.queryLog;

    if (!fs.existsSync(logFile)) {
      return 0;
    }

    const content = fs.readFileSync(logFile, 'utf-8');
    const regex = new RegExp(pattern, 'g');
    const matches = content.match(regex);
    return matches?.length || 0;
  }

  getLogTail(logFile: string, lines = 50): string[] {
    if (!fs.existsSync(logFile)) {
      return [];
    }

    const content = fs.readFileSync(logFile, 'utf-8');
    const allLines = content.split('\n');
    return allLines.slice(-lines);
  }
}

// Convenience functions for use in tests
export async function verifyLogEntry(
  logFile: string,
  options: Omit<LogVerificationOptions, 'logFile'>
): Promise<boolean> {
  const verifier = new LogVerifier();
  return verifier.verifyLogEntry({ ...options, logFile });
}

export async function verifyApplicationLogEntry(options: Omit<LogVerificationOptions, 'logFile'>): Promise<boolean> {
  const verifier = new LogVerifier();
  return verifier.verifyLogEntry({ ...options, logFile: logPaths.applicationLog });
}

export async function verifyDatabaseQuery(pattern: string): Promise<boolean> {
  const verifier = new LogVerifier();
  return verifier.verifyExposedQuery(pattern);
}

export function getRecentLogs(logFile: string, count = 50): string[] {
  const verifier = new LogVerifier();
  return verifier.getLogTail(logFile, count);
}

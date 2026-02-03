export interface PollingOptions {
  timeout?: number; // Total timeout in ms (default: 60000)
  interval?: number; // Poll interval in ms (default: 1000)
  message?: string; // Error message on timeout
}

const DEFAULT_TIMEOUT = 60000;
const DEFAULT_INTERVAL = 1000;

/**
 * Poll a condition function until it returns true or timeout is reached
 */
export async function pollUntil(
  condition: () => Promise<boolean>,
  options: PollingOptions = {}
): Promise<void> {
  const { timeout = DEFAULT_TIMEOUT, interval = DEFAULT_INTERVAL, message = 'Polling timed out' } = options;

  const endTime = Date.now() + timeout;

  while (Date.now() < endTime) {
    if (await condition()) {
      return;
    }
    await sleep(interval);
  }

  throw new Error(message);
}

/**
 * Poll a function until it returns a non-null/undefined value or timeout is reached
 */
export async function pollForValue<T>(
  getter: () => Promise<T | null | undefined>,
  options: PollingOptions = {}
): Promise<T> {
  const { timeout = DEFAULT_TIMEOUT, interval = DEFAULT_INTERVAL, message = 'Polling timed out - value not found' } = options;

  const endTime = Date.now() + timeout;

  while (Date.now() < endTime) {
    const value = await getter();
    if (value !== null && value !== undefined) {
      return value;
    }
    await sleep(interval);
  }

  throw new Error(message);
}

/**
 * Poll a function until the returned value matches the expected value
 */
export async function pollForMatch<T>(
  getter: () => Promise<T>,
  expected: T,
  options: PollingOptions = {}
): Promise<void> {
  const {
    timeout = DEFAULT_TIMEOUT,
    interval = DEFAULT_INTERVAL,
    message = `Polling timed out - expected ${expected}`,
  } = options;

  const endTime = Date.now() + timeout;

  while (Date.now() < endTime) {
    const value = await getter();
    if (value === expected) {
      return;
    }
    await sleep(interval);
  }

  throw new Error(message);
}

/**
 * Poll a function until the returned value is one of the expected values
 */
export async function pollForOneOf<T>(
  getter: () => Promise<T>,
  expectedValues: T[],
  options: PollingOptions = {}
): Promise<T> {
  const {
    timeout = DEFAULT_TIMEOUT,
    interval = DEFAULT_INTERVAL,
    message = `Polling timed out - expected one of ${expectedValues.join(', ')}`,
  } = options;

  const endTime = Date.now() + timeout;

  while (Date.now() < endTime) {
    const value = await getter();
    if (expectedValues.includes(value)) {
      return value;
    }
    await sleep(interval);
  }

  throw new Error(message);
}

/**
 * Retry an operation until it succeeds or max retries reached
 */
export async function retry<T>(
  operation: () => Promise<T>,
  options: { maxRetries?: number; delay?: number; backoff?: number } = {}
): Promise<T> {
  const { maxRetries = 3, delay = 1000, backoff = 1.5 } = options;

  let lastError: Error | undefined;
  let currentDelay = delay;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      lastError = error as Error;
      if (attempt < maxRetries) {
        await sleep(currentDelay);
        currentDelay *= backoff;
      }
    }
  }

  throw lastError || new Error('Max retries reached');
}

/**
 * Sleep for a specified duration
 */
export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Wait for a condition with exponential backoff
 */
export async function waitWithBackoff(
  condition: () => Promise<boolean>,
  options: { maxWait?: number; initialDelay?: number; maxDelay?: number } = {}
): Promise<void> {
  const { maxWait = 60000, initialDelay = 100, maxDelay = 5000 } = options;

  const endTime = Date.now() + maxWait;
  let currentDelay = initialDelay;

  while (Date.now() < endTime) {
    if (await condition()) {
      return;
    }

    await sleep(Math.min(currentDelay, maxDelay));
    currentDelay *= 2;
  }

  throw new Error('Wait with backoff timed out');
}

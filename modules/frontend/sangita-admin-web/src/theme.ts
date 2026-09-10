/**
 * Theme preference plumbing.
 *
 * `ThemePreference` is what the curator picks; `ResolvedTheme` is what actually
 * gets stamped on <html data-theme>, which is the hook every dark-mode token in
 * index.css hangs off. 'system' resolves live from prefers-color-scheme.
 *
 * The same resolve-and-stamp logic runs as an inline script in index.html so the
 * first paint is already the right theme — keep the two in sync.
 */

export type ThemePreference = 'light' | 'dark' | 'system';
export type ResolvedTheme = 'light' | 'dark';

export const THEME_STORAGE_KEY = 'sangita-theme';

export const isThemePreference = (value: unknown): value is ThemePreference =>
  value === 'light' || value === 'dark' || value === 'system';

/** Reads the stored preference, defaulting to 'system'. Safe if storage is blocked. */
export const readStoredTheme = (): ThemePreference => {
  try {
    const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
    return isThemePreference(stored) ? stored : 'system';
  } catch {
    return 'system';
  }
};

export const storeTheme = (preference: ThemePreference): void => {
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, preference);
  } catch {
    /* Private mode or blocked storage — the choice just won't survive a reload. */
  }
};

export const prefersDark = (): boolean =>
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-color-scheme: dark)').matches;

export const resolveTheme = (preference: ThemePreference): ResolvedTheme =>
  preference === 'system' ? (prefersDark() ? 'dark' : 'light') : preference;

export const applyTheme = (resolved: ResolvedTheme): void => {
  document.documentElement.setAttribute('data-theme', resolved);
};

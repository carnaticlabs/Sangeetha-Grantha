import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

import {
  ResolvedTheme,
  ThemePreference,
  applyTheme,
  readStoredTheme,
  resolveTheme,
  storeTheme,
} from '../theme';

interface ThemeContextValue {
  /** What the curator chose: 'light', 'dark' or 'system'. */
  preference: ThemePreference;
  /** What that currently resolves to — 'system' follows the OS live. */
  resolved: ResolvedTheme;
  setPreference: (preference: ThemePreference) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [preference, setPreferenceState] = useState<ThemePreference>(() => readStoredTheme());
  const [resolved, setResolved] = useState<ResolvedTheme>(() => resolveTheme(readStoredTheme()));

  // Stamp <html data-theme> whenever the preference changes.
  useEffect(() => {
    const next = resolveTheme(preference);
    setResolved(next);
    applyTheme(next);
  }, [preference]);

  // While on 'system', follow the OS flipping between light and dark.
  useEffect(() => {
    if (preference !== 'system' || typeof window.matchMedia !== 'function') return;

    const query = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = () => {
      const next: ResolvedTheme = query.matches ? 'dark' : 'light';
      setResolved(next);
      applyTheme(next);
    };
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, [preference]);

  const setPreference = useCallback((next: ThemePreference) => {
    storeTheme(next);
    setPreferenceState(next);
  }, []);

  const value = useMemo(
    () => ({ preference, resolved, setPreference }),
    [preference, resolved, setPreference],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useTheme = (): ThemeContextValue => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

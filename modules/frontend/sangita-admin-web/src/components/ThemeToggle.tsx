import React from 'react';

import { useTheme } from '../hooks/useTheme';
import { ThemePreference } from '../theme';

const OPTIONS: { value: ThemePreference; icon: string; label: string }[] = [
  { value: 'light', icon: 'light_mode', label: 'Light' },
  { value: 'dark', icon: 'dark_mode', label: 'Dark' },
  { value: 'system', icon: 'computer', label: 'System' },
];

/** Segmented light/dark/system control, styled after the search-mode pill. */
const ThemeToggle: React.FC = () => {
  const { preference, resolved, setPreference } = useTheme();

  return (
    <div
      role="radiogroup"
      aria-label="Colour theme"
      className="flex items-center bg-slate-200/70 p-1 rounded-lg border border-border-light/60"
    >
      {OPTIONS.map(({ value, icon, label }) => {
        const selected = preference === value;
        return (
          <button
            key={value}
            type="button"
            role="radio"
            aria-checked={selected}
            title={
              value === 'system'
                ? `System (currently ${resolved})`
                : label
            }
            onClick={() => setPreference(value)}
            className={`flex items-center justify-center w-8 h-7 rounded-md transition-all ${
              selected
                ? 'bg-surface-light text-primary shadow-sm'
                : 'text-ink-500 hover:text-ink-900'
            }`}
          >
            {/* aria-hidden: the ligature text ("dark_mode") would otherwise be
                read out as part of the button's accessible name. */}
            <span aria-hidden="true" className="material-symbols-outlined text-[17px]">
              {icon}
            </span>
            <span className="sr-only">{label}</span>
          </button>
        );
      })}
    </div>
  );
};

export default ThemeToggle;

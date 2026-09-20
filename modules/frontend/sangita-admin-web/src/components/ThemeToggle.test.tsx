import { beforeEach, describe, expect, it, vi } from 'vitest';

import { render, screen, userEvent } from '../test/test-utils';
import ThemeToggle from './ThemeToggle';
import { ThemeProvider } from '../hooks/useTheme';
import { THEME_STORAGE_KEY } from '../theme';

/** jsdom has no matchMedia; stub it so 'system' can resolve. */
function stubPrefersDark(matches: boolean) {
    const listeners = new Set<() => void>();
    vi.stubGlobal(
        'matchMedia',
        vi.fn().mockImplementation((query: string) => ({
            matches: query.includes('dark') && matches,
            media: query,
            addEventListener: (_: string, fn: () => void) => listeners.add(fn),
            removeEventListener: (_: string, fn: () => void) => listeners.delete(fn),
        })),
    );
}

const renderToggle = () =>
    render(
        <ThemeProvider>
            <ThemeToggle />
        </ThemeProvider>,
    );

describe('ThemeToggle', () => {
    beforeEach(() => {
        window.localStorage.clear();
        document.documentElement.removeAttribute('data-theme');
        stubPrefersDark(false);
    });

    it('defaults to System and resolves it from prefers-color-scheme', () => {
        stubPrefersDark(true);
        renderToggle();

        expect(screen.getByRole('radio', { name: 'System' })).toBeChecked();
        expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    });

    it('stamps the chosen theme on <html> and persists it', async () => {
        const user = userEvent.setup();
        renderToggle();

        await user.click(screen.getByRole('radio', { name: 'Dark' }));

        expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
        expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    });

    it('lets an explicit Light choice override a dark OS preference', async () => {
        stubPrefersDark(true);
        const user = userEvent.setup();
        renderToggle();

        await user.click(screen.getByRole('radio', { name: 'Light' }));

        expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    });

    it('restores a stored preference on mount', () => {
        window.localStorage.setItem(THEME_STORAGE_KEY, 'dark');
        renderToggle();

        expect(screen.getByRole('radio', { name: 'Dark' })).toBeChecked();
        expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    });
});

/**
 * Shared test rendering utilities.
 *
 * `render` wraps the UI in the providers page components assume:
 * - QueryClientProvider — a fresh QueryClient per render so cached data and
 *   retry state never leak between tests (retries off so failed queries
 *   reject immediately instead of timing the test out).
 * - MemoryRouter — pages use react-router primitives (Link, useNavigate).
 *
 * Import from here instead of @testing-library/react in component tests:
 *   import { render, screen, userEvent } from '../test/test-utils';
 */
import { ReactElement, ReactNode } from 'react';
import { render as rtlRender, RenderOptions } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';

interface AppRenderOptions extends Omit<RenderOptions, 'wrapper'> {
    /** Initial router entries, e.g. ['/curator-review']. Defaults to ['/']. */
    initialEntries?: string[];
}

function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: false, gcTime: 0 },
            mutations: { retry: false },
        },
    });
}

function render(ui: ReactElement, options: AppRenderOptions = {}) {
    const { initialEntries = ['/'], ...renderOptions } = options;
    const queryClient = createTestQueryClient();

    const Wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={initialEntries}>{children}</MemoryRouter>
        </QueryClientProvider>
    );

    return {
        user: userEvent.setup(),
        queryClient,
        ...rtlRender(ui, { wrapper: Wrapper, ...renderOptions }),
    };
}

export * from '@testing-library/react';
export { render, userEvent, createTestQueryClient };

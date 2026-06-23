/// <reference types="vitest/config" />
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

// Standalone test config (avoids merging with the function-based vite.config.ts).
// `defineConfig` must come from 'vitest/config' so the `test` field is typed.
// vitest bundles its own vite, so @vitejs/plugin-react (typed against the app's
// vite 7) is a nominally-distinct Plugin type across the two instances — cast to
// bridge them. Type-only concern; runtime plugin behaviour is unaffected.
export default defineConfig({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    plugins: [react()] as any,
    resolve: {
        alias: {
            '@': path.resolve(__dirname, '.'),
        },
    },
    test: {
        environment: 'jsdom',
        globals: true,
        setupFiles: [],
    },
});

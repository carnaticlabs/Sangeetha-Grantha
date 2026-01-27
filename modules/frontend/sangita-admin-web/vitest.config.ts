/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// Re-declaring the config to avoid complex merging issues with the function-based vite.config.ts
// This is often cleaner for strict testing setups
export default defineConfig({
    plugins: [react()],
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

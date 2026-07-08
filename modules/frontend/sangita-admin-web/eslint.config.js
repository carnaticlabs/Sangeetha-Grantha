import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true },
      ],
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      // Advisory performance rule (react-hooks v7). The existing sites are
      // intentional prop→local-state sync on modal open / data load; treat as a
      // visible warning rather than a blocker. Revisit under TRACK-118 coverage.
      'react-hooks/set-state-in-effect': 'warn',
    },
  },
  {
    // Playwright fixtures require the empty destructure `async ({}, use) => {}`
    // and empty `extend<{}, ...>` generics — idiomatic, not a smell here.
    files: ['e2e/**/*.{ts,tsx}'],
    rules: {
      'no-empty-pattern': 'off',
      '@typescript-eslint/no-empty-object-type': 'off',
    },
  },
);

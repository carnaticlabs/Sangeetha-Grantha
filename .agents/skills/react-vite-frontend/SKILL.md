---
name: react-vite-frontend
description: Guidelines for React 19, TypeScript 6.0, Tailwind CSS 4.3, Vite 8.1, and Bun in modules/frontend/sangita-admin-web.
---

# React, Vite & Bun Frontend Guidelines

This skill governs the development and style standards for the admin web application located in `modules/frontend/sangita-admin-web`.

## 1. Programming Paradigm: "Gold-Standard" TypeScript & React
Frontend development must reflect strict, modern type-safety and architectural excellence:
- **Strict Typing**: Never use `any`. Use descriptive interfaces or type definitions for all component props, state objects, and API response contracts. Use TypeScript utility types (e.g. `Pick`, `Omit`, `Partial`) to reuse definitions safely.
- **Functional Components**: All components must be React functional components. Use hooks (`useState`, `useEffect`, `useMemo`, `useCallback`) correctly, ensuring stable dependency arrays.
- **Example of Gold-Standard TypeScript & React**:
  ```tsx
  import React from 'react';

  interface SectionItemProps {
    readonly title: string;
    readonly lyrics: string;
    readonly type: 'PALLAVI' | 'ANUPALLAVI' | 'CHARANAM';
    readonly onSelect: (title: string) => void;
  }

  export const SectionItem: React.FC<SectionItemProps> = ({
    title,
    lyrics,
    type,
    onSelect,
  }) => {
    return (
      <div 
        className="p-4 border rounded-lg hover:shadow-md transition-shadow cursor-pointer"
        onClick={() => onSelect(title)}
      >
        <span className="text-xs uppercase tracking-wider text-muted">{type}</span>
        <h4 className="font-semibold text-lg">{title}</h4>
        <p className="mt-2 text-sm text-foreground/80 line-clamp-3 whitespace-pre-wrap">{lyrics}</p>
      </div>
    );
  };
  ```

## 2. Package & Build Tooling (Bun-centric)
- **Always use Bun**: Use `bun install` for package management, `bun run dev` to start the dev server (port 5001), and `bun run build` for compiling production bundles.
- **Never** use `npm`, `yarn`, or `pnpm` inside `modules/frontend/sangita-admin-web` to avoid lockfile conflicts.

## 3. Styling & Aesthetics
- Use **Tailwind CSS 4.3** utilities.
- Adhere to the project design rules:
  - Prioritize visual excellence: sleeks dark modes, clean gradients, modern typography (Inter/Outfit).
  - Use subtle micro-animations (transitions, transforms on hover) to make the UI feel alive.
  - Avoid crude, unstyled primary colors (e.g., standard red or blue). Use rich, tailored slate, indigo, emerald, or amber palettes.

## 4. Navigation & State
- **Routing**: Manage routing exclusively with `react-router-dom`. Define explicit routes, link paths, and parameters.
- **API Clients**: Keep API requests centralized using a structured fetch wrapper or client pattern to handle auth headers and JWT tokens automatically.

## 5. Testing
- **Unit/Component Testing**: Written with **Vitest**. Run via `bun test` or `bun run test:unit`.
- **E2E Testing**: Managed with **Playwright**. Run via `make test-frontend` or the `e2e-test-runner` workflow.

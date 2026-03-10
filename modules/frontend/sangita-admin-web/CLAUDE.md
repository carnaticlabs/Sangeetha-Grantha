# Admin Web Frontend

React 19 + TypeScript + Vite + Tailwind CSS admin console.

## Quick Reference
```bash
bun install          # Install dependencies
bun run dev          # Dev server on port 5001
bun run build        # Production build
bun run lint         # Lint check
```

## Key Rules
- Function components with explicit TypeScript interfaces for props
- Tailwind utility classes — follow shadcn UI patterns
- `react-router-dom` for routing
- TanStack Query (React Query) for data fetching, caching, and mutations to backend on port 8080
- Strict TypeScript — no implicit `any`
- Keep DTOs synchronized with backend shared domain models

## Project Structure
- `src/pages/` — route-level page components
- `src/components/` — reusable UI components
- `src/services/` — API service functions
- `src/types/` — TypeScript type definitions

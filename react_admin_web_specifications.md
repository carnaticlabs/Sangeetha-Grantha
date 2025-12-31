# Agent Instructions: React Admin Web

## Tech Stack
- **React 19.2**, **TypeScript 5.8**, **Tailwind CSS**.

## Component Design
- Use arrow functions for components.
- Props must be strictly typed via Interfaces.
- UI elements (Badges, Inputs) should be reused from the `src/components` directory.

## State & Data
- Keep UI models in sync with Kotlin Shared DTOs.
- Handle `DRAFT` vs `PUBLISHED` states visually to support the editorial workflow.

| Metadata | Value |
|:---|:---|
| **Status** | Archived |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Archive |

# React Admin Web Specifications

---

> [!NOTE]
> Historical evidence: results, counts, commands, and observations below belong to the original work described here. The editorial update date is not a new test or corpus verification. For present behavior, use [current feature map](./../01-requirements/features/README.md).


**Status:** Draft  
**Version:** 1.1  

---

## Tech Stack

- **React**: 19.2.0
- **TypeScript**: 5.8.3
- **Vite**: 7.1.7 (build tool)
- **Tailwind CSS**: 3.4.13
- **React Router**: Latest (for navigation)
- **React Query (TanStack Query)**: For data fetching and caching
- **Axios/Fetch**: For HTTP requests

---

## Component Design

### General Principles

- Use **arrow functions** for components
- Props must be **strictly typed** via TypeScript interfaces
- UI elements (Badges, Inputs, Buttons, etc.) should be reused from `src/components` directory
- Keep components **small and composable**
- Use **Tailwind utility classes** for styling
- Avoid `any` type; prefer `ReactNode`, `PropsWithChildren`, discriminated unions

### Component Structure

```text
src/
├── components/          # Reusable UI primitives
│   ├── Button.tsx
│   ├── Input.tsx
│   ├── Badge.tsx
│   ├── Toast.tsx
│   └── ErrorBoundary.tsx
├── pages/              # Page-level components
│   ├── KrithiList.tsx
│   ├── KrithiDetail.tsx
│   ├── KrithiEdit.tsx
│   ├── NotationEditor.tsx
│   └── ImportReview.tsx
├── api/                # API client and types
│   ├── client.ts
│   ├── krithiApi.ts
│   ├── notationApi.ts
│   └── types.ts
└── hooks/               # Custom React hooks
    ├── useKrithi.ts
    ├── useNotation.ts
    └── useAuth.ts
```

---

## State & Data Management

### Data Fetching

- Use **React Query** for all server state
- Keep UI models in sync with Kotlin Shared DTOs from `modules/shared/domain`
- Handle `DRAFT`, `IN_REVIEW`, `PUBLISHED`, `ARCHIVED` states visually
- Implement proper loading and error states

### Type Safety

- All API responses must match TypeScript interfaces
- DTOs from shared domain should be mirrored in frontend types
- Use discriminated unions for workflow states

### Example Query Hook

import { useQuery } from '@tanstack/react-query';
import { krithiApi } from '../api/krithiApi';

```text
export function useKrithi(id: string) {
  return useQuery({
    queryKey: ['krithis', id],
    queryFn: () => krithiApi.getById(id),
    enabled: !!id,
  });
}
```

---

## Key Features

### 1. Krithi Management

- **Create/Edit Form**: Includes `musicalForm` dropdown (KRITHI, VARNAM, SWARAJATHI)
- **Workflow State**: Visual indicators and state transition buttons
- **Lyric Variants**: Multi-language/script variant management
- **Sections**: Structural section editor (pallavi, anupallavi, charanams, etc.)

### 2. Notation Management (Varnams & Swarajathis)

- **Notation Variant Editor**: Only shown for compositions with `musicalForm` = VARNAM or SWARAJATHI
- **Notation Type**: SWARA or JATHI selector
- **Tala/Kalai/Eduppu**: Metadata fields for each variant
- **Notation Rows Editor**: 
  - Section-aligned row editor
  - Swara text input
  - Optional sahitya per row
  - Tala markers
  - Order management

### 3. Reference Data Management

- CRUD interfaces for Composers, Ragas, Talas, Deities, Temples
- Search and filter capabilities
- Validation to prevent deletion of in-use entities

### 4. Import Review

- List imported Krithis by status
- Side-by-side comparison of imported vs canonical data
- Mapping interface to link imports to existing entities
- Batch review capabilities

### 5. Search & Filters

- Multi-criteria search (title, composer, raga, tala, etc.)
- Filter by workflow state, musical form, language
- Full-text lyric search (admin view)

---

## API Integration Patterns

### Authentication

- JWT token stored securely (memory or httpOnly cookie)
- Token included in `Authorization: Bearer <token>` header
- Automatic redirect to login on 401 responses

### Error Handling

- Structured error responses from API
- Field-level validation errors displayed inline
- Toast notifications for success/error messages
- Error boundary for unexpected errors

### Cache Management

- React Query handles caching automatically
- Invalidate queries on mutations:
  - Create/update Krithi → invalidate `['krithis']` and `['admin', 'krithis']`
  - Create/update notation → invalidate `['krithis', id, 'notation']`
  - Import mapping → invalidate `['admin', 'imports']`

---

## UI/UX Patterns

### Workflow State Indicators

- **DRAFT**: Gray badge, editable
- **IN_REVIEW**: Yellow badge, limited editing
- **PUBLISHED**: Green badge, read-only (archive only)
- **ARCHIVED**: Red badge, read-only

### Musical Form Indicators

- **KRITHI**: Standard lyric-focused interface
- **VARNAM**: Show notation editor tab
- **SWARAJATHI**: Show both lyrics and notation tabs

### Form Validation

- Client-side validation before API calls
- Required field indicators
- Enum validation (musicalForm, workflowState, etc.)
- UUID format validation for IDs

---

## Implementation Status

### Completed ✅

- Basic component structure
- API client setup
- Authentication flow
- Basic routing

### In Progress 🔄

- Krithi CRUD forms
- Lyric variant management
- Section editor
- Notation editor (Varnams/Swarajathis)
- Import review interface

### Planned 📋

- Advanced search UI
- Bulk operations
- Dashboard analytics
- User management interface

---

## Key Constraints

- ❌ **Never use `any` type** - use proper TypeScript types
- ✅ **Always validate `musicalForm`** before showing notation editor
- ✅ **Use React Query** for all data fetching
- ✅ **Follow existing component patterns** in `src/components`
- ✅ **Keep DTOs in sync** with `modules/shared/domain`

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)

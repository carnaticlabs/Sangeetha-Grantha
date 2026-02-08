| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# ADR-002: Frontend Architecture Decision - React vs Kotlin/JS


## Context

Sangita Grantha requires a modern web-based admin console for content management (Krithis, Composers, Ragas, Talas, Tags, Imports). The platform needed to choose between:

1. **React with TypeScript**: Industry-standard web framework with large ecosystem
2. **Kotlin/JS (Compose for Web)**: Shared codebase potential with KMP mobile app

The admin console requires:
- Rapid development and iteration
- Rich UI components for forms, tables, and content editing
- Integration with REST APIs
- Type-safe client code
- Modern developer experience

## Decision

Choose **React 19 with TypeScript** for the admin web console (`modules/frontend/sangita-admin-web`).

The frontend stack includes:
- **React**: 19.2.0 (functional components, hooks)
- **TypeScript**: 5.8.3 (strict type safety)
- **Vite**: 7.1.7 (modern build tool, fast HMR)
- **Tailwind CSS**: 3.4.13 (utility-first styling)
- **React Router**: 7.11.0 (client-side routing)

## Rationale

The decision was driven by several factors:

1. **Mature Ecosystem**: React has extensive UI libraries, documentation, and community support
2. **Developer Productivity**: TypeScript + React provides excellent DX with type safety and tooling
3. **Performance**: React 19 offers improved performance and Vite provides fast development builds
4. **Hiring & Maintenance**: React skills are widely available, reducing onboarding time
5. **Separation of Concerns**: Admin web is distinct from mobile app; shared UI code was not a primary requirement
6. **UI Library Availability**: Rich component ecosystems (Material UI, Ant Design) accelerate development

**Alternative Considered**: Kotlin/JS with Compose for Web was evaluated but rejected due to:
- Less mature ecosystem for web components
- Smaller community and fewer examples
- Slower iteration cycle
- Limited UI library options

## Implementation Details

### Project Structure

```text
modules/frontend/sangita-admin-web/
├── src/
│   ├── components/        # Reusable UI components
│   ├── pages/            # Route-level page components
│   ├── api/              # API client utilities
│   ├── App.tsx           # Main app router
│   └── index.css         # Tailwind CSS setup
├── package.json          # Dependencies (React 19.2.0, TypeScript 5.8.3)
├── vite.config.ts        # Vite configuration
└── tsconfig.json         # TypeScript strict mode config
```

### Key Patterns

- **Functional Components**: All components use function syntax with hooks
- **TypeScript Strict Mode**: Explicit types, no `any` types
- **Tailwind Utility Classes**: Consistent styling via utility classes
- **Client-Side Routing**: React Router for navigation
- **API Client**: Centralized API utilities for backend communication
- **Bearer Token Auth**: Simple token-based authentication (matches backend)

### Current Implementation Status

✅ **Completed**:
- Core architecture (React 19 + TypeScript + Vite + Tailwind)
- Routing setup (Dashboard, Krithis, Reference Data, Imports, Tags)
- API client utilities
- Basic UI components (Sidebar, TopBar)
- Authentication token handling

🔄 **In Progress**:
- Full CRUD operations for all entities
- Advanced search and filtering
- Rich text editing for lyrics

📋 **Planned**:
- Role-based UI adaptations
- Advanced visualization features
- Bulk import workflows

## Consequences

### Positive

- **Rapid Development**: React ecosystem accelerates feature development
- **Type Safety**: TypeScript catches errors at compile time
- **Modern DX**: Vite provides fast hot module replacement
- **Maintainability**: Well-established patterns and conventions
- **Scalability**: Component-based architecture supports growth
- **Large Talent Pool**: Easy to find developers with React/TypeScript experience

### Negative

- **Separate Codebase**: Admin web code is separate from KMP mobile app (no shared UI code)
- **Additional Dependency**: Another tech stack to maintain (React vs Kotlin)
- **Bundle Size**: React adds runtime overhead (mitigated by code splitting and Vite optimization)

### Neutral

- **Mobile App**: Mobile app uses Kotlin Multiplatform with Compose (separate codebase)
- **Shared Domain**: Domain models are shared via `modules/shared/domain` (KMP)

## Follow-up

- ✅ Core architecture implemented and stable
- ✅ TypeScript strict mode enforced
- ⏳ Add end-to-end testing (Vitest/Playwright) (planned)
- ⏳ Implement advanced features (search, filtering, bulk operations) (in progress)

## References

- [Admin Web PRD](../../01-requirements/admin-web/prd.md)
- [Admin Web UI Specs](../../05-frontend/admin-web/ui-specs.md)
- [Tech Stack](../tech-stack.md)
- [Frontend Architecture Decision](../../02-architecture/decisions/ADR-002-frontend-architecture.md)
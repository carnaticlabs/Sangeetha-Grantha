# Frontend UI Libraries

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


## 1. Admin Web
**Stack**: React 19, TypeScript, Tailwind CSS v4, Vite.

| Category | Library | Purpose |
|---|---|---|
| **Core UI** | **Radix UI** | Accessible, unstyled primitives (Dialog, Popover, Dropdown, Toggle). |
| **Styling** | **Tailwind CSS v4** | Utility-first styling. Configured via `theme.css`. |
| **Icons** | **Lucide React** | Consistent, crisp icon set. |
| **Data Fetching** | **TanStack Query (React Query)** | Server state management, caching, debouncing. |
| **Routing** | **React Router DOM** | Client-side routing. |
| **Forms** | **React Hook Form** | Performant, validation-first forms. |
| **Validation** | **Zod** | Schema validation for forms and API responses. |
| **Toast** | **Sonner** | Opinionated, accessible toast notifications. |

## 2. Mobile (KMP)
**Stack**: Kotlin Multiplatform, Compose Multiplatform.

| Category | Library | Purpose |
|---|---|---|
| **UI** | **Compose Multiplatform** | Shared UI code for Android and iOS. |
| **Components** | **Material 3** | Google's Material Design system implementation. |
| **Navigation** | **Jetpack Navigation** | Type-safe navigation graph. |
| **Image Loading** | **Coil 3** | KMP-native image loading and caching. |
| **Icons** | **Material Icons Extended** | Standard Material icons. |

## 3. Shared Design Tokens
Colors, Typography, and Spacing scales are aligned between Tailwind (Web) and Material Theme (Mobile) to ensure brand consistency.
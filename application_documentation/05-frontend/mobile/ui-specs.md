# Mobile UI Specification (KMP)

> **Status**: Draft | **Version**: 1.0 | **Last Updated**: 2025-01-27
> **Owners**: Mobile Platform Team

**Related Documents**
- [Prd](../../01-requirements/mobile/prd.md)
- [Integration Spec](../../03-api/integration-spec.md)
- [Schema](../../04-database/schema.md)

# Mobile UI Specification (KMP)

## 1. Scope
This document specifies the UI/UX implementation for the Sangita Grantha mobile application using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. It focuses on the read-only discovery experience for Rasikas.

## 2. Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin (100% shared) |
| **UI Framework** | Compose Multiplatform (Android + iOS) |
| **Navigation** | Jetpack Navigation (Compose) |
| **State Management** | Decompose or Voyager (TBD) |
| **Network** | Ktor Client |
| **Image Loading** | Coil 3.x |
| **Local Storage** | SQLDelight / Room (KMP) |

## 3. Navigation Architecture

### Root Graph
- **Home**: Discovery feed (Composers, Ragas, Suggested)
- **Search**: Global search interface
- **Library**: User favourites and history
- **Settings**: App preferences

## 4. Screen Specifications

### 4.1 Home Screen
- **Components**:
  - `HeroCarousel`: Featured Krithis or daily picks.
  - `CategoryRail`: Horizontal scroll of Composers, Ragas, Deities.
  - `RecentList`: Vertical list of recently viewed items.
- **Data Source**: `/v1/krithis/search` (filtered) + Local History.

### 4.2 Search Screen
- **Input**: Debounced text field hitting `/v1/krithis/search`.
- **Filters**: Bottom sheet for Musical Form, Language, Sampradaya.
- **Results**:
  - `KrithiCard`: Title, Incipit, Composer, Raga.
  - Highlight matching substrings in lyrics if applicable.

### 4.3 Krithi Detail Screen
- **Header**: Large title, Composer/Raga subtitles.
- **Tabs**:
  - **Lyrics**: Main view. Segmented control for Script/Language.
  - **Meta**: Detailed metadata, Tags, Temples.
  - **Notation** (Future): Swara/Sahitya view.
- **Actions**: Favourite (Heart icon), Share.

## 5. Shared Design System

### Typography (Material 3)
- `DisplayLarge`: Krithi Titles
- `HeadlineMedium`: Section Headers (Pallavi, Anupallavi)
- `BodyLarge`: Lyric text (Optimize line height for Indian scripts)

### Colors
- **Primary**: Deep Amber/Saffron (Traditional/Spiritual tone)
- **Surface**: Off-white/Paper (Readability focus)
- **Dark Mode**: High contrast slate.

## 6. Integration Notes
- **DTOs**: Directly consume `modules/shared/domain` objects.
- **Error Handling**: Unified `NetworkError` component for offline/server issues.
- **Offline Mode**: Cache `KrithiDto` in local DB for Favorites/History.

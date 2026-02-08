| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# System Flows

This document provides visual representation of key system flows in Sangita Grantha using Mermaid diagrams.

---

## 1. System Architecture Overview

### 1.1 High-Level Architecture

```mermaid
flowchart TB
    subgraph Clients
        MW[Mobile App<br/>iOS/Android]
        AW[Admin Web<br/>React]
    end

    subgraph Backend["Backend (Ktor)"]
        API[API Layer<br/>Routes]
        SVC[Service Layer<br/>Business Logic]
        DAL[Data Access Layer<br/>Exposed ORM]
    end

    subgraph Data
        PG[(PostgreSQL<br/>Database)]
        AUDIT[(Audit Log)]
    end

    subgraph External
        GEMINI[Google Gemini<br/>AI Services]
    end

    MW -->|REST API| API
    AW -->|REST API| API
    API --> SVC
    SVC --> DAL
    DAL --> PG
    SVC --> AUDIT
    SVC -.->|Future| GEMINI
```

### 1.2 Module Structure

```mermaid
flowchart LR
    subgraph Shared["modules/shared"]
        DOM[domain<br/>DTOs & Models]
        PRES[presentation<br/>UI Components]
    end

    subgraph Backend["modules/backend"]
        BAPI[api<br/>Ktor Routes]
        BDAL[dal<br/>Exposed ORM]
    end

    subgraph Frontend["modules/frontend"]
        ADMIN[sangita-admin-web<br/>React Admin]
    end

    DOM --> BAPI
    DOM --> BDAL
    DOM --> ADMIN
    PRES --> ADMIN
    BDAL --> BAPI
```

---

## 2. Authentication Flows

### 2.1 Admin Login Flow

```mermaid
sequenceDiagram
    participant U as Admin User
    participant FE as Admin Web
    participant API as Backend API
    participant DB as Database

    U->>FE: Navigate to /login
    FE->>U: Show login form

    U->>FE: Enter Admin Token + User UUID
    FE->>API: POST /auth/token

    API->>DB: Validate user & token

    alt Valid Credentials
        DB-->>API: User found
        API->>API: Generate JWT
        API-->>FE: 200 OK + JWT
        FE->>FE: Store JWT in localStorage
        FE->>U: Redirect to Dashboard
    else Invalid Credentials
        DB-->>API: User not found
        API-->>FE: 401 Unauthorized
        FE->>U: Show error message
    end
```

### 2.2 Protected API Request

```mermaid
sequenceDiagram
    participant FE as Admin Web
    participant API as Backend API
    participant MW as Auth Middleware
    participant SVC as Service Layer
    participant DB as Database

    FE->>API: GET /v1/admin/krithis<br/>Authorization: Bearer <JWT>
    API->>MW: Validate JWT

    alt Valid JWT
        MW->>MW: Extract claims (userId, roles)
        MW->>SVC: Forward request with context
        SVC->>DB: Query krithis
        DB-->>SVC: Results
        SVC-->>API: KrithiDto[]
        API-->>FE: 200 OK + data
    else Invalid/Expired JWT
        MW-->>API: Reject
        API-->>FE: 401 Unauthorized
    end
```

---

## 3. Krithi Management Flows

### 3.1 Create Krithi Flow

```mermaid
sequenceDiagram
    participant U as Editor
    participant FE as Admin Web
    participant API as Backend API
    participant SVC as Krithi Service
    participant DB as Database
    participant AUDIT as Audit Log

    U->>FE: Fill Krithi form
    U->>FE: Click "Create"

    FE->>API: POST /v1/admin/krithis<br/>{title, composer_id, raga_id, ...}

    API->>SVC: createKrithi(dto, userId)
    SVC->>SVC: Validate input
    SVC->>DB: Insert krithi (workflow_state = 'draft')
    DB-->>SVC: New krithi ID

    SVC->>AUDIT: Log CREATE action
    AUDIT-->>SVC: Logged

    SVC-->>API: KrithiDto
    API-->>FE: 201 Created
    FE->>U: Show success + navigate to detail
```

### 3.2 Edit Krithi with Lyric Variants

```mermaid
sequenceDiagram
    participant U as Editor
    participant FE as Admin Web
    participant API as Backend API
    participant SVC as Service Layer
    participant DB as Database

    U->>FE: Open Krithi detail page
    FE->>API: GET /v1/admin/krithis/{id}
    API->>DB: Fetch krithi + variants + sections
    DB-->>API: Full krithi data
    API-->>FE: KrithiDetailDto
    FE->>U: Display krithi with variants

    U->>FE: Add new lyric variant
    U->>FE: Select language/script
    U->>FE: Enter lyrics by section
    U->>FE: Click "Save Variant"

    FE->>API: POST /v1/admin/krithis/{id}/variants<br/>{language, script, sections[]}
    API->>SVC: createVariant(krithiId, dto)
    SVC->>DB: Insert krithi_lyric_variant
    SVC->>DB: Insert krithi_lyric_sections
    DB-->>SVC: New variant ID
    SVC-->>API: VariantDto
    API-->>FE: 201 Created
    FE->>U: Show updated variant list
```

### 3.3 Workflow State Transitions

```mermaid
stateDiagram-v2
    [*] --> draft: Create Krithi

    draft --> in_review: Submit for Review
    note right of in_review: Reviewer assigned

    in_review --> draft: Request Changes
    note left of draft: Editor revises

    in_review --> published: Approve
    note right of published: Visible to public

    published --> archived: Archive
    note right of archived: Hidden from search

    archived --> published: Restore
```

---

## 4. Search & Discovery Flows

### 4.1 Public Search Flow

```mermaid
sequenceDiagram
    participant U as Mobile User
    participant APP as Mobile App
    participant API as Backend API
    participant DB as Database

    U->>APP: Enter search query
    U->>APP: Apply filters (raga, composer)

    APP->>API: GET /v1/krithis/search?q=xxx&ragaId=yyy

    API->>DB: SELECT krithis<br/>WHERE workflow_state = 'published'<br/>AND (title ILIKE %q% OR incipit ILIKE %q%)

    DB-->>API: Matching krithis
    API-->>APP: SearchResultDto
    APP->>U: Display results list

    U->>APP: Tap on result
    APP->>API: GET /v1/krithis/{id}
    API->>DB: Fetch full krithi detail
    DB-->>API: Krithi + variants + sections
    API-->>APP: KrithiDetailDto
    APP->>U: Display krithi with lyrics
```

### 4.2 Admin Search with Filters

```mermaid
flowchart TB
    subgraph Filters
        Q[Free Text Query]
        C[Composer Filter]
        R[Raga Filter]
        T[Tala Filter]
        W[Workflow State]
        TAG[Tags]
    end

    subgraph API["API Request"]
        REQ["GET /v1/admin/krithis?<br/>q=xxx&<br/>composerId=xxx&<br/>ragaId=xxx&<br/>workflowState=draft"]
    end

    subgraph Query["Database Query"]
        BASE["SELECT * FROM krithis"]
        WHERE["WHERE conditions"]
        JOIN["JOIN composers, ragas, talas"]
        PAGE["LIMIT/OFFSET pagination"]
    end

    Q --> REQ
    C --> REQ
    R --> REQ
    T --> REQ
    W --> REQ
    TAG --> REQ

    REQ --> BASE
    BASE --> WHERE
    WHERE --> JOIN
    JOIN --> PAGE
```

---

## 5. Bulk Import Pipeline

### 5.1 CSV Import Flow

```mermaid
sequenceDiagram
    participant U as Admin
    participant FE as Admin Web
    participant API as Backend API
    participant IMP as Import Service
    participant DB as Database

    U->>FE: Upload CSV file
    FE->>API: POST /v1/admin/imports/upload<br/>Content-Type: multipart/form-data

    API->>IMP: processUpload(file)
    IMP->>IMP: Parse CSV rows

    loop For each row
        IMP->>IMP: Validate row structure
        IMP->>DB: INSERT imported_krithis<br/>(status = 'pending')
    end

    IMP-->>API: ImportResultDto
    API-->>FE: 200 OK + {imported: N, errors: []}
    FE->>U: Show import summary
```

### 5.2 Import Review Workflow

```mermaid
flowchart TB
    subgraph Import["Import State Machine"]
        PEND[pending]
        REV[in_review]
        MAP[mapped]
        REJ[rejected]
        DISC[discarded]
    end

    subgraph Actions["Reviewer Actions"]
        START[Start Review]
        MATCH[Match to Existing]
        CREATE[Create New Krithi]
        REJECT[Reject with Notes]
        DISCARD[Discard Duplicate]
    end

    PEND -->|START| REV
    REV -->|MATCH| MAP
    REV -->|CREATE| MAP
    REV -->|REJECT| REJ
    REV -->|DISCARD| DISC
```

### 5.3 Entity Resolution Flow

```mermaid
sequenceDiagram
    participant REV as Reviewer
    participant FE as Admin Web
    participant API as Backend API
    participant RES as Resolution Service
    participant DB as Database

    REV->>FE: Select imported krithi
    FE->>API: GET /v1/admin/imports/krithis/{id}
    API-->>FE: ImportedKrithiDto

    FE->>API: GET /v1/admin/imports/krithis/{id}/suggestions
    API->>RES: findMatches(rawTitle, rawComposer, rawRaga)

    RES->>DB: Search composers (fuzzy match)
    RES->>DB: Search ragas (fuzzy match)
    RES->>DB: Search existing krithis

    RES-->>API: SuggestionsDto
    API-->>FE: {composers: [], ragas: [], existingKrithis: []}
    FE->>REV: Show suggestions

    REV->>FE: Select/confirm mappings
    REV->>FE: Click "Map to Krithi"

    FE->>API: POST /v1/admin/imports/krithis/{id}/map<br/>{composerId, ragaId, ...}
    API->>RES: mapToKrithi(importId, mappings)
    RES->>DB: Create or link krithi
    RES->>DB: Update import status = 'mapped'
    RES-->>API: KrithiDto
    API-->>FE: 200 OK
    FE->>REV: Show success
```

---

## 6. Data Access Patterns

### 6.1 Repository Pattern

```mermaid
flowchart TB
    subgraph Routes["API Routes (Ktor)"]
        KR[KrithiRoutes]
        CR[ComposerRoutes]
        RR[RagaRoutes]
    end

    subgraph Services["Services"]
        KS[KrithiService]
        CS[ComposerService]
        RS[RagaService]
    end

    subgraph Repositories["Repositories (DAL)"]
        KREP[KrithiRepository]
        CREP[ComposerRepository]
        RREP[RagaRepository]
    end

    subgraph DB["Database"]
        DBQ["DatabaseFactory.dbQuery { }"]
        EXP["Exposed DSL"]
    end

    KR --> KS
    CR --> CS
    RR --> RS

    KS --> KREP
    CS --> CREP
    RS --> RREP

    KREP --> DBQ
    CREP --> DBQ
    RREP --> DBQ

    DBQ --> EXP
```

### 6.2 Audit Logging Pattern

```mermaid
sequenceDiagram
    participant SVC as Service
    participant AUDIT as AuditService
    participant DB as Database

    Note over SVC: Any mutation operation

    SVC->>SVC: Perform mutation
    SVC->>AUDIT: logAction(actor, action, entity, oldValue, newValue)

    AUDIT->>AUDIT: Build audit entry
    Note over AUDIT: {<br/>actor_id: userId,<br/>action: "UPDATE",<br/>entity_type: "krithi",<br/>entity_id: krithiId,<br/>old_value: {...},<br/>new_value: {...},<br/>context: {ip, userAgent}<br/>}

    AUDIT->>DB: INSERT INTO audit_log
    DB-->>AUDIT: Logged
    AUDIT-->>SVC: Success
```

---

## 7. Frontend Component Architecture

### 7.1 Admin Web Information Architecture

```mermaid
flowchart TB
    subgraph App["App.tsx"]
        ROUTER[React Router]
    end

    subgraph Pages
        DASH[Dashboard]
        KLIST[KrithiListPage]
        KDET[KrithiDetailPage]
        COMP[ComposersPage]
        RAGA[RagasPage]
        IMP[ImportsPage]
    end

    subgraph Shared["Shared Components"]
        NAV[Navigation]
        FORM[Form Components]
        TABLE[DataTable]
        MODAL[Modal]
    end

    ROUTER --> DASH
    ROUTER --> KLIST
    ROUTER --> KDET
    ROUTER --> COMP
    ROUTER --> RAGA
    ROUTER --> IMP

    DASH --> NAV
    KLIST --> NAV
    KLIST --> TABLE
    KDET --> FORM
    KDET --> MODAL
```

### 7.2 State Management Flow

```mermaid
flowchart LR
    subgraph ReactQuery["React Query"]
        QC[Query Cache]
        MUT[Mutations]
    end

    subgraph API["API Client"]
        FETCH[fetchKrithis]
        CREATE[createKrithi]
        UPDATE[updateKrithi]
    end

    subgraph Backend
        BAPI[Backend API]
    end

    QC -->|useQuery| FETCH
    MUT -->|useMutation| CREATE
    MUT -->|useMutation| UPDATE

    FETCH --> BAPI
    CREATE --> BAPI
    UPDATE --> BAPI

    BAPI -->|Response| QC
```

---

## 8. Development Workflow

### 8.1 Steel Thread Test Flow

```mermaid
flowchart TB
    subgraph Phase1["Phase 1: Database"]
        DB1[Start PostgreSQL]
        DB2[Apply Migrations]
        DB3[Seed Test Data]
    end

    subgraph Phase2["Phase 2: Backend"]
        BE1[Build Backend]
        BE2[Start Ktor Server]
        BE3[Wait for /health]
        BE4[Test Public API]
        BE5[Test Admin API]
    end

    subgraph Phase3["Phase 3: Frontend"]
        FE1[Install Dependencies]
        FE2[Start Dev Server]
        FE3[Verify Accessible]
    end

    subgraph Phase4["Phase 4: Manual QA"]
        QA1[Services Running]
        QA2[Manual Testing]
    end

    DB1 --> DB2 --> DB3
    DB3 --> BE1 --> BE2 --> BE3 --> BE4 --> BE5
    BE5 --> FE1 --> FE2 --> FE3
    FE3 --> QA1 --> QA2
```

### 8.2 Database Migration Flow

```mermaid
flowchart TB
    subgraph Dev["Development"]
        D1[Create migration file<br/>NN__description.sql]
        D2[Write SQL<br/>migrate:up section]
        D3[Test locally<br/>cargo run -- db migrate]
    end

    subgraph Review["Code Review"]
        R1[Review migration SQL]
        R2[Check for breaking changes]
        R3[Verify documentation]
    end

    subgraph Deploy["Deployment"]
        P1[Apply to staging]
        P2[Verify schema]
        P3[Apply to production]
    end

    D1 --> D2 --> D3
    D3 --> R1 --> R2 --> R3
    R3 --> P1 --> P2 --> P3
```

---

## 9. Related Documents

- [Backend System Design](../backend-system-design.md) - Architecture details
- [API Contract](../../03-api/api-contract.md) - Endpoint specifications
- [ERD](./erd.md) - Entity relationship diagrams
- [Steel Thread Implementation](../../06-backend/steel-thread-implementation.md) - Test workflow
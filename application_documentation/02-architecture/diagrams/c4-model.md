| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# C4 Model Diagrams

This document provides C4 model diagrams for Sangita Grantha at multiple levels of abstraction.

---

## 1. Overview

The [C4 model](https://c4model.com/) provides a hierarchical set of software architecture diagrams:

1. **System Context** - Shows how the system fits into the world
2. **Container** - High-level technology choices and responsibilities
3. **Component** - Decomposition of containers into components
4. **Code** - Implementation details (typically UML, not shown here)

---

## 2. System Context Diagram

This diagram shows Sangita Grantha and its relationships with users and external systems.

```mermaid
C4Context
    title System Context Diagram - Sangita Grantha

    Person(rasika, "Rasika/Learner", "End user searching and viewing Carnatic music compositions")
    Person(editor, "Editor/Curator", "Manages and curates the krithi catalog")
    Person(admin, "Administrator", "Manages users, imports, and system configuration")

    System(sangita, "Sangita Grantha", "Digital compendium of Carnatic classical music compositions with multilingual lyrics and notation")

    System_Ext(karnatik, "Karnatik.com", "External source for krithi data")
    System_Ext(shivkumar, "Shivkumar.org", "External source for notation and lyrics")
    System_Ext(gemini, "Google Gemini", "AI services for transliteration and content validation")

    Rel(rasika, sangita, "Searches and views krithis", "HTTPS/Mobile App")
    Rel(editor, sangita, "Curates krithi content", "HTTPS/Admin Web")
    Rel(admin, sangita, "Manages system", "HTTPS/Admin Web")

    Rel(sangita, karnatik, "Imports krithi data", "Web Scraping")
    Rel(sangita, shivkumar, "Imports notation", "Web Scraping")
    Rel(sangita, gemini, "Transliteration, validation", "HTTPS/API")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

### Context Diagram (Alternative Flowchart)

```mermaid
flowchart TB
    subgraph Users["Users"]
        R[👤 Rasika/Learner]
        E[👤 Editor/Curator]
        A[👤 Administrator]
    end

    subgraph SG["Sangita Grantha System"]
        CORE[🎵 Sangita Grantha<br/>Digital Carnatic Music Compendium]
    end

    subgraph External["External Systems"]
        K[📚 Karnatik.com]
        S[📚 Shivkumar.org]
        G[🤖 Google Gemini]
    end

    R -->|"Search & View<br/>(Mobile App)"| CORE
    E -->|"Curate Content<br/>(Admin Web)"| CORE
    A -->|"Manage System<br/>(Admin Web)"| CORE

    CORE -->|"Import Data"| K
    CORE -->|"Import Notation"| S
    CORE -->|"AI Services"| G

    style CORE fill:#438DD5,color:#fff
    style R fill:#08427B,color:#fff
    style E fill:#08427B,color:#fff
    style A fill:#08427B,color:#fff
```

---

## 3. Container Diagram

This diagram shows the high-level technology choices and how containers communicate.

```mermaid
C4Container
    title Container Diagram - Sangita Grantha

    Person(rasika, "Rasika", "End user")
    Person(editor, "Editor", "Content curator")

    System_Boundary(sangita, "Sangita Grantha") {
        Container(mobile, "Mobile App", "Kotlin Multiplatform", "iOS/Android app for searching and viewing krithis")
        Container(admin, "Admin Web", "React 19, TypeScript", "Web application for content management")
        Container(api, "Backend API", "Kotlin, Ktor", "REST API serving all clients")
        ContainerDb(db, "Database", "PostgreSQL 15", "Stores krithis, variants, notation, and audit logs")
    }

    System_Ext(gemini, "Google Gemini", "AI Services")

    Rel(rasika, mobile, "Uses", "HTTPS")
    Rel(editor, admin, "Uses", "HTTPS")

    Rel(mobile, api, "API calls", "HTTPS/JSON")
    Rel(admin, api, "API calls", "HTTPS/JSON")

    Rel(api, db, "Reads/Writes", "JDBC/SQL")
    Rel(api, gemini, "AI requests", "HTTPS/API")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

### Container Diagram (Alternative Flowchart)

```mermaid
flowchart TB
    subgraph Users
        R[👤 Rasika]
        E[👤 Editor]
    end

    subgraph SG["Sangita Grantha System Boundary"]
        subgraph Clients["Client Applications"]
            MOB["📱 Mobile App<br/><small>Kotlin Multiplatform<br/>iOS & Android</small>"]
            ADM["🖥️ Admin Web<br/><small>React 19 + TypeScript<br/>Vite + Tailwind</small>"]
        end

        subgraph Backend["Backend Services"]
            API["⚙️ Backend API<br/><small>Kotlin + Ktor<br/>REST API</small>"]
        end

        subgraph Data["Data Layer"]
            DB[("🗄️ PostgreSQL 15<br/><small>Krithis, Variants<br/>Notation, Audit</small>")]
        end
    end

    subgraph External["External Services"]
        GEM["🤖 Google Gemini<br/><small>AI Services</small>"]
    end

    R --> MOB
    E --> ADM

    MOB -->|"HTTPS/JSON"| API
    ADM -->|"HTTPS/JSON"| API

    API -->|"JDBC"| DB
    API -.->|"HTTPS"| GEM

    style API fill:#438DD5,color:#fff
    style DB fill:#438DD5,color:#fff
    style MOB fill:#85BBF0,color:#000
    style ADM fill:#85BBF0,color:#000
```

---

## 4. Component Diagram - Backend API

This diagram shows the internal structure of the Backend API container.

```mermaid
flowchart TB
    subgraph Clients["External"]
        MOB["Mobile App"]
        ADM["Admin Web"]
    end

    subgraph API["Backend API Container"]
        subgraph Routes["API Layer (Ktor Routes)"]
            PR["Public Routes<br/><small>/v1/krithis/*<br/>/v1/composers<br/>/v1/ragas</small>"]
            AR["Admin Routes<br/><small>/v1/admin/*<br/>/auth/*</small>"]
            IR["Import Routes<br/><small>/v1/admin/imports/*</small>"]
        end

        subgraph Middleware["Middleware"]
            AUTH["Auth Middleware<br/><small>JWT Validation</small>"]
            CORS["CORS Handler"]
            LOG["Request Logger"]
        end

        subgraph Services["Service Layer"]
            KS["KrithiService<br/><small>CRUD, Search</small>"]
            VS["VariantService<br/><small>Lyrics, Notation</small>"]
            IS["ImportService<br/><small>CSV, Scraping</small>"]
            AS["AuditService<br/><small>Logging</small>"]
            AIS["AIService<br/><small>Gemini Integration</small>"]
        end

        subgraph Repositories["Data Access Layer (Exposed ORM)"]
            KR["KrithiRepository"]
            CR["ComposerRepository"]
            RR["RagaRepository"]
            VR["VariantRepository"]
            IR2["ImportRepository"]
            AR2["AuditRepository"]
        end

        subgraph DB["Database Factory"]
            DBF["DatabaseFactory<br/><small>Connection Pool<br/>Transaction Mgmt</small>"]
        end
    end

    subgraph External["External"]
        PG[("PostgreSQL")]
        GEM["Gemini API"]
    end

    MOB --> PR
    ADM --> AR
    ADM --> IR

    PR --> AUTH
    AR --> AUTH
    IR --> AUTH

    AUTH --> KS
    AUTH --> VS
    AUTH --> IS

    KS --> KR
    KS --> AS
    VS --> VR
    VS --> AS
    IS --> IR2
    IS --> AIS
    IS --> AS

    KR --> DBF
    CR --> DBF
    RR --> DBF
    VR --> DBF
    IR2 --> DBF
    AR2 --> DBF

    DBF --> PG
    AIS --> GEM

    style API fill:#f5f5f5
    style Services fill:#e1f5fe
    style Repositories fill:#fff3e0
```

---

## 5. Component Diagram - Admin Web

This diagram shows the internal structure of the Admin Web container.

```mermaid
flowchart TB
    subgraph User["User"]
        ED["Editor/Admin"]
    end

    subgraph AdminWeb["Admin Web Container"]
        subgraph Pages["Pages (React Router)"]
            LP["LoginPage"]
            DP["DashboardPage"]
            KLP["KrithiListPage"]
            KDP["KrithiDetailPage"]
            IMP["ImportsPage"]
            REF["ReferencePagesComposers, Ragas, Talas"]
        end

        subgraph Components["Shared Components"]
            NAV["Navigation"]
            DT["DataTable"]
            FORM["FormComponents"]
            MOD["Modal"]
        end

        subgraph State["State Management"]
            RQ["React Query<br/><small>Server State</small>"]
            CTX["React Context<br/><small>Auth, Theme</small>"]
        end

        subgraph API["API Layer"]
            AC["API Client<br/><small>Fetch Wrapper</small>"]
            INT["Interceptors<br/><small>Auth, Error</small>"]
        end
    end

    subgraph Backend["Backend"]
        BAPI["Backend API"]
    end

    ED --> LP
    ED --> DP
    ED --> KLP
    ED --> KDP
    ED --> IMP
    ED --> REF

    LP --> CTX
    DP --> RQ
    KLP --> RQ
    KLP --> DT
    KDP --> RQ
    KDP --> FORM
    IMP --> RQ

    RQ --> AC
    AC --> INT
    INT --> BAPI

    style AdminWeb fill:#f5f5f5
    style Pages fill:#e8f5e9
    style State fill:#fff3e0
```

---

## 6. Component Diagram - Data Layer

This diagram shows the database schema organization.

```mermaid
flowchart TB
    subgraph Schema["PostgreSQL Schema"]
        subgraph Reference["Reference Data"]
            COMP["composers"]
            RAGA["ragas"]
            TALA["talas"]
            DEITY["deities"]
            TEMPLE["temples"]
            TAG["tags"]
            SAMP["sampradayas"]
        end

        subgraph Core["Core Entities"]
            KRITHI["krithis"]
            KR["krithi_ragas"]
            KS["krithi_sections"]
        end

        subgraph Lyrics["Lyric Layer"]
            KLV["krithi_lyric_variants"]
            KLS["krithi_lyric_sections"]
        end

        subgraph Notation["Notation Layer"]
            KNV["krithi_notation_variants"]
            KNR["krithi_notation_rows"]
        end

        subgraph Import["Import Pipeline"]
            IS["import_sources"]
            IK["imported_krithis"]
        end

        subgraph System["System Tables"]
            USR["users"]
            ROLE["roles"]
            AUDIT["audit_log"]
        end
    end

    KRITHI --> COMP
    KRITHI --> RAGA
    KRITHI --> TALA
    KRITHI --> DEITY
    KRITHI --> TEMPLE

    KR --> KRITHI
    KR --> RAGA
    KS --> KRITHI

    KLV --> KRITHI
    KLV --> SAMP
    KLS --> KLV
    KLS --> KS

    KNV --> KRITHI
    KNR --> KNV
    KNR --> KS

    IK --> IS
    IK -.-> KRITHI

    AUDIT --> USR

    style Core fill:#e3f2fd
    style Lyrics fill:#f3e5f5
    style Notation fill:#fff8e1
    style Import fill:#e8f5e9
```

---

## 7. Deployment Diagram

This diagram shows how containers are deployed in production.

```mermaid
flowchart TB
    subgraph Internet["Internet"]
        USER["Users"]
    end

    subgraph GCP["Google Cloud Platform"]
        subgraph LB["Load Balancing"]
            GLB["Cloud Load Balancer<br/><small>HTTPS Termination</small>"]
        end

        subgraph Compute["Compute"]
            subgraph CR["Cloud Run"]
                API1["API Instance 1"]
                API2["API Instance 2"]
                APIN["API Instance N"]
            end
        end

        subgraph Storage["Static Hosting"]
            GCS["Cloud Storage<br/><small>Admin Web Assets</small>"]
            CDN["Cloud CDN"]
        end

        subgraph Data["Data Services"]
            SQL["Cloud SQL<br/><small>PostgreSQL 15</small>"]
            SEC["Secret Manager"]
        end

        subgraph Monitoring["Observability"]
            LOG["Cloud Logging"]
            MON["Cloud Monitoring"]
            TRACE["Cloud Trace"]
        end
    end

    USER --> GLB
    GLB --> CDN
    GLB --> CR
    CDN --> GCS

    API1 --> SQL
    API2 --> SQL
    APIN --> SQL

    API1 --> SEC
    API1 --> LOG

    style GCP fill:#f5f5f5
    style Compute fill:#e3f2fd
    style Data fill:#fff3e0
```

---

## 8. Key Architectural Decisions

| Decision | Rationale |
|----------|-----------|
| **Kotlin Multiplatform** | Share domain logic between mobile platforms |
| **Ktor + Exposed** | Lightweight, Kotlin-native backend stack |
| **React 19** | Modern frontend with server components capability |
| **PostgreSQL** | Robust relational DB with excellent text search |
| **Cloud Run** | Serverless scaling, pay-per-use |
| **Layered Architecture** | Clear separation of concerns |

---

## 9. Related Documents

- [ERD](./erd.md) - Entity relationship diagrams
- [Flows](./flows.md) - System flow diagrams
- [Backend System Design](../backend-system-design.md) - Detailed backend architecture
- [Tech Stack](../tech-stack.md) - Technology choices
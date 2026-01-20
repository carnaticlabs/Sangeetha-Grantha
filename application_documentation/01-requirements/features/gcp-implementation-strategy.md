# GCP Implementation Strategy: Serverless & Scalable

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


> **Status**: Recommended | **Version**: 1.0 | **Date**: 2026-01-14
> **Target**: Cost-effective scaling from 0 to 5M+ MAU
> **Philosophy**: "Serverless First" (Pay per use, minimal ops)

## 1. Executive Summary

This document maps the [Global Scale Architecture](./global-scale-architecture.md) to specific **Google Cloud Platform (GCP)** services.

The strategy prioritizes **Cloud Run** (Serverless Compute) and **Cloud SQL** (Managed Database) to minimize operational overhead. This "Scale-to-Zero" capability ensures the platform is incredibly cost-effective during early growth phases ($50-$100/mo) but automagically scales to support millions of users without re-architecture.

## 2. Architecture Diagram (GCP)

```mermaid
graph TD
    User((User)) --> GCLB[Global Ext. Load Balancer]
    GCLB --> Armor[Cloud Armor WAF]
    GCLB --> CDN[Cloud CDN]
    
    subgraph "Edge / Static"
        CDN --> GCS[Cloud Storage (Static Assets)]
    end

    subgraph "Compute (Serverless)"
        GCLB --> |API Traffic| CloudRun_API[Cloud Run: Backend API]
        CloudRun_API --> |Async| PubSub[Pub/Sub]
        PubSub --> CloudRun_Jobs[Cloud Run Jobs: AI Workers]
    end

    subgraph "Data & State"
        CloudRun_API --> |Auth/Fast Read| Redis[Memorystore (Redis)]
        CloudRun_API --> |Transactional| CloudSQL_Primary[Cloud SQL: PostgreSQL]
        
        CloudSQL_Primary -.-> |Rep| CloudSQL_Read[Cloud SQL: Read Replicas]
        CloudRun_API --> |Search| VertexSearch[Vertex AI Search]
    end
    
    subgraph "GCP Managed AI"
        CloudRun_Jobs --> Gemini[Vertex AI (Gemini Models)]
    end
```

---

## 3. Component Mapping

| Generic Component | Recommended GCP Service | Why? |
| :--- | :--- | :--- |
| **Edge / CDN** | **Cloud CDN + Media CDN** | Global anycast IP, deepest edge caching integration with GCS. |
| **Compute / API** | **Cloud Run (Gen 2)** | Scales to 0. Auto-scales to N based on concurrency. No K8s management overhead. |
| **Database** | **Cloud SQL Enterprise Plus** | 99.99% SLA, Data Cache enabled for faster reads. Easier than managing AlloyDB for this scale. |
| **Caching** | **Memorystore for Redis** | Fully managed. "Cluster" tier for eventual global scale, "Standard" for now. |
| **Search** | **Vertex AI Search** | Fully managed semantic & keyword search. Removes need to manage Elasticsearch clusters. |
| **Async Queue** | **Pub/Sub** | Global message bus. Dead-letter queues included. Durable. |
| **Object Store** | **Cloud Storage (GCS)** | Standard class for uploads. Cheap, durable, integrates with CDN. |
| **Security** | **Cloud Armor** | DDoS protection & WAF rules at the edge. |

---

## 4. Implementation Details

### 4.1 Compute: Cloud Run (The Core)
*   **Service**: Deploy the Ktor JAR as a container.
*   **Config**:
    *   `min-instances: 0` (Dev), `min-instances: 1` (Prod - to avoid cold starts).
    *   `cpu-boost: true` (Faster startup).
    *   `concurrency: 80` (Ktor is async, can handle many concurrent requests per container).
*   **Cost Efficiency**: You only pay when a request is processing. Idle containers cost nothing (if min-instances=0).

### 4.2 Database: Cloud SQL
*   **Version**: PostgreSQL 16+.
*   **Connection**: Use **Cloud SQL Auth Proxy** (sidecar in Cloud Run) or **Private Service Connect** for secure, private IP access.
*   **Scaling**:
    *   Start with `db-f1-micro` (shared core) for Dev/Staging (~$10/mo).
    *   Scale to `4 vCPU / 16GB RAM` for Prod Primary.
    *   Add **Read Replicas** in other regions (e.g., `us-central1` primary, `asia-south1` replica) to serve generic read traffic.

### 4.3 Search: Vertex AI Search vs. Elasticsearch
*   **Recommendation**: **Vertex AI Search for Retail/Media**.
*   **Rationale**:
    *   Sangita Grantha is a "media catalog".
    *   Vertex AI Search handles "fuzzy" intent automatically (e.g., "songs by tyagaraja about rama" works out of the box).
    *   **Zero Ops**: No index fragmentation or shard management.
    *   **Vector Search**: Built-in. Can find similar krithis based on lyrical meaning/sentiments.

### 4.4 AI Pipeline: Pub/Sub + Cloud Run Jobs
*   **Trigger**: Editor clicks "Import from URL".
*   **Flow**:
    1.  API publishes message `{"url": "...", "jobId": "123"}` to **Pub/Sub**.
    2.  Pub/Sub push subscription triggers **Cloud Run Service** (or pulls via **Cloud Run Job**).
    3.  Worker calls **Vertex AI (Gemini Pro)** to parse HTML.
    4.  Worker saves draft to Cloud SQL.
*   **Benefit**: Decouples slow AI processing (10s+) from fast API responses (<100ms).

---

## 5. Cost Analysis (Estimates)

### Scenario A: "Bootstrap" (0 - 1k MAU)
*   **Cloud Run**: Free Tier eligible (first 180,000 vCPU-seconds free). **~$0**.
*   **Cloud SQL**: db-f1-micro (Shared). **~$9/mo**.
*   **Redis**: Not needed (use in-memory) OR Redis Tier 2 (Basic). **~$35/mo**.
*   **Total**: **~$10 - $45 / month**.

### Scenario B: "Growth" (100k MAU)
*   **Cloud Run**: 2 vCPU instances, moderate traffic. **~$50/mo**.
*   **Cloud SQL**: 2 vCPU dedicated + 1 Read Replica. **~$150/mo**.
*   **Redis**: Standard HA (5 GB). **~$80/mo**.
*   **Load Balancer + CDN**: Ingress/Egress. **~$40/mo**.
*   **Vertex AI**: Search + Gemini calls. **~$50/mo**.
*   **Total**: **~$370 / month**.

### Scenario C: "Global Scale" (5M+ MAU)
*   **Cloud Run**: Autoscaled to 50+ instances. **~$600/mo**.
*   **Cloud SQL**: Enterprise Plus (High Availability) + 3 Global Replicas. **~$1,200/mo**.
*   **Redis**: Cluster Mode. **~$300/mo**.
*   **Network**: Significant Egress (cached by CDN). **~$500/mo**.
*   **Total**: **~$2,500 - $3,000 / month** (Cost per user: < $0.0006).

---

## 6. Key Advantages of this GCP Stack

1.  **No Kubernetes Tax**: No GKE control plane fees or cluster upgrade nightmares. Cloud Run abstracts the cluster.
2.  **Global Private Network**: Google's premium tier network routes user traffic onto their backbone immediately (at the nearest PoP), bypassing the public internet's congestion.
3.  **Integrated AI**: Vertex AI (Gemini) is co-located with your data. Minimal latency for "smart" features.
4.  **Developer Experience**: `gcloud run deploy` is a single command. CI/CD integration via Cloud Build is trivial.

## 7. Migration Roadmap to GCP

1.  **Containerize**: Ensure `Dockerfile` builds a lean JAR (done).
2.  **IaC (Terraform)**: Script the environment creation (VPC, Cloud SQL, AlloyDB, etc.).
3.  **Deploy Pipeline**: Connect GitHub Actions to **Google Artifact Registry** and **Cloud Run**.
4.  **Database Migration**: Use **Database Migration Service (DMS)** to move local/existing Postgres to Cloud SQL with minimal downtime.
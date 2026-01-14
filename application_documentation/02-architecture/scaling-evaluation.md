# Sangeetha Grantha: Architecture Evaluation & Scaling Strategy

> **Status**: Evaluation | **Version**: 1.0 | **Date**: 2026-01-14  
> **Evaluator**: Architecture Review  
> **Target Scale**: Millions of users worldwide

## Executive Summary

Sangeetha Grantha is a well-architected, domain-driven application with strong musicological modeling and clean separation of concerns. The current architecture is **production-ready for small to medium scale** (thousands of concurrent users), but requires significant enhancements to scale to **millions of users worldwide**.

**Key Strengths:**
- ✅ Clean domain model with proper normalization
- ✅ Modern tech stack (Ktor, PostgreSQL, React)
- ✅ Strong audit and governance
- ✅ Optimized DAL with RETURNING clauses and smart diffing
- ✅ Multi-platform support (KMM mobile, React web)

**Critical Gaps for Global Scale:**
- ❌ No caching layer (every request hits database)
- ❌ No CDN for static assets and API responses
- ❌ No rate limiting on public endpoints
- ❌ Single database instance (no read replicas)
- ❌ No horizontal scaling strategy
- ❌ No geographic distribution
- ❌ Search relies solely on PostgreSQL (no dedicated search engine)
- ❌ Synchronous AI service calls (blocking operations)
- ❌ No message queue for async operations

---

## 1. Current Architecture Analysis

### 1.1 System Components

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Mobile App  │     │ Admin Web   │     │   Public    │
│  (KMM)      │     │  (React)    │     │   Web?      │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       └───────────────────┴───────────────────┘
                          │
                  ┌───────▼───────┐
                  │  Ktor Backend │
                  │  (Single Pod) │
                  └───────┬───────┘
                          │
                  ┌───────▼───────┐
                  │  PostgreSQL   │
                  │  (Primary)    │
                  └───────────────┘
```

**Current Scale Assumptions:**
- Single Ktor server instance
- Single PostgreSQL database (primary only)
- Default HikariCP pool: 10 connections
- No caching infrastructure
- Direct Gemini API calls (synchronous)

### 1.2 Performance Characteristics

**Current Optimizations (Good):**
- ✅ Exposed RETURNING clauses (single round-trip creates/updates)
- ✅ Smart diffing for collection updates (minimizes writes)
- ✅ Trigram indexes for lyric search
- ✅ Normalized schema (reduces redundancy)

**Bottlenecks Identified:**
1. **Database Connection Pool**: 10 connections is insufficient for high concurrency
2. **No Query Result Caching**: Repeated searches hit database every time
3. **No CDN**: Static assets and API responses served from origin
4. **Synchronous AI Calls**: Gemini API calls block request threads
5. **Single Database**: No read scaling capability
6. **No Search Index**: Full-text search on PostgreSQL only (will degrade)

---

## 2. Scaling Challenges & Solutions

### 2.1 Database Layer

#### Current State
- Single PostgreSQL primary instance
- HikariCP pool: 10 connections (default)
- No read replicas
- All queries hit primary database

#### Scaling Strategy

**Phase 1: Connection Pool Optimization**
```kotlin
// Current: maxPoolSize = 10
// Recommended for scale:
maxPoolSize = 50-100 (per instance)
minIdle = 10-20
connectionTimeout = 30_000
maxLifetime = 1_800_000
```

**Phase 2: Read Replicas**
```
Primary (Write) ──┐
                  ├──> Read Replica 1 (US-East)
                  ├──> Read Replica 2 (EU-West)
                  └──> Read Replica 3 (AP-South)
```

**Implementation:**
- Use PostgreSQL streaming replication
- Route read queries to replicas via connection pool routing
- Use primary only for writes and critical reads (e.g., admin operations)

**Phase 3: Connection Pooling Service (PgBouncer)**
- Deploy PgBouncer in transaction pooling mode
- Reduces connection overhead
- Allows more concurrent connections to database

**Phase 4: Database Sharding (Future)**
- If data exceeds single database capacity (>100M krithis)
- Shard by composer_id or raga_id (hash-based)
- Requires application-level routing logic

### 2.2 Caching Layer

#### Current State
- ❌ No caching implemented
- Every request hits database
- Reference data (composers, ragas, talas) queried repeatedly

#### Scaling Strategy

**Phase 1: Application-Level Caching (Redis)**
```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Ktor    │────▶│  Redis   │────▶│PostgreSQL│
│ Backend  │     │  Cache   │     │          │
└──────────┘     └──────────┘     └──────────┘
```

**Cache Strategy:**
1. **Reference Data** (TTL: 1 hour)
   - Composers, Ragas, Talas, Deities, Temples
   - Rarely changes, high read frequency
   - Cache key: `ref:composers`, `ref:ragas`, etc.

2. **Krithi Detail** (TTL: 5 minutes)
   - Full krithi with sections, lyrics, notation
   - Cache key: `krithi:{id}`
   - Invalidate on update/publish

3. **Search Results** (TTL: 1 minute)
   - Paginated search results
   - Cache key: `search:{hash(query+params)}`
   - Short TTL due to frequent updates

4. **Public API Responses** (TTL: 5 minutes)
   - Entire JSON response for `/v1/krithis/{id}`
   - Reduces database load significantly

**Implementation:**
```kotlin
// Add Redis client (e.g., Lettuce or Jedis)
class CacheService(private val redis: RedisClient) {
    suspend fun <T> getOrSet(
        key: String,
        ttl: Duration,
        fetch: suspend () -> T
    ): T {
        val cached = redis.get(key)
        if (cached != null) return deserialize(cached)
        val value = fetch()
        redis.setex(key, ttl.seconds, serialize(value))
        return value
    }
}
```

**Phase 2: CDN for Static Assets**
- Deploy React admin web to CDN (CloudFront, Cloudflare)
- Cache API responses at edge (for public endpoints)
- Cache-Control headers: `public, max-age=300`

**Phase 3: HTTP Response Caching**
- Add ETag support for krithi detail endpoints
- 304 Not Modified responses for unchanged content
- Reduces bandwidth and database queries

### 2.3 Search & Indexing

#### Current State
- PostgreSQL trigram indexes for lyric search
- Full-text search on `krithi_lyric_sections`
- No dedicated search engine

#### Scaling Strategy

**Phase 1: PostgreSQL Full-Text Search Enhancement**
- Use `tsvector` and `tsquery` for better performance
- Create GIN indexes on text search vectors
- Still limited to single database

**Phase 2: Dedicated Search Engine (Elasticsearch/OpenSearch)**
```
┌──────────┐     ┌──────────────┐
│  Ktor    │────▶│ Elasticsearch│
│ Backend  │     │  (Search)    │
└──────────┘     └──────────────┘
```

**Benefits:**
- Fast full-text search across millions of documents
- Faceted search (composer, raga, tala filters)
- Relevance ranking
- Multi-language support
- Geographic distribution

**Implementation:**
- Index krithis on publish/update
- Async indexing via message queue
- Search API queries Elasticsearch, not PostgreSQL
- Fallback to PostgreSQL if Elasticsearch unavailable

**Alternative: PostgreSQL with pg_trgm + Materialized Views**
- If Elasticsearch is too complex, optimize PostgreSQL search
- Pre-compute search indexes in materialized views
- Refresh periodically (e.g., every 5 minutes)

### 2.4 API Layer Scaling

#### Current State
- Single Ktor server instance
- Synchronous request handling
- No rate limiting
- No load balancing

#### Scaling Strategy

**Phase 1: Horizontal Scaling**
```
                    ┌──────────┐
                    │   LB     │
                    │ (Nginx/  │
                    │  ALB)    │
                    └────┬─────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   ┌────▼────┐      ┌────▼────┐      ┌────▼────┐
   │ Ktor 1  │      │ Ktor 2  │      │ Ktor 3  │
   └─────────┘      └─────────┘      └─────────┘
```

**Implementation:**
- Deploy multiple Ktor instances behind load balancer
- Stateless application (no session affinity required)
- Shared Redis cache for session data (if needed)

**Phase 2: Rate Limiting**
```kotlin
// Add rate limiting plugin (e.g., Bucket4j)
install(RateLimiter) {
    rateLimiter = RateLimiter.create(100.0) // 100 req/sec per IP
    // Or use Redis-based distributed rate limiting
}
```

**Rate Limits:**
- Public endpoints: 100 req/min per IP
- Admin endpoints: 1000 req/min per user
- Search endpoints: 50 req/min per IP (more expensive)

**Phase 3: API Gateway (Optional)**
- Use AWS API Gateway or Kong
- Centralized rate limiting, authentication, monitoring
- Request/response transformation

### 2.5 Asynchronous Operations

#### Current State
- Synchronous Gemini API calls
- Blocking request threads
- No background job processing

#### Scaling Strategy

**Phase 1: Message Queue (RabbitMQ/AWS SQS)**
```
┌──────────┐     ┌──────────┐     ┌──────────┐
│  Ktor    │────▶│  Queue   │────▶│  Worker  │
│ Backend  │     │ (SQS)    │     │ (Gemini) │
└──────────┘     └──────────┘     └──────────┘
```

**Use Cases:**
- Web scraping (async)
- Transliteration (async)
- Search index updates (async)
- Email notifications (future)
- Bulk imports (async)

**Implementation:**
```kotlin
// Async job submission
suspend fun transliterateAsync(content: String): JobId {
    val job = TransliterationJob(content)
    queue.enqueue(job)
    return job.id
}

// Worker processes jobs
class TransliterationWorker {
    suspend fun process(job: TransliterationJob) {
        val result = geminiClient.transliterate(job.content)
        // Update database or cache
    }
}
```

**Phase 2: Background Job Framework**
- Use Kotlin Coroutines with persistent job queue
- Or use dedicated framework (e.g., BullMQ, Celery equivalent)

### 2.6 Geographic Distribution

#### Current State
- Single region deployment
- No CDN
- High latency for global users

#### Scaling Strategy

**Phase 1: Multi-Region Deployment**
```
US-East (Primary) ──┐
                    ├──> Database Replication
EU-West ────────────┤
                    └──> AP-South
```

**Architecture:**
- Primary region: US-East (writes)
- Secondary regions: EU-West, AP-South (reads)
- Database: PostgreSQL streaming replication
- Cache: Redis Cluster (multi-region)
- CDN: CloudFront/Cloudflare (edge caching)

**Phase 2: Edge Computing**
- Deploy API Gateway at edge locations
- Cache frequently accessed krithis at edge
- Reduce latency for global users

### 2.7 Monitoring & Observability

#### Current State
- Request logging (Ktor CallLogging)
- Audit logs (database table)
- No metrics/monitoring infrastructure

#### Scaling Strategy

**Phase 1: Application Metrics**
- Add Micrometer/Prometheus metrics
- Track: request rate, latency (p50/p95/p99), error rate
- Database connection pool metrics
- Cache hit/miss rates

**Phase 2: Distributed Tracing**
- Add OpenTelemetry/Jaeger
- Trace requests across services
- Identify bottlenecks

**Phase 3: Alerting**
- Set up alerts for:
  - High error rate (>1%)
  - High latency (p95 > 500ms)
  - Database connection pool exhaustion
  - Cache unavailability

---

## 3. Implementation Roadmap

### Phase 1: Foundation (Months 1-2)
**Priority: Critical**

1. **Connection Pool Tuning**
   - Increase HikariCP pool to 50-100
   - Add connection pool monitoring

2. **Redis Caching**
   - Deploy Redis cluster
   - Implement cache service
   - Cache reference data and krithi details

3. **Rate Limiting**
   - Add rate limiting middleware
   - Protect public endpoints

4. **CDN Setup**
   - Deploy static assets to CDN
   - Cache public API responses

**Expected Impact:**
- 50-70% reduction in database load
- 3-5x improvement in response times
- Support for 10-50k concurrent users

### Phase 2: Scaling (Months 3-4)
**Priority: High**

1. **Read Replicas**
   - Set up PostgreSQL read replicas
   - Route read queries to replicas

2. **Horizontal Scaling**
   - Deploy multiple Ktor instances
   - Add load balancer

3. **Search Engine**
   - Deploy Elasticsearch/OpenSearch
   - Index krithis asynchronously
   - Migrate search endpoints

**Expected Impact:**
- 10x increase in read capacity
- Support for 100k-500k concurrent users
- Sub-100ms search latency

### Phase 3: Advanced (Months 5-6)
**Priority: Medium**

1. **Message Queue**
   - Deploy RabbitMQ/AWS SQS
   - Move AI operations to async

2. **Multi-Region**
   - Deploy to 2-3 regions
   - Set up database replication

3. **Advanced Monitoring**
   - Add distributed tracing
   - Set up alerting

**Expected Impact:**
- Global low-latency access
- Support for 1M+ concurrent users
- Improved reliability

---

## 4. Cost Estimation (Rough)

### Current (Single Region, Small Scale)
- EC2: $50-100/month (t3.medium)
- RDS PostgreSQL: $100-200/month (db.t3.medium)
- **Total: ~$150-300/month**

### Phase 1 (With Caching)
- EC2: $100-200/month (2x t3.medium)
- RDS PostgreSQL: $200-400/month (db.t3.large)
- ElastiCache Redis: $50-100/month
- CloudFront CDN: $20-50/month
- **Total: ~$370-750/month**

### Phase 2 (With Read Replicas)
- EC2: $300-600/month (4x t3.large)
- RDS PostgreSQL: $600-1200/month (primary + 2 replicas)
- ElastiCache Redis: $100-200/month
- Elasticsearch: $200-400/month
- CloudFront CDN: $50-100/month
- **Total: ~$1,250-2,500/month**

### Phase 3 (Multi-Region)
- 3x Phase 2 costs
- **Total: ~$3,750-7,500/month**

---

## 5. Risk Assessment

### High Risk
1. **Database Bottleneck**: Single database will fail under load
   - **Mitigation**: Read replicas + caching (Phase 1-2)

2. **No Rate Limiting**: Vulnerable to DDoS
   - **Mitigation**: Implement rate limiting (Phase 1)

3. **Synchronous AI Calls**: Blocks request threads
   - **Mitigation**: Move to async queue (Phase 3)

### Medium Risk
1. **Search Performance**: PostgreSQL search will degrade
   - **Mitigation**: Elasticsearch (Phase 2)

2. **Single Point of Failure**: No redundancy
   - **Mitigation**: Multi-instance + read replicas (Phase 2)

### Low Risk
1. **Geographic Latency**: Acceptable for v1
   - **Mitigation**: Multi-region (Phase 3)

---

## 6. Recommendations Summary

### Immediate Actions (Before Scale)
1. ✅ **Add Redis caching** for reference data and krithi details
2. ✅ **Increase connection pool** to 50-100
3. ✅ **Add rate limiting** on public endpoints
4. ✅ **Deploy CDN** for static assets

### Short-Term (3-6 months)
1. ✅ **Add read replicas** for database scaling
2. ✅ **Deploy Elasticsearch** for search
3. ✅ **Horizontal scaling** (multiple Ktor instances)
4. ✅ **Message queue** for async operations

### Long-Term (6-12 months)
1. ✅ **Multi-region deployment**
2. ✅ **Advanced monitoring** and alerting
3. ✅ **Database sharding** (if needed)

---

## 7. Architecture Decision Records (ADRs) Needed

1. **ADR-007: Caching Strategy** (Redis vs Memcached vs In-Memory)
2. **ADR-008: Search Engine** (Elasticsearch vs PostgreSQL FTS vs Algolia)
3. **ADR-009: Message Queue** (RabbitMQ vs AWS SQS vs Kafka)
4. **ADR-010: Multi-Region Strategy** (Active-Passive vs Active-Active)
5. **ADR-011: CDN Provider** (CloudFront vs Cloudflare vs Fastly)

---

## 8. Conclusion

Sangeetha Grantha has a **solid foundation** with clean architecture and modern technologies. To scale to **millions of users worldwide**, the following are **critical**:

1. **Caching layer** (Redis) - Reduces database load by 50-70%
2. **Read replicas** - Enables horizontal read scaling
3. **CDN** - Reduces latency and bandwidth costs
4. **Rate limiting** - Protects against abuse
5. **Search engine** - Maintains fast search at scale
6. **Horizontal scaling** - Supports high concurrency
7. **Async operations** - Prevents blocking on AI calls

With these enhancements, the system can scale from **thousands to millions of users** while maintaining the musicological rigor and editorial governance that makes Sangeetha Grantha valuable.

**Estimated Timeline**: 6-12 months for full scaling implementation  
**Estimated Cost**: $1,250-7,500/month depending on scale  
**Expected Capacity**: 1M+ concurrent users with proper implementation

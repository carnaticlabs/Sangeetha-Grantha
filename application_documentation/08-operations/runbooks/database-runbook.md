| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Database Runbook

This runbook provides procedures for database operations, maintenance, backup, and disaster recovery for Sangita Grantha.

---

## 1. Overview

### 1.1 Database Configuration

| Environment | Engine | Version | Instance |
|-------------|--------|---------|----------|
| Local | PostgreSQL | 15 | Docker container |
| Staging | Cloud SQL | 15 | db-f1-micro |
| Production | Cloud SQL | 15 | db-custom-2-8192 |

### 1.2 Connection Information

**Local Development:**
```text
Host: localhost
Port: 5432
Database: sangita_grantha
User: sangita
Password: sangita
```

**Staging/Production:**
- Connection via Cloud SQL Proxy
- Credentials in Secret Manager

---

## 2. Backup Procedures

### 2.1 Local Backup

```bash
# Full database dump
pg_dump -h localhost -U sangita -d sangita_grantha -F c -f backup_$(date +%Y%m%d_%H%M%S).dump

# Schema only
pg_dump -h localhost -U sangita -d sangita_grantha --schema-only -f schema_$(date +%Y%m%d).sql

# Data only
pg_dump -h localhost -U sangita -d sangita_grantha --data-only -f data_$(date +%Y%m%d).sql

# Specific tables
pg_dump -h localhost -U sangita -d sangita_grantha -t krithis -t composers -f krithis_backup.dump
```

### 2.2 Cloud SQL Backup

**Automated Backups:**
- Enabled by default
- Retention: 7 days
- Window: 03:00-04:00 UTC

**Manual Backup:**
```bash
# Create on-demand backup
gcloud sql backups create \
  --instance=sangita-grantha-prod \
  --description="Pre-release backup $(date +%Y%m%d)"

# List backups
gcloud sql backups list --instance=sangita-grantha-prod

# Delete old backup
gcloud sql backups delete <backup-id> --instance=sangita-grantha-prod
```

**Point-in-Time Recovery:**
- Enabled on production
- Allows recovery to any point within retention period

### 2.3 Backup Verification

```bash
# Test restore to local
pg_restore -h localhost -U postgres -d sangita_grantha_test backup.dump

# Verify row counts
psql -h localhost -U sangita -d sangita_grantha_test -c "
  SELECT 'krithis' as table_name, COUNT(*) FROM krithis
  UNION ALL SELECT 'composers', COUNT(*) FROM composers
  UNION ALL SELECT 'ragas', COUNT(*) FROM ragas;
"
```

---

## 3. Restore Procedures

### 3.1 Local Restore

```bash
# Drop and recreate database
dropdb -h localhost -U postgres sangita_grantha
createdb -h localhost -U postgres -O sangita sangita_grantha

# Restore from dump
pg_restore -h localhost -U postgres -d sangita_grantha backup.dump

# Or from SQL
psql -h localhost -U sangita -d sangita_grantha -f backup.sql
```

### 3.2 Cloud SQL Restore

**From Automated Backup:**
```bash
# List available backups
gcloud sql backups list --instance=sangita-grantha-prod

# Restore to same instance (destructive!)
gcloud sql backups restore <backup-id> \
  --restore-instance=sangita-grantha-prod

# Restore to new instance (safer)
gcloud sql instances clone sangita-grantha-prod sangita-grantha-restored \
  --point-in-time='2026-01-29T10:00:00Z'
```

**Point-in-Time Recovery:**
```bash
# Clone to specific point in time
gcloud sql instances clone sangita-grantha-prod sangita-grantha-pitr \
  --point-in-time='2026-01-29T14:30:00Z'

# Verify restored data
# Then swap traffic if needed
```

---

## 4. Migration Procedures

### 4.1 Creating Migrations

**Naming Convention:**
```text
NN__description.sql
Examples:
  07__add-user-preferences.sql
  08__create-favorites-table.sql
```

**Migration Template:**
```sql
-- migrate:up
SET search_path TO public;

-- Your migration SQL here
CREATE TABLE IF NOT EXISTS new_table (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- migrate:down
-- DROP TABLE IF EXISTS new_table;
```

### 4.2 Running Migrations

**Local:**
```bash
cd tools/sangita-cli

# Apply pending migrations
cargo run -- db migrate

# Reset (drop + create + migrate + seed)
cargo run -- db reset

# Check health
cargo run -- db health
```

**Staging/Production:**
```bash
# Connect via Cloud SQL Proxy
./cloud_sql_proxy -instances=PROJECT:REGION:INSTANCE=tcp:5433 &

# Set connection string
export DATABASE_URL="postgres://sangita:<password>@localhost:5433/sangita_grantha"

# Apply migrations
cargo run -- db migrate
```

### 4.3 Migration Best Practices

| Do | Don't |
|----|-------|
| ✅ Use `IF NOT EXISTS` for creates | ❌ Modify existing migrations |
| ✅ Test on staging first | ❌ Use `DROP CASCADE` carelessly |
| ✅ Back up before migrating | ❌ Deploy migrations during peak hours |
| ✅ Include rollback SQL (commented) | ❌ Skip code review for migrations |
| ✅ Update documentation | ❌ Hardcode data in migrations |

### 4.4 Rollback Procedures

> ⚠️ **Warning**: Rollbacks may cause data loss. Always back up first.

**Manual Rollback:**
```sql
-- Uncomment and run the migrate:down section
-- Or manually reverse changes

-- Example: Remove added column
ALTER TABLE krithis DROP COLUMN IF EXISTS new_column;

-- Example: Drop added table
DROP TABLE IF EXISTS new_table;
```

**From Backup:**
```bash
# If migration caused issues, restore from pre-migration backup
gcloud sql backups restore <pre-migration-backup-id> \
  --restore-instance=sangita-grantha-prod
```

---

## 5. Maintenance Procedures

### 5.1 Vacuum and Analyze

**Local:**
```bash
# Full vacuum (locks tables)
psql -h localhost -U sangita -d sangita_grantha -c "VACUUM FULL ANALYZE;"

# Regular vacuum (no locks)
psql -h localhost -U sangita -d sangita_grantha -c "VACUUM ANALYZE;"

# Specific table
psql -h localhost -U sangita -d sangita_grantha -c "VACUUM ANALYZE krithis;"
```

**Cloud SQL:**
- Automatic vacuum enabled by default
- Monitor via Cloud SQL metrics

### 5.2 Index Maintenance

```sql
-- Check index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Find unused indexes
SELECT
    schemaname || '.' || tablename as table,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
AND indexname NOT LIKE '%_pkey';

-- Reindex (if needed)
REINDEX INDEX idx_krithis_title;
```

### 5.3 Query Performance

```sql
-- Enable query stats
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Find slow queries
SELECT
    query,
    calls,
    mean_time,
    total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;

-- Find queries with high I/O
SELECT
    query,
    shared_blks_read,
    shared_blks_hit,
    (shared_blks_hit::float / NULLIF(shared_blks_read + shared_blks_hit, 0)) * 100 as cache_hit_ratio
FROM pg_stat_statements
ORDER BY shared_blks_read DESC
LIMIT 20;
```

### 5.4 Table Statistics

```sql
-- Table sizes
SELECT
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) as total_size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC;

-- Row counts
SELECT
    tablename,
    n_live_tup as row_count
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC;
```

---

## 6. Disaster Recovery

### 6.1 Recovery Time Objectives

| Scenario | RTO | RPO |
|----------|-----|-----|
| Minor outage | 15 min | 5 min |
| Data corruption | 1 hour | 15 min |
| Complete failure | 4 hours | 1 hour |

### 6.2 Failure Scenarios

**Scenario 1: Accidental Data Deletion**

```bash
# 1. Stop application to prevent further changes
gcloud run services update-traffic sangita-grantha-api-prod --to-revisions=REVISION=0

# 2. Identify deletion time from audit log
psql -c "SELECT * FROM audit_log WHERE action = 'DELETE' ORDER BY created_at DESC LIMIT 10;"

# 3. Clone to point before deletion
gcloud sql instances clone sangita-grantha-prod sangita-grantha-recovery \
  --point-in-time='2026-01-29T14:00:00Z'

# 4. Extract deleted data
pg_dump -h <recovery-instance> -t krithis --data-only > deleted_data.sql

# 5. Restore to production
psql -h <prod-instance> -f deleted_data.sql

# 6. Resume traffic
gcloud run services update-traffic sangita-grantha-api-prod --to-revisions=REVISION=100
```

**Scenario 2: Database Corruption**

```bash
# 1. Create backup of corrupted state (for analysis)
gcloud sql backups create --instance=sangita-grantha-prod --description="Corrupted state"

# 2. Restore from last known good backup
gcloud sql backups restore <good-backup-id> --restore-instance=sangita-grantha-prod

# 3. Re-apply any migrations since backup
cargo run -- db migrate

# 4. Verify data integrity
psql -c "SELECT COUNT(*) FROM krithis;"
```

**Scenario 3: Complete Instance Failure**

```bash
# 1. Create new instance from backup
gcloud sql instances create sangita-grantha-prod-new \
  --source-instance=sangita-grantha-prod \
  --source-backup=<backup-id>

# 2. Update Cloud Run to point to new instance
gcloud run services update sangita-grantha-api-prod \
  --set-env-vars="DATABASE_URL=<new-instance-connection>"

# 3. Update DNS if needed

# 4. Delete old instance after verification
gcloud sql instances delete sangita-grantha-prod
```

### 6.3 Recovery Verification

After any recovery:

```sql
-- Verify table counts
SELECT
    'krithis' as table_name, COUNT(*) as count FROM krithis
UNION ALL SELECT 'composers', COUNT(*) FROM composers
UNION ALL SELECT 'ragas', COUNT(*) FROM ragas
UNION ALL SELECT 'audit_log', COUNT(*) FROM audit_log;

-- Verify recent data
SELECT * FROM krithis ORDER BY created_at DESC LIMIT 10;

-- Verify constraints
SELECT conname, contype FROM pg_constraint WHERE conrelid = 'krithis'::regclass;
```

---

## 7. Monitoring

### 7.1 Key Metrics

| Metric | Warning | Critical |
|--------|---------|----------|
| Connection count | > 80% max | > 95% max |
| Disk usage | > 75% | > 90% |
| CPU usage | > 70% | > 90% |
| Memory usage | > 80% | > 95% |
| Replication lag | > 10s | > 60s |

### 7.2 Health Checks

```sql
-- Connection status
SELECT count(*) as connections FROM pg_stat_activity;

-- Lock status
SELECT * FROM pg_locks WHERE NOT granted;

-- Replication status (if applicable)
SELECT * FROM pg_stat_replication;

-- Database size
SELECT pg_size_pretty(pg_database_size('sangita_grantha'));
```

---

## 8. Emergency Contacts

| Role | Contact | Escalation |
|------|---------|------------|
| On-call DBA | ops@sangitagrantha.org | PagerDuty |
| GCP Support | GCP Console | Support ticket |
| Team Lead | lead@sangitagrantha.org | Slack #incidents |

---

## 9. Related Documents

- [Migrations](../../04-database/migrations.md)
- [Schema Overview](../../04-database/schema.md)
- [Deployment Guide](../deployment.md)
- [Steel Thread Runbook](./steel-thread-runbook.md)
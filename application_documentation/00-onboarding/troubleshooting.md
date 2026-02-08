| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Troubleshooting Guide

This guide covers common issues encountered during development and their solutions.

---

## Quick Diagnosis

Before diving into specific issues, run this quick health check:

```bash
# Check all services
docker compose ps                          # Database running?
curl -s http://localhost:8080/health       # Backend healthy?
curl -s http://localhost:5001 > /dev/null && echo "Frontend OK"  # Frontend running?

# Check tool versions
mise exec -- java --version                # Java 25+?
mise exec -- rustc --version               # Rust 1.92+?
mise exec -- bun --version                 # Bun 1.3+?
```

---

## 1. Database Issues

### 1.1 Cannot Connect to Database

**Symptoms:**
- `Connection refused` errors
- Backend fails to start with database errors
- `psql: could not connect to server`

**Solutions:**

```bash
# Check if PostgreSQL container is running
docker compose ps

# If not running, start it
docker compose up -d postgres

# If container exists but not running, check logs
docker compose logs postgres

# Restart container
docker compose restart postgres

# If container is corrupted, recreate
docker compose down -v
docker compose up -d postgres
```

**Common Causes:**
- Docker not running
- Port 5432 already in use by another process
- Container crashed due to resource constraints

### 1.2 Migration Failures

**Symptoms:**
- `cargo run -- db migrate` fails
- SQL syntax errors during migration
- Foreign key constraint violations

**Solutions:**

```bash
# Reset database completely (development only!)
cd tools/sangita-cli
cargo run -- db reset

# If reset fails, manually drop and recreate
psql -h localhost -U postgres -c "DROP DATABASE IF EXISTS sangita_grantha"
psql -h localhost -U postgres -c "CREATE DATABASE sangita_grantha OWNER sangita"
cargo run -- db migrate
```

**Check Migration Syntax:**
```bash
# Verify migration files exist
ls -la database/migrations/

# Check for SQL syntax errors in specific migration
psql -h localhost -U sangita -d sangita_grantha -f database/migrations/NN__migration.sql
```

### 1.3 Permission Denied

**Symptoms:**
- `permission denied for table`
- `permission denied for schema public`

**Solutions:**

```bash
# Grant permissions to sangita user
psql -h localhost -U postgres -d sangita_grantha << 'EOF'
GRANT ALL PRIVILEGES ON DATABASE sangita_grantha TO sangita;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sangita;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO sangita;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sangita;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO sangita;
EOF
```

### 1.4 Data Inconsistencies

**Symptoms:**
- Foreign key violations
- Missing reference data
- Queries return unexpected results

**Solutions:**

```bash
# Verify reference data exists
psql -h localhost -U sangita -d sangita_grantha << 'EOF'
SELECT 'composers' as table_name, COUNT(*) FROM composers
UNION ALL SELECT 'ragas', COUNT(*) FROM ragas
UNION ALL SELECT 'talas', COUNT(*) FROM talas
UNION ALL SELECT 'users', COUNT(*) FROM users;
EOF

# Re-seed if data is missing
./gradlew :modules:backend:api:seedDatabase

# Or full reset
cargo run -- db reset
```

---

## 2. Backend Issues

### 2.1 Backend Won't Start

**Symptoms:**
- Gradle build fails
- Server crashes immediately
- Port binding errors

**Solutions:**

**Build Failures:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew :modules:backend:api:build --refresh-dependencies

# Check for Kotlin version issues
./gradlew --version

# Clear Gradle cache if needed
rm -rf ~/.gradle/caches/
./gradlew build
```

**Port Already in Use:**
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or use a different port
API_PORT=8081 ./gradlew :modules:backend:api:run
```

**Database Connection:**
```bash
# Verify DATABASE_URL is set correctly
echo $DATABASE_URL

# Test connection
psql $DATABASE_URL -c "SELECT 1"
```

### 2.2 Build Errors

**Symptoms:**
- `Unresolved reference` errors
- `Type mismatch` errors
- Serialization errors

**Solutions:**

```bash
# Regenerate Kotlin serialization
./gradlew clean
./gradlew :modules:shared:domain:build
./gradlew :modules:backend:api:build

# If DTO sync issues
# Check modules/shared/domain/src/commonMain/kotlin/...
# Ensure @Serializable annotations are present

# Invalidate IDE caches
# In IntelliJ: File > Invalidate Caches > Invalidate and Restart
```

### 2.3 API Returns 500 Errors

**Symptoms:**
- Internal server errors on API calls
- Stack traces in logs
- Unexpected null values

**Solutions:**

```bash
# Check backend logs for stack trace
# Logs appear in terminal where Gradle is running

# Common issues:
# 1. Database not connected - restart docker compose
# 2. Missing seed data - run seedDatabase
# 3. Null pointer - check for null handling in service layer
```

**Enable Debug Logging:**
```kotlin
// In application.conf or application.local.toml
// Set log level to DEBUG temporarily
```

### 2.4 Authentication Failures

**Symptoms:**
- `401 Unauthorized` on admin endpoints
- JWT validation errors
- Token expired errors

**Solutions:**

```bash
# Verify admin user exists
psql -h localhost -U sangita -d sangita_grantha -c \
  "SELECT id, email FROM users WHERE email = 'admin@sangitagrantha.org'"

# Verify admin token in config
grep -i admin_token config/application.local.toml

# Get fresh token
ADMIN_USER_ID=$(psql -h localhost -U sangita -d sangita_grantha -t -c \
  "SELECT id FROM users WHERE email = 'admin@sangitagrantha.org'" | tr -d ' ')

curl -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d "{\"adminToken\": \"dev-admin-token\", \"userId\": \"$ADMIN_USER_ID\"}"
```

---

## 3. Frontend Issues

### 3.1 Frontend Won't Start

**Symptoms:**
- `bun run dev` fails
- Module not found errors
- Port binding errors

**Solutions:**

**Clean Install:**
```bash
cd modules/frontend/sangita-admin-web
rm -rf node_modules bun.lockb
bun install
bun run dev
```

**Port Already in Use:**
```bash
# Find process using port 5001
lsof -i :5001
kill -9 <PID>

# Or use different port
bun run dev --port 5002
```

### 3.2 API Connection Errors

**Symptoms:**
- Network errors in browser console
- CORS errors
- `Failed to fetch` errors

**Solutions:**

**Verify Backend is Running:**
```bash
curl -s http://localhost:8080/health
# Should return {"status":"ok"}
```

**Check API URL Configuration:**
```bash
# Verify VITE_API_URL in .env.local or environment
echo $VITE_API_URL
# Should be http://localhost:8080

# Create/update .env.local in frontend directory
echo "VITE_API_URL=http://localhost:8080" > .env.local
```

**CORS Issues:**
- Backend must allow `http://localhost:5001` origin
- Check Ktor CORS configuration in backend

### 3.3 TypeScript Errors

**Symptoms:**
- Type errors in IDE
- Build fails with type errors
- `Property does not exist` errors

**Solutions:**

```bash
# Regenerate types
bun run build

# If API types changed, sync with backend DTOs
# Check modules/frontend/sangita-admin-web/src/types/

# Clear TypeScript cache
rm -rf node_modules/.cache
```

### 3.4 Blank Page / White Screen

**Symptoms:**
- App loads but shows nothing
- Console errors about undefined
- React errors

**Solutions:**

```bash
# Check browser console for errors
# Open DevTools (F12) > Console

# Common causes:
# 1. API not returning expected data
# 2. Missing environment variables
# 3. JavaScript runtime errors

# Try hard refresh
# Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)

# Clear local storage
# DevTools > Application > Local Storage > Clear
```

---

## 4. CLI Tool Issues

### 4.1 Rust Build Failures

**Symptoms:**
- `cargo build` fails
- Dependency resolution errors
- Compilation errors

**Solutions:**

```bash
# Update Rust
rustup update stable

# Clean and rebuild
cd tools/sangita-cli
cargo clean
cargo build

# If dependency issues
rm Cargo.lock
cargo build
```

### 4.2 CLI Commands Fail

**Symptoms:**
- `cargo run -- db migrate` fails
- Connection errors from CLI
- Timeout errors

**Solutions:**

```bash
# Verify config exists
ls -la config/application.local.toml

# Check DATABASE_URL
grep -i database config/application.local.toml

# Test database connectivity
cargo run -- db health

# Run with verbose output
RUST_LOG=debug cargo run -- db migrate
```

---

## 5. Docker Issues

### 5.1 Container Won't Start

**Symptoms:**
- `docker compose up` fails
- Container exits immediately
- Resource allocation errors

**Solutions:**

```bash
# Check Docker is running
docker info

# View container logs
docker compose logs postgres

# Remove and recreate
docker compose down -v
docker compose up -d

# If resource issues, prune unused resources
docker system prune -a
```

### 5.2 Volume Permission Issues

**Symptoms:**
- Permission denied on mounted volumes
- Data not persisting

**Solutions:**

```bash
# Fix volume permissions (Linux/Mac)
sudo chown -R $(whoami) ./docker-data/

# Or remove volumes and start fresh
docker compose down -v
docker compose up -d
```

---

## 6. IDE Issues

### 6.1 IntelliJ IDEA

**Gradle Sync Issues:**
1. File > Invalidate Caches > Invalidate and Restart
2. View > Tool Windows > Gradle > Refresh
3. Delete `.idea` folder and reimport project

**Kotlin Issues:**
1. Ensure Kotlin plugin is up to date
2. Check Project Structure > SDKs has correct JDK
3. Invalidate caches and restart

### 6.2 VS Code

**TypeScript Issues:**
1. Cmd/Ctrl+Shift+P > TypeScript: Restart TS Server
2. Check `tsconfig.json` is valid
3. Reinstall TypeScript extension

**ESLint Issues:**
1. Check `.eslintrc` configuration
2. Run `bun run lint` to see errors
3. Reinstall ESLint extension

---

## 7. Common Error Messages

### Error Reference Table

| Error Message | Likely Cause | Solution |
|---------------|--------------|----------|
| `Connection refused` | Database not running | `docker compose up -d postgres` |
| `401 Unauthorized` | Invalid/missing token | Re-authenticate, check token |
| `404 Not Found` | Wrong URL or missing resource | Check endpoint URL, verify data exists |
| `EADDRINUSE` | Port already in use | Kill process or use different port |
| `Module not found` | Missing dependency | `bun install` or `./gradlew build` |
| `CORS error` | Backend CORS misconfigured | Check Ktor CORS settings |
| `Type mismatch` | DTO sync issue | Regenerate shared module |
| `OOM` | Out of memory | Increase Docker/JVM memory |

---

## 8. Getting Help

### Before Asking for Help

1. Check this troubleshooting guide
2. Search existing issues on GitHub
3. Check the logs (backend, frontend, Docker)
4. Try a clean build/reset

### Information to Include

When reporting an issue, include:

```markdown
**Environment:**
- OS: [e.g., macOS 14.x, Ubuntu 22.04]
- Java version: [output of `java --version`]
- Rust version: [output of `rustc --version`]
- Bun version: [output of `bun --version`]

**Steps to Reproduce:**
1. ...
2. ...

**Expected Behavior:**
...

**Actual Behavior:**
...

**Logs/Error Messages:**
```
[paste relevant logs]
```text

**What I've Tried:**
...
```

---

## Related Documents

- [Getting Started](./getting-started.md)
- [Steel Thread Runbook](../08-operations/runbooks/steel-thread-runbook.md)
- [Database Runbook](../08-operations/runbooks/database-runbook.md)
- [Deployment Guide](../08-operations/deployment.md)
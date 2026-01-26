| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# ADR-004: Authentication Strategy - Bearer Token with Role-Based Access Control


## Context

Sangita Grantha requires authentication and authorization for the admin console and API endpoints. The platform needed to choose an authentication strategy that:

1. Secures admin endpoints (content management, imports, user management)
2. Supports role-based access control (RBAC) for fine-grained permissions
3. Integrates with Ktor server framework
4. Scales to multiple users with different roles
5. Supports future expansion (JWT tokens, OAuth, SSO)

The system needs to support:
- Admin authentication for content management operations
- Role-based permissions (admin, editor, reviewer, viewer)
- User management capabilities
- Fine-grained resource-level permissions (krithis, composers, notation, etc.)

## Decision

Adopt **Bearer Token Authentication** (currently simple token-based, migrating to JWT) with **Role-Based Access Control (RBAC)** using capability-based permissions.

### Current Implementation (v1.0)

**Authentication**: Bearer token authentication using Ktor's `bearer` authentication provider
- Simple token comparison against `ADMIN_TOKEN` environment variable
- Default token: `dev-admin-token` (development)
- Token configured via `ADMIN_TOKEN` environment variable or config

**Authorization**: RBAC infrastructure in place (database schema ready)
- `roles` table with `capabilities` JSONB column
- `role_assignments` table linking users to roles
- Authentication middleware protects admin routes
- Full RBAC implementation planned (see below)

### Planned Enhancement (v2.0)

**JWT Token Support**: Migrate from simple bearer tokens to JWT tokens
- Signed JWT tokens with user ID and roles in claims
- Token expiration and refresh token support
- Ktor JWT authentication provider (already included in dependencies)

**Capability-Based RBAC**: Fine-grained permission system
- JSONB capabilities define resource-action permissions
- Route-level authorization interceptors
- Standard roles with defined capabilities

## Rationale

The decision to use bearer token authentication with RBAC was driven by:

1. **Simplicity**: Bearer tokens provide simple, stateless authentication (suitable for development and small teams)
2. **Ktor Integration**: Ktor's bearer authentication provider is built-in and well-documented
3. **RBAC Foundation**: Database schema (roles, role_assignments) provides foundation for fine-grained permissions
4. **Flexibility**: Simple tokens can migrate to JWT without breaking API changes
5. **Security**: Token-based auth separates authentication from authorization
6. **Scalability**: RBAC design supports multiple users with different permission levels

**Bearer Token vs JWT**:
- **Current (Bearer)**: Simple, stateless, no token parsing overhead
- **Planned (JWT)**: Self-contained, supports expiration, includes user context in token

**RBAC Design**:
- **Capability-Based**: JSONB capabilities allow flexible permission definition without schema changes
- **Role-Based**: Standard roles (admin, editor, reviewer, viewer) provide clear permission levels
- **Resource-Action Model**: Permissions defined as `resource.action` (e.g., `krithis.create`, `composers.update`)

## Implementation Details

### Current Authentication Implementation

**Security Plugin** (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Security.kt`):

```kotlin
fun Application.configureSecurity(env: ApiEnvironment) {
    install(Authentication) {
        bearer("admin-auth") {
            authenticate { credentials ->
                if (credentials.token == env.adminToken) {
                    UserIdPrincipal("admin")
                } else {
                    null
                }
            }
        }
    }
}
```

**Configuration**:
- Token loaded from `ADMIN_TOKEN` environment variable (default: `dev-admin-token`)
- Token configured in `ApiEnvironment` and passed to security plugin
- Routes protected with `authenticate("admin-auth")` directive

**Protected Routes**:
- `/v1/admin/*` - All admin endpoints require authentication
- `/v1/krithis/{id}` (PUT/POST) - Mutations require authentication
- Optional auth on public read endpoints for enhanced features

### RBAC Database Schema

**Roles Table** (`database/migrations/01__baseline-schema-and-types.sql`):
```sql
CREATE TABLE IF NOT EXISTS roles (
  code TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  capabilities JSONB NOT NULL DEFAULT '{}'::jsonb
);
```

**Role Assignments Table** (`database/migrations/02__domain-tables.sql`):
```sql
CREATE TABLE IF NOT EXISTS role_assignments (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_code TEXT NOT NULL REFERENCES roles(code) ON DELETE CASCADE,
  assigned_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  PRIMARY KEY (user_id, role_code)
);
```

### Planned RBAC Implementation

**Standard Roles and Capabilities** (from API Coverage Plan):

| Role Code | Description | Key Capabilities |
|-----------|-------------|------------------|
| `super_admin` | Full system access | All capabilities: true |
| `admin` | Content management | CRUD on all content, no user management restrictions |
| `editor` | Content editing | Create/Update on krithis, composers, notation; no delete/publish |
| `reviewer` | Content review | Read all, update workflow state, no delete |
| `viewer` | Read-only access | Read capabilities only |

**Capability Structure (JSONB)**:
```json
{
  "krithis": {
    "create": true,
    "read": true,
    "update": true,
    "delete": true,
    "publish": true
  },
  "composers": {
    "create": true,
    "read": true,
    "update": true,
    "delete": false
  },
  "notation": {
    "create": true,
    "read": true,
    "update": true,
    "delete": false
  },
  "users": {
    "manage": true  // All authenticated users have this
  }
}
```

**User Management Policy**:
- **All authenticated users** have privilege to add, update, and remove users
- User management routes: `/v1/admin/users`
- No additional role checks required (beyond authentication)

**Content Management Policy**:
- Fine-grained RBAC based on resource and action capabilities
- Resources: `krithis`, `composers`, `ragas`, `talas`, `temples`, `tags`, `notation`, `imports`
- Actions: `create`, `read`, `update`, `delete`, `publish` (resource-specific)
- Permission checking: Route-level authorization interceptors

**Planned Authorization Service**:
```kotlin
class AuthorizationService(private val dal: SangitaDal) {
    suspend fun hasPermission(userId: Uuid, permission: Permission): Boolean {
        val roles = dal.users.getUserRoles(userId)
        return roles.any { role ->
            val capabilities = role.capabilities as? JsonObject ?: return@any false
            val resourceCap = capabilities[permission.resource] as? JsonObject ?: return@any false
            val actionValue = resourceCap[permission.action]?.jsonPrimitive?.content
            actionValue == "true" || actionValue == true.toString()
        }
    }
    
    suspend fun requirePermission(userId: Uuid, permission: Permission) {
        if (!hasPermission(userId, permission)) {
            throw SecurityException("User does not have permission: ${permission.resource}.${permission.action}")
        }
    }
}
```

### Current Implementation Status

✅ **Completed**:
- Bearer token authentication implemented (simple token-based)
- Security plugin configured with Ktor authentication
- Admin routes protected with `authenticate("admin-auth")` directive
- RBAC database schema in place (roles, role_assignments tables)
- Token configuration via environment variables
- CORS configured to allow Authorization header

🔄 **In Progress**:
- Full RBAC implementation (authorization service, route interceptors)
- User management routes (`/v1/admin/users`)
- Role management routes (`/v1/admin/roles`)

📋 **Planned**:
- JWT token migration (signed tokens with user ID and roles)
- Capability-based permission checking
- Standard role definitions with capabilities
- Route-level authorization interceptors
- Token expiration and refresh token support

## Consequences

### Positive

- **Simple Implementation**: Bearer tokens are straightforward to implement and debug
- **Stateless**: No server-side session storage required
- **RBAC Ready**: Database schema supports fine-grained permissions
- **Flexible**: Capability-based model allows permission changes without schema updates
- **Scalable**: RBAC design supports multiple users with different roles
- **Standards-Based**: Bearer tokens follow HTTP authentication standards
- **Migration Path**: Can upgrade to JWT without breaking API changes

### Negative

- **Simple Token Security**: Current bearer tokens are shared secrets (no per-user tokens)
- **No Expiration**: Current tokens don't expire (mitigated by environment-specific tokens)
- **No User Context**: Current tokens don't include user ID (all authenticated requests treated as "admin")
- **Manual Token Management**: Tokens managed via environment variables (no token rotation UI)

**Mitigation**:
- JWT migration planned to address token security and user context
- RBAC implementation will provide fine-grained permissions
- Token expiration will be added with JWT migration

### Neutral

- **Development vs Production**: Simple tokens suitable for development; JWT recommended for production
- **Token Storage**: Tokens stored in environment variables (can migrate to secure key management)

## Follow-up

- ✅ Bearer token authentication implemented and working
- ✅ RBAC database schema in place
- ✅ Admin routes protected with authentication
- ⏳ Implement AuthorizationService with capability checking (planned - Phase 4)
- ⏳ Implement user management routes (`/v1/admin/users`) (planned - Phase 3)
- ⏳ Implement role management routes (`/v1/admin/roles`) (planned - Phase 4)
- ⏳ Migrate to JWT tokens with user ID and roles in claims (planned - v2.0)
- ⏳ Add token expiration and refresh token support (planned - v2.0)
- ⏳ Add route-level authorization interceptors (planned - Phase 4)

## References

- [API Coverage Implementation Plan](../../07-quality/api-coverage-implementation-plan.md) - Detailed RBAC implementation plan
- [Security Requirements](../../06-backend/security-requirements.md)
- [Tech Stack](../tech-stack.md) - JWT library (Auth0 JWT 4.5.0) included
- [Database Schema](../../04-database/schema.md) - Roles and role_assignments tables
- [Mutation Handlers](../../06-backend/mutation-handlers.md) - Audit logging requirements

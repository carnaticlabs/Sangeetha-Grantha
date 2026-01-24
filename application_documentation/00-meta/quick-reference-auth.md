| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Quick Reference: Admin Authentication

| Metadata | Value |
|:---|:---|
| **Status** | Current |
| **Version** | 1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |
| **Related Documents** | - [Security Requirements](../06-backend/security-requirements.md)<br>- [API Contract](../03-api/api-contract.md) |

## Overview

Sangita Grantha uses **Bearer token authentication** for admin endpoints. In the current implementation (v1), authentication is simplified using a static bearer token configured via environment variables.

**Note**: Future versions will implement JWT-based authentication with role-based access control (RBAC) as described in the security requirements.

## Admin Authentication (Bearer Token)

### Configuration

The admin token is configured in `config/application.local.toml`:

```toml
[backend]
admin_token = "dev-admin-token"
token_ttl_seconds = 3600
```

Or via environment variable:
```bash
export ADMIN_TOKEN="your-admin-token-here"
```

### Using the Token

All admin endpoints require the `Authorization: Bearer <token>` header.

### cURL Example

```bash
# Get admin audit logs
curl -H "Authorization: Bearer dev-admin-token" \
  http://localhost:8080/v1/audit/logs

# Create a Krithi (example)
curl -X POST \
  -H "Authorization: Bearer dev-admin-token" \
  -H "Content-Type: application/json" \
  -d '{"title": "Vatapi Ganapatim", ...}' \
  http://localhost:8080/v1/admin/krithis
```

### Response

Successful authenticated requests return `200 OK` (or appropriate status codes).

Unauthenticated requests return `401 Unauthorized`:

```json
{
  "error": "Unauthorized",
  "message": "Missing or invalid authentication token"
}
```

## Test Credentials

For local development, the default admin token is `dev-admin-token` (configured in `config/application.local.toml`).

**⚠️ Security Note**: Never use the default token in production. Always use a strong, randomly generated token.

## Common Endpoints

### Public Endpoints (No Auth Required)

- `GET /health` - Health check
- `GET /v1/krithis/search` - Search Krithis
- `GET /v1/krithis/{id}` - Get Krithi details

### Admin Endpoints (Bearer Token Required)

- `GET /v1/audit/logs` - View audit logs
- `GET /v1/admin/krithis` - List all Krithis (including drafts)
- `POST /v1/admin/krithis` - Create Krithi
- `PUT /v1/admin/krithis/{id}` - Update Krithi
- `POST /v1/admin/krithis/{id}/variants` - Add lyric variant
- `POST /v1/admin/krithis/{id}/notation` - Add notation variant
- `GET /v1/admin/dashboard/stats` - Dashboard statistics

See [API Contract](../03-api/api-contract.md) for complete endpoint documentation.

## Quick Test Script

```bash
#!/bin/bash
# Quick auth test script

API_URL="http://localhost:8080"
TOKEN="dev-admin-token"

# Test health (no auth)
echo "Testing health endpoint..."
curl -s "$API_URL/health" | jq .

# Test admin endpoint (with auth)
echo -e "\nTesting admin audit logs..."
curl -s -H "Authorization: Bearer $TOKEN" \
  "$API_URL/v1/audit/logs" | jq .
```

## Using the CLI Tool

The Sangita CLI (`tools/sangita-cli`) handles authentication automatically:

```bash
# Run steel thread test (includes auth verification)
cd tools/sangita-cli
cargo run -- test steel-thread
```

## Troubleshooting

### 401 Unauthorized

- Verify the token matches the configured `admin_token` in config
- Check that the `Authorization: Bearer <token>` header is present
- Ensure the backend is reading the correct config file

### Token Not Working

- Check backend logs for authentication errors
- Verify environment variables are set correctly
- Ensure the token is not expired (if TTL is enforced)

## Future Enhancements

Planned improvements (not yet implemented):

- JWT-based authentication with proper signing
- Role-based access control (RBAC)
- User accounts with email/password
- OTP-based authentication for participants
- Refresh token support
- Token rotation and revocation

See [Security Requirements](../06-backend/security-requirements.md) for the planned security architecture.

## Documentation

- [Security Requirements](../06-backend/security-requirements.md)
- [API Contract](../03-api/api-contract.md)
- [Backend Architecture](../02-architecture/backend-system-design.md)

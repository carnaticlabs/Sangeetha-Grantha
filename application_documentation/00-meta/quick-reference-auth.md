| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-01-29 |
| **Author** | Sangeetha Grantha Team |

# Quick Reference: Admin Authentication

## Overview

Sangita Grantha uses **JWT-based authentication** for admin endpoints. The previous static bearer token approach has been superseded by a proper authentication flow.

Users must now obtain a JWT by providing an **Admin Token** (acting as a master credential) along with their **User ID** or **Email**. This JWT must then be included in the `Authorization` header for all protected requests.

## Admin Authentication Flow

### 1. Obtain a JWT

To get a token, send a `POST` request to `/v1/auth/token`.

**Request Body:**
```json
{
  "adminToken": "dev-admin-token",
  "email": "admin@sangitagrantha.org",
  "roles": ["ADMIN"]
}
```
*Note: You can also use `userId` instead of `email`.*

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresInSeconds": 86400
}
```

### 2. Use the JWT

Include the received token in the `Authorization` header as a Bearer token:

`Authorization: Bearer <your-jwt-token>`

---

## Configuration

Authentication is configured via environment variables in `config/.env.development` (or equivalent):

| Variable | Description | Default (Dev) |
|:---|:---|:---|
| `ADMIN_TOKEN` | Master credential for obtaining JWTs | `dev-admin-token` |
| `JWT_SECRET` | Secret key for signing JWTs | Defaults to `ADMIN_TOKEN` |
| `TOKEN_TTL_SECONDS` | Token lifespan in seconds | `86400` (24h) |
| `JWT_ISSUER` | JWT Issuer claim | `sangita-grantha` |
| `JWT_AUDIENCE` | JWT Audience claim | `sangita-users` |

---

## Tooling & UI

### Login Page
The Admin Web application now features a dedicated login page at `/login`.
1. Enter the **Admin Token** (master secret).
2. Enter your **Email** or **User ID**.
3. Upon success, the JWT is stored in `localStorage` and used for all subsequent API calls.

### API Client
The frontend API client (`modules/frontend/sangita-admin-web/src/api/client.ts`) handles the `Authorization` header automatically once logged in.

### cURL Example

```bash
# 1. Get Token
TOKEN=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"adminToken": "dev-admin-token", "email": "admin@sangitagrantha.org"}' \
  http://localhost:8080/v1/auth/token | jq -r .token)

# 2. Use Token
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/v1/admin/dashboard/stats
```

---

## Common Endpoints

### Public / Auth Endpoints (No JWT Required)

- `GET /health` - Health check
- `POST /v1/auth/token` - Exchange credentials for JWT
- `GET /v1/krithis/search` - Search Krithis
- `GET /v1/krithis/{id}` - Get Krithi details

### Admin Endpoints (JWT Required)

- `POST /v1/auth/refresh` - Refresh an existing JWT
- `GET /v1/audit/logs` - View audit logs
- `GET /v1/admin/krithis` - List all Krithis (including drafts)
- `POST /v1/admin/krithis` - Create Krithi
- `PUT /v1/admin/krithis/{id}` - Update Krithi
- `GET /v1/admin/dashboard/stats` - Dashboard statistics

---

## Troubleshooting

### 401 Unauthorized
- **Login Endpoint**: Verify the `adminToken` matches the server configuration.
- **Admin Endpoints**: Ensure the JWT is present in the `Authorization: Bearer <token>` header and has not expired.
- **Backend Logs**: Check for "Invalid admin token" or "Missing JWT" messages.

### 404 User Not Found
- Ensure the `email` or `userId` provided during login exists in the `users` table.
- For local dev, ensure `database/seed_data/01_reference_data.sql` has been run (`cargo run -- db reset`).

---

## Documentation Links

- [Security Requirements](../06-backend/security-requirements.md)
- [API Contract](../03-api/api-contract.md)
- [Backend Architecture](../02-architecture/backend-system-design.md)

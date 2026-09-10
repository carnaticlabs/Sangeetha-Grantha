| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Admin authentication

---

The Curator Console currently authenticates by exchanging an admin token and an existing user identity for a JWT. Password hashing and account provisioning are implemented, but interactive password/OAuth/OTP login is separate deferred work.

## Provision the account

Run `make bootstrap-admin` with `ADMIN_EMAIL` and `ADMIN_PASSWORD` available in the command's environment. The helper creates or updates the user, stores an argon2id hash, and assigns the seeded `grp_sangita_admin` role. Reference migrations do not create a usable admin account by themselves.

Use the intended local database settings from [configuration](../08-operations/config.md). The account password is not the `ADMIN_TOKEN` used by the current console login.

## Obtain a JWT

Send `POST /v1/auth/token` with the configured admin token and either an existing email or user ID. This illustrative request uses the development default token:

```json
{
  "adminToken": "dev-admin-token",
  "email": "admin@sangitagrantha.org"
}
```

The response fields are `token` and `expiresInSeconds`. Roles are loaded from stored assignments. Do not send a role list to request privileges.

The Login page at `/login` accepts the admin token and email. The [frontend client](../../modules/frontend/sangita-admin-web/src/api/client.ts) stores and sends the resulting JWT. The API also accepts `userId` as an alternative to email.

## Use and refresh the token

Protected requests carry:

```http
Authorization: Bearer <token>
```

`POST /v1/auth/refresh` requires a valid JWT and reloads the user's role assignments. Main admin routes require `grp_sangita_admin`; authentication alone is insufficient. Dashboard statistics are an explicit optional-auth exception in [Routing.kt](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Routing.kt).

## Configuration

| Variable | Purpose | Development loader behavior |
|:---|:---|:---|
| `ADMIN_TOKEN` | Token-exchange credential | Defaults to `dev-admin-token` |
| `JWT_SECRET` | JWT signing key | Falls back to the admin token |
| `TOKEN_TTL_SECONDS` | Access-token lifetime | Defaults to 86400 seconds |
| `JWT_ISSUER` | Token issuer | `sangita-grantha` |
| `JWT_AUDIENCE` | Token audience | `sangita-users` |

These defaults describe local behavior, not production credentials. See [deployment readiness](../08-operations/deployment.md) for the remaining deployment work.

## Diagnose failures

| Symptom | Check |
|:---|:---|
| Token exchange returns 401 | Submitted admin token matches the backend configuration |
| Token exchange returns 404 | User exists; run provisioning against the intended database |
| Token exchange returns 400 | Either email or user ID is supplied and valid |
| Protected call returns 401 | JWT is present, valid, and unexpired |
| Protected call returns 403 | Stored role assignment permits the operation; refresh after a role change |
| Browser calls the wrong server | `VITE_API_BASE_URL` and `API_PROXY_TARGET` |

Do not reset a database to repair a missing user. Provision the account or correct its identity/role. The mounted route is `/v1/auth/token`, not `/v1/admin/login`.

[AuthRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AuthRoutes.kt) · [API contract](../03-api/api-contract.md) · [ADR-004](../02-architecture/decisions/ADR-004-authentication-strategy.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)

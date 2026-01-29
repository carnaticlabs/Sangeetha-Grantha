| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-27 |
| **Author** | Antigravity |

# TRACK-027: Authentication & Login Page Implementation

## 1. Goal
Implement a simple but secure authentication mechanism for the Admin Dashboard to replace the previous bypassing "dev-admin-token" logic. This involves creating a proper Login UI, ensuring the API client handles authentication correctly via JWTs, and providing a consistent developer experience for local setup.

## 2. Requirements
- **Login Page**: A dedicated `/login` route that accepts an Admin Token and User UUID.
- **API Integration**: The `login` function in the client must POST to `/auth/token` and store the received JWT.
- **App Routing**: The root App component should route to `/login` and protect other routes (implicitly or explicitly).
- **Developer Experience**: Provide SQL scripts or instructions to easy create a default admin user for local development.
- **Bug Fix**: Resolve the pending 401 Unauthorized errors caused by invalid token headers.

## 3. Implementation Plan

### 3.1 Frontend Implementation
- [x] Create `Login.tsx` page component.
  - [x] Form with "Admin Token" and "User UUID" fields.
  - [x] Handle submission and error states.
- [x] Update `client.ts`.
  - [x] Add `login` function.
  - [x] Ensure `request` function uses the stored `authToken`.
- [x] Update `App.tsx`.
  - [x] Add `/login` route.
  - [x] Remove the `useEffect` that was auto-setting the invalid token.

### 3.2 Database / Seeds
- [x] Consolidate admin user creation into `database/seed_data/01_reference_data.sql`.
  - [x] Admin user uses `gen_random_uuid()` so ID is stable per DB, idempotent across re-seeds (ON CONFLICT on email).
  - [x] Role assignment uses `SELECT id FROM users WHERE email = 'admin@sangitagrantha.org'` so no hard-coded UUID.

## 4. Verification
- [x] **Manual Test**: Navigate to `/login`, enter `dev-admin-token` and the admin user's UUID (from `SELECT id FROM users WHERE email = 'admin@sangitagrantha.org'`).
- [x] **Success Criteria**: User is redirected to Dashboard, and subsequent API calls (like stats) return 200 OK instead of 401.

## 5. Outcome
- The application now has a functional login flow.
- 401 errors on startup are resolved.
- Local development is standardized with a known admin user.

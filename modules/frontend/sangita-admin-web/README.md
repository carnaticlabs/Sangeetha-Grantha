| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |

# Curator Console

---

The React application for editing Sangeetha Grantha's catalogue, reviewing imports, managing references, and inspecting source evidence. It includes lexical/hybrid/semantic search, a composition editor, curator review, and a consolidated sourcing workspace.

[User and screen guide](../../../application_documentation/05-frontend/admin-web/ui-specs.md) · [API contract](../../../application_documentation/03-api/api-contract.md) · [Local setup](../../../application_documentation/00-onboarding/getting-started.md)

## Develop

From this directory:

```bash
bun install --frozen-lockfile
bun run dev
```

The development server runs on port 5001. Start the backend separately or use `make dev` from the repository root for the Compose stack.

## API configuration

The client defaults to `/v1`. Set `VITE_API_BASE_URL` to override it. Vite proxies `/v1` to `http://localhost:8080`, with `API_PROXY_TARGET` controlling the target when the backend runs in another container. `VITE_API_URL` is not the variable consumed by the client.

The current Vite `envDir` is `../../config` relative to this module, not repository-root `config/`. Do not assume that editing `config/local.env` configures the browser. Prefer explicit process environment and consult [configuration](../../../application_documentation/08-operations/config.md) before changing deployment settings.

Login uses an admin-token exchange with an existing user email; see [authentication](../../../application_documentation/00-meta/quick-reference-auth.md). Backend authorization governs access.

## Checks

```bash
bun run typecheck
bun run test:unit
bun run build
```

Additional scripts include `lint`, `test:e2e`, `test:e2e:money`, `test:e2e:headed`, `test:e2e:debug`, and `test:e2e:report`. Browser tests require the configured test environment; see [E2E testing](../../../application_documentation/07-quality/qa/e2e-testing.md).

## Source map

- [App.tsx](./src/App.tsx): routes, application shell, lazy-loaded sourcing pages.
- [api/client.ts](./src/api/client.ts): transport, authentication, editorial requests.
- [pages](./src/pages): catalogue, editor, imports, references, curator, sourcing experiences.
- [components](./src/components): shared controls, editors, forms, and layout.
- [e2e](./e2e): browser fixtures, page objects, and test journeys.

Versions are maintained in [Current Versions](../../../application_documentation/00-meta/current-versions.md). User/role pages and some sourcing quality responses remain incomplete; the [feature map](../../../application_documentation/01-requirements/features/README.md) documents those boundaries.

# Session-Based Authentication Design (B2C + B2B)

Status: Draft — design approved, implementation pending
Author: quang.ha
Last updated: 2026-07-19

## 1. Background

`auth-session-service` is a Spring Boot 3.3.8 / Java 21 skeleton (`com.dumy`) intended to manage authentication and authorization for both B2C and B2B customers.

The data model already reflects multi-tenancy intent:

- `User` — an individual account (`id`, `username`, `password`, timestamps).
- `Tenant` — an organization (`id`, `name`, `createdAt`).
- `TenantUser` — join entity linking `User` ↔ `Tenant`, with a `status` column and a unique `(tenant_id, user_id)` constraint.

Nothing currently consumes these entities: there is no `service/`, `controller/`, `dto/`, or auth-related package. Login, sessions, and tenant selection do not exist yet.

## 2. Goals

- Authenticate a user with username/password and issue a **session-based (opaque) ID**, not a JWT.
- Support **B2C**: a user with no tenant memberships logs straight in.
- Support **B2B**: a user who belongs to one or more tenants selects a tenant at login time; the resulting session is bound to exactly that one tenant.
- Keep the design simple and consistent with the existing 3-tier skeleton style (reuse `ApiResponse`, `BusinessException`, `ErrorCode`, `GlobalExceptionHandler`, `ObjectsValidator`).

## 3. Non-goals (explicitly out of scope)

- Roles/permissions (RBAC) within a tenant. Session only carries identity (`user_id`) + tenant context (`tenant_id`); authorization decisions are left for a future pass. The design must not block adding a `role` column to `TenantUser` later.
- Redis or any external session store.
- JWT / stateless tokens.
- Spring Security integration (filter chain, `UserDetailsService`, etc.) — revisited only if RBAC is added later.
- Tenant slug/subdomain-based login (e.g. `acme.myapp.com`) — `Tenant` has no domain column and there's no routing layer to exercise it yet.

## 4. Key design decisions

| Decision | Choice | Why |
|---|---|---|
| Session storage | New DB table `tbl_session` (opaque UUID id = the token) | No new infra dependency; fits existing JPA/H2 stack; trivially revocable (delete/flag a row). |
| Tenant binding | One active tenant per session, set at creation, immutable | Keeps the mental model simple: a session is "this user, in this context." Switching tenants = new session, not mutating an existing one. |
| Tenant selection UX | Always require explicit tenant pick when the user has ≥1 tenant, even if only one | Predictable contract: "0 tenants → logged straight in; ≥1 → always pick." Auto-selecting a sole tenant is a trivial future optimization. |
| Credential re-verification on tenant selection | Step 2 (`login/tenant`) re-checks username/password rather than using a pre-auth ticket | Avoids a second stateful concept (extra table / short-lived token) just to bridge two calls. |
| Session validation mechanism | Custom `OncePerRequestFilter`, not Spring Security | No Spring Security dependency exists today; RBAC (where it would pay off) is out of scope; matches the project's plain, unmagical style. Still swappable for a real `AuthenticationProvider` later since lookup logic lives in `SessionService`. |
| Expiration model | Absolute TTL, fixed at creation | Simplest; no per-request write. Sliding expiration is a one-line future change in the filter. |

## 5. Data model

### `tbl_session` (new)

| Column | Type | Notes |
|---|---|---|
| `id` | varchar (PK, UUID) | The opaque session token itself — no separate token column. |
| `user_id` | varchar, FK → `tbl_user.id`, not null | Session owner. |
| `tenant_id` | varchar, FK → `tbl_tenant.id`, **nullable** | Null = B2C session. Set once, never mutated. |
| `created_at` | timestamp, not null | Audit field (`@CreatedDate`). |
| `expires_at` | timestamp, not null | `created_at + ttl`, computed once at creation. |
| `revoked_at` | timestamp, nullable | Null = active; set on logout. `isValid = revoked_at == null && expires_at > now`. |

Indexes: `user_id`, `tenant_id`, `expires_at` (for cleanup queries).

Optional/future columns (not required for MVP): `user_agent`, `ip_address` — pure metadata, no logic depends on them.

**Deliberately excluded:** any role/permission data. Role resolution for a tenant-bound session always goes through `TenantUserRepository.findByTenant_IdAndUser_Id` at the point of use — this is what keeps RBAC addable later without a session-table migration.

### Existing `TenantUser.status`

Already present but unused. Treat as membership state (e.g. `ACTIVE` / `PENDING`) and filter on it everywhere tenant membership is checked during login, so a pending/disabled membership can't establish a session.

### Updated ER overview

```
tbl_tenant  (id, name, created_at)
tbl_user    (id, username, password, created_at, updated_at)
tbl_tenant_user (id, tenant_id -> tenant, user_id -> user, status, created_at)  [unique: tenant_id+user_id]
tbl_session (id, user_id -> user, tenant_id -> tenant [nullable], created_at, expires_at, revoked_at)
```

(`dbdiagram.txt` in the repo root should be updated to add `tbl_session` in this shape.)

## 6. API design

### `POST /api/v1/auth/register`
Request: `{ username, password }`
Creates a `User` with a BCrypt-hashed password. Reuses existing `USERNAME_ALREADY_TAKEN` error.

### `POST /api/v1/auth/login`
Request: `{ username, password }`

Flow:
1. Look up user by username → not found → `INVALID_CREDENTIALS`.
2. Verify password → mismatch → `INVALID_CREDENTIALS` (same error as "not found," to avoid username enumeration).
3. Look up active tenant memberships (`TenantUser` where `status = ACTIVE`):
   - **None** → B2C → create `Session(tenant=null)` → respond `AUTHENTICATED` + session token.
   - **≥1** → respond `TENANT_SELECTION_REQUIRED` + list of `{tenantId, tenantName}` (no session issued yet).

### `POST /api/v1/auth/login/tenant`
Request: `{ username, password, tenantId }`

Flow:
1. Re-verify credentials (same check as `login`).
2. Confirm active membership in `tenantId` → not a member → `USER_NOT_MEMBER_OF_TENANT`; unknown tenant → `TENANT_NOT_FOUND`.
3. Create `Session(tenant=tenantId)` → respond `AUTHENTICATED` + session token.

### `POST /api/v1/auth/logout` (protected)
Revokes the current session (`revoked_at = now`). Idempotent.

### `GET /api/v1/auth/me` (protected, optional)
Returns the current `userId` / `tenantId` resolved from the session, for manual testing.

### Response shape

Single `LoginResponse` DTO with a `status` discriminator (`AUTHENTICATED` | `TENANT_SELECTION_REQUIRED`) rather than modeling tenant-selection as an error — it's a normal flow branch, not a failure, so it doesn't go through `GlobalExceptionHandler`.

## 7. Session validation

Every protected request carries the token in header `X-Session-Token`.

- `SessionContext` — request-scoped (ThreadLocal) holder exposing `getUserId()`, `getTenantId()` (`Optional`), `isAuthenticated()`.
- `SessionAuthFilter` — `OncePerRequestFilter`:
  - Allowlists `register`, `login`, `login/tenant`, Swagger, and H2 console paths.
  - Missing/invalid/expired/revoked token on a protected path → writes `ApiResponse.error(...)` with HTTP 401 directly (filters run before `@ControllerAdvice`, so `GlobalExceptionHandler` can't catch this).
  - Valid token → populates `SessionContext`, proceeds down the chain, then **clears the ThreadLocal in a `finally` block** to avoid leaking context across pooled threads.
- Controllers/services read identity exclusively through `SessionContext`, never by parsing the header directly.

## 8. Session lifecycle

- **Creation**: TTL from config (`app.session.ttl-hours`, default 24).
- **Validation**: not found → `SESSION_NOT_FOUND`; revoked → `SESSION_REVOKED`; expired → `SESSION_EXPIRED`.
- **Revocation**: logout sets `revoked_at`.
- **Cleanup**: a scheduled job (`@Scheduled`, cron-configurable) deletes rows past `expires_at`. Requires `@EnableScheduling`.

## 9. Error codes (new)

Following the existing `ERROR_<httpStatus>_<sequentialCode>` convention (current codes end at 2002/4001/5000); new range starts at 3001:

| Enum | Code | HTTP | Message |
|---|---|---|---|
| `ERROR_401_3001` | 3001 | 401 | Invalid credentials |
| `ERROR_401_3002` | 3002 | 401 | Session not found |
| `ERROR_401_3003` | 3003 | 401 | Session expired |
| `ERROR_401_3004` | 3004 | 401 | Session revoked |
| `ERROR_404_3005` | 3005 | 404 | Tenant not found |
| `ERROR_403_3006` | 3006 | 403 | User is not a member of this tenant |
| `ERROR_400_3007` | 3007 | 400 | Tenant id is required for this account |

## 10. Package structure

| Package | New files |
|---|---|
| `entity/` | `Session.java` |
| `repository/` | `SessionRepository.java` |
| `dto/` | `RegisterRequest`, `LoginRequest`, `TenantLoginRequest`, `LoginResponse`, `TenantOptionResponse` |
| `service/` | `SessionService`, `AuthService`, `SessionCleanupScheduler` |
| `controller/` | `AuthController` |
| `session/` | `SessionContext`, `SessionAuthFilter` |
| `config/` | `PasswordConfig` (BCrypt bean); extend `ThreadPoolConfig` (+`@EnableScheduling`), `SwaggerConfig` (+ optional security scheme) |
| `exception/` | extend `ErrorCode`, `GlobalExceptionHandler` |

New dependency: `spring-security-crypto` (for `BCryptPasswordEncoder` only — not the full Spring Security filter chain).

## 11. Milestones

| # | Milestone | Key files |
|---|---|---|
| M0 | Dependencies & config | `pom.xml`, `application.yaml` |
| M1 | Data model | `entity/Session.java`, `repository/SessionRepository.java`, `dbdiagram.txt` |
| M2 | Error handling | `ErrorCode.java`, `GlobalExceptionHandler.java`, `config/PasswordConfig.java` |
| M3 | DTOs | `dto/*` |
| M4 | `SessionService` | create/validate/revoke/find-expired |
| M5 | `AuthService` | register/login/loginWithTenant/logout |
| M6 | `AuthController` | wire endpoints |
| M7 | Filter & context | `session/SessionContext.java`, `session/SessionAuthFilter.java` |
| M8 | Scheduled cleanup | `SessionCleanupScheduler`, `@EnableScheduling` |
| M9 | Test-support data | seed `data.sql` or minimal tenant test endpoints |
| M10 | Docs & verification | `README.md`, this doc, `dbdiagram.txt` |

## 12. Verification plan (manual, via Swagger on :8088)

1. Register → B2C login → call a protected endpoint with `X-Session-Token` → logout → confirm the same token now returns 401.
2. Seed a tenant + active membership for a user → login → expect `TENANT_SELECTION_REQUIRED` with the tenant listed → `login/tenant` → confirm a protected call resolves `tenantId` via `SessionContext`.
3. Confirm a user who isn't a member of a tenant gets `USER_NOT_MEMBER_OF_TENANT` from `login/tenant`.
4. Shrink `app.session.ttl-hours` for a quick test → confirm an expired session is rejected with `SESSION_EXPIRED` and is removed by `SessionCleanupScheduler` after the cleanup interval.

## 13. Future extensions (not built now, but not blocked)

- RBAC: add a `role` column to `TenantUser`, resolve authorization per-request via `SessionContext.getTenantId()` + `TenantUserRepository`.
- Sliding session expiration.
- Pre-auth ticket for tenant selection (avoids resending credentials in step 2).
- Tenant slug/subdomain-based login.
- Swap the custom filter for a real Spring Security `AuthenticationProvider` if RBAC/OAuth2 is ever needed.

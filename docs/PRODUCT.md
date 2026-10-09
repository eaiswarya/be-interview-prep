# Product Document — Backend Interview Prep

| | |
|---|---|
| **Status** | Draft v1 |
| **Date** | 2026-10-09 |
| **Source** | *Backend Interview Prep Assignment (Java / Spring Boot)* |
| **Repo** | `eaiswarya/be-interview-prep` |

---

## 1. Purpose

Build five independent backend features in one Spring Boot service, each shipped as its own branch, PR, and merge. The goal is to show working code, clean structure, tests, and Git/PR discipline, and to be able to explain every design decision and its alternatives in a follow-up interview.

### Goals
- Deliver Q1–Q5 as production-style REST APIs on one shared, layered code base.
- Every feature has automated tests, including concurrency tests where correctness under load is a requirement.
- Uniform conventions across all features: one error format, one package layout, and Flyway-managed schema.
- Each PR description records the decisions and trade-offs, so it doubles as interview notes.

### Non-goals
- Front-end or UI.
- Production deployment, CI/CD pipelines, multi-region setup.
- Optional stretch items (listed in section 9) until all five core questions are merged.

### Constraints
| Constraint | Value |
|---|---|
| Language / framework | Java 21 (17+ required), Spring Boot 3.5.x (3.x required) |
| Build | Maven (wrapper committed) |
| Database | PostgreSQL 16 |
| Time box | 2 h for all 5 PRs (Q1/Q2 ≈ 15 min, Q3/Q4 ≈ 25 min, Q5 ≈ 40 min) + 30 min for video |
| Secrets | None hard-coded; supplied by env vars / gitignored `.env` |

---

## 2. Users & clients

| Actor | Description |
|---|---|
| API client | Web and mobile apps calling the REST API. They are stateless, so auth uses tokens, not sessions. |
| USER | Authenticated end user (Q3). |
| ADMIN | Authenticated user with admin privileges (Q3). |
| Interviewer | Reviews PRs and asks the author to explain or change code live. |

---

## 3. Architecture

### 3.1 Layered structure

```
com.bemock.prep
├── controller   HTTP ↔ DTO mapping, @Valid input, status codes. No business logic.
├── service      Business rules, @Transactional boundaries, entity ↔ DTO mapping.
├── repository   Spring Data JPA interfaces; queries, specifications, locks.
├── model        JPA entities (extend BaseEntity: id, createdAt, updatedAt).
├── dto          Request/response records; ApiErrorResponse.
├── exception    ApiException hierarchy + GlobalExceptionHandler.
└── config       Spring configuration (security, cache, OpenAPI) as features need it.
```

Request flow: `HTTP → Controller (@Valid DTO) → Service (@Transactional) → Repository → PostgreSQL`, with exceptions bubbling up to `GlobalExceptionHandler`.

**Rules**
- Controllers never touch repositories or return entities.
- Services throw `ApiException` subclasses (`ResourceNotFoundException` → 404, `ConflictException` → 409, `BadRequestException` → 400). They never build HTTP responses.
- Schema changes go in Flyway migrations only (`V<n>__<desc>.sql`). Hibernate runs with `ddl-auto=validate`.
- `spring.jpa.open-in-view=false`, so lazy loading cannot leak into the web layer.

### 3.2 Error contract (all features)

Every error response has this shape, whatever the cause:

```json
{
  "timestamp": "2026-10-09T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tasks",
  "fieldErrors": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

| Situation | Status |
|---|---|
| Bean validation failure / malformed JSON / bad parameter type | 400 |
| Not authenticated (Q3) | 401 |
| Authenticated but wrong role (Q3) | 403 |
| Unknown resource or route | 404 |
| Unsupported HTTP method | 405 |
| Business conflict (duplicate, insufficient stock) | 409 |
| Expired resource (Q2) | 410 |
| Unexpected error (no internal details leaked) | 500 |

`fieldErrors` only appears when there are field errors.

### 3.3 Tech stack

| Concern | Choice | Why |
|---|---|---|
| Persistence | Spring Data JPA + PostgreSQL | Relational integrity, row locks, and conditional updates for Q2/Q5 concurrency |
| Migrations | Flyway | Versioned, reviewable schema history per PR |
| Validation | Jakarta Bean Validation | Declarative, field-level messages |
| Tests | JUnit 5, MockMvc, Testcontainers (PostgreSQL) | Concurrency and locking behaviour must be tested on the real engine; H2 behaves differently |
| Ops | Spring Boot Actuator (`/actuator/health`) | Liveness check |
| Local DB | `docker-compose.yml` (postgres:16-alpine) | One command to start |

---

## 4. Feature requirements

### Q1 — Task Manager API  · branch `feature/q1-task-api`

**Domain:** `Task { id, title, description, status, dueDate, createdAt }`
`status ∈ { TODO, IN_PROGRESS, DONE }`

| Rule | Detail |
|---|---|
| title | required, ≤ 100 chars |
| description | optional |
| dueDate | must not be in the past (`@FutureOrPresent`) |
| createdAt | set by the server |

**Endpoints**

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/tasks` | 201 + `Location` | 400 |
| GET | `/api/tasks?status=TODO` | 200 (list, optional filter) | 400 bad status |
| GET | `/api/tasks/{id}` | 200 | 404 |
| PUT | `/api/tasks/{id}` | 200 | 400, 404 |
| DELETE | `/api/tasks/{id}` | 204 | 404 |

**Acceptance:** invalid input → 400 with field messages; unknown id → 404; ≥ 1 automated test.

---

### Q2 — URL Shortener  · branch `feature/q2-url-shortener`

**Domain:** `ShortUrl { id, code (≤ 8, unique, URL-safe), originalUrl, expiresAt?, visitCount, createdAt }`

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/urls` `{ url, expiresAt? }` | 201 `{ code, shortUrl }` | 400 invalid URL / past expiry |
| GET | `/{code}` | 302 redirect to the original URL | 404 unknown, 410 expired |
| GET | `/api/urls/{code}/stats` | 200 `{ originalUrl, visitCount, createdAt }` | 404 |

**Decisions to make and defend**
- **Code generation:** Base62 encoding of a sequence id (short, collision-free) vs. random Base62 with a retry on unique-constraint violation (not guessable). Either way, a DB unique index is the final guarantee.
- **Same URL twice:** return the existing code (idempotent, saves space) **or** a new code each time (separate stats per link). The decision has to be written down. **Chosen:** a new code every time, so each link has its own expiry and stats and creation needs no lookup or race handling.
- **Accurate counts under load:** one atomic `UPDATE short_url SET visit_count = visit_count + 1 WHERE code = ?`, not read-modify-write in Java.

**Acceptance:** duplicate-shorten behaviour is explained; concurrent visit test shows an exact count; ≥ 1 automated test.

---

### Q3 — Authentication & Roles  · branch `feature/q3-auth`

**Domain:** `User { id, email (unique), passwordHash, role ∈ {USER, ADMIN}, createdAt }`

| Method | Path | Access | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | public | BCrypt password hash; 409 if the email exists |
| POST | `/api/auth/login` | public | returns a JWT, `expiresIn = 900` s |
| GET | `/api/users/me` | USER, ADMIN | own profile |
| GET | `/api/users` | ADMIN | list all users |

**Non-functional**
- Stateless: `SessionCreationPolicy.STATELESS`, JWT signed with HS256. The signing key comes from the `JWT_SECRET` env var and is never committed.
- Token TTL is 15 minutes.
- A custom `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) write `ApiErrorResponse` JSON, not an HTML page.

**Acceptance:** a test proves USER → admin endpoint returns 403; no secrets in source.

---

### Q4 — Product Catalog  · branch `feature/q4-product-catalog`

**Domain:** `Product { id, name, category, price, stock, rating, createdAt }`. 100 products are seeded on startup (Flyway data migration or an idempotent `ApplicationRunner`).

| Method | Path | Notes |
|---|---|---|
| GET | `/api/products?category=&minPrice=&maxPrice=&inStock=&q=&page=&size=&sort=price,desc` | All filters optional and combinable (JPA `Specification`); response has `content, page, size, totalElements, totalPages`; `size` capped at 100 |
| GET | `/api/products/{id}` | Cached |
| PUT / DELETE | `/api/products/{id}` | ADMIN only; evict the cache entry after commit |

**Caching:** Spring Cache + Caffeine. `@Cacheable("products")` on lookup and `@CacheEvict` on update/delete, applied after the transaction commits (`TransactionAwareCacheManagerProxy`), so updated or deleted products are never served stale. A test spies on the repository and proves the second lookup does not hit the DB. Sorting accepts any entity field; an unknown field returns 400. Indexes on `category`, `price`, and a trigram index on `lower(name)`.

**Acceptance:** any filter combination works in one request; repeated lookups skip the DB, and this can be demonstrated; ≥ 1 automated test.

---

### Q5 — Order Service  · branch `feature/q5-order-service`

**Domain:** `Order { id, status ∈ {PLACED, CANCELLED}, idempotencyKey (unique), items[], createdAt }`, `OrderItem { productId, quantity }`

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/orders` + header `Idempotency-Key` | 201 (or the original order on replay) | 400, 404 product, 409 insufficient stock |
| GET | `/api/orders/{id}` | 200 | 404 |
| POST | `/api/orders/{id}/cancel` | 200, stock restored | 404, 409 already cancelled |

**Correctness design**
- **All-or-nothing:** a single `@Transactional` service method. Any failed item throws `ConflictException`, which rolls back every reservation.
- **No overselling:** a conditional atomic decrement, `UPDATE product SET stock = stock - :q WHERE id = :id AND stock >= :q`. If 0 rows are updated, there is not enough stock. Products are locked in ascending id order to avoid deadlocks. The alternative is `SELECT … FOR UPDATE` (pessimistic) or `@Version` (optimistic + retry), to be compared in the PR.
- **Idempotent retries:** the client sends an `Idempotency-Key` header and a unique constraint on `orders.idempotency_key` is the arbiter. A replay returns the stored order, and a concurrent duplicate that loses the race reads the winner's order.
- **Cancel:** state-checked (`PLACED → CANCELLED` only) and restores stock in the same transaction.

**Acceptance:** a test fires 50 concurrent orders at stock 10 → exactly 10 succeed and stock ends at 0; retrying with the same key creates one order. Tests run on Testcontainers PostgreSQL.

---

## 5. Non-functional requirements

| Area | Requirement |
|---|---|
| Correctness under concurrency | Q2 visit counts and Q5 stock/idempotency verified by multi-threaded tests on real PostgreSQL |
| Performance | Q4 paginated queries are indexed; single-product reads are cached |
| Security | BCrypt passwords, stateless JWT, secrets via env, no stack traces in responses |
| Consistency | One error format; one package layout; Flyway-only schema changes |
| Observability | Actuator health endpoint; server-side errors logged with stack trace |
| Testability | `./mvnw verify` runs everything; Docker is the only external prerequisite |

---

## 6. Delivery plan & Git workflow

| # | Branch | PR | Depends on |
|---|---|---|---|
| 0 | `chore/3-base-project` | Base project + this doc | — |
| 1 | `feature/q1-task-api` | Task Manager API | 0 |
| 2 | `feature/q2-url-shortener` | URL Shortener | 1 |
| 3 | `feature/q3-auth` | Authentication & Roles | 2 |
| 4 | `feature/q4-product-catalog` | Product Catalog | 3 |
| 5 | `feature/q5-order-service` | Order Service | 4 |

- Branch from the latest `main` each time; merge in order; pull `main` before the next branch.
- Small, meaningful commits (`Add create task endpoint`, `Return field errors for invalid input`).
- PR template: **Problem / Approach / Decisions & trade-offs / How to test**.
- Review your own diff before merging.

> Q3 adds Spring Security. After it merges, Q4/Q5 endpoints must either require authentication or be explicitly permitted in the security config. Decide this when Q3 is designed.

---

## 7. Definition of done (per PR)

- [ ] Acceptance criteria for the question are met
- [ ] Tests added and `./mvnw verify` is green
- [ ] Flyway migration for any schema change
- [ ] Errors follow the shared contract
- [ ] PR uses the template; README table updated with the PR link
- [ ] The author can explain the request → DB flow, the concurrency behaviour, and the alternatives

---

## 8. Interview-readiness checklist

For each feature, be ready to answer:
1. Walk through the request from HTTP to the database.
2. Why this approach, and what alternatives were considered?
3. What breaks if two requests hit this endpoint at the same time?
4. What happens if a specific line is removed?
5. Make a small live change (e.g. new field, new filter, different status code).

---

## 9. Stretch goals (only after Q1–Q5 merge)

| Q | Stretch |
|---|---|
| Q1 | Interactive API docs (springdoc-openapi / Swagger UI) |
| Q2 | Custom short codes chosen by the user |
| Q3 | Refresh tokens + logout that revokes them |
| Q4 | Distributed cache (Redis) for multi-instance deployments |
| Q5 | Second concurrency strategy + written comparison |

---

## 10. Risks & open questions

| Risk / question | Mitigation |
|---|---|
| 2-hour time box | Base project ready in advance; shared error handling and test setup reused by every PR |
| Docker unavailable on the test machine | Testcontainers is required for meaningful concurrency tests; document it as a prerequisite |
| Q2 duplicate-URL semantics | Decide in the Q2 PR and document why |
| Q5 retry key design | `Idempotency-Key` header (client-generated UUID) vs. a request-body hash; header recommended |
| Security config affecting Q4/Q5 | Decide public vs. authenticated endpoints during Q3 |

# be-mock-prep

Backend interview prep: five Spring Boot features (Task API, URL shortener, auth & roles, product catalog, order service) built on one layered code base with PostgreSQL.

Product requirements, architecture and per-feature design: [`docs/PRODUCT.md`](docs/PRODUCT.md).

## Progress

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

Video:

## Tech stack

Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL 16 · Flyway · Bean Validation · JUnit 5 / MockMvc / Testcontainers · Maven wrapper

## Prerequisites

- JDK 21+
- Docker (for the local database and for the integration tests)

No Maven install needed. Use `./mvnw` (or `mvnw.cmd` on Windows).

## Run the app

```bash
cp .env.example .env          # then set DB_PASSWORD
docker compose up -d          # starts PostgreSQL on localhost:5432
./mvnw spring-boot:run
```

Check it: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`

Configuration comes from environment variables (or the gitignored `.env`):

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/bemock` | JDBC URL |
| `DB_USERNAME` | `bemock` | DB user |
| `DB_PASSWORD` | *(none)* | DB password |
| `DB_PORT` | `5432` | Host port docker compose publishes (if 5432 is taken, change it and `DB_URL`) |

**No local database?** Run `TestBeMockPrepApplication` from `src/test/java`. It starts the app against a throwaway PostgreSQL container (Testcontainers).

## Run the tests

```bash
./mvnw verify
```

Integration tests start their own PostgreSQL 16 container, so Docker must be running. You don't need `docker compose`.

## Project structure

```
src/main/java/com/bemock/prep
├── controller   REST controllers (HTTP ↔ DTO, @Valid)
├── service      business logic, transactions
├── repository   Spring Data JPA repositories
├── model        JPA entities (BaseEntity: id, createdAt, updatedAt)
├── dto          request/response records, ApiErrorResponse
└── exception    ApiException hierarchy + GlobalExceptionHandler
src/main/resources
├── application.yml
└── db/migration Flyway scripts (V1__baseline.sql, ...)
```

## Error format

Every error returns the same JSON body with the matching HTTP status:

```json
{
  "timestamp": "2026-10-09T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tasks",
  "fieldErrors": [{ "field": "title", "message": "must not be blank" }]
}
```

## Workflow

One issue → one branch → one PR per change. Branches are cut from the latest `main`, and PRs use the template *Problem / Approach / Decisions & trade-offs / How to test*.

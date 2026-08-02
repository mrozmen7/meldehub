# MeldeHub

A municipal case-routing platform (citizen reports → Kafka events → department routing)
— and the Engineering Graph system that built it.

Citizen reports arrive via web form; each new case publishes a `case-created` event to
Kafka, and a routing consumer maps the category to the responsible municipal department.
Frontend (Angular) and backend (Spring Boot) are served from a single origin (nginx)
in production — see ADR-0007.

Built as a production-minded reference: versioned schema migrations, role-based security,
automated quality gates, and architecture decisions recorded as ADRs.

## Quick start (Docker)

Prerequisite: Docker. Only ports 4200 and 8080 are exposed on the host;
PostgreSQL and Kafka stay internal.

```bash
docker compose up --build
```

Services: `postgres` (16-alpine), `kafka` (single-node KRaft), `backend` (Spring Boot,
Flyway provisions the schema on first start), `frontend` (Angular build + nginx).

| URL | Purpose |
|---|---|
| http://localhost:4200/ | Web UI (Angular, served by nginx) |
| http://localhost:4200/api/cases | REST API (nginx → backend proxy, single origin) |
| http://localhost:4200/swagger-ui.html | Swagger UI (via nginx) |
| http://localhost:8080/ | Direct backend access (debug) |

### Demo flow

1. Open http://localhost:4200/ — you are greeted by the **login screen**.
   Sign in as `citizen` / `meldehub123` (the reporter role).
2. Submit a report (e.g. category LIGHTING). The case is persisted with status `NEW`;
   in the background a `case-created` event is published to Kafka and the routing
   consumer maps it to a department (`docker compose logs backend`).
3. Log out, sign in as `operator` / `meldehub123` — advance the case through the
   state machine: NEW → TRIAGED → IN_PROGRESS → RESOLVED → CLOSED.
   Invalid transitions are rejected (HTTP 409). The citizen role cannot access the
   operator panel (HTTP 403).

Shut down: `docker compose down` (add `-v` to wipe data).

## Feature overview

| Area | Implementation |
|---|---|
| Case API | Spring Boot 3.5, Java 21 — paginated & filterable list (`page`, `size` ≤ 100, `status`) |
| State machine | NEW → TRIAGED → IN_PROGRESS → RESOLVED → CLOSED, invalid transitions → 409 |
| Messaging | Kafka `case-created` producer + routing consumer + DLQ; API stays up when Kafka is down (ADR-0006) |
| Transactional Outbox | Case + outbox row written in one DB transaction; scheduled relay publishes to Kafka — at-least-once delivery, consumers must be idempotent (CASE-252, ADR-0008) |
| Persistence | PostgreSQL, Flyway migrations (V1 cases, V2 users), Hibernate `validate` |
| Security | JWT login, roles CITIZEN / OPERATOR, stateless filter chain, 401/403 JSON errors (ADR-0009) |
| Frontend | Angular 22, standalone components, reactive forms, route guards, HTTP interceptor |
| Quality | CI gates: backend tests + JaCoCo ≥ 80 %, frontend tests, secret scan — same scripts locally and in CI |
| Delivery | Multi-stage Dockerfiles, single-command compose stack, single origin (no CORS) |

Demo users (seeded by Flyway V2, both with password `meldehub123`):

| User | Role | Permissions |
|---|---|---|
| `citizen` | CITIZEN | Submit reports (`POST /api/cases`) |
| `operator` | OPERATOR | Reports + case list/detail + status management (`GET`, `PATCH`) |

## Architecture

```
Browser (Angular SPA)
      │  same origin :4200
      ▼
   nginx ── /api ──► Spring Boot backend :8080
                        ├── PostgreSQL (Flyway-managed schema)
                        └── Kafka ──► routing consumer ──► department mapping
                                          └── DLQ on poison messages
```

## Tests & quality gates

```bash
cd backend && mvn clean verify        # tests + JaCoCo 80 % coverage gate
cd frontend && npm test -- --watch=false
./scripts/run-quality-checks.sh       # all gates in one run
```

## This repository is two things

1. **The product:** a case-routing platform that normalizes citizen reports and
   forwards them to the right department over Kafka.
2. **The process:** the agentic engineering system that built it — specialist agents,
   isolated worktrees, automated quality/security gates, human approval checkpoints,
   and repository memory (ADR-based persistent decisions).

## Repository map

- Code map: `engineering-graph/memory/code-map.md`
- Working rules: `AGENTS.md`
- Architecture decisions (ADR-0001 … ADR-0009): `engineering-graph/memory/decisions/`
- Backend: `backend/` · Frontend: `frontend/` · CI: `.github/workflows/ci.yml`

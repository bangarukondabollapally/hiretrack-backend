# HireTrack — Backend

Spring Boot REST API for HireTrack, an AI-powered job application management system.

## Stack
- Java 21, Spring Boot 3.3, Maven
- Spring Security (JWT), Spring Data JPA
- MySQL (from TASK-002), Lombok, Bean Validation
- AI: Groq API (Phase 2, TASK-027)

## Documentation
All project documentation lives in `../docs/` (shared parent workspace):
- `../docs/PRD.md` — product requirements
- `../docs/ARCHITECTURE.md` — system architecture and layering
- `../docs/API.md` — full endpoint contract (coordination boundary with frontend)
- `../docs/TASKS.md` — task breakdown and implementation order

## Local Development

### Prerequisites
- Java 21+
- Maven 3.9+
- MySQL 8+ (required from TASK-002 onward)

### Setup
```bash
cp .env.example .env
# Edit .env with real values (see .env.example for required variables per task)
```

### Run
```bash
mvn spring-boot:run
```
Server starts on `http://localhost:8080`.

### Build
```bash
mvn clean package
```

### Test
```bash
mvn test
```

### Health check
```bash
curl http://localhost:8080/api/health
# Expected: {"status":"UP"}
```

## Package Structure
```
src/main/java/com/hiretrack/
├── HiretrackApplication.java   — entry point
├── auth/                       — JWT auth (TASK-006/007/008)
├── user/                       — User + Profile entities (TASK-005/024)
├── application/                — Application CRUD (TASK-011/012/013)
├── interview/                  — Interview CRUD (TASK-018)
├── tag/                        — Tags (TASK-020)
├── dashboard/                  — Dashboard aggregation (TASK-022)
├── ai/                         — Groq client + prompt builder (TASK-027/028/029)
├── common/                     — Shared utilities, health endpoint, error handling
└── config/                     — Security, CORS, caching, Swagger
```

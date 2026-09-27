# HireTrack — Backend

Spring Boot REST API for HireTrack, an AI-powered job application management system.

## Tech Stack
- **Java 21**, Spring Boot 3.3.4, Maven
- **Spring Security** with Stateless JWT Authentication & BCrypt Password Hashing
- **Spring Data JPA** & **MySQL 8.0**
- **AI Integration**: Groq API (`llama-3.3-70b-versatile`) for interactive career coaching and contextual prompt generation
- **Documentation**: Springdoc OpenAPI / Swagger UI (`/swagger-ui.html`)
- **Containerization**: Multi-stage Docker build with Docker Compose

## Documentation
All architectural and API design documentation lives in `../docs/`:
- `../docs/PRD.md` — Product requirements
- `../docs/ARCHITECTURE.md` — System architecture and layering
- `../docs/API.md` — Complete REST API specification
- `../docs/SECURITY.md` — Security baseline and data isolation rules

---

## Configuration & Environment Variables

Copy `.env.example` to `.env` in the `backend/` directory:
```bash
cp .env.example .env
```

Ensure the following variables are configured in `.env`:

| Variable | Required | Description | Example / Default |
|---|---|---|---|
| `MYSQL_ROOT_PASSWORD` | Yes | MySQL root password for Docker container | (Generate a strong password) |
| `DB_URL` | Yes | JDBC URL | `jdbc:mysql://localhost:3307/hiretrack` (Local) / `jdbc:mysql://mysql:3306/hiretrack` (Docker) |
| `DB_USERNAME` | Yes | Database user name | `hiretrack_user` |
| `DB_PASSWORD` | Yes | Database password | (Generate a strong password) |
| `JWT_SECRET` | Yes | HMAC-SHA secret (at least 256 bits) | (Generate a strong secret key) |
| `JWT_EXPIRATION_MS` | Optional | JWT validity duration in milliseconds | `86400000` (24 hours) |
| `GROQ_API_KEY` | Yes | API Key from Groq Cloud Console | `gsk_...` |
| `GROQ_MODEL` | Optional | Groq LLM model name | `llama-3.3-70b-versatile` |

---

## Running with Docker (Recommended)

HireTrack includes a multi-container Docker Compose setup (`backend/docker-compose.yml`) containing the Spring Boot API service and MySQL 8.0 database.

> **Host Port Note for MySQL**: To prevent conflicts with any pre-existing local MySQL service running on host port `3306`, Docker maps host port `3307` to container port `3306` (`3307:3306`).
> When connecting to MySQL from your host machine (e.g. via MySQL Workbench or DBeaver), use port `3307`. Inside the Docker network, the backend connects directly to `mysql:3306`.

### 1. Build and Start Services
```bash
docker compose up --build
```

### 2. Verify Container Health
Once running, check the service status:
```bash
docker compose ps
```
Or test the health endpoint:
```bash
curl http://localhost:8080/api/health
# Response: {"status":"UP"}
```

### 3. Stop Services
```bash
docker compose down
# To also remove persistent database volumes:
docker compose down -v
```

---

## Local Development (Without Docker)

### Prerequisites
- **Java 21 JDK**
- **Maven 3.9+**
- **MySQL 8.0+** running locally

### 1. Configure Local Database
Create database and user in MySQL:
```sql
CREATE DATABASE hiretrack;
CREATE USER 'hiretrack_user'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON hiretrack.* TO 'hiretrack_user'@'%';
FLUSH PRIVILEGES;
```

### 2. Run the Application
```bash
mvn spring-boot:run
```
The application starts at `http://localhost:8080`.

### 3. Run Test Suite
Runs all unit and MockMvc integration tests (including authentication, data isolation, and authorization tests):
```bash
mvn test
```

### 4. Build Production JAR
```bash
mvn clean package -DskipTests
```

---

## API Endpoints & Interactive Documentation

Once the server is running, access Swagger UI for interactive testing:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Specification**: `http://localhost:8080/v3/api-docs`

---

## Package Structure

```
src/main/java/com/hiretrack/
├── HiretrackApplication.java   — Spring Boot entry point
├── auth/                       — JWT Authentication (Register, Login, Password Hashing)
├── user/                       — User entity & Profile management (Resume Text, Target Role)
├── application/                — Job Application CRUD & Filtering
├── interview/                  — Interview Schedule & Timeline Tracking
├── tag/                        — Application Tags & Association
├── dashboard/                  — Metrics Aggregation (Application Counts, Conversion Rates)
├── ai/                         — Groq AI integration & Contextual Prompt Builder
├── common/                     — Global Exception Handler, Health Endpoint, Custom Exceptions
└── config/                     — Security Filter Chain, CORS, OpenAPI Swagger Config
```

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Build and Run
- `./gradlew build` - Build the project
- `./gradlew bootRun` - Run the application locally
- `./gradlew test` - Run all tests
- `./gradlew test --tests "ClassName"` - Run specific test class

### Docker Development
- `docker-compose up -d` - Start all services (PostgreSQL, Redis, Observability Stack)
- `docker-compose down` - Stop containers
- Application runs on port 8080 locally, 8082 in Docker

### Observability Stack
- **Grafana**: http://localhost:3000 (admin/admin) - Unified observability dashboard
- **Prometheus**: http://localhost:9090 - Metrics storage and queries
- **Tempo**: http://localhost:3200 - Distributed tracing
- **Loki**: http://localhost:3100 - Log aggregation
- See `OBSERVABILITY.md` for detailed setup and usage

### Database
- Local PostgreSQL: `jdbc:postgresql://localhost:5432/snapguidedb`
- Docker PostgreSQL: Port 5433 mapped to container's 5432
- Uses PostGIS extension for spatial data (location-based features)
- Redis cache: Port 6379 locally, 6380 in Docker

## Architecture Overview

### Package Structure
- `domain/` - Domain entities and business logic
  - `member/` - User management and authentication
  - `media/` - Photo/video handling with EXIF metadata extraction
  - `location/` - Geographic data with PostGIS spatial queries
  - `guide/` - Travel guide content
  - `comment/` - User comments system
- `security/` - OAuth2 + JWT authentication with Google Login
- `service/` - Business services including file storage abstraction
- `repository/` - JPA repositories with QueryDSL for complex queries
- `controller/` - REST API controllers (Swagger documented at `/swagger-ui.html`)
- `infrastructure/` - Cross-cutting concerns (AOP, caching, persistence)

### Key Technologies
- **Spring Boot 3.4.5** with Java 17
- **PostgreSQL + PostGIS** for spatial data
- **Redis** for caching and session management
- **OAuth2 + JWT** for authentication
- **QueryDSL** for type-safe database queries
- **AWS S3** for file storage (with local fallback)
- **EXIF metadata extraction** for photo analysis
- **Google Maps API** for geocoding
- **OpenTelemetry** for observability (traces, metrics, logs)
- **Grafana Stack** for monitoring and visualization (Prometheus, Loki, Tempo)

### Storage Abstraction
The application supports multiple storage backends configured via `storage.type`:
- `local` - Local filesystem storage
- `s3` - AWS S3 storage (mount directory)
- `nas` - NAS storage 

File paths are abstracted through storage service interfaces.

### Authentication Flow
- OAuth2 with Google Login
- JWT tokens with Redis-based refresh token storage
- Cookie-based session management
- Access tokens expire in 30 minutes (configurable)

### Testing
- JUnit 5 with Mockito
- Testcontainers for integration tests (implied by PostgreSQL setup)
- Test classes follow `*Test.java` naming convention

## Configuration Structure

### Common Settings (`application.yml`)
- Shared JPA, multipart, and Swagger configurations
- Common static resource locations
- OpenAPI documentation settings

### Local Development (`application-local.yml`)
- Local PostgreSQL and Redis connections
- **Environment variables for sensitive data (see ENV_SETUP.md)**
- SQL logging enabled for debugging
- File storage to local filesystem

### Docker/Production (`application-docker.yml`)
- Environment variable-based configuration
- Container-specific paths and hosts
- Production-ready security settings
- File storage using S3

## Development Notes

- Uses Lombok for boilerplate reduction
- AOP for performance monitoring (`@TimeTrace`)
- HEIC to JPEG conversion support for iOS photos
- Spatial queries for location-based features
- Thumbnail generation for uploaded media
- P6Spy for SQL logging in development

### Observability & Monitoring
- **OpenTelemetry Java Agent** for automatic instrumentation (no code changes needed)
- **Traces**: Auto-traces HTTP requests, database queries, Redis operations, external API calls
- **Metrics**: JVM metrics, HTTP metrics, custom metrics via Micrometer
- **Logs**: JSON structured logging with trace correlation (trace_id, span_id)
- **Grafana**: Unified dashboard for metrics, logs, and traces with correlation
- See `OBSERVABILITY.md` for setup and usage details

## Environment Setup

⚠️ **IMPORTANT: Security Configuration Required**

Before running the application locally, you must set up environment variables:

1. Create `.env` file from template:
   ```bash
   cp .env.example .env
   ```

2. Fill in actual values for:
   - Database password
   - JWT secret key (generate with `openssl rand -hex 64`)
   - Google OAuth2 credentials
   - Google Maps API key
   - AWS credentials

3. See `ENV_SETUP.md` for detailed setup instructions

**Never commit `.env` files or hardcoded secrets to Git!**
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Build and Run
- `./gradlew build` - Build the project
- `./gradlew bootRun` - Run the application locally
- `./gradlew test` - Run all tests
- `./gradlew test --tests "ClassName"` - Run specific test class

### Docker Development
- `docker-compose up -d` - Start PostgreSQL (with PostGIS) and Redis containers
- `docker-compose down` - Stop containers
- Application runs on port 8080 locally, 8082 in Docker

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
- Hardcoded credentials for development
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

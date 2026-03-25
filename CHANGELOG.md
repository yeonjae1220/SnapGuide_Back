# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [0.1.0] - 2026-03-17

### Added
- PWA support with offline mode, install prompt, and Web Push notifications
- S3 presigned URL support for thumbnails
- Frontend redesign with landing/main view split
- Redis caching and cursor-based pagination
- AWS S3 storage integration with Docker
- User account deletion with cascading guide/storage cleanup
- Google OAuth login for web browsers
- k6 load test scripts and performance monitoring (Prometheus, Grafana, OTEL)

### Changed
- Async file upload and atomic like count optimization
- Apply SOLID principles, design patterns, and clean code improvements across services
- Upgrade SpringDoc to 2.8.6 for Spring Boot 3.4.x compatibility

### Fixed
- Content Security Policy blocking inline scripts and Google Maps API
- YAML indentation error in application-docker.yml causing misconfigured properties
- REST API contract violations
- JPA anti-patterns (N+1, lazy loading)
- OWASP security vulnerabilities
- Google OAuth login issues in web browsers
- HEIC file type handling
- Replace hardcoded Grafana password with environment variable
- Docker Compose host port conflicts (DB 5435, OTEL 4327/4328, Prometheus 9091, Grafana 3002)

[unreleased]: https://github.com/yeonjae1220/SnapGuide_Back/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/yeonjae1220/SnapGuide_Back/releases/tag/v0.1.0

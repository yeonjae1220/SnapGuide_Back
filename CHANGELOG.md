# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-03-16

### Security
- Address OWASP security vulnerabilities
- Replace hardcoded Grafana password with environment variable
- Hide critical configuration values and improve overall security posture

### Added
- PWA support with offline mode, install prompt, and Web Push notifications
- S3 presigned URL support for thumbnails
- Frontend redesign with landing/main view split and upload bug fix
- Redis caching, cursor-based pagination, and auth improvements
- AWS S3 storage integration with Docker support
- User account deletion with cascading guide and storage data cleanup
- Like functionality with current-location-based and map-based search
- Guide save, fetch, update, and delete functionality
- Distance-based guide lookup using bounding box
- HEIC to JPG conversion on server side
- Upload photos with tips
- Local file I/O with large file handling
- JWT-based signup and login
- Logout with Redis-based token blacklist
- [SNAP-11] Google login integration using OAuth2
- k6 load test scripts and performance monitoring (Prometheus, Grafana, OpenTelemetry)
- PostGIS spatial query support for location-based features

### Changed
- Async file upload and atomic like count optimization
- Apply SOLID principles across service and storage layers
- Apply design patterns to reduce coupling and responsibilities
- Apply clean code improvements project-wide
- Restructure packages and refactor EXIF extractor code
- Apply CustomException to JWT error handling and improve header validation
- Change session login to cookie login in Google OAuth2 flow
- Change token transfer procedure to one-time code with mobile authorization support

### Fixed
- REST API contract violations
- JPA anti-patterns (N+1, lazy loading, transaction boundaries)
- Google OAuth login issues for web browsers
- HEIC file type detection and handling
- Broken pipe error when loading multiple images
- Circular dependency issue between beans
- YAML indentation errors causing misconfigured properties in Docker environment
- Move server config to top-level in YAML files

[Unreleased]: https://github.com/yeonjae1220/SnapGuide_Back/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/yeonjae1220/SnapGuide_Back/releases/tag/v1.0.0

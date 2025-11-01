# 환경 변수 설정 가이드

## 개요
보안을 위해 민감한 정보(API 키, DB 비밀번호 등)는 `.env` 파일로 분리하여 관리합니다.

## 설정 방법

### 1. .env 파일 생성

프로젝트 루트 디렉토리에 `.env` 파일을 생성합니다:

```bash
cp .env.example .env
```

### 2. 환경 변수 설정

`.env` 파일을 열어서 실제 값으로 변경하세요:

```bash
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/snapguidedb
DB_USERNAME=postgres
DB_PASSWORD=your_actual_db_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT Configuration (최소 256비트 이상의 안전한 키 생성 필요)
JWT_SECRET=your_generated_jwt_secret_key_here
JWT_ACCESS_TOKEN_EXPIRATION=1800000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# OAuth2 Google Configuration
GOOGLE_CLIENT_ID=your_actual_google_client_id
GOOGLE_CLIENT_SECRET=your_actual_google_client_secret

# Google Maps API
GOOGLE_MAPS_API_KEY=your_actual_google_maps_api_key

# AWS Configuration
AWS_ACCESS_KEY=your_actual_aws_access_key
AWS_SECRET_KEY=your_actual_aws_secret_key
AWS_S3_BUCKET=snapguide-bucket
AWS_REGION=us-east-2

# Storage Configuration
STORAGE_TYPE=local
STORAGE_LOCAL_BASE_DIR=/Users/kim-yeonjae/Desktop/Study/snapguide/uploads
STORAGE_NAS_MOUNT_DIR=/mnt/nas/snapguide

# Application URLs
FRONTEND_REDIRECT_URL=http://localhost:8080
APP_REDIRECT_URI=snapguide://oauth/callback
```

### 3. JWT Secret 키 생성

안전한 JWT Secret 키는 다음 명령어로 생성할 수 있습니다:

```bash
# Linux/Mac
openssl rand -hex 64

# 또는 온라인 생성기 사용
# https://www.allkeysgenerator.com/Random/Security-Encryption-Key-Generator.aspx
```

### 4. IntelliJ IDEA에서 환경 변수 설정

IntelliJ에서 `.env` 파일을 자동으로 로드하려면:

#### 방법 1: EnvFile 플러그인 사용 (권장)

1. `Settings` > `Plugins`에서 "EnvFile" 검색 및 설치
2. `Run/Debug Configurations`에서 실행 설정 선택
3. `EnvFile` 탭에서 `.env` 파일 추가

#### 방법 2: 수동 설정

1. `Run/Debug Configurations` 열기
2. `Environment variables` 필드에 수동으로 추가
3. 또는 `Edit Configurations` > `Environment variables`에서 `.env` 파일 import

### 5. Gradle 실행 시 환경 변수 로드

`build.gradle`에 다음을 추가하여 환경 변수를 로드할 수 있습니다:

```gradle
tasks.named('bootRun') {
    // .env 파일에서 환경 변수 로드
    if (file('.env').exists()) {
        file('.env').readLines().each {
            def (key, value) = it.split('=', 2)
            if (key && value && !key.startsWith('#')) {
                environment key.trim(), value.trim()
            }
        }
    }
}
```

## 주의사항

⚠️ **절대 `.env` 파일을 Git에 커밋하지 마세요!**

- `.env` 파일은 `.gitignore`에 이미 추가되어 있습니다
- 실제 키 값은 팀원들과 안전한 방법(AWS Secrets Manager, 1Password 등)으로 공유하세요
- 공개 저장소에 키가 노출되었다면 즉시 재발급하세요

## 프로파일별 설정

### Local 환경 (`application-local.yml`)
- 개발용 로컬 환경
- `.env` 파일의 환경 변수 사용
- 기본값이 설정되어 있어 일부 환경 변수는 선택적

### Docker 환경 (`application-docker.yml`)
- 컨테이너 환경에서 실행
- 환경 변수는 `docker-compose.yml` 또는 Kubernetes ConfigMap/Secret에서 주입

## 문제 해결

### 환경 변수가 로드되지 않을 때

1. `.env` 파일이 프로젝트 루트에 있는지 확인
2. IntelliJ를 재시작
3. Gradle을 다시 빌드: `./gradlew clean build`
4. 환경 변수 이름과 `application-local.yml`의 플레이스홀더가 일치하는지 확인

### 특정 환경 변수만 오류가 날 때

`application-local.yml`에서 해당 변수에 기본값이 설정되어 있는지 확인:

```yaml
# 기본값 있음 (환경 변수 없어도 동작)
username: ${DB_USERNAME:postgres}

# 기본값 없음 (환경 변수 필수)
password: ${DB_PASSWORD}
```

## API 키 재발급 체크리스트

보안 사고 발생 시 다음 키들을 재발급하세요:

- [ ] Google OAuth2 Client ID & Secret
- [ ] Google Maps API Key
- [ ] JWT Secret Key
- [ ] AWS Access Key & Secret Key
- [ ] Database Password (필요시)

## 참고 자료

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [12 Factor App - Config](https://12factor.net/config)

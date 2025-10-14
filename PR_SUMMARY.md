# PR Summary: JWT Token Refresh & Media File Serving Fixes

## 📋 Overview
JWT Access Token 재발급 기능 버그 수정 및 로컬/S3 스토리지 통합 파일 서빙 기능 개선

## 🐛 Issues Fixed

### Issue #1: Access Token 재발급 실패
- **문제**: Access Token 만료 시 `/api/auth/reissue` 호출해도 재발급 실패
- **원인**:
  1. 만료된 토큰 파싱 불가능
  2. 블랙리스트 TTL 처리 오류 (음수 값)
  3. LocalDateTime 직렬화 오류

### Issue #2: 로컬 프로필에서 사진 로딩 실패
- **문제**: `uploads` 디렉토리에 파일이 있는데 웹에서 접근 불가
- **원인**: MediaController가 S3 전용으로만 구현되어 있음

---

## 🔧 Changes

### 1. JWT Token Refresh 수정

#### 1.1 JwtTokenProvider.java
**파일 위치**: `src/main/java/yeonjae/snapguide/security/authentication/jwt/JwtTokenProvider.java`

**추가된 메서드**:
```java
/**
 * 만료된 토큰도 파싱할 수 있는 메서드 (재발급 시 사용)
 * ExpiredJwtException에서 Claims를 추출하여 반환
 */
public Claims parseExpiredToken(String token) {
    try {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    } catch (ExpiredJwtException e) {
        log.info("만료된 토큰에서 Claims 추출 - 생성일자: {}, 만료시간: {}",
                e.getClaims().getIssuedAt(), e.getClaims().getExpiration());
        return e.getClaims(); // 만료된 토큰의 Claims 반환
    }
}
```

**수정된 메서드**:
```java
public long getExpiration(String token) {
    Claims claims = parseExpiredToken(token); // 만료된 토큰도 처리 가능하도록 변경
    return claims.getExpiration().getTime() - System.currentTimeMillis();
}
```

**변경 이유**:
- 재발급 시 만료된 Access Token에서 사용자 정보를 추출해야 함
- 블랙리스트 TTL 계산을 위해 만료된 토큰의 만료 시간도 조회 필요

---

#### 1.2 AuthService.java
**파일 위치**: `src/main/java/yeonjae/snapguide/service/AuthService.java`

**수정 전**:
```java
// 2. Access Token 에서 Member ID 가져오기
Authentication authentication = jwtTokenProvider.getAuthentication(tokenRequestDTO.getAccessToken());
// ❌ 만료된 토큰은 getAuthentication() 호출 시 예외 발생
```

**수정 후**:
```java
// 2. 만료된 Access Token에서 Member ID 가져오기 (만료된 토큰도 파싱 가능)
Claims claims = jwtTokenProvider.parseExpiredToken(tokenRequestDTO.getAccessToken());
String userId = claims.getSubject();

// 권한 정보 추출
Collection<? extends GrantedAuthority> authorities =
    java.util.Arrays.stream(claims.get("Authorization").toString().split(","))
        .map(String::trim)
        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
        .collect(java.util.stream.Collectors.toList());
```

**블랙리스트 처리 개선**:
```java
// 5. 기존 Access Token 블랙리스트 등록 (만료된 토큰도 처리)
long accessTokenExpiry = jwtTokenProvider.getExpiration(tokenRequestDTO.getAccessToken());
// 만료된 토큰의 경우 음수가 나오므로, 양수일 때만 블랙리스트 등록
if (accessTokenExpiry > 0) {
    tokenBlacklistService.blacklistAccessToken(tokenRequestDTO.getAccessToken(), accessTokenExpiry);
    log.info("기존 Access Token 블랙리스트 등록 완료 (TTL: {}ms)", accessTokenExpiry);
} else {
    log.info("Access Token 이미 만료됨 - 블랙리스트 등록 스킵");
}
```

**변경 이유**:
- 만료된 토큰에서도 사용자 정보를 안전하게 추출
- 음수 TTL로 인한 Redis 오류 방지
- 디버깅을 위한 로그 추가

---

#### 1.3 JwtAuthenticationFilter.java
**파일 위치**: `src/main/java/yeonjae/snapguide/security/authentication/jwt/JwtAuthenticationFilter.java`

**수정 사항**:
```java
private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
    response.setContentType("application/json;charset=UTF-8");
    response.setStatus(errorCode.getStatus().value());

    ObjectMapper objectMapper = new ObjectMapper();
    // ✅ Java 8 날짜/시간 타입(LocalDateTime 등) 지원을 위한 모듈 등록
    objectMapper.findAndRegisterModules();

    String jsonResponse = objectMapper.writeValueAsString(new ErrorResponse(errorCode));
    response.getWriter().write(jsonResponse);
}
```

**변경 이유**:
- `ErrorResponse`의 `LocalDateTime timestamp` 필드 직렬화 오류 수정
- `InvalidDefinitionException: Java 8 date/time type not supported` 해결

---

### 2. Media File Serving 수정

#### 2.1 MediaController.java
**파일 위치**: `src/main/java/yeonjae/snapguide/controller/mediaController/MediaController.java`

**수정 전** (S3 전용):
```java
@GetMapping("/files/{filename:.+}")
public ResponseEntity<?> serveFileFromS3(@PathVariable String filename) {
    if (!(fileStorageService instanceof S3FileStorageService)) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("S3 storage is not configured.");
    }
    // S3 Presigned URL 리다이렉트
}
```

**수정 후** (Local + S3 통합):
```java
@GetMapping("/files/{filename:.+}")
public ResponseEntity<?> serveFile(@PathVariable String filename) throws IOException {
    // S3 스토리지인 경우
    if (fileStorageService instanceof S3FileStorageService) {
        S3FileStorageService s3Service = (S3FileStorageService) fileStorageService;
        String presignedUrl = s3Service.generatePresignedUrl(filename);

        if (presignedUrl == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(presignedUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // 로컬 스토리지인 경우
    try {
        Path baseDir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        Path primaryDir = baseDir.resolve("originals");

        // 우선순위 1: 'uploads/originals' 디렉토리에서 파일을 찾음
        Path filePath = primaryDir.resolve(filename).normalize();

        // 우선순위 2: 'uploads' 디렉토리에서 다시 찾음
        if (!Files.exists(filePath)) {
            filePath = baseDir.resolve(filename).normalize();
        }

        // 보안 체크: 최종 경로가 허용된 기본 디렉토리(uploads)를 벗어나는지 확인
        if (!filePath.startsWith(baseDir)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 파일 존재 및 읽기 가능 확인
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        // Content-Type 결정 및 파일 전송
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);

    } catch (MalformedURLException e) {
        return ResponseEntity.badRequest().build();
    } catch (IOException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

**변경 이유**:
- `storage.type=local` 설정 시에도 파일 서빙 가능
- Storage 추상화 계층 활용 (S3/Local/NAS)
- Path Traversal 공격 방지를 위한 보안 체크 포함

---

## 📝 Import 추가

### AuthService.java
```java
import io.jsonwebtoken.Claims;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
```

---

## ✅ 테스트 시나리오

### 1. Token Refresh 테스트
```bash
# 1. 로그인하여 토큰 발급
POST /api/auth/login
{
  "email": "test@example.com",
  "password": "password"
}

# 2. Access Token 만료 대기 (jwt.access-token-expiration 설정값)

# 3. 재발급 요청
POST /api/auth/reissue
{
  "accessToken": "만료된_액세스_토큰",
  "refreshToken": "유효한_리프레시_토큰"
}

# 4. 새로운 토큰 수신 확인
```

**예상 결과**:
- ✅ 200 OK
- ✅ 새로운 `accessToken` 반환
- ✅ Refresh Token 유효기간에 따라 `refreshToken` 재발급 또는 null

### 2. Local File Serving 테스트
```bash
# application-local.yml에서 storage.type: local 설정

# 파일 업로드
POST /media/upload
(multipart/form-data)

# 파일 접근
GET /media/files/{filename}.jpg
```

**예상 결과**:
- ✅ 200 OK
- ✅ 이미지 파일 정상 로드
- ✅ Content-Type: image/jpeg

---

## 🔍 주요 개선 사항

### 보안
- ✅ 만료된 토큰 블랙리스트 처리 개선 (음수 TTL 방지)
- ✅ Path Traversal 공격 방지 (파일 서빙 시)
- ✅ 토큰 재발급 시 Redis 저장소 검증 강화

### 성능
- ✅ 불필요한 블랙리스트 등록 스킵 (이미 만료된 토큰)
- ✅ 로그 최적화 (디버깅 용이성)

### 유지보수성
- ✅ Storage 추상화 계층 활용 (Local/S3/NAS)
- ✅ 명확한 로그 메시지
- ✅ 코드 가독성 향상

---

## 📚 관련 이슈
- #37 Storage Abstraction (SNAP-24)
- Token Refresh 버그 수정
- Local Profile 파일 서빙 오류 수정

---

## 🚀 배포 영향도
- **Breaking Change**: ❌ 없음
- **Database Migration**: ❌ 불필요
- **Configuration Change**: ❌ 기존 설정 그대로 사용 가능
- **API 변경**: ❌ 없음 (기존 엔드포인트 유지)

---

## 📌 후속 작업
- [ ] 클라이언트 Axios Interceptor 구현 (자동 토큰 재발급)
- [ ] 토큰 재발급 성능 모니터링
- [ ] E2E 테스트 추가

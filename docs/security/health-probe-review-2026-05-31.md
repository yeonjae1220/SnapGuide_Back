# Health Probe 보안·가용성 리뷰

> 리뷰 일시: 2026-05-31  
> 범위: Snapguide k8s probe 설정 전면 점검

---

## CRITICAL (수정 완료)

### [CRITICAL-1] 백엔드 readiness probe — tcpSocket → httpGet 전환

**원인**  
`backend.yaml`의 readiness/liveness probe가 `tcpSocket`을 사용하고 있어 포트가 열려 있기만 하면 정상으로 판정했다. Spring Boot 기동 중이거나 DB 연결 실패 상태에서도 probe를 통과해 트래픽이 유입될 수 있었다.

**수정**
```yaml
# 변경 전
readinessProbe:
  tcpSocket:
    port: 8080

# 변경 후
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  timeoutSeconds: 5
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  timeoutSeconds: 5
```

---

## HIGH (수정 완료)

### [HIGH-1] NetworkPolicy — backend-ingress가 frontend pod 차단

**원인**  
`backend-ingress` NetworkPolicy가 `ingress-nginx`만 허용했다. `/health/ready` route가 백엔드 `/actuator/health/readiness`를 호출하는데 NetworkPolicy에서 드롭되어 readiness probe가 항상 503 → Pod가 Ready 상태 진입 불가.

**수정**
```yaml
ingress:
  - from:
      - namespaceSelector:
          matchLabels:
            kubernetes.io/metadata.name: ingress-nginx
      - podSelector:         # ← 추가
          matchLabels:
            app: frontend
    ports:
      - port: 8080
```

---

## 추가된 설정

### actuator health groups

```yaml
# application-docker.yml에 추가
management:
  health:
    probes:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, db, redis
```

### 프론트엔드 health endpoints

- `src/app/health/live/route.ts` — liveness용, 항상 200
- `src/app/health/ready/route.ts` — readiness용, 백엔드 연결 실패 시 503

기존 `readinessProbe: path: /` → `/health/ready`, liveness probe 신규 추가.

---

## 보안 검토 항목

| 항목 | 결과 |
|------|------|
| `/health/ready` SSRF 가능성 | API_URL은 환경변수(k8s env)로만 주입, 사용자 입력 아님 — 안전 |
| actuator 민감정보 노출 | `show-details: never`, `include: health`만 — 안전 |
| actuator 인증 | SecurityConfig에서 `/actuator/health/**` permitAll 확인 ✅ |
| 에러 메시지 노출 | catch에서 `{status: 'unavailable'}`만 반환, 내부 에러 미노출 — 안전 |

// k6 Load Test Configuration
export const config = {
  // Base URL - 환경에 맞게 수정
  baseUrl: __ENV.BASE_URL || 'http://localhost:8080',

  // 테스트 단계별 설정
  stages: {
    // 워밍업: 천천히 부하 증가
    warmup: { duration: '30s', target: 10 },
    // 정상 부하: 일반적인 사용자 수
    normal: { duration: '1m', target: 50 },
    // 피크 부하: 최대 사용자 수
    peak: { duration: '2m', target: 100 },
    // 스트레스: 한계 테스트
    stress: { duration: '1m', target: 200 },
    // 쿨다운: 부하 감소
    cooldown: { duration: '30s', target: 0 },
  },

  // 성능 임계값 (SLA)
  thresholds: {
    // HTTP 요청 실패율 < 1%
    http_req_failed: ['rate<0.01'],
    // 95% 요청이 500ms 이내 응답
    http_req_duration: ['p(95)<500'],
    // 99% 요청이 1초 이내 응답
    'http_req_duration{type:api}': ['p(99)<1000'],
    // 파일 업로드는 95%가 5초 이내
    'http_req_duration{type:upload}': ['p(95)<5000'],
  },

  // 인증 토큰 (테스트용)
  auth: {
    // TODO: 실제 JWT 토큰으로 교체
    token: __ENV.AUTH_TOKEN || 'your-test-jwt-token',
  },

  // 테스트 데이터
  testData: {
    memberIds: [1, 2, 3, 4, 5],
    locationIds: [1, 2, 3, 4, 5],
    // 서울 중심 좌표
    defaultLat: 37.5665,
    defaultLng: 126.9780,
    radius: 5000, // 5km
  },
};

// 공통 헤더
export function getHeaders(includeAuth = false) {
  const headers = {
    'Content-Type': 'application/json',
  };

  if (includeAuth && config.auth.token) {
    headers['Authorization'] = `Bearer ${config.auth.token}`;
  }

  return headers;
}

// 랜덤 선택 헬퍼
export function randomItem(array) {
  return array[Math.floor(Math.random() * array.length)];
}

// 랜덤 좌표 생성 (서울 기준 ±0.1도)
export function randomCoordinate() {
  return {
    lat: config.testData.defaultLat + (Math.random() - 0.5) * 0.2,
    lng: config.testData.defaultLng + (Math.random() - 0.5) * 0.2,
  };
}

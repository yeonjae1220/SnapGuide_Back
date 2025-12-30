/**
 * 테스트용 JWT 토큰 발급 스크립트
 * 실행: k6 run k6-tests/scripts/get-test-token.js
 */
import http from 'k6/http';

export default function () {
  // 로그인 API 호출 (실제 엔드포인트에 맞게 수정)
  const loginRes = http.post(
    'http://localhost:8082/auth/login',
    JSON.stringify({
      email: 'test@example.com',
      password: 'testpassword123'
    }),
    {
      headers: { 'Content-Type': 'application/json' }
    }
  );

  if (loginRes.status === 200) {
    const token = JSON.parse(loginRes.body).accessToken;
    console.log('✅ JWT Token:');
    console.log(token);
  } else {
    console.error('❌ Login failed:', loginRes.status, loginRes.body);
  }
}

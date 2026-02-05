/**
 * Spin-wait 발생 여부 확인용 짧은 부하 테스트
 *
 * 목적: HikariCP 커넥션 풀 경합 상태 확인
 * 실행: k6 run k6-tests/scripts/quick-spinwait-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { config, getHeaders } from '../config.js';

export const options = {
  stages: [
    { duration: '10s', target: 50 },   // 빠르게 50 VU로 증가
    { duration: '30s', target: 100 },  // 100 VU 유지 (풀 사이즈 15보다 훨씬 많음)
    { duration: '20s', target: 150 },  // 150 VU로 스트레스
    { duration: '10s', target: 0 },    // 종료
  ],
  thresholds: {
    'http_req_duration': ['p(95)<2000'],
  },
};

const testLocations = [
  { name: 'Seoul', lat: 37.5665, lng: 126.9780 },
  { name: 'Busan', lat: 35.1796, lng: 129.0756 },
  { name: 'Incheon', lat: 37.4563, lng: 126.7052 },
];

export default function () {
  const baseUrl = config.baseUrl;
  const headers = getHeaders(false);

  const location = testLocations[Math.floor(Math.random() * testLocations.length)];
  const radius = 10;

  // DB 쿼리를 발생시키는 API 호출
  const res = http.get(
    `${baseUrl}/guide/api/nearby?lat=${location.lat}&lng=${location.lng}&radius=${radius}`,
    { headers }
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  // 짧은 대기 (더 많은 동시 요청 발생)
  sleep(Math.random() * 0.5);
}

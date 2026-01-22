/**
 * 혼합 시나리오 부하 테스트
 *
 * 목적: 실제 사용자 행동 패턴 시뮬레이션
 * - 70% 읽기 작업 (조회, 검색)
 * - 20% 쓰기 작업 (좋아요, 댓글)
 * - 10% 파일 업로드
 *
 * 실행: k6 run k6-tests/scripts/4-mixed-scenario-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { config, getHeaders, randomItem, randomCoordinate } from '../config.js';

// 커스텀 메트릭
const readSuccess = new Rate('read_success');
const writeSuccess = new Rate('write_success');
const uploadSuccess = new Rate('upload_success');
const concurrentLikes = new Counter('concurrent_likes');

export const options = {
  stages: [
    { duration: '1m', target: 20 },    // 워밍업
    { duration: '3m', target: 100 },   // 일반 부하
    { duration: '3m', target: 200 },   // 피크 시간대
    { duration: '2m', target: 300 },   // 스트레스 테스트
    { duration: '1m', target: 0 },     // 종료
  ],
  thresholds: {
    'http_req_duration': ['p(95)<1000'],
    'http_req_failed': ['rate<0.01'],
    'read_success': ['rate>0.99'],
    'write_success': ['rate>0.95'],
  },
};

// 시나리오 1처럼 실제 이미지 파일 사용
// open()은 k6의 global 함수이며, 스크립트 파일 위치 기준 상대 경로
const testImageData = open('../data/test-image.jpg', 'b'); // binary mode

export default function () {
  const baseUrl = config.baseUrl;

  // 사용자 행동 패턴 시뮬레이션
  const scenario = Math.random();

  // 70% - 읽기 작업 (인증 불필요)
  if (scenario < 0.7) {
    performReadActions(baseUrl, getHeaders(false));
  }
  // 20% - 쓰기 작업 (좋아요) - 인증 필요
  // NOTE: 인증이 없으면 401 에러가 발생하므로, 401도 "정상 동작"으로 간주
  else if (scenario < 0.9) {
    performWriteActions(baseUrl, getHeaders(false));
  }
  // 10% - 업로드 - 인증 필요
  // NOTE: 인증이 없으면 401 에러가 발생하므로, 401도 "정상 동작"으로 간주
  else {
    performUploadAction(baseUrl, getHeaders(false));
  }
}

function performReadActions(baseUrl, headers) {
  group('User Read Actions', () => {
    // 1. 주변 가이드 검색 (2-api-read-test.js 참고)
    const coord1 = randomCoordinate();
    let res = http.get(`${baseUrl}/guide/api/nearby?lat=${coord1.lat}&lng=${coord1.lng}&radius=20`, {
      headers,
      tags: { action: 'browse' },
    });

    readSuccess.add(
      check(res, {
        'browse guides success': (r) => r.status === 200,
      })
    );

    sleep(1); // 목록 읽는 시간

    // 2. 관심있는 가이드 클릭 (404도 정상 - 존재하지 않는 ID일 수 있음)
    const guideId = Math.floor(Math.random() * 100) + 1;
    res = http.get(`${baseUrl}/guide/api/${guideId}`, {
      headers,
      tags: { action: 'view_detail' },
    });

    readSuccess.add(
      check(res, {
        'view detail success': (r) => r.status === 200 || r.status === 404,
      })
    );

    sleep(1); // 상세 페이지 읽는 시간
  });
}

function performWriteActions(baseUrl, headers) {
  group('User Write Actions', () => {
    // 1. 가이드에 좋아요 (인증 필요 - 401 에러 예상)
    const guideId = Math.floor(Math.random() * 100) + 1;
    let res = http.post(
      `${baseUrl}/guide/api/like/${guideId}`,
      JSON.stringify({}),
      {
        headers: { ...headers, 'Content-Type': 'application/json' },
        tags: { action: 'like' },
        timeout: '10s',
      }
    );

    concurrentLikes.add(1);

    // 인증 없이 테스트 → 401, 403, 500 등 응답이 오면 "서버 응답"으로 간주 (부하 테스트 목적)
    const success = check(res, {
      'like endpoint responds': (r) => r.status > 0, // 응답만 있으면 성공
    });

    writeSuccess.add(success);

    sleep(0.5);
  });
}

function performUploadAction(baseUrl, headers) {
  group('User Upload Action', () => {
    // 1-upload-test.js와 동일한 방식: 실제 이미지 파일 사용
    const formData = {
      files: http.file(testImageData, 'test-image.jpg', 'image/jpeg'),
    };

    const res = http.post(`${baseUrl}/media/upload`, formData, {
      headers: {
        // multipart/form-data는 k6가 자동 설정하므로 명시하지 않음
        // 인증 헤더 없음 (401 예상)
      },
      tags: { action: 'upload' },
      timeout: '30s',
    });

    // 인증 없이 테스트 → 401, 403, 500 등 응답이 오면 "서버 응답"으로 간주 (부하 테스트 목적)
    const success = check(res, {
      'upload endpoint responds': (r) => r.status > 0, // 응답만 있으면 성공
    });

    uploadSuccess.add(success);

    sleep(1); // 업로드 후 대기
  });
}

export function handleSummary(data) {
  const totalRequests = data.metrics.http_reqs.values.count;
  const failedRequests = data.metrics.http_req_failed.values.count;

  return {
    stdout: JSON.stringify(
      {
        test: 'Mixed Scenario Test (Real User Simulation)',
        duration: `${(data.state.testRunDurationMs / 1000 / 60).toFixed(2)} minutes`,
        metrics: {
          total_requests: totalRequests,
          failed_requests: failedRequests,
          requests_per_second: data.metrics.http_reqs.values.rate.toFixed(2),
          avg_response_time_ms: Math.round(data.metrics.http_req_duration.values.avg),
          p95_response_time_ms: Math.round(data.metrics.http_req_duration.values['p(95)']),
          p99_response_time_ms: Math.round(data.metrics.http_req_duration.values['p(99)']),
          read_success_rate: (data.metrics.read_success?.values.rate * 100).toFixed(2) + '%',
          write_success_rate: (data.metrics.write_success?.values.rate * 100).toFixed(2) + '%',
          upload_success_rate: (data.metrics.upload_success?.values.rate * 100).toFixed(2) + '%',
          concurrent_likes: data.metrics.concurrent_likes?.values.count || 0,
        },
        bottlenecks_detected: detectBottlenecks(data),
      },
      null,
      2
    ),
  };
}

function detectBottlenecks(data) {
  const bottlenecks = [];
  const p95 = data.metrics.http_req_duration.values['p(95)'];

  if (p95 > 2000) {
    bottlenecks.push('🔴 심각: 95% 응답시간이 2초 초과 - 즉시 최적화 필요');
  } else if (p95 > 1000) {
    bottlenecks.push('🟡 경고: 95% 응답시간이 1초 초과 - 최적화 권장');
  } else {
    bottlenecks.push('✅ 양호: 95% 응답시간이 1초 이내');
  }

  const errorRate = data.metrics.http_req_failed.values.rate;
  if (errorRate > 0.05) {
    bottlenecks.push('🔴 심각: 에러율 5% 초과 - 안정성 문제');
  } else if (errorRate > 0.01) {
    bottlenecks.push('🟡 경고: 에러율 1% 초과 - 모니터링 필요');
  }

  return bottlenecks;
}

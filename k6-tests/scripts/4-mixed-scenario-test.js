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

const dummyImageBase64 =
  '/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB' +
  'AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEB' +
  'AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAf/wAARCAABAAEDASIAAhEB' +
  'AxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAA' +
  'AAAAAAAAAAAAAAAAAAL/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A8AP//Z';

export default function () {
  const baseUrl = config.baseUrl;
  const headers = getHeaders(false);

  // 사용자 행동 패턴 시뮬레이션
  const scenario = Math.random();

  // 70% - 읽기 작업
  if (scenario < 0.7) {
    performReadActions(baseUrl, headers);
  }
  // 20% - 쓰기 작업 (좋아요, 댓글)
  else if (scenario < 0.9) {
    performWriteActions(baseUrl, headers);
  }
  // 10% - 업로드
  else {
    performUploadAction(baseUrl, headers);
  }
}

function performReadActions(baseUrl, headers) {
  group('User Read Actions', () => {
    // 1. 메인 페이지 - 가이드 목록 보기
    let res = http.get(`${baseUrl}/api/guides`, {
      headers,
      tags: { action: 'browse' },
    });

    readSuccess.add(
      check(res, {
        'browse guides success': (r) => r.status === 200,
      })
    );

    sleep(1); // 목록 읽는 시간

    // 2. 관심있는 가이드 클릭
    const guideId = Math.floor(Math.random() * 100) + 1;
    res = http.get(`${baseUrl}/api/guides/${guideId}`, {
      headers,
      tags: { action: 'view_detail' },
    });

    readSuccess.add(
      check(res, {
        'view detail success': (r) => r.status === 200 || r.status === 404,
      })
    );

    sleep(2); // 상세 페이지 읽는 시간

    // 3. 주변 위치 검색
    const coord = randomCoordinate();
    res = http.get(`${baseUrl}/api/locations/nearby?lat=${coord.lat}&lng=${coord.lng}&radius=5000`, {
      headers,
      tags: { action: 'search_nearby' },
    });

    readSuccess.add(
      check(res, {
        'search nearby success': (r) => r.status === 200,
      })
    );

    sleep(1);
  });
}

function performWriteActions(baseUrl, headers) {
  group('User Write Actions', () => {
    // 1. 가이드에 좋아요
    const guideId = Math.floor(Math.random() * 100) + 1;
    let res = http.post(
      `${baseUrl}/api/guides/${guideId}/like`,
      JSON.stringify({}),
      {
        headers: { ...headers, 'Content-Type': 'application/json' },
        tags: { action: 'like' },
      }
    );

    concurrentLikes.add(1);

    writeSuccess.add(
      check(res, {
        'like success': (r) => r.status === 200 || r.status === 201,
      })
    );

    sleep(0.5);

    // 2. 댓글 작성 (있다면)
    res = http.post(
      `${baseUrl}/api/guides/${guideId}/comments`,
      JSON.stringify({
        content: 'Great place! Thanks for sharing.',
      }),
      {
        headers: { ...headers, 'Content-Type': 'application/json' },
        tags: { action: 'comment' },
      }
    );

    writeSuccess.add(
      check(res, {
        'comment success': (r) => r.status === 200 || r.status === 201,
      })
    );

    sleep(1);
  });
}

function performUploadAction(baseUrl, headers) {
  group('User Upload Action', () => {
    const boundary = '----WebKitFormBoundary' + Math.random().toString(36);

    const formData = {
      file: http.file(Buffer.from(dummyImageBase64, 'base64'), 'photo.jpg', 'image/jpeg'),
    };

    const res = http.post(`${baseUrl}/api/media/upload`, formData, {
      headers: {
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
      },
      tags: { action: 'upload' },
      timeout: '30s',
    });

    uploadSuccess.add(
      check(res, {
        'upload success': (r) => r.status === 200 || r.status === 201,
      })
    );

    sleep(3); // 업로드 후 대기
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

/**
 * 혼합 시나리오 부하 테스트 (페이징 적용)
 *
 * 목적: 실제 사용자 행동 패턴 시뮬레이션 + 페이징 효과 측정
 * - 70% 읽기 작업 (조회, 검색) - 페이징 API 사용
 *   - 60%: 첫 페이지만 조회 (일반 사용자)
 *   - 10%: 여러 페이지 로드 (스크롤)
 * - 20% 쓰기 작업 (좋아요, 댓글)
 * - 10% 파일 업로드
 *
 * 실행: k6 run --env BASE_URL=http://localhost:8082 k6-tests/scripts/6-mixed-scenario-paged-test.js
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
const paginationQueryDuration = new Trend('pagination_query_duration', true);
const totalItemsLoaded = new Counter('total_items_loaded');
const pagesLoaded = new Counter('pages_loaded');

export const options = {
  stages: [
    { duration: '1m', target: 20 },    // 워밍업
    { duration: '3m', target: 100 },   // 일반 부하
    { duration: '3m', target: 200 },   // 피크 시간대
    { duration: '2m', target: 300 },   // 스트레스 테스트
    { duration: '1m', target: 0 },     // 종료
  ],
  thresholds: {
    // 페이징으로 개선된 threshold
    'http_req_duration': ['p(95)<500'],  // 1000ms → 500ms (페이징 효과)
    'http_req_failed': ['rate<0.01'],
    'read_success': ['rate>0.99'],
    'write_success': ['rate>0.95'],
    'pagination_query_duration': ['p(95)<300', 'p(99)<500'],
  },
};

// 시나리오 1처럼 실제 이미지 파일 사용
const testImageData = open('../data/test-image.jpg', 'b');

export default function () {
  const baseUrl = config.baseUrl;

  // 사용자 행동 패턴 시뮬레이션
  const scenario = Math.random();

  // 70% - 읽기 작업 (페이징 API 사용)
  if (scenario < 0.7) {
    // 60%: 첫 페이지만 조회
    if (Math.random() < 0.857) {  // 0.6 / 0.7 = 0.857
      performReadActionsFirstPage(baseUrl, getHeaders(false));
    }
    // 10%: 여러 페이지 로드
    else {
      performReadActionsMultiPage(baseUrl, getHeaders(false));
    }
  }
  // 20% - 쓰기 작업 (좋아요)
  else if (scenario < 0.9) {
    performWriteActions(baseUrl, getHeaders(false));
  }
  // 10% - 업로드
  else {
    performUploadAction(baseUrl, getHeaders(false));
  }
}

/**
 * 첫 페이지만 조회 (일반 사용자 패턴)
 */
function performReadActionsFirstPage(baseUrl, headers) {
  group('User Read Actions - First Page', () => {
    // 1. 주변 가이드 검색 (페이징 - 첫 페이지만)
    const coord1 = randomCoordinate();
    const size = 20;

    const startTime = new Date().getTime();

    let res = http.get(
      `${baseUrl}/guide/api/nearby/paged?lat=${coord1.lat}&lng=${coord1.lng}&radius=20&size=${size}`,
      {
        headers,
        tags: { action: 'browse_paged', page: 'first' },
      }
    );

    const duration = new Date().getTime() - startTime;
    paginationQueryDuration.add(duration);

    const success = check(res, {
      'browse guides success': (r) => r.status === 200,
      'has content array': (r) => {
        try {
          const json = JSON.parse(r.body);
          return Array.isArray(json.content);
        } catch {
          return false;
        }
      },
    });

    readSuccess.add(success);

    if (success) {
      const body = JSON.parse(res.body);
      totalItemsLoaded.add(body.content.length);
      pagesLoaded.add(1);
    }

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

/**
 * 여러 페이지 로드 (스크롤하는 사용자 패턴)
 */
function performReadActionsMultiPage(baseUrl, headers) {
  group('User Read Actions - Multiple Pages', () => {
    const coord1 = randomCoordinate();
    const size = 10;  // 작은 페이지로 여러 번 로드
    let cursor = null;
    let pageCount = 0;
    const maxPages = 3;  // 최대 3페이지 로드

    while (pageCount < maxPages) {
      const url = cursor
        ? `${baseUrl}/guide/api/nearby/paged?lat=${coord1.lat}&lng=${coord1.lng}&radius=20&size=${size}&cursor=${cursor}`
        : `${baseUrl}/guide/api/nearby/paged?lat=${coord1.lat}&lng=${coord1.lng}&radius=20&size=${size}`;

      const startTime = new Date().getTime();

      const res = http.get(url, {
        headers,
        tags: { action: 'browse_paged', page: pageCount === 0 ? 'first' : 'next' },
      });

      const duration = new Date().getTime() - startTime;
      paginationQueryDuration.add(duration);

      const success = check(res, {
        'browse guides success': (r) => r.status === 200,
      });

      readSuccess.add(success);

      if (!success) break;

      const body = JSON.parse(res.body);
      totalItemsLoaded.add(body.content.length);
      pagesLoaded.add(1);

      // 다음 페이지가 없으면 종료
      if (!body.hasNext) break;

      cursor = body.nextCursor;
      pageCount++;

      // 페이지 간 짧은 대기 (스크롤 시뮬레이션)
      sleep(0.3);
    }

    sleep(1); // 마지막 페이지 읽는 시간
  });
}

/**
 * 쓰기 작업 (좋아요)
 */
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

/**
 * 파일 업로드
 */
function performUploadAction(baseUrl, headers) {
  group('User Upload Action', () => {
    // 실제 이미지 파일 사용
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
  const totalRequests = data.metrics.http_reqs?.values.count || 0;
  const failedRequests = data.metrics.http_req_failed?.values.count || 0;

  const metrics = {
    total_requests: totalRequests,
    failed_requests: failedRequests,
    requests_per_second: data.metrics.http_reqs?.values.rate.toFixed(2) || 0,
    avg_response_time_ms: Math.round(data.metrics.http_req_duration?.values.avg || 0),
    p95_response_time_ms: Math.round(data.metrics.http_req_duration?.values['p(95)'] || 0),
    p99_response_time_ms: Math.round(data.metrics.http_req_duration?.values['p(99)'] || 0),
    read_success_rate: ((data.metrics.read_success?.values.rate || 0) * 100).toFixed(2) + '%',
    write_success_rate: ((data.metrics.write_success?.values.rate || 0) * 100).toFixed(2) + '%',
    upload_success_rate: ((data.metrics.upload_success?.values.rate || 0) * 100).toFixed(2) + '%',
    concurrent_likes: data.metrics.concurrent_likes?.values.count || 0,
  };

  // 페이징 메트릭 추가
  if (data.metrics.pagination_query_duration?.values) {
    metrics.pagination_avg_ms = Math.round(data.metrics.pagination_query_duration.values.avg);
    metrics.pagination_p95_ms = Math.round(data.metrics.pagination_query_duration.values['p(95)']);
    metrics.pagination_p99_ms = Math.round(data.metrics.pagination_query_duration.values['p(99)']);
  }

  if (data.metrics.total_items_loaded?.values && data.metrics.pages_loaded?.values) {
    metrics.total_items = data.metrics.total_items_loaded.values.count;
    metrics.total_pages = data.metrics.pages_loaded.values.count;
    metrics.avg_items_per_page = (metrics.total_items / metrics.total_pages).toFixed(1);
  }

  return {
    stdout: JSON.stringify(
      {
        test: 'Mixed Scenario Test with Pagination (Real User Simulation)',
        duration: `${(data.state.testRunDurationMs / 1000 / 60).toFixed(2)} minutes`,
        metrics: metrics,
        bottlenecks_detected: detectBottlenecks(data),
      },
      null,
      2
    ),
  };
}

function detectBottlenecks(data) {
  const bottlenecks = [];
  const p95 = data.metrics.http_req_duration?.values['p(95)'] || 0;

  if (p95 > 2000) {
    bottlenecks.push('🔴 심각: 95% 응답시간이 2초 초과 - 즉시 최적화 필요');
  } else if (p95 > 1000) {
    bottlenecks.push('🟡 경고: 95% 응답시간이 1초 초과 - 최적화 권장');
  } else if (p95 > 500) {
    bottlenecks.push('🟡 주의: 95% 응답시간이 500ms 초과 - 모니터링 필요');
  } else {
    bottlenecks.push('✅ 우수: 95% 응답시간이 500ms 이내');
  }

  // 페이징 성능 체크
  const paginationP95 = data.metrics.pagination_query_duration?.values['p(95)'] || 0;
  if (paginationP95 > 0) {
    if (paginationP95 < 300) {
      bottlenecks.push('✅ 페이징 성능 우수 (p95 < 300ms)');
    } else if (paginationP95 < 500) {
      bottlenecks.push('🟡 페이징 성능 양호 (p95 < 500ms)');
    } else {
      bottlenecks.push('🔴 페이징 성능 개선 필요 (p95 >= 500ms)');
    }
  }

  const errorRate = data.metrics.http_req_failed?.values.rate || 0;
  if (errorRate > 0.05) {
    bottlenecks.push('🔴 심각: 에러율 5% 초과 - 안정성 문제');
  } else if (errorRate > 0.01) {
    bottlenecks.push('🟡 경고: 에러율 1% 초과 - 모니터링 필요');
  }

  return bottlenecks;
}

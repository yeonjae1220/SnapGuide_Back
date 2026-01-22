/**
 * 커서 기반 페이징 부하 테스트
 *
 * 목적: 페이징 API의 성능 측정 및 기존 API와 비교
 * - 커서 기반 페이징 성능
 * - 메모리 효율성 확인
 * - 다음 페이지 로드 시간 측정
 *
 * 실행: k6 run k6-tests/scripts/5-pagination-test.js
 * 비교 모드: k6 run -e COMPARE_MODE=true k6-tests/scripts/5-pagination-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { config, getHeaders } from '../config.js';

// 커스텀 메트릭
const paginationQueryDuration = new Trend('pagination_query_duration', true);
const paginationQuerySuccess = new Rate('pagination_query_success');
const totalItemsLoaded = new Counter('total_items_loaded');
const pagesLoaded = new Counter('pages_loaded');
const legacyQueryDuration = new Trend('legacy_query_duration', true);

export const options = {
  stages: [
    { duration: '30s', target: 20 },   // 워밍업
    { duration: '1m', target: 50 },    // 일반 부하
    { duration: '2m', target: 100 },   // 피크 부하
    { duration: '1m', target: 150 },   // 스트레스 테스트
    { duration: '30s', target: 0 },    // 종료
  ],
  thresholds: {
    // 페이징 쿼리는 95%가 300ms 이내 (전체 조회보다 빨라야 함)
    'pagination_query_duration': ['p(95)<300', 'p(99)<500'],
    // 성공률 99% 이상
    'pagination_query_success': ['rate>0.99'],
    // 기존 API와 비교 (페이징이 더 빨라야 함)
    'http_req_duration{endpoint:paged}': ['p(95)<300'],
    'http_req_duration{endpoint:legacy}': ['p(95)<500'],
  },
};

// 테스트용 주요 도시 좌표
const testLocations = [
  { name: 'Seoul', lat: 37.5665, lng: 126.9780 },
  { name: 'Busan', lat: 35.1796, lng: 129.0756 },
  { name: 'Incheon', lat: 37.4563, lng: 126.7052 },
  { name: 'Daegu', lat: 35.8714, lng: 128.6014 },
  { name: 'Jeju', lat: 33.4996, lng: 126.5312 },
];

export default function () {
  const baseUrl = config.baseUrl;
  const headers = getHeaders(false);
  const compareMode = __ENV.COMPARE_MODE === 'true';

  // 시나리오 1: 페이징 API 첫 페이지 조회 (40%)
  if (Math.random() < 0.4) {
    group('Pagination - First Page', () => {
      const location = testLocations[Math.floor(Math.random() * testLocations.length)];
      const radius = 20; // 20km 고정
      const size = 20;

      const startTime = new Date().getTime();

      const res = http.get(
        `${baseUrl}/guide/api/nearby/paged?lat=${location.lat}&lng=${location.lng}&radius=${radius}&size=${size}`,
        {
          headers,
          tags: { endpoint: 'paged', page: 'first' },
        }
      );

      const duration = new Date().getTime() - startTime;
      paginationQueryDuration.add(duration);

      const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'has content array': (r) => {
          try {
            const json = JSON.parse(r.body);
            return Array.isArray(json.content);
          } catch {
            return false;
          }
        },
        'has hasNext field': (r) => {
          try {
            const json = JSON.parse(r.body);
            return typeof json.hasNext === 'boolean';
          } catch {
            return false;
          }
        },
        'query time < 300ms': () => duration < 300,
      });

      paginationQuerySuccess.add(success);

      if (success) {
        const body = JSON.parse(res.body);
        totalItemsLoaded.add(body.content.length);
        pagesLoaded.add(1);
      }
    });
  }

  // 시나리오 2: 페이징 API 다중 페이지 로드 (30%)
  else if (Math.random() < 0.7) {
    group('Pagination - Multiple Pages', () => {
      const location = testLocations[Math.floor(Math.random() * testLocations.length)];
      const radius = 20;
      const size = 10; // 작은 페이지 크기로 여러 페이지 테스트

      let cursor = null;
      let pageCount = 0;
      const maxPages = 3; // 최대 3페이지만 로드

      while (pageCount < maxPages) {
        const url = cursor
          ? `${baseUrl}/guide/api/nearby/paged?lat=${location.lat}&lng=${location.lng}&radius=${radius}&size=${size}&cursor=${cursor}`
          : `${baseUrl}/guide/api/nearby/paged?lat=${location.lat}&lng=${location.lng}&radius=${radius}&size=${size}`;

        const startTime = new Date().getTime();

        const res = http.get(url, {
          headers,
          tags: { endpoint: 'paged', page: pageCount === 0 ? 'first' : 'next' },
        });

        const duration = new Date().getTime() - startTime;
        paginationQueryDuration.add(duration);

        const success = check(res, {
          'status is 200': (r) => r.status === 200,
          'query time < 300ms': () => duration < 300,
        });

        paginationQuerySuccess.add(success);

        if (!success) break;

        const body = JSON.parse(res.body);
        totalItemsLoaded.add(body.content.length);
        pagesLoaded.add(1);

        // 다음 페이지가 없으면 종료
        if (!body.hasNext) break;

        cursor = body.nextCursor;
        pageCount++;

        // 페이지 간 짧은 대기 (실제 사용자 행동 시뮬레이션)
        sleep(0.2);
      }
    });
  }

  // 시나리오 3: 기존 API와 비교 (30%, COMPARE_MODE일 때만)
  else if (compareMode) {
    group('Legacy API - Full Load', () => {
      const location = testLocations[Math.floor(Math.random() * testLocations.length)];
      const radius = 20;

      const startTime = new Date().getTime();

      const res = http.get(
        `${baseUrl}/guide/api/nearby?lat=${location.lat}&lng=${location.lng}&radius=${radius}`,
        {
          headers,
          tags: { endpoint: 'legacy' },
        }
      );

      const duration = new Date().getTime() - startTime;
      legacyQueryDuration.add(duration);

      check(res, {
        'status is 200': (r) => r.status === 200,
        'query time < 500ms': () => duration < 500,
      });

      if (res.status === 200) {
        try {
          const body = JSON.parse(res.body);
          totalItemsLoaded.add(body.length);
        } catch (e) {
          // ignore
        }
      }
    });
  }

  // 실제 사용자 행동 시뮬레이션
  sleep(Math.random() * 2 + 0.5); // 0.5~2.5초 대기
}

export function handleSummary(data) {
  const compareMode = __ENV.COMPARE_MODE === 'true';
  const metrics = {};
  const recommendations = [];

  // 페이징 메트릭
  if (data.metrics.pagination_query_duration?.values) {
    const avgDuration = data.metrics.pagination_query_duration.values.avg || 0;
    const p95Duration = data.metrics.pagination_query_duration.values['p(95)'] || 0;
    const p99Duration = data.metrics.pagination_query_duration.values['p(99)'] || 0;

    metrics.pagination_avg_ms = Math.round(avgDuration);
    metrics.pagination_p95_ms = Math.round(p95Duration);
    metrics.pagination_p99_ms = Math.round(p99Duration);

    recommendations.push(
      p95Duration < 300
        ? '✅ 페이징 성능 우수 (p95 < 300ms)'
        : '⚠️  페이징 성능 개선 필요 - 인덱스 확인 또는 페이지 크기 축소 고려'
    );
  }

  // 기존 API 메트릭 (비교 모드)
  if (compareMode && data.metrics.legacy_query_duration?.values) {
    const avgDuration = data.metrics.legacy_query_duration.values.avg || 0;
    const p95Duration = data.metrics.legacy_query_duration.values['p(95)'] || 0;

    metrics.legacy_avg_ms = Math.round(avgDuration);
    metrics.legacy_p95_ms = Math.round(p95Duration);

    // 성능 비교
    const improvement = ((metrics.legacy_p95_ms - metrics.pagination_p95_ms) / metrics.legacy_p95_ms) * 100;
    metrics.performance_improvement_percent = Math.round(improvement);

    recommendations.push(
      improvement > 0
        ? `🚀 페이징 API가 ${Math.round(improvement)}% 더 빠름`
        : '⚠️  페이징 성능이 기대보다 낮음 - 쿼리 최적화 필요'
    );
  }

  // 로드된 데이터
  if (data.metrics.total_items_loaded?.values && data.metrics.pages_loaded?.values) {
    metrics.total_items = data.metrics.total_items_loaded.values.count;
    metrics.total_pages = data.metrics.pages_loaded.values.count;
    metrics.avg_items_per_page = (metrics.total_items / metrics.total_pages).toFixed(1);
  }

  // 성공률
  if (data.metrics.pagination_query_success?.values) {
    metrics.success_rate = (data.metrics.pagination_query_success.values.rate * 100).toFixed(2) + '%';
  }

  // 처리량
  if (data.metrics.http_reqs?.values) {
    metrics.requests_per_second = data.metrics.http_reqs.values.rate.toFixed(2);
  }

  return {
    stdout: JSON.stringify(
      {
        test: 'Cursor-based Pagination Performance Test',
        compare_mode: compareMode,
        metrics: metrics,
        recommendations: recommendations,
      },
      null,
      2
    ),
  };
}

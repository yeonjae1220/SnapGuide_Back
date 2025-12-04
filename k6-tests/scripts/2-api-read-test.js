/**
 * API 읽기 작업 부하 테스트
 *
 * 목적: 가이드 조회 API의 성능 측정 (캐싱 효과 확인)
 * - 전체 가이드 목록 조회
 * - 단건 가이드 조회
 * - 사용자별 가이드 조회
 *
 * 실행: k6 run k6-tests/scripts/2-api-read-test.js
 * 실행 (캐싱 활성화 후): k6 run -e CACHE_ENABLED=true k6-tests/scripts/2-api-read-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { config, getHeaders, randomItem } from '../config.js';

// 커스텀 메트릭
const cacheHitRate = new Rate('cache_hit_rate');
const apiSuccessRate = new Rate('api_success_rate');
const queryDuration = new Trend('query_duration', true);
const queriesCount = new Counter('total_queries');

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // 워밍업
    { duration: '1m', target: 50 },    // 일반 부하
    { duration: '2m', target: 100 },   // 피크 부하
    { duration: '1m', target: 200 },   // 스트레스 테스트
    { duration: '30s', target: 0 },    // 종료
  ],
  thresholds: {
    // 95%가 500ms 이내
    'http_req_duration': ['p(95)<500'],
    // 99%가 1초 이내
    'http_req_duration{api_type:read}': ['p(99)<1000'],
    // 성공률 99% 이상
    'api_success_rate': ['rate>0.99'],
    // 에러율 1% 미만
    'http_req_failed': ['rate<0.01'],
  },
};

export default function () {
  const baseUrl = config.baseUrl;
  const headers = getHeaders(false);

  // 시나리오 1: 전체 가이드 목록 조회 (30%)
  if (Math.random() < 0.3) {
    group('Get All Guides', () => {
      const startTime = new Date().getTime();
      const res = http.get(`${baseUrl}/api/guides`, {
        headers,
        tags: { api_type: 'read', endpoint: 'all_guides' },
      });

      queryDuration.add(new Date().getTime() - startTime);
      queriesCount.add(1);

      const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'has guides array': (r) => {
          try {
            const json = JSON.parse(r.body);
            return Array.isArray(json) || Array.isArray(json.data);
          } catch {
            return false;
          }
        },
        'response time < 500ms': (r) => r.timings.duration < 500,
      });

      apiSuccessRate.add(success);

      // 캐시 히트 체크 (응답 헤더에 X-Cache-Hit 같은 헤더가 있다면)
      if (res.headers['X-Cache-Hit'] === 'true') {
        cacheHitRate.add(1);
      } else {
        cacheHitRate.add(0);
      }
    });
  }

  // 시나리오 2: 특정 가이드 상세 조회 (40%)
  else if (Math.random() < 0.7) {
    group('Get Guide Detail', () => {
      // 1~100 사이의 랜덤 ID (실제 데이터에 맞게 수정)
      const guideId = Math.floor(Math.random() * 100) + 1;
      const startTime = new Date().getTime();

      const res = http.get(`${baseUrl}/api/guides/${guideId}`, {
        headers,
        tags: { api_type: 'read', endpoint: 'guide_detail' },
      });

      queryDuration.add(new Date().getTime() - startTime);
      queriesCount.add(1);

      const success = check(res, {
        'status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'response time < 300ms': (r) => r.timings.duration < 300,
      });

      apiSuccessRate.add(success);

      if (res.headers['X-Cache-Hit'] === 'true') {
        cacheHitRate.add(1);
      } else {
        cacheHitRate.add(0);
      }
    });
  }

  // 시나리오 3: 사용자별 가이드 조회 (30%)
  else {
    group('Get Guides By Member', () => {
      const memberId = randomItem(config.testData.memberIds);
      const startTime = new Date().getTime();

      const res = http.get(`${baseUrl}/api/guides/member/${memberId}`, {
        headers,
        tags: { api_type: 'read', endpoint: 'member_guides' },
      });

      queryDuration.add(new Date().getTime() - startTime);
      queriesCount.add(1);

      const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 500ms': (r) => r.timings.duration < 500,
      });

      apiSuccessRate.add(success);

      if (res.headers['X-Cache-Hit'] === 'true') {
        cacheHitRate.add(1);
      } else {
        cacheHitRate.add(0);
      }
    });
  }

  // 사용자는 페이지를 읽는 데 시간이 필요
  sleep(Math.random() * 3 + 1); // 1~4초 대기
}

export function handleSummary(data) {
  const cacheEnabled = __ENV.CACHE_ENABLED === 'true';

  return {
    stdout: JSON.stringify(
      {
        test: 'API Read Test',
        cache_enabled: cacheEnabled,
        metrics: {
          total_queries: data.metrics.total_queries.values.count,
          query_duration_avg: data.metrics.query_duration.values.avg,
          query_duration_p95: data.metrics.query_duration.values['p(95)'],
          query_duration_p99: data.metrics.query_duration.values['p(99)'],
          api_success_rate: data.metrics.api_success_rate.values.rate,
          cache_hit_rate: data.metrics.cache_hit_rate?.values.rate || 0,
          http_req_failed: data.metrics.http_req_failed.values.rate,
          requests_per_second: data.metrics.http_reqs.values.rate,
        },
        summary: cacheEnabled
          ? '✅ 캐싱 활성화 - 성능 개선 확인'
          : '⚠️  캐싱 비활성화 - 베이스라인 측정',
      },
      null,
      2
    ),
  };
}

/**
 * 공간 쿼리 (PostGIS) 부하 테스트
 *
 * 목적: PostGIS ST_DWithin 쿼리 성능 측정
 * - 반경 검색 (주변 위치 찾기)
 * - 공간 인덱스(GIST) 효과 확인
 *
 * 실행: k6 run k6-tests/scripts/3-spatial-query-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { config, getHeaders, randomCoordinate } from '../config.js';

// 커스텀 메트릭
const spatialQueryDuration = new Trend('spatial_query_duration', true);
const spatialQuerySuccess = new Rate('spatial_query_success');
const slowQueriesCount = new Counter('slow_queries_count');

export const options = {
  stages: [
    { duration: '30s', target: 20 },   // 워밍업
    { duration: '1m', target: 50 },    // 일반 부하
    { duration: '2m', target: 100 },   // 피크 부하
    { duration: '1m', target: 150 },   // 스트레스 테스트
    { duration: '30s', target: 0 },    // 종료
  ],
  thresholds: {
    // 공간 쿼리는 복잡하므로 95%가 1초 이내
    'spatial_query_duration': ['p(95)<500', 'p(99)<1000'],
    // 성공률 99% 이상
    'spatial_query_success': ['rate>0.99'],
    // 느린 쿼리(2초 이상)가 전체의 5% 미만
    'slow_queries_count': ['count<50'],
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

  // 시나리오 1: 고정 위치 주변 검색 (50%)
  if (Math.random() < 0.5) {
    group('Spatial Query - Fixed Location', () => {
      const location = testLocations[Math.floor(Math.random() * testLocations.length)];
      const radius = [5, 10, 20, 50][Math.floor(Math.random() * 4)]; // 5km, 10km, 20km, 50km (단위: km)

      const startTime = new Date().getTime();

      const res = http.get(
        `${baseUrl}/guide/api/nearby?lat=${location.lat}&lng=${location.lng}&radius=${radius}`,
        {
          headers,
          tags: { query_type: 'spatial', location: location.name, radius: radius },
        }
      );

      const duration = new Date().getTime() - startTime;
      spatialQueryDuration.add(duration);

      if (duration > 500) {
        slowQueriesCount.add(1);
        console.warn(`Slow query detected: ${duration}ms at ${location.name} with radius ${radius}km`);
      }

      const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'has results': (r) => {
          try {
            const json = JSON.parse(r.body);
            return json && (Array.isArray(json) || Array.isArray(json.data));
          } catch {
            return false;
          }
        },
        'query time < 0.5s': () => duration < 500,
        'query time < 1s': () => duration < 1000,
      });

      spatialQuerySuccess.add(success);
    });
  }

  // 시나리오 2: 랜덤 위치 주변 검색 (30%)
  else if (Math.random() < 0.6) {
    group('Spatial Query - Random Location', () => {
      const coord = randomCoordinate();
      const radius = 5; // 5km 고정

      const startTime = new Date().getTime();

      const res = http.get(
        `${baseUrl}/guide/api/nearby?lat=${coord.lat}&lng=${coord.lng}&radius=${radius}`,
        {
          headers,
          tags: { query_type: 'spatial', location: 'random', radius: radius },
        }
      );

      const duration = new Date().getTime() - startTime;
      spatialQueryDuration.add(duration);

      if (duration > 500) {
        slowQueriesCount.add(1);
      }

      const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'query time < 0.5s': () => duration < 500,
      });

      spatialQuerySuccess.add(success);
    });
  }

  // 시나리오 3: 가이드 공간 검색 (20%)
  else {
    group('Spatial Query - Guides Nearby', () => {
      const location = testLocations[Math.floor(Math.random() * testLocations.length)];
      const radius = 5; // 5km 고정

      const startTime = new Date().getTime();

      const res = http.get(
        `${baseUrl}/guide/api/nearby?lat=${location.lat}&lng=${location.lng}&radius=${radius}`,
        {
          headers,
          tags: { query_type: 'spatial', endpoint: 'guides', radius: radius },
        }
      );

      const duration = new Date().getTime() - startTime;
      spatialQueryDuration.add(duration);

      if (duration > 500) {
        slowQueriesCount.add(1);
      }

      const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'query time < 0.5s': () => duration < 500,
      });

      spatialQuerySuccess.add(success);
    });
  }

  // 실제 사용자는 지도를 보며 탐색
  sleep(Math.random() * 2 + 0.5); // 0.5~2.5초 대기
}

export function handleSummary(data) {
  const metrics = {};
  const recommendations = [];

  // 안전하게 메트릭 접근
  if (data.metrics.spatial_query_duration?.values) {
    const avgDuration = data.metrics.spatial_query_duration.values.avg || 0;
    const p95Duration = data.metrics.spatial_query_duration.values['p(95)'] || 0;
    const p99Duration = data.metrics.spatial_query_duration.values['p(99)'] || 0;

    metrics.avg_duration_ms = Math.round(avgDuration);
    metrics.p95_duration_ms = Math.round(p95Duration);
    metrics.p99_duration_ms = Math.round(p99Duration);

    // 인덱스 추천
    recommendations.push(
      p95Duration > 500
        ? '⚠️  GIST 인덱스 확인 필요: CREATE INDEX idx_location_coordinate ON location USING GIST(coordinate);'
        : '✅ 공간 인덱스가 잘 작동하고 있습니다'
    );
  }

  if (data.metrics.spatial_query_success?.values) {
    metrics.success_rate = data.metrics.spatial_query_success.values.rate;
  }

  if (data.metrics.slow_queries_count?.values && data.metrics.http_reqs?.values) {
    const slowQueries = data.metrics.slow_queries_count.values.count;
    const totalRequests = data.metrics.http_reqs.values.count;

    metrics.slow_queries_count = slowQueries;
    metrics.slow_queries_percentage = ((slowQueries / totalRequests) * 100).toFixed(2) + '%';

    // 느린 쿼리 추천
    recommendations.push(
      slowQueries > totalRequests * 0.05
        ? '⚠️  느린 쿼리가 많습니다. 반경 크기를 줄이거나 결과를 제한하세요'
        : '✅ 쿼리 성능이 양호합니다'
    );
  }

  if (data.metrics.http_reqs?.values) {
    metrics.requests_per_second = data.metrics.http_reqs.values.rate.toFixed(2);
  }

  return {
    stdout: JSON.stringify(
      {
        test: 'Spatial Query Test (PostGIS)',
        metrics: metrics,
        recommendations: recommendations,
      },
      null,
      2
    ),
  };
}

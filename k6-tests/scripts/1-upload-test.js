/**
 * 파일 업로드 부하 테스트
 *
 * 목적: 가장 큰 병목인 파일 업로드 성능 측정
 * - HEIC 변환
 * - 썸네일 생성
 * - EXIF 추출
 * - Google Maps API 호출
 * - DB 저장
 *
 * 실행: k6 run k6-tests/scripts/1-upload-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { config, getHeaders } from '../config.js';

// 커스텀 메트릭
const uploadSuccessRate = new Rate('upload_success_rate');
const uploadDuration = new Trend('upload_duration', true);

export const options = {
  stages: [
    { duration: '30s', target: 5 },   // 천천히 시작 (동시 업로드 5개)
    { duration: '1m', target: 10 },   // 일반 부하
    { duration: '1m', target: 20 },   // 피크 부하
    { duration: '30s', target: 0 },   // 종료
  ],
  thresholds: {
    // 업로드는 95%가 10초 이내 (현재 병목 상태)
    'http_req_duration': ['p(95)<10000'],
    // 성공률 95% 이상
    'upload_success_rate': ['rate>0.95'],
    // HTTP 에러율 5% 미만
    'http_req_failed': ['rate<0.05'],
  },
};

// 테스트용 더미 이미지 데이터 (1KB 정도의 작은 JPG)
const dummyImageBase64 =
  '/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEB' +
  'AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEB' +
  'AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAf/wAARCAABAAEDASIAAhEB' +
  'AxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAA' +
  'AAAAAAAAAAAAAAAAAAL/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A8AP//Z';

export default function () {
  const url = `${config.baseUrl}/api/media/upload`;

  // Multipart form data 구성
  const boundary = '----WebKitFormBoundary' + Math.random().toString(36);

  const formData = {
    file: http.file(Buffer.from(dummyImageBase64, 'base64'), 'test-image.jpg', 'image/jpeg'),
  };

  const params = {
    headers: {
      'Content-Type': `multipart/form-data; boundary=${boundary}`,
      // 인증이 필요하면 추가
      // 'Authorization': `Bearer ${config.auth.token}`,
    },
    tags: { type: 'upload' },
    timeout: '30s', // 타임아웃 30초
  };

  // 업로드 시작 시간
  const startTime = new Date().getTime();

  // 파일 업로드
  const res = http.post(url, formData, params);

  // 소요 시간 측정
  const duration = new Date().getTime() - startTime;
  uploadDuration.add(duration);

  // 결과 체크
  const success = check(res, {
    'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
    'response has data': (r) => r.body && r.body.length > 0,
    'upload took less than 10s': () => duration < 10000,
    'upload took less than 5s': () => duration < 5000, // 목표치
  });

  uploadSuccessRate.add(success);

  if (!success) {
    console.error(`Upload failed: ${res.status} - ${res.body}`);
  }

  // 요청 간 간격 (실제 사용자는 연속으로 업로드하지 않음)
  sleep(Math.random() * 2 + 1); // 1~3초 대기
}

// 테스트 종료 시 요약 출력
export function handleSummary(data) {
  return {
    'stdout': JSON.stringify({
      test: 'File Upload Test',
      metrics: {
        upload_duration_avg: data.metrics.upload_duration.values.avg,
        upload_duration_p95: data.metrics.upload_duration.values['p(95)'],
        upload_duration_p99: data.metrics.upload_duration.values['p(99)'],
        upload_success_rate: data.metrics.upload_success_rate.values.rate,
        http_req_failed: data.metrics.http_req_failed.values.rate,
      },
    }, null, 2),
  };
}

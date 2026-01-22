/**
 * k6 부하테스트 스크립트 - 가이드 API
 *
 * 실행 방법:
 * k6 run k6/scripts/load-test-guide-api.js
 *
 * 옵션 설정:
 * k6 run --vus 100 --duration 30s k6/scripts/load-test-guide-api.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js';

// CSV 데이터 로드 (한 번만 로드되고 모든 VU가 공유)
const members = new SharedArray('members', function () {
    return papaparse.parse(open('../data/members.csv'), { header: true }).data;
});

const guides = new SharedArray('guides', function () {
    return papaparse.parse(open('../data/guides.csv'), { header: true }).data;
});

// 테스트 설정
export const options = {
    // 시나리오 1: 점진적 부하 증가
    stages: [
        { duration: '1m', target: 50 },   // 1분 동안 50 VU로 증가
        { duration: '3m', target: 100 },  // 3분 동안 100 VU 유지
        { duration: '2m', target: 200 },  // 2분 동안 200 VU로 증가
        { duration: '2m', target: 200 },  // 2분 동안 200 VU 유지
        { duration: '1m', target: 0 },    // 1분 동안 0으로 감소
    ],

    // 성능 임계값 설정
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95%는 500ms, 99%는 1000ms 이하
        http_req_failed: ['rate<0.01'],  // 실패율 1% 미만
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';

// 테스트 시작 전 로그인하여 토큰 획득
export function setup() {
    console.log('🚀 테스트 준비 중...');
    console.log(`📊 로드된 회원 수: ${members.length}`);
    console.log(`📊 로드된 가이드 수: ${guides.length}`);

    // 테스트용 회원으로 로그인
    const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
        email: 'loadtest1@example.com',
        password: 'test1234'
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    if (loginRes.status === 200) {
        const token = loginRes.json('accessToken');
        console.log('✅ 로그인 성공');
        return { token };
    } else {
        console.error('❌ 로그인 실패:', loginRes.status, loginRes.body);
        return null;
    }
}

// 메인 테스트 시나리오
export default function (data) {
    if (!data || !data.token) {
        console.error('❌ 토큰이 없습니다. setup() 확인 필요');
        return;
    }

    const headers = {
        'Authorization': `Bearer ${data.token}`,
        'Content-Type': 'application/json',
    };

    // 시나리오 1: 가이드 목록 조회 (70%)
    if (Math.random() < 0.7) {
        testGuideList(headers);
    }
    // 시나리오 2: 특정 가이드 조회 (20%)
    else if (Math.random() < 0.9) {
        testGuideDetail(headers);
    }
    // 시나리오 3: 내 가이드 조회 (10%)
    else {
        testMyGuides(headers);
    }

    sleep(1); // 사용자 행동 시뮬레이션
}

/**
 * 가이드 목록 조회 (위치 기반)
 */
function testGuideList(headers) {
    // 한국 내 랜덤 위치 (서울 근처)
    const lat = 37.5 + (Math.random() - 0.5) * 0.5;  // 37.25 ~ 37.75
    const lng = 127.0 + (Math.random() - 0.5) * 0.5; // 126.75 ~ 127.25
    const radius = [5, 10, 20, 50][Math.floor(Math.random() * 4)];

    const res = http.get(
        `${BASE_URL}/guide/api/nearby?lat=${lat}&lng=${lng}&radius=${radius}`,
        { headers }
    );

    check(res, {
        '가이드 목록 조회 성공': (r) => r.status === 200,
        '가이드 목록 응답 시간 < 500ms': (r) => r.timings.duration < 500,
        '가이드 목록 데이터 존재': (r) => {
            try {
                const body = JSON.parse(r.body);
                return Array.isArray(body);
            } catch {
                return false;
            }
        },
    });
}

/**
 * 특정 가이드 상세 조회
 */
function testGuideDetail(headers) {
    // 랜덤 가이드 선택
    const randomGuide = guides[Math.floor(Math.random() * guides.length)];

    const res = http.get(
        `${BASE_URL}/guide/api/${randomGuide.id}`,
        { headers }
    );

    check(res, {
        '가이드 상세 조회 성공': (r) => r.status === 200,
        '가이드 상세 응답 시간 < 300ms': (r) => r.timings.duration < 300,
    });
}

/**
 * 내 가이드 목록 조회
 */
function testMyGuides(headers) {
    const res = http.get(`${BASE_URL}/guide/api/my`, { headers });

    check(res, {
        '내 가이드 조회 성공': (r) => r.status === 200,
        '내 가이드 응답 시간 < 400ms': (r) => r.timings.duration < 400,
    });
}

// 테스트 종료 후 요약
export function teardown(data) {
    console.log('🏁 테스트 완료!');
}

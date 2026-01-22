#!/bin/bash

###############################################################################
# SnapGuide k6 부하 테스트 실행 스크립트
#
# 사용법:
#   ./k6-tests/run-all-tests.sh                    # 모든 테스트 실행
#   ./k6-tests/run-all-tests.sh baseline           # 베이스라인 측정
#   ./k6-tests/run-all-tests.sh optimized          # 최적화 후 측정
#   ./k6-tests/run-all-tests.sh compare            # 결과 비교
###############################################################################

set -e  # 에러 발생 시 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 설정
BASE_URL="${BASE_URL:-http://localhost:8080}"
RESULTS_DIR="k6-tests/results"
MODE="${1:-all}"

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

# 헬퍼 함수
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 서버 헬스체크
check_server() {
    print_header "서버 상태 확인"

    if curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        print_success "서버가 정상적으로 실행 중입니다: ${BASE_URL}"
    else
        print_error "서버에 접속할 수 없습니다: ${BASE_URL}"
        print_warning "애플리케이션을 먼저 실행하세요: ./gradlew bootRun"
        exit 1
    fi
}

# k6 설치 확인
check_k6() {
    if ! command -v k6 &> /dev/null; then
        print_error "k6가 설치되어 있지 않습니다"
        echo -e "\n설치 방법:"
        echo -e "  macOS:   ${YELLOW}brew install k6${NC}"
        echo -e "  Linux:   공식 문서 참조 (https://k6.io/docs/getting-started/installation/)"
        echo -e "  Windows: ${YELLOW}choco install k6${NC}"
        exit 1
    fi
    print_success "k6 버전: $(k6 version)"
}

# 단일 테스트 실행
run_test() {
    local test_name=$1
    local test_file=$2
    local output_file=$3

    print_header "실행: ${test_name}"

    echo "테스트 파일: ${test_file}"
    echo "결과 저장: ${output_file}"
    echo ""

    if k6 run \
        -e BASE_URL="${BASE_URL}" \
        --out json="${output_file}" \
        "${test_file}"; then
        print_success "${test_name} 완료"
        return 0
    else
        print_error "${test_name} 실패"
        return 1
    fi
}

# 모든 테스트 실행
run_all_tests() {
    local suffix=$1
    local failed_tests=()

    print_header "전체 부하 테스트 시작"
    echo "모드: ${suffix:-기본}"
    echo "서버: ${BASE_URL}"
    echo ""

    # 1. 파일 업로드 테스트
    if ! run_test \
        "파일 업로드 테스트" \
        "k6-tests/scripts/1-upload-test.js" \
        "${RESULTS_DIR}/${suffix}upload.json"; then
        failed_tests+=("upload")
    fi

    sleep 5  # 서버 회복 시간

    # 2. API 읽기 테스트
    if ! run_test \
        "API 읽기 테스트" \
        "k6-tests/scripts/2-api-read-test.js" \
        "${RESULTS_DIR}/${suffix}api-read.json"; then
        failed_tests+=("api-read")
    fi

    sleep 5

    # 3. 공간 쿼리 테스트
    if ! run_test \
        "공간 쿼리 테스트" \
        "k6-tests/scripts/3-spatial-query-test.js" \
        "${RESULTS_DIR}/${suffix}spatial.json"; then
        failed_tests+=("spatial")
    fi

    sleep 5

    # 4. 혼합 시나리오 테스트
    if ! run_test \
        "혼합 시나리오 테스트" \
        "k6-tests/scripts/4-mixed-scenario-test.js" \
        "${RESULTS_DIR}/${suffix}mixed.json"; then
        failed_tests+=("mixed")
    fi

    # 결과 요약
    print_header "테스트 완료"

    if [ ${#failed_tests[@]} -eq 0 ]; then
        print_success "모든 테스트가 성공적으로 완료되었습니다!"
    else
        print_warning "일부 테스트가 실패했습니다: ${failed_tests[*]}"
    fi

    echo ""
    echo "결과 파일 위치: ${RESULTS_DIR}"
    ls -lh "${RESULTS_DIR}/${suffix}"*.json 2>/dev/null || true
}

# 결과 비교
compare_results() {
    print_header "성능 비교 분석"

    if [ ! -f "${RESULTS_DIR}/baseline-mixed.json" ] || [ ! -f "${RESULTS_DIR}/optimized-mixed.json" ]; then
        print_error "비교할 결과 파일이 없습니다"
        echo "먼저 베이스라인과 최적화 테스트를 실행하세요:"
        echo "  ./k6-tests/run-all-tests.sh baseline"
        echo "  ./k6-tests/run-all-tests.sh optimized"
        exit 1
    fi

    echo "베이스라인: ${RESULTS_DIR}/baseline-mixed.json"
    echo "최적화 후: ${RESULTS_DIR}/optimized-mixed.json"
    echo ""

    # 간단한 비교 (jq 사용)
    if command -v jq &> /dev/null; then
        print_success "성능 개선 결과:"

        baseline_p95=$(jq -r '.metrics.http_req_duration.values."p(95)"' "${RESULTS_DIR}/baseline-mixed.json" 2>/dev/null || echo "N/A")
        optimized_p95=$(jq -r '.metrics.http_req_duration.values."p(95)"' "${RESULTS_DIR}/optimized-mixed.json" 2>/dev/null || echo "N/A")

        echo "  P95 응답시간:"
        echo "    Before: ${baseline_p95}ms"
        echo "    After:  ${optimized_p95}ms"

        if [ "$baseline_p95" != "N/A" ] && [ "$optimized_p95" != "N/A" ]; then
            improvement=$(echo "scale=2; (($baseline_p95 - $optimized_p95) / $baseline_p95) * 100" | bc)
            echo "    개선율: ${improvement}%"
        fi
    else
        print_warning "jq가 설치되어 있지 않아 자동 비교를 건너뜁니다"
        echo "수동으로 JSON 파일을 비교하세요"
    fi
}

# 메인 로직
main() {
    echo -e "${GREEN}"
    echo "╔═══════════════════════════════════════╗"
    echo "║  SnapGuide k6 부하 테스트 도구       ║"
    echo "╚═══════════════════════════════════════╝"
    echo -e "${NC}"

    # 사전 체크
    check_k6
    check_server

    # 모드에 따라 실행
    case "$MODE" in
        baseline)
            run_all_tests "baseline-"
            ;;
        optimized)
            run_all_tests "optimized-"
            ;;
        compare)
            compare_results
            ;;
        all|*)
            run_all_tests ""
            ;;
    esac

    print_header "모든 작업 완료"
    print_success "테스트 결과는 ${RESULTS_DIR} 디렉토리에서 확인하세요"

    echo ""
    echo "다음 단계:"
    echo "  1. Grafana 대시보드 확인: http://localhost:3000"
    echo "  2. 병목 지점 분석 및 코드 개선"
    echo "  3. 개선 후 다시 테스트 실행"
    echo ""
}

# 스크립트 실행
main

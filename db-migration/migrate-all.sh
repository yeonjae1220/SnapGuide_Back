#!/bin/bash

###############################################################################
# 위치 데이터를 Local → Docker 환경으로 한 번에 마이그레이션하는 스크립트
#
# 사용법:
#   ./db-migration/migrate-all.sh [docker|nas-docker|both]
#
# 기능:
#   1. Local DB에서 위치 데이터 백업
#   2. 선택한 환경(들)로 데이터 가져오기
#   3. 검증 및 결과 리포트
###############################################################################

set -e

# PostgreSQL 경로 설정
if [ -d "/Library/PostgreSQL/17/bin" ]; then
    export PATH="/Library/PostgreSQL/17/bin:$PATH"
elif [ -d "/Library/PostgreSQL/16/bin" ]; then
    export PATH="/Library/PostgreSQL/16/bin:$PATH"
elif [ -d "/Library/PostgreSQL/15/bin" ]; then
    export PATH="/Library/PostgreSQL/15/bin:$PATH"
elif [ -d "/opt/homebrew/opt/postgresql@14/bin" ]; then
    export PATH="/opt/homebrew/opt/postgresql@14/bin:$PATH"
elif [ -d "/opt/homebrew/opt/postgresql@15/bin" ]; then
    export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"
elif [ -d "/opt/homebrew/opt/postgresql/bin" ]; then
    export PATH="/opt/homebrew/opt/postgresql/bin:$PATH"
elif [ -d "/usr/local/opt/postgresql@14/bin" ]; then
    export PATH="/usr/local/opt/postgresql@14/bin:$PATH"
elif [ -d "/usr/local/opt/postgresql/bin" ]; then
    export PATH="/usr/local/opt/postgresql/bin:$PATH"
fi

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

TARGET="${1:-docker}"

print_header() {
    echo -e "\n${PURPLE}╔═══════════════════════════════════════╗${NC}"
    echo -e "${PURPLE}║  $1${NC}"
    echo -e "${PURPLE}╚═══════════════════════════════════════╝${NC}\n"
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

print_header "SnapGuide 위치 데이터 마이그레이션"

echo "마이그레이션 대상: ${TARGET}"
echo ""

# Step 1: Export
print_warning "Step 1/3: Local 데이터베이스에서 데이터 백업 중..."
if ./db-migration/export-location-data.sh; then
    print_success "백업 완료"
else
    print_error "백업 실패"
    exit 1
fi

echo ""
sleep 2

# Step 2: Import
if [ "$TARGET" == "both" ]; then
    # Docker 환경
    print_warning "Step 2a/3: Docker 환경으로 가져오기..."
    if ./db-migration/import-location-data.sh docker; then
        print_success "Docker 가져오기 완료"
    else
        print_error "Docker 가져오기 실패"
        exit 1
    fi

    echo ""
    sleep 2

    # NAS-Docker 환경
    print_warning "Step 2b/3: NAS-Docker 환경으로 가져오기..."
    if ./db-migration/import-location-data.sh nas-docker; then
        print_success "NAS-Docker 가져오기 완료"
    else
        print_error "NAS-Docker 가져오기 실패"
        exit 1
    fi
else
    print_warning "Step 2/3: ${TARGET} 환경으로 가져오기..."
    if ./db-migration/import-location-data.sh "$TARGET"; then
        print_success "${TARGET} 가져오기 완료"
    else
        print_error "${TARGET} 가져오기 실패"
        exit 1
    fi
fi

echo ""
sleep 2

# Step 3: Verification
print_warning "Step 3/3: 마이그레이션 검증 중..."

# .env 로드
if [ -f .env ]; then
    # 주석 제거 및 공백 정리
    while IFS= read -r line; do
        # 빈 줄과 주석 무시
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        # 공백 제거 및 export
        line=$(echo "$line" | sed 's/[[:space:]]*=[[:space:]]*/=/g')
        export "$line"
    done < .env
fi

export PGPASSWORD="${POSTGRES_PASSWORD}"

# Local 데이터 개수
LOCAL_COUNT=$(PGPASSWORD="${POSTGRES_PASSWORD_LOCAL}" psql -h localhost -p 5432 -U "${POSTGRES_USER_LOCAL}" -d "${POSTGRES_DB_LOCAL:-snapguidedb}" -t -c "SELECT COUNT(*) FROM location;" 2>/dev/null | xargs || echo "0")

# Docker 데이터 개수
if [ "$TARGET" == "docker" ] || [ "$TARGET" == "both" ]; then
    DOCKER_COUNT=$(psql -h localhost -p 5433 -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -t -c "SELECT COUNT(*) FROM location;" 2>/dev/null | xargs || echo "0")
else
    DOCKER_COUNT="N/A"
fi

# NAS-Docker 데이터 개수
if [ "$TARGET" == "nas-docker" ] || [ "$TARGET" == "both" ]; then
    NAS_DOCKER_COUNT=$(psql -h localhost -p 5434 -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -t -c "SELECT COUNT(*) FROM location;" 2>/dev/null | xargs || echo "0")
else
    NAS_DOCKER_COUNT="N/A"
fi

unset PGPASSWORD

# 결과 리포트
print_header "마이그레이션 결과 리포트"

echo "┌─────────────────────┬──────────────┐"
echo "│ 환경                │ 위치 데이터  │"
echo "├─────────────────────┼──────────────┤"
printf "│ Local (원본)        │ %12s │\n" "$LOCAL_COUNT"
printf "│ Docker              │ %12s │\n" "$DOCKER_COUNT"
printf "│ NAS-Docker          │ %12s │\n" "$NAS_DOCKER_COUNT"
echo "└─────────────────────┴──────────────┘"
echo ""

# 검증
SUCCESS=true

if [ "$TARGET" == "docker" ] || [ "$TARGET" == "both" ]; then
    if [ "$DOCKER_COUNT" == "$LOCAL_COUNT" ]; then
        print_success "Docker: 데이터 개수 일치 (${DOCKER_COUNT}개)"
    else
        print_error "Docker: 데이터 개수 불일치 (Local: ${LOCAL_COUNT}, Docker: ${DOCKER_COUNT})"
        SUCCESS=false
    fi
fi

if [ "$TARGET" == "nas-docker" ] || [ "$TARGET" == "both" ]; then
    if [ "$NAS_DOCKER_COUNT" == "$LOCAL_COUNT" ]; then
        print_success "NAS-Docker: 데이터 개수 일치 (${NAS_DOCKER_COUNT}개)"
    else
        print_error "NAS-Docker: 데이터 개수 불일치 (Local: ${LOCAL_COUNT}, NAS-Docker: ${NAS_DOCKER_COUNT})"
        SUCCESS=false
    fi
fi

echo ""

if [ "$SUCCESS" == true ]; then
    print_header "🎉 마이그레이션 성공!"

    echo "다음 단계:"
    echo "  1. 애플리케이션 재시작:"
    echo "     docker-compose restart backend"
    echo ""
    echo "  2. API 테스트:"
    echo "     curl http://localhost:8082/api/locations | jq"
    echo ""
    echo "  3. k6 부하 테스트 실행:"
    echo "     k6 run k6-tests/scripts/3-spatial-query-test.js"
    echo ""
else
    print_header "⚠️  마이그레이션 경고"
    echo "데이터 개수가 일치하지 않습니다."
    echo "수동으로 확인이 필요합니다."
fi

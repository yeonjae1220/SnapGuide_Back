#!/bin/bash

###############################################################################
# Local 데이터베이스의 위치 데이터를 백업하는 스크립트
#
# 사용법:
#   ./db-migration/export-location-data.sh
#
# 결과:
#   - db-migration/location_data.sql 파일 생성
#   - PostGIS geometry 타입 포함
###############################################################################

set -e  # 에러 발생 시 중단

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
NC='\033[0m' # No Color

# 설정 - .env 파일에서 읽기
if [ -f .env ]; then
    # 주석 제거 및 공백 정리
    while IFS= read -r line; do
        # 빈 줄과 주석 무시
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        # 공백 제거 및 export
        line=$(echo "$line" | sed 's/[[:space:]]*=[[:space:]]*/=/g')
        export "$line"
    done < .env
else
    echo -e "${RED}❌ .env 파일을 찾을 수 없습니다${NC}"
    exit 1
fi

# Local DB 설정
LOCAL_DB_HOST="${POSTGRES_HOST_LOCAL:-localhost}"
LOCAL_DB_PORT="${POSTGRES_PORT_LOCAL:-5432}"
LOCAL_DB_NAME="${POSTGRES_DB_LOCAL:-snapguidedb}"
LOCAL_DB_USER="${POSTGRES_USER_LOCAL}"
LOCAL_DB_PASSWORD="${POSTGRES_PASSWORD_LOCAL}"

# 출력 파일
OUTPUT_DIR="db-migration"
OUTPUT_FILE="${OUTPUT_DIR}/location_data.sql"

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

# 디렉토리 생성
mkdir -p "$OUTPUT_DIR"

print_header "Local 데이터베이스에서 위치 데이터 내보내기"

echo "설정:"
echo "  호스트: ${LOCAL_DB_HOST}:${LOCAL_DB_PORT}"
echo "  데이터베이스: ${LOCAL_DB_NAME}"
echo "  사용자: ${LOCAL_DB_USER}"
echo "  출력 파일: ${OUTPUT_FILE}"
echo ""

# DB 연결 테스트
print_warning "데이터베이스 연결 확인 중..."

export PGPASSWORD="${LOCAL_DB_PASSWORD}"

if ! psql -h "$LOCAL_DB_HOST" -p "$LOCAL_DB_PORT" -U "$LOCAL_DB_USER" -d "$LOCAL_DB_NAME" -c "SELECT 1" > /dev/null 2>&1; then
    print_error "데이터베이스에 연결할 수 없습니다"
    echo "다음을 확인하세요:"
    echo "  1. PostgreSQL이 실행 중인가?"
    echo "  2. .env 파일의 DB 정보가 정확한가?"
    echo "  3. 방화벽이 차단하고 있지 않은가?"
    exit 1
fi

print_success "데이터베이스 연결 성공"

# 위치 데이터 개수 확인
print_warning "위치 데이터 개수 확인 중..."

LOCATION_COUNT=$(psql -h "$LOCAL_DB_HOST" -p "$LOCAL_DB_PORT" -U "$LOCAL_DB_USER" -d "$LOCAL_DB_NAME" -t -c "SELECT COUNT(*) FROM location;" | xargs)

if [ "$LOCATION_COUNT" -eq 0 ]; then
    print_warning "위치 데이터가 없습니다. 백업을 건너뜁니다."
    exit 0
fi

print_success "위치 데이터 ${LOCATION_COUNT}개 발견"

# 데이터 백업
print_warning "위치 데이터 백업 시작..."

# location 테이블만 백업 (PostGIS geometry 포함)
pg_dump \
    -h "$LOCAL_DB_HOST" \
    -p "$LOCAL_DB_PORT" \
    -U "$LOCAL_DB_USER" \
    -d "$LOCAL_DB_NAME" \
    --table=location \
    --data-only \
    --column-inserts \
    --no-owner \
    --no-privileges \
    > "$OUTPUT_FILE"

if [ $? -eq 0 ]; then
    print_success "백업 완료: ${OUTPUT_FILE}"

    # 파일 크기 확인
    FILE_SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
    echo "  파일 크기: ${FILE_SIZE}"
    echo "  데이터 개수: ${LOCATION_COUNT}개"
else
    print_error "백업 실패"
    exit 1
fi

# 백업 파일 검증
print_warning "백업 파일 검증 중..."

if grep -q "INSERT INTO" "$OUTPUT_FILE"; then
    print_success "백업 파일이 정상적으로 생성되었습니다"
else
    print_error "백업 파일에 데이터가 없습니다"
    exit 1
fi

print_header "백업 완료"
print_success "위치 데이터가 성공적으로 백업되었습니다"

echo ""
echo "다음 단계:"
echo "  1. Docker 환경 실행: docker-compose up -d"
echo "  2. 데이터 가져오기: ./db-migration/import-location-data.sh docker"
echo "  3. NAS-Docker에도 동일하게 적용"
echo ""

# 비밀번호 변수 제거
unset PGPASSWORD

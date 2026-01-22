#!/bin/bash

###############################################################################
# 백업된 위치 데이터를 Docker 데이터베이스로 가져오는 스크립트
#
# 사용법:
#   ./db-migration/import-location-data.sh docker      # Docker 환경으로 가져오기
#   ./db-migration/import-location-data.sh nas-docker  # NAS-Docker 환경으로 가져오기
#
# 전제 조건:
#   - db-migration/location_data.sql 파일 존재
#   - Docker 컨테이너가 실행 중
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

# 환경 선택
TARGET_ENV="${1:-docker}"

if [ "$TARGET_ENV" != "docker" ] && [ "$TARGET_ENV" != "nas-docker" ]; then
    echo -e "${RED}❌ 잘못된 환경입니다. 'docker' 또는 'nas-docker'를 지정하세요${NC}"
    echo "사용법: ./db-migration/import-location-data.sh [docker|nas-docker]"
    exit 1
fi

# .env 파일에서 설정 읽기
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

# Docker DB 설정 (환경에 따라 다름)
if [ "$TARGET_ENV" == "docker" ]; then
    TARGET_DB_HOST="localhost"
    TARGET_DB_PORT="5433"  # docker-compose.yml의 포트 매핑
elif [ "$TARGET_ENV" == "nas-docker" ]; then
    TARGET_DB_HOST="localhost"
    TARGET_DB_PORT="5434"  # NAS-Docker용 포트 (필요시 수정)
fi

TARGET_DB_NAME="${POSTGRES_DB}"
TARGET_DB_USER="${POSTGRES_USER}"
TARGET_DB_PASSWORD="${POSTGRES_PASSWORD}"

# 백업 파일
INPUT_FILE="db-migration/location_data.sql"

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

print_header "${TARGET_ENV} 환경으로 위치 데이터 가져오기"

# 백업 파일 확인
if [ ! -f "$INPUT_FILE" ]; then
    print_error "백업 파일을 찾을 수 없습니다: ${INPUT_FILE}"
    echo "먼저 export 스크립트를 실행하세요:"
    echo "  ./db-migration/export-location-data.sh"
    exit 1
fi

print_success "백업 파일 발견: ${INPUT_FILE}"

FILE_SIZE=$(du -h "$INPUT_FILE" | cut -f1)
echo "  파일 크기: ${FILE_SIZE}"
echo ""

echo "대상 데이터베이스:"
echo "  호스트: ${TARGET_DB_HOST}:${TARGET_DB_PORT}"
echo "  데이터베이스: ${TARGET_DB_NAME}"
echo "  사용자: ${TARGET_DB_USER}"
echo ""

# Docker 컨테이너 상태 확인
print_warning "Docker 컨테이너 상태 확인 중..."

if [ "$TARGET_ENV" == "docker" ]; then
    CONTAINER_NAME="snapguide-postgres-db"
else
    CONTAINER_NAME="snapguide-nas-postgres-db"  # NAS용 컨테이너명 (필요시 수정)
fi

if ! docker ps | grep -q "$CONTAINER_NAME"; then
    print_error "Docker 컨테이너가 실행 중이지 않습니다: ${CONTAINER_NAME}"
    echo "먼저 Docker Compose를 시작하세요:"
    echo "  docker-compose up -d"
    exit 1
fi

print_success "Docker 컨테이너 실행 중"

# DB 연결 테스트
print_warning "데이터베이스 연결 확인 중..."

export PGPASSWORD="${TARGET_DB_PASSWORD}"

if ! psql -h "$TARGET_DB_HOST" -p "$TARGET_DB_PORT" -U "$TARGET_DB_USER" -d "$TARGET_DB_NAME" -c "SELECT 1" > /dev/null 2>&1; then
    print_error "데이터베이스에 연결할 수 없습니다"
    echo "다음을 확인하세요:"
    echo "  1. Docker 컨테이너가 정상적으로 실행되고 있나요?"
    echo "  2. 포트 매핑이 정확한가요? (docker-compose.yml 확인)"
    echo "  3. .env 파일의 DB 정보가 정확한가요?"
    exit 1
fi

print_success "데이터베이스 연결 성공"

# 기존 데이터 확인
print_warning "기존 위치 데이터 확인 중..."

EXISTING_COUNT=$(psql -h "$TARGET_DB_HOST" -p "$TARGET_DB_PORT" -U "$TARGET_DB_USER" -d "$TARGET_DB_NAME" -t -c "SELECT COUNT(*) FROM location;" | xargs)

if [ "$EXISTING_COUNT" -gt 0 ]; then
    print_warning "기존 위치 데이터가 ${EXISTING_COUNT}개 존재합니다"
    echo ""
    echo "어떻게 하시겠습니까?"
    echo "  1) 기존 데이터 삭제 후 가져오기 (권장)"
    echo "  2) 기존 데이터 유지하고 추가"
    echo "  3) 취소"
    echo ""
    read -p "선택 (1-3): " choice

    case $choice in
        1)
            print_warning "기존 데이터 삭제 중..."
            psql -h "$TARGET_DB_HOST" -p "$TARGET_DB_PORT" -U "$TARGET_DB_USER" -d "$TARGET_DB_NAME" -c "TRUNCATE TABLE location RESTART IDENTITY CASCADE;" > /dev/null
            print_success "기존 데이터 삭제 완료"
            ;;
        2)
            print_warning "기존 데이터를 유지합니다"
            ;;
        3)
            print_warning "취소되었습니다"
            exit 0
            ;;
        *)
            print_error "잘못된 선택입니다"
            exit 1
            ;;
    esac
fi

# 데이터 가져오기
print_warning "위치 데이터 가져오기 시작..."

# SQL 파일 실행
psql -h "$TARGET_DB_HOST" -p "$TARGET_DB_PORT" -U "$TARGET_DB_USER" -d "$TARGET_DB_NAME" -f "$INPUT_FILE" > /dev/null 2>&1

if [ $? -eq 0 ]; then
    print_success "데이터 가져오기 완료"
else
    print_error "데이터 가져오기 실패"
    exit 1
fi

# 결과 확인
print_warning "결과 확인 중..."

NEW_COUNT=$(psql -h "$TARGET_DB_HOST" -p "$TARGET_DB_PORT" -U "$TARGET_DB_USER" -d "$TARGET_DB_NAME" -t -c "SELECT COUNT(*) FROM location;" | xargs)

print_success "현재 위치 데이터: ${NEW_COUNT}개"

# 샘플 데이터 조회
echo ""
echo "샘플 데이터 (최근 5개):"
psql -h "$TARGET_DB_HOST" -p "$TARGET_DB_PORT" -U "$TARGET_DB_USER" -d "$TARGET_DB_NAME" -c "
SELECT
    id,
    SUBSTRING(location_name, 1, 30) as location_name,
    country,
    city,
    ST_AsText(coordinate) as coordinate
FROM location
ORDER BY id DESC
LIMIT 5;
" || true

print_header "가져오기 완료"
print_success "위치 데이터가 성공적으로 ${TARGET_ENV} 환경으로 가져왔습니다"

echo ""
echo "다음 단계:"
echo "  1. 애플리케이션 재시작: docker-compose restart backend"
echo "  2. API 테스트: curl http://localhost:8082/api/locations"
echo "  3. k6 부하 테스트 실행"
echo ""

# 비밀번호 변수 제거
unset PGPASSWORD

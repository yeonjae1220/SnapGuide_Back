#!/bin/bash

# 데이터 오류 수정 스크립트
# NULL 값, 잘못된 geometry, 좌표 범위 오류 등을 수정합니다

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# .env 파일 로드
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
else
    echo -e "${RED}❌ .env 파일을 찾을 수 없습니다${NC}"
    exit 1
fi

# DB 연결 정보
DB_HOST="${POSTGRES_HOST_LOCAL:-localhost}"
DB_PORT="${POSTGRES_PORT_LOCAL:-5432}"
DB_NAME="${POSTGRES_DB_LOCAL:-snapguidedb}"
DB_USER="${POSTGRES_USER_LOCAL}"
DB_PASSWORD="${POSTGRES_PASSWORD_LOCAL}"

# psql 명령어 래퍼
run_query() {
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -A -c "$1"
}

run_query_formatted() {
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "$1"
}

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BLUE}🔧 데이터 오류 수정 시작${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 문제 확인
echo -e "${BLUE}현재 문제 확인 중...${NC}"
echo ""

NULL_LOCATION_NAME=$(run_query "SELECT COUNT(*) FROM location WHERE location_name IS NULL OR location_name = '';")
NULL_COUNTRY=$(run_query "SELECT COUNT(*) FROM location WHERE country IS NULL OR country = '';")
NULL_CITY=$(run_query "SELECT COUNT(*) FROM location WHERE city IS NULL OR city = '';")
INVALID_GEOMETRY=$(run_query "SELECT COUNT(*) FROM location WHERE coordinate IS NOT NULL AND NOT ST_IsValid(coordinate);")
INVALID_COORDINATES=$(run_query "
SELECT COUNT(*) FROM location
WHERE coordinate IS NOT NULL
AND (
    ST_Y(coordinate) < -90 OR ST_Y(coordinate) > 90 OR
    ST_X(coordinate) < -180 OR ST_X(coordinate) > 180
);
")

TOTAL_ISSUES=$((NULL_LOCATION_NAME + INVALID_GEOMETRY + INVALID_COORDINATES))

if [ "$TOTAL_ISSUES" -eq 0 ]; then
    echo -e "${GREEN}✅ 치명적인 문제가 없습니다!${NC}"

    if [ "$NULL_COUNTRY" -gt 0 ] || [ "$NULL_CITY" -gt 0 ]; then
        echo -e "${YELLOW}⚠️  경미한 문제가 있습니다:${NC}"
        [ "$NULL_COUNTRY" -gt 0 ] && echo -e "  - country NULL: ${NULL_COUNTRY}개"
        [ "$NULL_CITY" -gt 0 ] && echo -e "  - city NULL: ${NULL_CITY}개"
        echo ""
        read -p "이 문제들을 수정하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo -e "${BLUE}취소되었습니다${NC}"
            exit 0
        fi
    else
        exit 0
    fi
else
    echo -e "${RED}발견된 치명적인 문제:${NC}"
    [ "$NULL_LOCATION_NAME" -gt 0 ] && echo -e "  ${RED}❌ location_name NULL/빈값: ${NULL_LOCATION_NAME}개${NC}"
    [ "$INVALID_GEOMETRY" -gt 0 ] && echo -e "  ${RED}❌ 잘못된 geometry: ${INVALID_GEOMETRY}개${NC}"
    [ "$INVALID_COORDINATES" -gt 0 ] && echo -e "  ${RED}❌ 좌표 범위 오류: ${INVALID_COORDINATES}개${NC}"

    if [ "$NULL_COUNTRY" -gt 0 ] || [ "$NULL_CITY" -gt 0 ]; then
        echo ""
        echo -e "${YELLOW}발견된 경미한 문제:${NC}"
        [ "$NULL_COUNTRY" -gt 0 ] && echo -e "  ${YELLOW}⚠️  country NULL/빈값: ${NULL_COUNTRY}개${NC}"
        [ "$NULL_CITY" -gt 0 ] && echo -e "  ${YELLOW}⚠️  city NULL/빈값: ${NULL_CITY}개${NC}"
    fi

    echo ""
fi

echo ""
echo -e "${YELLOW}수정 전략:${NC}"
[ "$NULL_LOCATION_NAME" -gt 0 ] && echo "- location_name이 NULL/빈값인 레코드는 삭제됩니다"
[ "$INVALID_GEOMETRY" -gt 0 ] && echo "- 잘못된 geometry를 가진 레코드는 삭제됩니다"
[ "$INVALID_COORDINATES" -gt 0 ] && echo "- 좌표 범위를 벗어난 레코드는 삭제됩니다"
[ "$NULL_COUNTRY" -gt 0 ] && echo "- country가 NULL/빈값인 레코드는 'Unknown'으로 설정됩니다"
[ "$NULL_CITY" -gt 0 ] && echo "- city가 NULL/빈값인 레코드는 'Unknown'으로 설정됩니다"
echo ""

read -p "데이터 오류를 수정하시겠습니까? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}취소되었습니다${NC}"
    exit 0
fi

echo ""
echo -e "${BLUE}데이터 수정 중...${NC}"

# 백업 테이블 생성
echo "백업 테이블 생성 중..."
BACKUP_TABLE="location_backup_$(date +%Y%m%d_%H%M%S)"
run_query "CREATE TABLE ${BACKUP_TABLE} AS SELECT * FROM location;"
echo -e "${GREEN}✅ 백업 완료: ${BACKUP_TABLE}${NC}"

# 트랜잭션으로 안전하게 처리
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" <<EOF

BEGIN;

-- 1. country, city NULL 값 수정
UPDATE location
SET country = COALESCE(NULLIF(country, ''), 'Unknown')
WHERE country IS NULL OR country = '';

UPDATE location
SET city = COALESCE(NULLIF(city, ''), 'Unknown')
WHERE city IS NULL OR city = '';

\echo '✅ country, city NULL 값 수정 완료'

-- 2. 삭제할 레코드 확인
CREATE TEMP TABLE locations_to_delete AS
SELECT id, location_name,
    CASE
        WHEN location_name IS NULL OR location_name = '' THEN 'NULL location_name'
        WHEN coordinate IS NOT NULL AND NOT ST_IsValid(coordinate) THEN 'Invalid geometry'
        WHEN coordinate IS NOT NULL AND (
            ST_Y(coordinate) < -90 OR ST_Y(coordinate) > 90 OR
            ST_X(coordinate) < -180 OR ST_X(coordinate) > 180
        ) THEN 'Invalid coordinate range'
        ELSE 'Unknown'
    END as reason
FROM location
WHERE
    (location_name IS NULL OR location_name = '')
    OR (coordinate IS NOT NULL AND NOT ST_IsValid(coordinate))
    OR (coordinate IS NOT NULL AND (
        ST_Y(coordinate) < -90 OR ST_Y(coordinate) > 90 OR
        ST_X(coordinate) < -180 OR ST_X(coordinate) > 180
    ));

\echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
\echo '삭제할 레코드:'
SELECT reason, COUNT(*) as count
FROM locations_to_delete
GROUP BY reason;
\echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'

-- 3. 관련 외래 키 데이터 정리
-- media 테이블에서 해당 location을 참조하는 레코드의 location_id를 NULL로
UPDATE media
SET location_id = NULL
WHERE location_id IN (SELECT id FROM locations_to_delete);

\echo '✅ media 테이블 정리 완료'

-- guide 테이블에서 해당 location을 참조하는 레코드의 location_id를 NULL로
UPDATE guide
SET location_id = NULL
WHERE location_id IN (SELECT id FROM locations_to_delete);

\echo '✅ guide 테이블 정리 완료'

-- 4. 문제 있는 location 삭제
DELETE FROM location
WHERE id IN (SELECT id FROM locations_to_delete);

\echo '✅ 문제 있는 location 삭제 완료'

COMMIT;

EOF

echo ""
echo -e "${GREEN}✅ 데이터 수정 완료!${NC}"
echo ""

# 결과 확인
echo -e "${BLUE}수정 후 통계:${NC}"
FINAL_COUNT=$(run_query "SELECT COUNT(*) FROM location;")
echo -e "현재 레코드 수: ${GREEN}${FINAL_COUNT}${NC}"

REMAINING_ISSUES=$(run_query "
SELECT COUNT(*) FROM location
WHERE
    (location_name IS NULL OR location_name = '')
    OR (coordinate IS NOT NULL AND NOT ST_IsValid(coordinate))
    OR (coordinate IS NOT NULL AND (
        ST_Y(coordinate) < -90 OR ST_Y(coordinate) > 90 OR
        ST_X(coordinate) < -180 OR ST_X(coordinate) > 180
    ));
")

if [ "$REMAINING_ISSUES" -eq 0 ]; then
    echo -e "${GREEN}✅ 모든 치명적인 문제가 해결되었습니다!${NC}"
else
    echo -e "${RED}❌ ${REMAINING_ISSUES}개의 문제가 남아있습니다${NC}"
fi

echo ""
echo -e "${YELLOW}💡 참고:${NC}"
echo -e "백업 테이블: ${BACKUP_TABLE}"
echo -e "필요시 복원: CREATE TABLE location AS SELECT * FROM ${BACKUP_TABLE};"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

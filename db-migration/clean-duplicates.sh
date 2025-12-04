#!/bin/bash

# 중복 데이터 정리 스크립트
# 동일한 위치명+좌표를 가진 중복 데이터를 제거합니다

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
echo -e "${BLUE}🧹 중복 데이터 정리 시작${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 중복 데이터 확인
DUPLICATE_COUNT=$(run_query "
SELECT COUNT(*) FROM (
    SELECT location_name, ST_AsText(coordinate) as coord
    FROM location
    WHERE coordinate IS NOT NULL
    GROUP BY location_name, coord
    HAVING COUNT(*) > 1
) AS duplicates;
")

if [ "$DUPLICATE_COUNT" -eq 0 ]; then
    echo -e "${GREEN}✅ 중복 데이터가 없습니다!${NC}"
    exit 0
fi

echo -e "${YELLOW}⚠️  ${DUPLICATE_COUNT}개의 중복된 위치를 발견했습니다${NC}"
echo ""

echo "중복 데이터 예시 (상위 10개):"
run_query_formatted "
SELECT
    location_name,
    country,
    city,
    ST_AsText(coordinate) as coordinate,
    COUNT(*) as duplicate_count
FROM location
WHERE coordinate IS NOT NULL
GROUP BY location_name, country, city, coordinate
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC
LIMIT 10;
"

echo ""
echo -e "${YELLOW}중복 제거 전략:${NC}"
echo "- 동일한 위치명+좌표를 가진 데이터 중 가장 오래된 것(ID가 작은 것)을 유지합니다"
echo "- media, guide 테이블의 외래 키는 자동으로 업데이트됩니다"
echo ""

read -p "중복 데이터를 제거하시겠습니까? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}취소되었습니다${NC}"
    exit 0
fi

echo ""
echo -e "${BLUE}중복 제거 중...${NC}"

# 트랜잭션으로 안전하게 처리
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" <<EOF

BEGIN;

-- 임시 테이블: 유지할 location ID 목록
CREATE TEMP TABLE locations_to_keep AS
SELECT MIN(id) as keep_id
FROM location
WHERE coordinate IS NOT NULL
GROUP BY location_name, ST_AsText(coordinate);

-- 임시 테이블: 삭제할 location ID 목록
CREATE TEMP TABLE locations_to_delete AS
SELECT l.id as delete_id, k.keep_id
FROM location l
INNER JOIN locations_to_keep k ON
    l.location_name = (SELECT location_name FROM location WHERE id = k.keep_id)
    AND ST_AsText(l.coordinate) = ST_AsText((SELECT coordinate FROM location WHERE id = k.keep_id))
WHERE l.id != k.keep_id;

-- 삭제할 레코드 수 출력
\echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'
\echo '삭제할 중복 레코드:'
SELECT COUNT(*) as will_delete FROM locations_to_delete;
\echo '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━'

-- media 테이블의 외래 키 업데이트
UPDATE media m
SET location_id = d.keep_id
FROM locations_to_delete d
WHERE m.location_id = d.delete_id;

\echo 'media 테이블 업데이트 완료'

-- guide 테이블의 외래 키 업데이트
UPDATE guide g
SET location_id = d.keep_id
FROM locations_to_delete d
WHERE g.location_id = d.delete_id;

\echo 'guide 테이블 업데이트 완료'

-- 중복 location 삭제
DELETE FROM location
WHERE id IN (SELECT delete_id FROM locations_to_delete);

\echo 'location 중복 제거 완료'

COMMIT;

EOF

echo ""
echo -e "${GREEN}✅ 중복 제거 완료!${NC}"
echo ""

# 결과 확인
echo -e "${BLUE}정리 후 통계:${NC}"
FINAL_COUNT=$(run_query "SELECT COUNT(*) FROM location;")
echo -e "현재 레코드 수: ${GREEN}${FINAL_COUNT}${NC}"

REMAINING_DUPLICATES=$(run_query "
SELECT COUNT(*) FROM (
    SELECT location_name, ST_AsText(coordinate) as coord
    FROM location
    WHERE coordinate IS NOT NULL
    GROUP BY location_name, coord
    HAVING COUNT(*) > 1
) AS duplicates;
")

if [ "$REMAINING_DUPLICATES" -eq 0 ]; then
    echo -e "${GREEN}✅ 모든 중복이 제거되었습니다!${NC}"
else
    echo -e "${YELLOW}⚠️  ${REMAINING_DUPLICATES}개의 중복이 남아있습니다${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

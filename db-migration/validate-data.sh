#!/bin/bash

# 데이터베이스 마이그레이션 전 검증 스크립트
# Local PostgreSQL의 location 데이터를 검증하고 문제점을 리포트합니다

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
echo -e "${BLUE}📊 Location 데이터 검증 시작${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 1. 기본 통계
echo -e "${BLUE}1. 기본 통계${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

TOTAL_COUNT=$(run_query "SELECT COUNT(*) FROM location;")
echo -e "총 레코드 수: ${GREEN}${TOTAL_COUNT}${NC}"

WITH_COORDINATE=$(run_query "SELECT COUNT(*) FROM location WHERE coordinate IS NOT NULL;")
WITHOUT_COORDINATE=$(run_query "SELECT COUNT(*) FROM location WHERE coordinate IS NULL;")
echo -e "Coordinate 있음: ${GREEN}${WITH_COORDINATE}${NC}"
echo -e "Coordinate 없음: ${YELLOW}${WITHOUT_COORDINATE}${NC}"

echo ""

# 2. 중복 검사
echo -e "${BLUE}2. 중복 데이터 검사${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ID 중복 (있으면 안 됨)
DUPLICATE_IDS=$(run_query "SELECT COUNT(*) FROM (SELECT id FROM location GROUP BY id HAVING COUNT(*) > 1) AS duplicates;")
if [ "$DUPLICATE_IDS" -gt 0 ]; then
    echo -e "${RED}❌ 중복된 ID: ${DUPLICATE_IDS}개${NC}"
    run_query_formatted "SELECT id, COUNT(*) as count FROM location GROUP BY id HAVING COUNT(*) > 1 ORDER BY count DESC LIMIT 10;"
else
    echo -e "${GREEN}✅ ID 중복 없음${NC}"
fi

echo ""

# 위치명 + 좌표 중복
DUPLICATE_LOCATIONS=$(run_query "
SELECT COUNT(*) FROM (
    SELECT location_name, ST_AsText(coordinate) as coord
    FROM location
    WHERE coordinate IS NOT NULL
    GROUP BY location_name, coord
    HAVING COUNT(*) > 1
) AS duplicates;
")

if [ "$DUPLICATE_LOCATIONS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  동일 위치명 + 좌표 중복: ${DUPLICATE_LOCATIONS}개${NC}"
    echo "중복된 위치 예시 (상위 10개):"
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
else
    echo -e "${GREEN}✅ 위치명+좌표 중복 없음${NC}"
fi

echo ""

# 3. NULL 값 검사
echo -e "${BLUE}3. 필수 필드 NULL 검사${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

NULL_LOCATION_NAME=$(run_query "SELECT COUNT(*) FROM location WHERE location_name IS NULL OR location_name = '';")
NULL_COUNTRY=$(run_query "SELECT COUNT(*) FROM location WHERE country IS NULL OR country = '';")
NULL_CITY=$(run_query "SELECT COUNT(*) FROM location WHERE city IS NULL OR city = '';")

if [ "$NULL_LOCATION_NAME" -gt 0 ]; then
    echo -e "${RED}❌ location_name이 NULL/빈값: ${NULL_LOCATION_NAME}개${NC}"
else
    echo -e "${GREEN}✅ location_name 모두 유효${NC}"
fi

if [ "$NULL_COUNTRY" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  country가 NULL/빈값: ${NULL_COUNTRY}개${NC}"
else
    echo -e "${GREEN}✅ country 모두 유효${NC}"
fi

if [ "$NULL_CITY" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  city가 NULL/빈값: ${NULL_CITY}개${NC}"
else
    echo -e "${GREEN}✅ city 모두 유효${NC}"
fi

echo ""

# 4. Geometry 유효성 검사
echo -e "${BLUE}4. Geometry 데이터 유효성 검사${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

INVALID_GEOMETRY=$(run_query "SELECT COUNT(*) FROM location WHERE coordinate IS NOT NULL AND NOT ST_IsValid(coordinate);")
if [ "$INVALID_GEOMETRY" -gt 0 ]; then
    echo -e "${RED}❌ 유효하지 않은 geometry: ${INVALID_GEOMETRY}개${NC}"
    run_query_formatted "
SELECT
    id,
    location_name,
    ST_AsText(coordinate) as invalid_coordinate,
    ST_IsValidReason(coordinate) as reason
FROM location
WHERE coordinate IS NOT NULL AND NOT ST_IsValid(coordinate)
LIMIT 10;
"
else
    echo -e "${GREEN}✅ 모든 geometry 유효${NC}"
fi

echo ""

# 좌표 범위 검사 (위도: -90~90, 경도: -180~180)
INVALID_COORDINATES=$(run_query "
SELECT COUNT(*) FROM location
WHERE coordinate IS NOT NULL
AND (
    ST_Y(coordinate) < -90 OR ST_Y(coordinate) > 90 OR
    ST_X(coordinate) < -180 OR ST_X(coordinate) > 180
);
")

if [ "$INVALID_COORDINATES" -gt 0 ]; then
    echo -e "${RED}❌ 좌표 범위를 벗어난 데이터: ${INVALID_COORDINATES}개${NC}"
    run_query_formatted "
SELECT
    id,
    location_name,
    ST_Y(coordinate) as latitude,
    ST_X(coordinate) as longitude
FROM location
WHERE coordinate IS NOT NULL
AND (
    ST_Y(coordinate) < -90 OR ST_Y(coordinate) > 90 OR
    ST_X(coordinate) < -180 OR ST_X(coordinate) > 180
)
LIMIT 10;
"
else
    echo -e "${GREEN}✅ 모든 좌표가 유효한 범위 내${NC}"
fi

echo ""

# 5. 외래 키 참조 검사 (media, guide 테이블)
echo -e "${BLUE}5. 외래 키 참조 검사${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# location을 참조하는 media 레코드 체크
ORPHAN_MEDIA=$(run_query "
SELECT COUNT(*) FROM media m
WHERE m.location_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM location l WHERE l.id = m.location_id);
" 2>/dev/null || echo "0")

if [ "$ORPHAN_MEDIA" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  참조할 location이 없는 media: ${ORPHAN_MEDIA}개${NC}"
else
    echo -e "${GREEN}✅ media 외래 키 모두 유효${NC}"
fi

# location을 참조하는 guide 레코드 체크
ORPHAN_GUIDE=$(run_query "
SELECT COUNT(*) FROM guide g
WHERE g.location_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM location l WHERE l.id = g.location_id);
" 2>/dev/null || echo "0")

if [ "$ORPHAN_GUIDE" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  참조할 location이 없는 guide: ${ORPHAN_GUIDE}개${NC}"
else
    echo -e "${GREEN}✅ guide 외래 키 모두 유효${NC}"
fi

echo ""

# 6. 샘플 데이터 출력
echo -e "${BLUE}6. 샘플 데이터 (상위 5개)${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
run_query_formatted "
SELECT
    id,
    location_name,
    country,
    city,
    ST_Y(coordinate) as latitude,
    ST_X(coordinate) as longitude
FROM location
ORDER BY id
LIMIT 5;
"

echo ""

# 7. 최종 요약
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BLUE}📋 검증 요약${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

TOTAL_ISSUES=0

if [ "$DUPLICATE_IDS" -gt 0 ]; then
    echo -e "${RED}❌ ID 중복: ${DUPLICATE_IDS}개${NC}"
    TOTAL_ISSUES=$((TOTAL_ISSUES + DUPLICATE_IDS))
fi

if [ "$DUPLICATE_LOCATIONS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  위치 중복: ${DUPLICATE_LOCATIONS}개${NC}"
fi

if [ "$NULL_LOCATION_NAME" -gt 0 ]; then
    echo -e "${RED}❌ location_name NULL: ${NULL_LOCATION_NAME}개${NC}"
    TOTAL_ISSUES=$((TOTAL_ISSUES + NULL_LOCATION_NAME))
fi

if [ "$INVALID_GEOMETRY" -gt 0 ]; then
    echo -e "${RED}❌ 잘못된 geometry: ${INVALID_GEOMETRY}개${NC}"
    TOTAL_ISSUES=$((TOTAL_ISSUES + INVALID_GEOMETRY))
fi

if [ "$INVALID_COORDINATES" -gt 0 ]; then
    echo -e "${RED}❌ 좌표 범위 오류: ${INVALID_COORDINATES}개${NC}"
    TOTAL_ISSUES=$((TOTAL_ISSUES + INVALID_COORDINATES))
fi

echo ""

if [ "$TOTAL_ISSUES" -eq 0 ] && [ "$DUPLICATE_LOCATIONS" -eq 0 ]; then
    echo -e "${GREEN}✅ 모든 검증 통과! 마이그레이션 가능합니다.${NC}"
    echo ""
    echo -e "다음 명령어로 마이그레이션을 진행하세요:"
    echo -e "${BLUE}./db-migration/migrate-all.sh docker${NC}"
elif [ "$TOTAL_ISSUES" -eq 0 ] && [ "$DUPLICATE_LOCATIONS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️  중복 데이터가 있지만 치명적이지 않습니다.${NC}"
    echo ""
    echo -e "선택 사항:"
    echo -e "1. ${BLUE}./db-migration/clean-duplicates.sh${NC} - 중복 제거 후 마이그레이션"
    echo -e "2. ${BLUE}./db-migration/migrate-all.sh docker${NC} - 그대로 마이그레이션"
else
    echo -e "${RED}❌ ${TOTAL_ISSUES}개의 치명적인 문제 발견!${NC}"
    echo ""
    echo -e "다음 명령어로 데이터를 정리하세요:"
    echo -e "${BLUE}./db-migration/fix-data-issues.sh${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

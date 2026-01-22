-- ========================================
-- Location ID 목록을 CSV로 추출
-- ========================================
-- 사용법:
-- docker exec -it snapguide-postgres psql -U postgres -d snapguidedb -f /tmp/export-location-ids.sql
-- ========================================

\echo '📍 Location ID 추출 중...'

-- Location ID를 CSV로 추출
\copy (SELECT id FROM location ORDER BY id) TO '/tmp/location_ids.csv' CSV HEADER

\echo '✅ Location ID 추출 완료: /tmp/location_ids.csv'

-- 통계 출력
SELECT
    COUNT(*) AS total_locations,
    MIN(id) AS min_id,
    MAX(id) AS max_id,
    MAX(id) - MIN(id) + 1 - COUNT(*) AS missing_ids
FROM location;

\echo ''
\echo '다음 단계:'
\echo '  docker cp snapguide-postgres:/tmp/location_ids.csv src/main/resources/load-test-data/'

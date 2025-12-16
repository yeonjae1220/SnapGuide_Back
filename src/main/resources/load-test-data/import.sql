-- ========================================
-- k6 부하테스트용 대량 데이터 임포트 스크립트
-- ========================================
-- 사용법:
-- 1. CSV 파일을 PostgreSQL 컨테이너로 복사:
--    docker cp src/main/resources/load-test-data/*.csv snapguide-postgres:/tmp/
--
-- 2. PostgreSQL 컨테이너에 접속:
--    docker exec -it snapguide-postgres psql -U postgres -d snapguidedb
--
-- 3. 이 스크립트 실행:
--    \i /tmp/import.sql
--
-- 또는 한 줄로:
--    docker exec -i snapguide-postgres psql -U postgres -d snapguidedb < src/main/resources/load-test-data/import.sql
-- ========================================

-- 기존 테스트 데이터 삭제 (선택사항)
-- 주의: 실제 운영 데이터가 있다면 이 부분을 주석 처리하세요!
DO $$
BEGIN
    RAISE NOTICE '⚠️  기존 데이터 삭제 시작...';

    -- FK 제약 조건 때문에 순서 중요
    DELETE FROM comment WHERE member_id IN (SELECT id FROM member WHERE email LIKE 'loadtest%');
    DELETE FROM media WHERE guide_id IN (SELECT id FROM guide WHERE member_id IN (SELECT id FROM member WHERE email LIKE 'loadtest%'));
    DELETE FROM media_meta_data WHERE id IN (SELECT media_metadata_id FROM media WHERE guide_id IN (SELECT id FROM guide WHERE member_id IN (SELECT id FROM member WHERE email LIKE 'loadtest%')));
    DELETE FROM guide WHERE member_id IN (SELECT id FROM member WHERE email LIKE 'loadtest%');
    DELETE FROM member_authority WHERE member_id IN (SELECT id FROM member WHERE email LIKE 'loadtest%');
    DELETE FROM member WHERE email LIKE 'loadtest%';

    RAISE NOTICE '✅ 기존 테스트 데이터 삭제 완료';
END $$;

-- 시퀀스 리셋 (선택사항)
-- 주의: 기존 데이터가 있으면 이 부분을 주석 처리하세요!
-- ALTER SEQUENCE member_id_seq RESTART WITH 1;
-- ALTER SEQUENCE guide_id_seq RESTART WITH 1;
-- ALTER SEQUENCE media_id_seq RESTART WITH 1;
-- ALTER SEQUENCE media_meta_data_id_seq RESTART WITH 1;
-- ALTER SEQUENCE camera_model_id_seq RESTART WITH 1;
-- ALTER SEQUENCE comment_id_seq RESTART WITH 1;

-- ========================================
-- 1. 카메라 모델 임포트
-- ========================================
\echo '📷 카메라 모델 임포트 중...'
COPY camera_model (id, manufacturer, model, lens)
FROM '/tmp/camera_models.csv'
DELIMITER ','
CSV HEADER;

SELECT COUNT(*) AS camera_model_count FROM camera_model;

-- ========================================
-- 2. 회원 임포트
-- ========================================
\echo '👤 회원 임포트 중...'
COPY member (id, email, password, nickname, provider, provider_id, created_at, updated_at)
FROM '/tmp/members.csv'
DELIMITER ','
CSV HEADER
NULL '';  -- 빈 문자열을 NULL로 처리

SELECT COUNT(*) AS member_count FROM member;

-- ========================================
-- 3. 회원 권한 임포트 (ElementCollection)
-- ========================================
\echo '🔐 회원 권한 임포트 중...'
COPY member_authority (member_id, authority)
FROM '/tmp/member_authority.csv'
DELIMITER ','
CSV HEADER;

SELECT COUNT(*) AS authority_count FROM member_authority;

-- ========================================
-- 4. 가이드 임포트
-- ========================================
\echo '📍 가이드 임포트 중...'
COPY guide (id, tip, member_id, location_id, like_count, created_at, updated_at)
FROM '/tmp/guides.csv'
DELIMITER ','
CSV HEADER;

SELECT COUNT(*) AS guide_count FROM guide;

-- ========================================
-- 5. 미디어 메타데이터 임포트
-- ========================================
\echo '📊 미디어 메타데이터 임포트 중...'
COPY media_meta_data (id, camera_model_id, iso, shutter_speed, aperture, white_balance,
                      focal_length, exposure_compensation, flash_mode, flash_code,
                      zoom_level, roll, time)
FROM '/tmp/media_meta_data.csv'
DELIMITER ','
CSV HEADER;

SELECT COUNT(*) AS metadata_count FROM media_meta_data;

-- ========================================
-- 6. 미디어 임포트
-- ========================================
\echo '🖼️  미디어 임포트 중...'
COPY media (id, media_name, media_url, original_key, web_key, thumbnail_key,
            file_size, guide_id, media_metadata_id, location_id)
FROM '/tmp/medias.csv'
DELIMITER ','
CSV HEADER;

SELECT COUNT(*) AS media_count FROM media;

-- ========================================
-- 7. 댓글 임포트
-- ========================================
\echo '💬 댓글 임포트 중...'
COPY comment (id, comment, member_id, guide_id)
FROM '/tmp/comments.csv'
DELIMITER ','
CSV HEADER;

SELECT COUNT(*) AS comment_count FROM comment;

-- ========================================
-- 시퀀스 동기화 (중요!)
-- ========================================
-- COPY로 id를 직접 삽입했으므로 시퀀스를 최댓값으로 업데이트해야 함
\echo '🔄 시퀀스 동기화 중...'

SELECT setval('camera_model_id_seq', (SELECT MAX(id) FROM camera_model));
SELECT setval('member_id_seq', (SELECT MAX(id) FROM member));
SELECT setval('guide_id_seq', (SELECT MAX(id) FROM guide));
SELECT setval('media_meta_data_id_seq', (SELECT MAX(id) FROM media_meta_data));
SELECT setval('media_id_seq', (SELECT MAX(id) FROM media));
SELECT setval('comment_id_seq', (SELECT MAX(id) FROM comment));

-- ========================================
-- 임포트 완료 통계
-- ========================================
\echo ''
\echo '✅ 임포트 완료!'
\echo ''
\echo '📊 임포트 통계:'

SELECT
    '회원' AS 항목,
    COUNT(*) AS 개수
FROM member
WHERE email LIKE 'loadtest%'
UNION ALL
SELECT '가이드', COUNT(*) FROM guide
UNION ALL
SELECT '미디어', COUNT(*) FROM media
UNION ALL
SELECT '메타데이터', COUNT(*) FROM media_meta_data
UNION ALL
SELECT '댓글', COUNT(*) FROM comment
UNION ALL
SELECT '카메라 모델', COUNT(*) FROM camera_model;

-- 데이터 샘플 확인
\echo ''
\echo '📋 샘플 데이터:'
SELECT id, email, nickname FROM member WHERE email LIKE 'loadtest%' LIMIT 3;
SELECT id, tip, member_id, location_id FROM guide LIMIT 3;

-- 인덱스 및 제약조건 확인
\echo ''
\echo '🔍 인덱스 상태 확인:'
\di

\echo ''
\echo '🎉 모든 데이터 임포트 완료!'
\echo '다음 단계: k6 부하테스트 실행'

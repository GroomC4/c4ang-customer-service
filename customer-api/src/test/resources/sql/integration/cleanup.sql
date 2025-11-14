-- 통합 테스트용 공통 cleanup SQL
-- 테스트 후 데이터 정리를 위해 사용

-- 외래키 제약조건 순서대로 삭제
DELETE FROM p_user_refresh_token WHERE user_id IN (
    SELECT id FROM p_user WHERE email LIKE '%@example.com'
);

DELETE FROM p_user_profile WHERE user_id IN (
    SELECT id FROM p_user WHERE email LIKE '%@example.com'
);

DELETE FROM p_user WHERE email LIKE '%@example.com';

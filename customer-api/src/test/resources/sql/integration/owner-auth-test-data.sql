-- OwnerAuthenticationControllerIntegrationTest용 테스트 데이터
-- 비밀번호: password123!
-- Bcrypt 해시: $2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq

-- Owner 사용자 생성 (로그인 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440001', 'owner@example.com', '판매자테스트',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'OWNER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, created_at, updated_at)
VALUES
    ('950e8400-e29b-41d4-a716-446655440001', '850e8400-e29b-41d4-a716-446655440001',
     '판매자테스트', '010-1111-2222', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Owner 사용자 생성 (잘못된 비밀번호 테스트용)
-- 비밀번호: correctPassword123!
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440002', 'ownerwrongpwd@example.com', '판매자비번오류',
     '$2a$10$YAoWoHPEgj3r2SaRjz8QweQxMxP6FQzEqLQzEqLQzEqLQzEqLQz1a', 'OWNER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, created_at, updated_at)
VALUES
    ('950e8400-e29b-41d4-a716-446655440002', '850e8400-e29b-41d4-a716-446655440002',
     '판매자비번오류', '010-3333-4444', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Owner 사용자 생성 (로그아웃 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440003', 'logoutowner@example.com', '판매자로그아웃',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'OWNER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, created_at, updated_at)
VALUES
    ('950e8400-e29b-41d4-a716-446655440003', '850e8400-e29b-41d4-a716-446655440003',
     '판매자로그아웃', '010-9999-0000', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

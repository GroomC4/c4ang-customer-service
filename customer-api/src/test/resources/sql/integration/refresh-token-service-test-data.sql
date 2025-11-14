-- RefreshTokenServiceIntegrationTest용 테스트 데이터
-- 비밀번호: password123!
-- Bcrypt 해시: $2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq

-- Customer 사용자 생성 (토큰 갱신 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('650e8400-e29b-41d4-a716-446655440001', 'refresh@example.com', '토큰갱신테스트',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'CUSTOMER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('750e8400-e29b-41d4-a716-446655440001', '650e8400-e29b-41d4-a716-446655440001',
     '토큰갱신테스트', '010-1111-2222', '서울시 강남구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Customer 사용자 생성 (무효화 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('650e8400-e29b-41d4-a716-446655440002', 'invalidated@example.com', '무효화토큰',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'CUSTOMER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('750e8400-e29b-41d4-a716-446655440002', '650e8400-e29b-41d4-a716-446655440002',
     '무효화토큰', '010-3333-4444', '서울시 서초구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

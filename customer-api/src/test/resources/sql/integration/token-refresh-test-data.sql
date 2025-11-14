-- TokenRefreshControllerIntegrationTest용 테스트 데이터
-- 비밀번호: password123!
-- Bcrypt 해시: $2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq

-- Customer 사용자 생성
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'refreshtoken@example.com', '토큰갱신테스트',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'CUSTOMER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('550e8400-e29b-41d4-a716-446655440002', 'relogin@example.com', '재로그인테스트',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'CUSTOMER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001',
     '토큰갱신테스트', '010-5555-6666', '서울시 송파구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002',
     '재로그인테스트', '010-9999-8888', '서울시 강남구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Owner 사용자 생성
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('550e8400-e29b-41d4-a716-446655440003', 'ownerrelogin@example.com', '판매자재로그인테스트',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'OWNER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Owner UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, created_at, updated_at)
VALUES
    ('650e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003',
     '판매자재로그인테스트', '010-7777-6666', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

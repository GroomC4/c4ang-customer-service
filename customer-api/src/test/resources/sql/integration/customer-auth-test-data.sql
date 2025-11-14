-- CustomerAuthenticationControllerIntegrationTest용 테스트 데이터
-- 비밀번호: password123!
-- Bcrypt 해시: $2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq

-- Customer 사용자 생성 (로그인 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('750e8400-e29b-41d4-a716-446655440001', 'customer@example.com', '고객테스트',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'CUSTOMER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440001', '750e8400-e29b-41d4-a716-446655440001',
     '고객테스트', '010-1111-2222', '서울시 강남구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Customer 사용자 생성 (잘못된 비밀번호 테스트용)
-- 비밀번호: correctPassword123!
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('750e8400-e29b-41d4-a716-446655440002', 'wrongpwd@example.com', '비밀번호오류',
     '$2a$10$YAoWoHPEgj3r2SaRjz8QweQxMxP6FQzEqLQzEqLQzEqLQzEqLQz1a', 'CUSTOMER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440002', '750e8400-e29b-41d4-a716-446655440002',
     '비밀번호오류', '010-3333-4444', '서울시 서초구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Customer 사용자 생성 (로그아웃 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('750e8400-e29b-41d4-a716-446655440003', 'logoutcustomer@example.com', '로그아웃테스트',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'CUSTOMER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440003', '750e8400-e29b-41d4-a716-446655440003',
     '로그아웃테스트', '010-9999-0000', '서울시 강남구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

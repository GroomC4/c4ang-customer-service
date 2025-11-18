-- InternalUserControllerIntegrationTest용 테스트 데이터
-- 비밀번호: password123!
-- Bcrypt 해시: $2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq

-- CUSTOMER 사용자 생성 (Internal API 조회 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at, last_login_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440011', 'internal-customer@example.com', '내부API고객',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'CUSTOMER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('950e8400-e29b-41d4-a716-446655440011', '850e8400-e29b-41d4-a716-446655440011',
     '내부API고객', '010-1111-2222', '서울시 강남구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- OWNER 사용자 생성 (역할 매핑 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at, last_login_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440012', 'internal-owner@example.com', '내부API점주',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'OWNER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('950e8400-e29b-41d4-a716-446655440012', '850e8400-e29b-41d4-a716-446655440012',
     '내부API점주', '010-3333-4444', '부산시 해운대구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- MANAGER 사용자 생성 (ADMIN 역할 매핑 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at, last_login_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440013', 'internal-manager@example.com', '내부API매니저',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'MANAGER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('950e8400-e29b-41d4-a716-446655440013', '850e8400-e29b-41d4-a716-446655440013',
     '내부API매니저', '010-5555-6666', '대구시 중구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- MASTER 사용자 생성 (ADMIN 역할 매핑 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at, last_login_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440014', 'internal-master@example.com', '내부API마스터',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'MASTER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- UserProfile 생성
INSERT INTO p_user_profile (id, user_id, full_name, phone_number, default_address, created_at, updated_at)
VALUES
    ('950e8400-e29b-41d4-a716-446655440014', '850e8400-e29b-41d4-a716-446655440014',
     '내부API마스터', '010-7777-8888', '인천시 남동구', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- profile이 없는 사용자 (nullable 필드 테스트용)
INSERT INTO p_user (id, email, username, password_hash, role, is_active, created_at, updated_at)
VALUES
    ('850e8400-e29b-41d4-a716-446655440015', 'internal-noprofile@example.com', '프로필없음',
     '$2y$10$GPA5baVHQy6hHc6LO1EHsOg3RYv4CvuKpvgU0/2trEmbl8X6CDgLq', 'CUSTOMER', true,
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Customer Service Database Schema for Testing

-- Extension for UUID support
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- p_user 테이블
CREATE TABLE IF NOT EXISTS p_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(10) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_email_role UNIQUE (email, role)
);

-- p_user_profile 테이블
CREATE TABLE IF NOT EXISTS p_user_profile (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    default_address TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES p_user(id) ON DELETE CASCADE
);

-- p_user_address 테이블
CREATE TABLE IF NOT EXISTS p_user_address (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    label VARCHAR(50) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    address_line1 TEXT NOT NULL,
    address_line2 TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_address_user FOREIGN KEY (user_id) REFERENCES p_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_address_label UNIQUE (user_id, label)
);

-- p_user_refresh_token 테이블
CREATE TABLE IF NOT EXISTS p_user_refresh_token (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    token TEXT,
    client_ip VARCHAR(45),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_user_email ON p_user(email);
CREATE INDEX IF NOT EXISTS idx_user_username ON p_user(username);
CREATE INDEX IF NOT EXISTS idx_user_role ON p_user(role);
CREATE INDEX IF NOT EXISTS idx_user_is_active ON p_user(is_active);
CREATE INDEX IF NOT EXISTS idx_user_deleted_at ON p_user(deleted_at);

CREATE INDEX IF NOT EXISTS idx_user_profile_user_id ON p_user_profile(user_id);

CREATE INDEX IF NOT EXISTS idx_user_address_user_id ON p_user_address(user_id);
CREATE INDEX IF NOT EXISTS idx_user_address_is_default ON p_user_address(is_default);
CREATE INDEX IF NOT EXISTS idx_user_address_deleted_at ON p_user_address(deleted_at);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON p_user_refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON p_user_refresh_token(expires_at);

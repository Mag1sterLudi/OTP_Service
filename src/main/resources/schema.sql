CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(255)       NOT NULL,
    role          VARCHAR(16)        NOT NULL
);

CREATE TABLE IF NOT EXISTS otp_config (
    id          BIGSERIAL PRIMARY KEY,
    code_length INTEGER NOT NULL,
    ttl_seconds INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS otp_codes (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id VARCHAR(128),
    code         VARCHAR(32)  NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    expires_at   TIMESTAMP    NOT NULL
);

CREATE TABLE users (
    user_id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email            varchar(255) NOT NULL,
    name             varchar(255) NOT NULL,
    password_hash    varchar(255) NULL,
    password_set_at  timestamptz  NULL,
    reset_token_hash varchar(64)  NULL,
    reset_expires_at timestamptz  NULL,
    platform_role    varchar(20)  NULL,
    status           varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at       timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_users_platform_role CHECK (
        platform_role IS NULL OR platform_role IN ('PLATFORM_ADMIN', 'SUPPORT')
    )
);

CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_platform_role ON users (platform_role)
    WHERE platform_role IS NOT NULL;

CREATE TABLE refresh_sessions (
    session_id       uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          uuid        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    organization_id  uuid        NULL,
    platform_session boolean     NOT NULL DEFAULT false,
    token_hash       varchar(64) NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       timestamptz NOT NULL,
    replaced_by      uuid        NULL,
    revoked_at       timestamptz NULL,
    remember_me      boolean     NOT NULL DEFAULT false,
    CONSTRAINT fk_refresh_replaced_by
        FOREIGN KEY (replaced_by) REFERENCES refresh_sessions(session_id)
        ON DELETE SET NULL
);

CREATE UNIQUE INDEX uq_refresh_sessions_token_hash ON refresh_sessions (token_hash);
CREATE INDEX idx_refresh_sessions_user
    ON refresh_sessions (user_id, platform_session);
CREATE INDEX idx_refresh_sessions_expires ON refresh_sessions (expires_at);

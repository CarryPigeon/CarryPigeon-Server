CREATE TABLE auth_account (
    id BIGINT NOT NULL,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_auth_account PRIMARY KEY (id),
    CONSTRAINT uk_auth_account_username UNIQUE (username)
);

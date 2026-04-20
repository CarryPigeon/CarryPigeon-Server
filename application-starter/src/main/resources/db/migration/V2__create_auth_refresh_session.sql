CREATE TABLE auth_refresh_session (
    id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_auth_refresh_session PRIMARY KEY (id),
    CONSTRAINT uk_auth_refresh_session_token_hash UNIQUE (refresh_token_hash),
    CONSTRAINT fk_auth_refresh_session_account FOREIGN KEY (account_id) REFERENCES auth_account (id)
);

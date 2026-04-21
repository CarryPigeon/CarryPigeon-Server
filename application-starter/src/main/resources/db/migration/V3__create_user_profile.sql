CREATE TABLE user_profile (
    account_id BIGINT NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(512) NOT NULL,
    bio VARCHAR(1024) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_user_profile PRIMARY KEY (account_id),
    CONSTRAINT fk_user_profile_account FOREIGN KEY (account_id) REFERENCES auth_account (id)
);

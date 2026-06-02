-- CarryPigeon backend database
-- All-in-one schema bootstrap for empty MySQL schemas only
-- Source: docs/sql/01-auth.sql ... docs/sql/05-notification.sql

-- Auth domain
CREATE TABLE auth_account (
    id BIGINT NOT NULL,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_auth_account PRIMARY KEY (id),
    CONSTRAINT uk_auth_account_username UNIQUE (username)
);

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

-- User domain
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

-- Channel domain
CREATE TABLE chat_channel (
    id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    is_default BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    brief VARCHAR(256) NOT NULL DEFAULT '',
    avatar VARCHAR(256) NOT NULL DEFAULT '',
    system_channel_guard TINYINT
        GENERATED ALWAYS AS (CASE WHEN type = 'system' THEN 1 ELSE NULL END) STORED,
    PRIMARY KEY (id)
);

CREATE INDEX idx_chat_channel_default_type
    ON chat_channel (is_default, type);

CREATE UNIQUE INDEX uk_chat_channel_system_channel_guard
    ON chat_channel (system_channel_guard);

CREATE TABLE chat_channel_member (
    channel_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL,
    muted_until TIMESTAMP NULL,
    PRIMARY KEY (channel_id, account_id),
    CONSTRAINT fk_chat_channel_member_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_channel_member_account FOREIGN KEY (account_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_channel_member_account
    ON chat_channel_member (account_id);

CREATE INDEX idx_chat_channel_member_channel_role
    ON chat_channel_member (channel_id, role);

CREATE TABLE chat_channel_invite (
    channel_id BIGINT NOT NULL,
    invitee_account_id BIGINT NOT NULL,
    inviter_account_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP NULL,
    application_id BIGINT NULL,
    PRIMARY KEY (channel_id, invitee_account_id),
    CONSTRAINT fk_chat_channel_invite_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_channel_invite_invitee FOREIGN KEY (invitee_account_id) REFERENCES auth_account (id),
    CONSTRAINT fk_chat_channel_invite_inviter FOREIGN KEY (inviter_account_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_channel_invite_status
    ON chat_channel_invite (channel_id, status);

CREATE INDEX idx_chat_channel_invite_inviter
    ON chat_channel_invite (inviter_account_id);

CREATE UNIQUE INDEX idx_chat_channel_invite_application_id
    ON chat_channel_invite (application_id);

CREATE TABLE chat_channel_ban (
    channel_id BIGINT NOT NULL,
    banned_account_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL DEFAULT '',
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    PRIMARY KEY (channel_id, banned_account_id),
    CONSTRAINT fk_chat_channel_ban_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_channel_ban_account FOREIGN KEY (banned_account_id) REFERENCES auth_account (id),
    CONSTRAINT fk_chat_channel_ban_operator FOREIGN KEY (operator_account_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_channel_ban_operator
    ON chat_channel_ban (operator_account_id);

CREATE INDEX idx_chat_channel_ban_revoked_at
    ON chat_channel_ban (channel_id, revoked_at);

CREATE TABLE chat_channel_audit_log (
    audit_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    actor_account_id BIGINT NULL,
    action_type VARCHAR(64) NOT NULL,
    target_account_id BIGINT NULL,
    metadata TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (audit_id),
    CONSTRAINT fk_chat_channel_audit_log_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_channel_audit_log_actor FOREIGN KEY (actor_account_id) REFERENCES auth_account (id),
    CONSTRAINT fk_chat_channel_audit_log_target FOREIGN KEY (target_account_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_channel_audit_log_channel_created_at
    ON chat_channel_audit_log (channel_id, created_at);

CREATE TABLE chat_channel_read_state (
    channel_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL,
    last_read_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (channel_id, account_id),
    CONSTRAINT fk_chat_channel_read_state_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_channel_read_state_account FOREIGN KEY (account_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_channel_read_state_account_id
    ON chat_channel_read_state (account_id);

CREATE TABLE chat_channel_pin (
    pin_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    pinned_by_account_id BIGINT NOT NULL,
    note VARCHAR(200) NOT NULL DEFAULT '',
    pinned_at TIMESTAMP NOT NULL,
    PRIMARY KEY (channel_id, message_id)
);

CREATE UNIQUE INDEX uk_chat_channel_pin_pin_id
    ON chat_channel_pin (pin_id);

CREATE INDEX idx_chat_channel_pin_channel_id_pinned_at
    ON chat_channel_pin (channel_id, pinned_at DESC);

CREATE INDEX idx_chat_channel_pin_channel_message_id
    ON chat_channel_pin (channel_id, message_id DESC);

INSERT INTO chat_channel (
    id,
    conversation_id,
    name,
    type,
    is_default,
    created_at,
    updated_at,
    brief,
    avatar
)
VALUES (1, 1, 'public', 'public', TRUE, '2026-04-22 00:00:00', '2026-04-22 00:00:00', '', '');

INSERT INTO chat_channel (
    id,
    conversation_id,
    name,
    type,
    is_default,
    created_at,
    updated_at,
    brief,
    avatar
)
VALUES (2, 2, 'system', 'system', FALSE, '2026-04-22 00:00:00', '2026-04-22 00:00:00', '', '');

-- Message domain
CREATE TABLE chat_message (
    message_id BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    conversation_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    body TEXT NOT NULL,
    preview_text VARCHAR(255) NOT NULL DEFAULT '',
    searchable_text TEXT NOT NULL,
    payload JSON NULL,
    metadata JSON NULL,
    mentions JSON NULL,
    forwarded_from JSON NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    edited_at TIMESTAMP NULL,
    edit_version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (message_id),
    CONSTRAINT fk_chat_message_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_message_channel_message
    ON chat_message (channel_id, message_id DESC);

CREATE INDEX idx_chat_message_channel_sender_message_id
    ON chat_message (channel_id, sender_id, message_id DESC);

CREATE INDEX idx_chat_message_channel_type_message_id
    ON chat_message (channel_id, message_type, message_id DESC);

CREATE TABLE chat_mention (
    mention_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    from_account_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_account_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_read BOOLEAN NOT NULL,
    PRIMARY KEY (mention_id)
);

CREATE INDEX idx_chat_mention_target_account_id_created_at
    ON chat_mention (target_account_id, created_at DESC);

CREATE INDEX idx_chat_mention_channel_id_created_at
    ON chat_mention (channel_id, created_at DESC);

CREATE INDEX idx_chat_mention_target_account_read_mention_id
    ON chat_mention (target_account_id, is_read, mention_id DESC);

CREATE INDEX idx_chat_mention_target_account_channel_mention_id
    ON chat_mention (target_account_id, channel_id, mention_id DESC);

-- Notification domain
CREATE TABLE chat_notification_server_preference (
    account_id BIGINT NOT NULL,
    mode VARCHAR(32) NOT NULL,
    muted_until BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (account_id),
    CONSTRAINT fk_chat_notification_server_preference_account FOREIGN KEY (account_id) REFERENCES auth_account (id)
);

CREATE TABLE chat_notification_channel_preference (
    account_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    mode VARCHAR(32) NOT NULL,
    muted_until BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (account_id, channel_id),
    CONSTRAINT fk_chat_notification_channel_preference_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_notification_channel_preference_account FOREIGN KEY (account_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_notification_channel_preference_account_id
    ON chat_notification_channel_preference (account_id, channel_id);

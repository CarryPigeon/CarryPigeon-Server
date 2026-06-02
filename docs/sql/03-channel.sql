-- Channel domain
-- Source: V4, V5, V5_1, V8, V10, V11, V12, V16, V17, V18

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

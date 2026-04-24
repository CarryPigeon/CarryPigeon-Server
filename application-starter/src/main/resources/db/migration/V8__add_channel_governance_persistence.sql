ALTER TABLE chat_channel_member
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'MEMBER' AFTER account_id,
    ADD COLUMN muted_until TIMESTAMP NULL AFTER joined_at;

CREATE INDEX idx_chat_channel_member_channel_role ON chat_channel_member (channel_id, role);

CREATE TABLE chat_channel_invite (
    channel_id BIGINT NOT NULL,
    invitee_account_id BIGINT NOT NULL,
    inviter_account_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP NULL,
    PRIMARY KEY (channel_id, invitee_account_id),
    CONSTRAINT fk_chat_channel_invite_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_channel_invite_invitee FOREIGN KEY (invitee_account_id) REFERENCES auth_account (id),
    CONSTRAINT fk_chat_channel_invite_inviter FOREIGN KEY (inviter_account_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_channel_invite_status ON chat_channel_invite (channel_id, status);
CREATE INDEX idx_chat_channel_invite_inviter ON chat_channel_invite (inviter_account_id);

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

CREATE INDEX idx_chat_channel_ban_operator ON chat_channel_ban (operator_account_id);
CREATE INDEX idx_chat_channel_ban_revoked_at ON chat_channel_ban (channel_id, revoked_at);

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

CREATE INDEX idx_chat_channel_audit_log_channel_created_at ON chat_channel_audit_log (channel_id, created_at);

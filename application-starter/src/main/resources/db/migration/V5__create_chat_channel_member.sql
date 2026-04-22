CREATE TABLE chat_channel_member (
    channel_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    PRIMARY KEY (channel_id, account_id),
    CONSTRAINT fk_chat_channel_member_channel FOREIGN KEY (channel_id) REFERENCES chat_channel (id),
    CONSTRAINT fk_chat_channel_member_account FOREIGN KEY (account_id) REFERENCES auth_account (id)
);

CREATE INDEX idx_chat_channel_member_account ON chat_channel_member (account_id);

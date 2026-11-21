-- 已读锚点已经保存消息 ID，保留原锚点即可切换排序和统计边界。
ALTER TABLE chat_message
    DROP INDEX uk_chat_message_channel_sequence,
    DROP INDEX idx_message_unread,
    DROP COLUMN sequence,
    ADD INDEX idx_message_channel_id (channel_id, message_id),
    ADD INDEX idx_message_unread (channel_id, status, message_id, sender_id);

ALTER TABLE chat_channel_read_state DROP COLUMN last_read_sequence;
DROP TABLE chat_message_sequence;

-- 收件箱与频道列表都按提及 ID 倒序翻页。
ALTER TABLE chat_mention
    ADD INDEX idx_mention_target_id (target_account_id, mention_id),
    ADD INDEX idx_mention_target_channel_id (target_account_id, channel_id, mention_id);

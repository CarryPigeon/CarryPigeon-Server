-- 空频道的成员也拥有已读状态，首次收到消息前没有可引用的消息 ID。
ALTER TABLE chat_channel_read_state MODIFY last_read_message_id BIGINT NULL;

-- 旧收件箱没有绑定成员关系。清除已删频道、已离开的收件人以及本次入群前留下的提及。
DELETE t FROM chat_mention t
LEFT JOIN chat_channel c ON c.channel_id = t.channel_id
LEFT JOIN chat_channel_member cm
    ON cm.channel_id = t.channel_id AND cm.account_id = t.target_account_id
WHERE c.channel_id IS NULL OR cm.member_id IS NULL OR t.created_at < cm.joined_at;

DELETE r FROM chat_channel_read_state r
LEFT JOIN chat_channel c ON c.channel_id = r.channel_id
LEFT JOIN chat_channel_member cm
    ON cm.channel_id = r.channel_id AND cm.account_id = r.account_id
WHERE c.channel_id IS NULL OR cm.member_id IS NULL OR r.updated_at < cm.joined_at;

-- 旧系统未记录入群瞬间的消息 ID，按本次入群时间重建下界。
-- 只纳入时间严格早于入群的消息；较新的已读进度保持原值。
INSERT INTO chat_channel_read_state (channel_id, account_id, last_read_message_id, created_at, updated_at)
SELECT cm.channel_id, cm.account_id, (
    SELECT m.message_id FROM chat_message m
    WHERE m.channel_id = cm.channel_id AND m.send_time < cm.joined_at
    ORDER BY m.message_id DESC LIMIT 1
), cm.joined_at, cm.joined_at
FROM chat_channel_member cm
JOIN chat_channel c ON c.channel_id = cm.channel_id
ON DUPLICATE KEY UPDATE last_read_message_id = NULLIF(GREATEST(
    COALESCE(chat_channel_read_state.last_read_message_id, 0),
    COALESCE(VALUES(last_read_message_id), 0)
), 0);

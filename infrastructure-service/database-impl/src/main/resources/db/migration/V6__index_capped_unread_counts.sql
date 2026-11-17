-- 未读查询只需要频道、状态、序号和发送者；覆盖索引避免读取 JSON 正文。
-- 同频道的已撤回数据先由等值状态过滤，再从已读序号之后定位，最多计数 10000 条。
ALTER TABLE chat_message ADD INDEX idx_message_unread (channel_id, status, sequence, sender_id);

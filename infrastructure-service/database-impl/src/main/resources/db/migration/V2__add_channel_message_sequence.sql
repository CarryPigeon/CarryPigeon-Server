-- 升级时必须停止旧版本写入，再迁移并统一切换新版本，不能与按消息 ID 分页的实例混跑。
-- 历史消息沿用旧版稳定的 message_id 顺序；新消息从回填后的频道最大序号继续。
ALTER TABLE chat_message ADD COLUMN sequence BIGINT NULL;

CREATE TEMPORARY TABLE message_sequence_backfill (
    message_id BIGINT PRIMARY KEY,
    sequence BIGINT NOT NULL
) ENGINE=InnoDB;

INSERT INTO message_sequence_backfill (message_id, sequence)
SELECT message_id, ROW_NUMBER() OVER (PARTITION BY channel_id ORDER BY message_id)
FROM chat_message;

UPDATE chat_message m JOIN message_sequence_backfill b ON b.message_id = m.message_id
SET m.sequence = b.sequence;
DROP TEMPORARY TABLE message_sequence_backfill;

ALTER TABLE chat_message
    MODIFY COLUMN sequence BIGINT NOT NULL,
    ADD UNIQUE KEY uk_chat_message_channel_sequence (channel_id, sequence),
    DROP INDEX idx_chat_message_channel_message;

CREATE TABLE chat_message_sequence (
    channel_id BIGINT NOT NULL,
    last_sequence BIGINT NOT NULL,
    PRIMARY KEY (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息事务持锁分配频道序号';

INSERT INTO chat_message_sequence (channel_id, last_sequence)
SELECT channel_id, MAX(sequence) FROM chat_message GROUP BY channel_id;

ALTER TABLE chat_channel_read_state ADD COLUMN last_read_sequence BIGINT NOT NULL DEFAULT 0;
-- 与旧版未读边界一致：使用不大于旧锚点 ID 的最大序号，兼容锚点消息已经物理删除的历史记录。
UPDATE chat_channel_read_state r
SET r.last_read_sequence = COALESCE((
    SELECT MAX(m.sequence) FROM chat_message m
    WHERE m.channel_id = r.channel_id AND m.message_id <= r.last_read_message_id
), 0);

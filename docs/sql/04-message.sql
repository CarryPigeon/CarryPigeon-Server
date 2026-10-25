-- ============================================================================
-- 消息域表结构
-- 状态：草稿（待评审）
-- 责任范围：chat_message、chat_mention
-- 可重复执行：是（CREATE TABLE IF NOT EXISTS）
-- 来源：database-impl 的 MessageEntity、MentionEntity
-- ============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
    message_id     BIGINT       NOT NULL COMMENT '消息 ID（雪花，客户端可见）',
    sender_id      BIGINT       NOT NULL COMMENT '发送者账号 ID',
    channel_id     BIGINT       NOT NULL COMMENT '频道 ID',
    domain         VARCHAR(128) NOT NULL COMMENT '消息 domain，格式为主分类:子分类:版本号',
    data           MEDIUMTEXT   NOT NULL COMMENT 'domain 载荷（JSON）',
    send_time      DATETIME(3)  NOT NULL COMMENT '发送时间',
    mentions       TEXT         NULL COMMENT '提及账号列表（序列化）',
    preview        VARCHAR(512) NULL COMMENT '列表预览文本',
    status         VARCHAR(16)  NOT NULL COMMENT '状态：sent/recalled',
    PRIMARY KEY (message_id),
    KEY idx_chat_message_channel_time (channel_id, send_time),
    KEY idx_chat_message_sender (sender_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '频道消息';

CREATE TABLE IF NOT EXISTS chat_mention (
    mention_id        BIGINT      NOT NULL COMMENT '提及 ID（雪花）',
    channel_id        BIGINT      NOT NULL COMMENT '频道 ID',
    message_id        BIGINT      NOT NULL COMMENT '消息 ID',
    from_account_id   BIGINT      NOT NULL COMMENT '发起人账号 ID',
    target_type       VARCHAR(16) NOT NULL COMMENT '目标类型',
    target_account_id BIGINT      NOT NULL COMMENT '被提及账号 ID',
    created_at        DATETIME(3) NOT NULL COMMENT '创建时间',
    `read`            TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否已读',
    PRIMARY KEY (mention_id),
    KEY idx_chat_mention_target (target_account_id, `read`, created_at),
    KEY idx_chat_mention_message (message_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '消息提及';

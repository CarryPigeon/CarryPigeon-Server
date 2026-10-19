-- ============================================================================
-- 频道域表结构
-- 状态：草稿（待评审）
-- 责任范围：chat_channel、chat_channel_member、chat_channel_application、
--           chat_channel_ban、chat_channel_read_state，以及唯一 system 频道种子
-- 可重复执行：是（CREATE TABLE IF NOT EXISTS + 幂等种子插入）
-- 来源：database-impl 的 ChannelEntity、ChannelMemberEntity、ChannelApplicationEntity、
--       ChannelBanEntity、ChannelReadStateEntity
-- ============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS chat_channel (
    channel_id BIGINT       NOT NULL COMMENT '频道 ID（雪花；system 频道固定为 1）',
    name       VARCHAR(128) NOT NULL COMMENT '频道名称',
    brief      VARCHAR(512) NULL COMMENT '频道简介',
    avatar     VARCHAR(512) NULL COMMENT '频道头像相对路径',
    is_system  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否系统频道',
    created_at DATETIME(3)  NOT NULL COMMENT '创建时间',
    updated_at DATETIME(3)  NOT NULL COMMENT '资料更新时间',
    PRIMARY KEY (channel_id),
    KEY idx_chat_channel_system (is_system)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '频道';

CREATE TABLE IF NOT EXISTS chat_channel_member (
    member_id  BIGINT      NOT NULL COMMENT '成员关系 ID（雪花）',
    channel_id BIGINT      NOT NULL COMMENT '频道 ID',
    account_id BIGINT      NOT NULL COMMENT '账号 ID',
    role       VARCHAR(16) NOT NULL COMMENT '角色：OWNER/ADMIN/MEMBER',
    joined_at  DATETIME(3) NOT NULL COMMENT '加入时间',
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_chat_channel_member (channel_id, account_id),
    KEY idx_chat_channel_member_account (account_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '频道成员关系';

CREATE TABLE IF NOT EXISTS chat_channel_application (
    application_id BIGINT       NOT NULL COMMENT '申请 ID（雪花）',
    channel_id     BIGINT       NOT NULL COMMENT '频道 ID',
    applicant_id   BIGINT       NOT NULL COMMENT '申请人账号 ID',
    reason         VARCHAR(512) NULL COMMENT '申请理由',
    status         VARCHAR(16)  NOT NULL COMMENT '状态：PENDING/ACCEPTED/REJECTED',
    created_at     DATETIME(3)  NOT NULL COMMENT '申请时间',
    responded_at   DATETIME(3)  NULL COMMENT '审批时间',
    PRIMARY KEY (application_id),
    KEY idx_chat_channel_application_channel (channel_id, status),
    KEY idx_chat_channel_application_applicant (applicant_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '入会申请';

CREATE TABLE IF NOT EXISTS chat_channel_ban (
    ban_id             BIGINT       NOT NULL COMMENT '禁言记录 ID（雪花）',
    channel_id         BIGINT       NOT NULL COMMENT '频道 ID',
    banned_account_id  BIGINT       NOT NULL COMMENT '被禁言账号',
    operator_account_id BIGINT      NOT NULL COMMENT '操作人账号',
    reason             VARCHAR(512) NULL COMMENT '禁言原因',
    expires_at         DATETIME(3)  NULL COMMENT '到期时间；空表示永久',
    created_at         DATETIME(3)  NOT NULL COMMENT '创建时间',
    revoked_at         DATETIME(3)  NULL COMMENT '解除时间；空表示仍生效',
    PRIMARY KEY (ban_id),
    KEY idx_chat_channel_ban_member (channel_id, banned_account_id),
    KEY idx_chat_channel_ban_expires (expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '频道禁言记录';

CREATE TABLE IF NOT EXISTS chat_channel_read_state (
    channel_id           BIGINT      NOT NULL COMMENT '频道 ID',
    account_id           BIGINT      NOT NULL COMMENT '账号 ID',
    last_read_message_id BIGINT      NULL COMMENT '最后已读消息 ID',
    last_read_time       DATETIME(3) NULL COMMENT '最后已读时间',
    created_at           DATETIME(3) NOT NULL COMMENT '创建时间',
    updated_at           DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (channel_id, account_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '频道已读位置';

-- 唯一 system 频道：应用按 is_system 查询，不依赖固定 ID；这里用 1 保持可读性。
INSERT INTO chat_channel (channel_id, name, brief, avatar, is_system, created_at, updated_at)
SELECT 1, 'CarryPigeon', '系统频道', '', 1, NOW(3), NOW(3)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM chat_channel WHERE is_system = 1);

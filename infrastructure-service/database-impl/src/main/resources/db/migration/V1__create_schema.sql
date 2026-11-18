-- 初始表结构以 database-impl 的 Entity 和现有仓储契约为准。
-- ID 由应用生成，使用有符号 BIGINT 与 Java long 对齐；时间保留微秒精度。
-- 关联数据的写入与清理由业务事务编排，本基线不隐式添加级联删除。

CREATE TABLE auth_account (
    id BIGINT NOT NULL,
    username VARCHAR(320) NOT NULL,
    password_hash VARCHAR(512) NOT NULL,
    email VARCHAR(320) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_account_username (username),
    UNIQUE KEY uk_auth_account_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认证账户';

CREATE TABLE auth_refresh_session (
    id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    refresh_token_hash VARCHAR(512) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_auth_refresh_session_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌会话';

CREATE TABLE user_profile (
    account_id BIGINT NOT NULL,
    avatar_url VARCHAR(512) NOT NULL,
    bio TEXT NOT NULL,
    sex BIGINT NOT NULL,
    birthday BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资料';

CREATE TABLE chat_channel (
    channel_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    brief TEXT NOT NULL,
    is_system BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道';

CREATE TABLE chat_channel_member (
    member_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_chat_channel_member_relation (channel_id, account_id),
    KEY idx_chat_channel_member_account_channel (account_id, channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道成员关系';

-- 当前申请模型会用新申请替换同一关系的历史记录，因此关系本身必须唯一。
CREATE TABLE chat_channel_application (
    application_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    reason TEXT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    responded_at DATETIME(6) NULL,
    PRIMARY KEY (application_id),
    UNIQUE KEY uk_chat_channel_application_relation (channel_id, applicant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入群申请';

-- 同一成员仅保留一条可更新的禁言记录；解除时间为空表示尚未解除。
CREATE TABLE chat_channel_ban (
    ban_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    banned_account_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    reason TEXT NULL,
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (ban_id),
    UNIQUE KEY uk_chat_channel_ban_relation (channel_id, banned_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道禁言';

-- JSON 存储完整业务载荷和数值型提及 ID，避免以消息预览代替正文。
CREATE TABLE chat_message (
    message_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    sequence BIGINT NOT NULL,
    domain VARCHAR(128) NOT NULL,
    data JSON NOT NULL,
    send_time DATETIME(6) NOT NULL,
    mentions JSON NOT NULL,
    preview TEXT NOT NULL,
    search_text TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (message_id),
    UNIQUE KEY uk_chat_message_channel_sequence (channel_id, sequence),
    KEY idx_message_unread (channel_id, status, sequence, sender_id),
    FULLTEXT KEY ft_chat_message_search_text (search_text) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道消息';

CREATE TABLE chat_mention (
    mention_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    from_account_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_account_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    is_read BOOLEAN NOT NULL,
    PRIMARY KEY (mention_id),
    KEY idx_chat_mention_target_read_id (target_account_id, is_read, mention_id),
    KEY idx_chat_mention_message (message_id),
    KEY idx_chat_mention_channel (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息提及';

-- 已读状态以频道和账户共同标识，不能仅将 Entity 上的 channel_id 注解视为完整主键。
CREATE TABLE chat_channel_read_state (
    channel_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL,
    last_read_sequence BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (channel_id, account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='频道已读状态';

CREATE TABLE chat_message_sequence (
    channel_id BIGINT NOT NULL,
    last_sequence BIGINT NOT NULL,
    PRIMARY KEY (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息事务持锁分配频道序号';

CREATE TABLE chat_message_send_request (
    account_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    payload_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    message_id BIGINT NOT NULL,
    PRIMARY KEY (account_id, channel_id, request_id),
    KEY idx_message_send_request_channel (channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发送请求幂等身份';

CREATE TABLE chat_channel_deletion (
    channel_id BIGINT NOT NULL,
    operator_account_id BIGINT NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (channel_id),
    KEY idx_channel_deletion_pending (completed_at, channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可恢复频道清理任务';

CREATE TABLE chat_file_upload (
    file_id BIGINT NOT NULL PRIMARY KEY,
    owner_account_id BIGINT NOT NULL,
    share_key VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    object_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    content_type VARCHAR(255) NULL,
    expires_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    reclaim_started_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_file_upload_share_key (share_key),
    KEY idx_file_upload_owner_status (owner_account_id, status),
    KEY idx_file_upload_status_expiry (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_file_upload_attempt (
    object_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    file_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_cleaned_at DATETIME(6) NULL,
    next_cleanup_at DATETIME(6) NULL,
    KEY idx_file_upload_attempt_file (file_id),
    KEY idx_file_attempt_next_cleanup (next_cleanup_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_file_quota (
    account_id BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

CREATE TABLE chat_file_avatar_reference (
    account_id BIGINT NOT NULL PRIMARY KEY,
    file_id BIGINT NOT NULL,
    KEY idx_file_avatar_reference_file (file_id)
) ENGINE=InnoDB;



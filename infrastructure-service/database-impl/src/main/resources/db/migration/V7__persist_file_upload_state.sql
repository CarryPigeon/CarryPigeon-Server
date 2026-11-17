-- 完成结果由数据库发布；对象写入成功不等同于文件已完成。
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
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_file_upload_share_key (share_key),
    KEY idx_file_upload_owner_status (owner_account_id, status),
    KEY idx_file_upload_status_expiry (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每次写入先登记独立对象键；进程中断或数据库发布失败后仍能追踪对象，不依赖内存补偿。
CREATE TABLE chat_file_upload_attempt (
    object_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    file_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_file_upload_attempt_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_file_avatar_reference (
    account_id BIGINT NOT NULL PRIMARY KEY,
    file_id BIGINT NOT NULL,
    KEY idx_file_avatar_reference_file (file_id)
) ENGINE=InnoDB;

ALTER TABLE chat_file_upload ADD COLUMN reclaim_started_at DATETIME(6) NULL;
ALTER TABLE chat_file_upload_attempt
    ADD COLUMN last_cleaned_at DATETIME(6) NULL,
    ADD COLUMN next_cleanup_at DATETIME(6) NULL,
    ADD KEY idx_file_attempt_next_cleanup (next_cleanup_at);

-- 当前有效写入和完成对象不进入清理队列；只补登记已失败或被新尝试取代的对象。
UPDATE chat_file_upload_attempt a JOIN chat_file_upload f ON f.file_id = a.file_id
SET a.next_cleanup_at = a.created_at
WHERE f.status = 'FAILED' OR a.object_key != f.object_key;

-- 仅用作账号级互斥行；额度来源是文件状态表，避免维护可能漂移的冗余计数。
CREATE TABLE chat_file_quota (
    account_id BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

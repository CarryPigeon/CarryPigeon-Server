-- CarryPigeon Backend - One-time migration (2026-02-03)
--
-- Scope:
--   - message: add domain_version + (cid,id) index for cursor pagination by mid
--   - channel_read_state: add last_read_mid and backfill from last_read_time
--   - file_info: add share_key/owner_uid/filename/uploaded/uploaded_time, make sha256 nullable
--   - channel_ban: add until_time/reason for richer mute semantics
--
-- Notes:
--   - This script is designed to be executed once.
--   - Please backup your database before applying.
--   - MySQL versions prior to 8.0 do NOT support "IF NOT EXISTS" for ALTER; do not re-run.
--
-- Usage:
--   mysql -uroot -p carrypigeon < doc/sql/migrations/2026-02-03_api_p1.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1) message
ALTER TABLE `message`
    ADD COLUMN `domain_version` VARCHAR(16) NOT NULL DEFAULT '1.0.0' COMMENT 'Domain version (SemVer)' AFTER `domain`;

ALTER TABLE `message`
    ADD KEY `idx_message_cid_id` (`cid`, `id`);

-- 2) channel_read_state
ALTER TABLE `channel_read_state`
    ADD COLUMN `last_read_mid` BIGINT NOT NULL DEFAULT 0 COMMENT 'Last read message id (0 means never)' AFTER `cid`;

-- Best-effort backfill: convert last_read_time -> last_read_mid.
-- If your dataset is large, run this during off-peak hours.
UPDATE `channel_read_state` s
SET s.`last_read_mid` = IFNULL((
    SELECT MAX(m.`id`)
    FROM `message` m
    WHERE m.`cid` = s.`cid`
      AND m.`send_time` <= FROM_UNIXTIME(s.`last_read_time` / 1000)
), 0);

-- 3) file_info
ALTER TABLE `file_info`
    ADD COLUMN `share_key` VARCHAR(64) NULL COMMENT 'Stable share key used by /api/files/download/{share_key}' AFTER `id`,
    ADD COLUMN `owner_uid` BIGINT NOT NULL DEFAULT 0 COMMENT 'Owner user id (uploader)' AFTER `share_key`,
    ADD COLUMN `filename` VARCHAR(255) NULL COMMENT 'Original filename (optional)' AFTER `owner_uid`;

ALTER TABLE `file_info`
    MODIFY COLUMN `sha256` CHAR(64) NULL COMMENT 'SHA-256 hex string (optional)';

ALTER TABLE `file_info`
    ADD COLUMN `uploaded` TINYINT NOT NULL DEFAULT 1 COMMENT '0 pending, 1 uploaded' AFTER `content_type`,
    ADD COLUMN `uploaded_time` DATETIME NULL COMMENT 'Uploaded time (null if not uploaded)' AFTER `uploaded`;

-- Backfill share_key for existing rows (P1: deterministic key shr_{id}).
UPDATE `file_info`
SET `share_key` = CONCAT('shr_', `id`)
WHERE `share_key` IS NULL OR `share_key` = '';

-- Mark existing files as uploaded so they can be downloaded.
UPDATE `file_info`
SET `uploaded` = 1,
    `uploaded_time` = IFNULL(`uploaded_time`, `create_time`)
WHERE `uploaded` IS NULL OR `uploaded` = 0;

ALTER TABLE `file_info`
    MODIFY COLUMN `share_key` VARCHAR(64) NOT NULL,
    ADD UNIQUE KEY `uk_file_info_share_key` (`share_key`);

-- 4) channel_ban
ALTER TABLE `channel_ban`
    ADD COLUMN `until_time` BIGINT NOT NULL DEFAULT 0 COMMENT 'Mute until (epoch millis, 0 means not set)' AFTER `duration`,
    ADD COLUMN `reason` VARCHAR(255) NULL COMMENT 'Mute reason (optional)' AFTER `until_time`;

SET FOREIGN_KEY_CHECKS = 1;


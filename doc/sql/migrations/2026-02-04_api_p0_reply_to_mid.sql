-- CarryPigeon Backend - One-time migration (2026-02-04) - reply_to_mid (P0)
--
-- Scope:
--   - message: add reply_to_mid for reply relationships
--
-- Notes:
--   - This script is designed to be executed once.
--   - Please backup your database before applying.
--   - MySQL versions prior to 8.0 do NOT support "IF NOT EXISTS" for ALTER; do not re-run.
--
-- Usage:
--   mysql -uroot -p carrypigeon < doc/sql/migrations/2026-02-04_api_p0_reply_to_mid.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `message`
    ADD COLUMN `reply_to_mid` BIGINT NOT NULL DEFAULT 0 COMMENT 'Reply target message id (0 means none)' AFTER `domain_version`;

SET FOREIGN_KEY_CHECKS = 1;


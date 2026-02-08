-- CarryPigeon Backend - One-time migration (2026-02-03) - Files ACL (P2)
--
-- Scope:
--   - file_info: add access_scope/scope_cid/scope_mid for download permission
--   - file_info: relax sha256 uniqueness to allow per-upload rows
--
-- Notes:
--   - This script is designed to be executed once.
--   - Please backup your database before applying.
--   - MySQL versions prior to 8.0 do NOT support "IF EXISTS" for DROP INDEX; do not re-run.
--
-- Usage:
--   mysql -uroot -p carrypigeon < doc/sql/migrations/2026-02-03_api_p2_files_acl.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Add access scope columns.
ALTER TABLE `file_info`
    ADD COLUMN `access_scope` VARCHAR(16) NOT NULL DEFAULT 'OWNER' COMMENT 'Access scope: OWNER|AUTH|CHANNEL|PUBLIC' AFTER `owner_uid`,
    ADD COLUMN `scope_cid` BIGINT NOT NULL DEFAULT 0 COMMENT 'Scope channel id (required when access_scope=CHANNEL)' AFTER `access_scope`,
    ADD COLUMN `scope_mid` BIGINT NOT NULL DEFAULT 0 COMMENT 'Scope message id (reserved for P2+)' AFTER `scope_cid`;

-- Existing P1 behavior was "logged-in users can download non-public files", so we migrate existing records to AUTH.
UPDATE `file_info`
SET `access_scope` = 'AUTH'
WHERE `access_scope` = 'OWNER';

-- Relax sha256 uniqueness: drop unique key and add a normal index.
DROP INDEX `uk_file_info_sha256` ON `file_info`;
CREATE INDEX `idx_file_info_sha256` ON `file_info` (`sha256`);

SET FOREIGN_KEY_CHECKS = 1;


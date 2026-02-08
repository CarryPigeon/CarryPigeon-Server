-- CarryPigeon Backend - MySQL 表结构（DDL）
-- 依据：
--   - MyBatis-Plus PO：dao/src/main/java/team/carrypigeon/backend/dao/database/mapper/**/*
--   - 文档：doc/domain/database-schema.md
--
-- 说明：
--   - 本项目使用 Snowflake 风格的 long id（脚本不使用 AUTO_INCREMENT）。
--   - 为兼容性，消息 data 使用 TEXT（存储 JSON 字符串），未使用 MySQL JSON 类型。
--
-- 用法示例：
--   mysql -uroot -pcarrypigeon -e "CREATE DATABASE IF NOT EXISTS carrypigeon DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
--   mysql -uroot -pcarrypigeon carrypigeon < doc/sql/carrypigeon_schema_mysql.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 如需重建表结构，可取消注释（按依赖顺序删除）
-- DROP TABLE IF EXISTS `message`;
-- DROP TABLE IF EXISTS `channel_ban`;
-- DROP TABLE IF EXISTS `channel_application`;
-- DROP TABLE IF EXISTS `channel_read_state`;
-- DROP TABLE IF EXISTS `channel_member`;
-- DROP TABLE IF EXISTS `user_token`;
-- DROP TABLE IF EXISTS `channel`;
-- DROP TABLE IF EXISTS `user`;
-- DROP TABLE IF EXISTS `file_info`;

CREATE TABLE IF NOT EXISTS `file_info` (
    `id` BIGINT NOT NULL COMMENT 'File id',
    `share_key` VARCHAR(64) NOT NULL COMMENT 'Stable share key used by /api/files/download/{share_key}',
    `owner_uid` BIGINT NOT NULL COMMENT 'Owner user id (uploader)',
    `access_scope` VARCHAR(16) NOT NULL DEFAULT 'OWNER' COMMENT 'Access scope: OWNER|AUTH|CHANNEL|PUBLIC',
    `scope_cid` BIGINT NOT NULL DEFAULT 0 COMMENT 'Scope channel id (required when access_scope=CHANNEL)',
    `scope_mid` BIGINT NOT NULL DEFAULT 0 COMMENT 'Scope message id (reserved for P2+)',
    `filename` VARCHAR(255) NULL COMMENT 'Original filename (optional)',
    `sha256` CHAR(64) NULL COMMENT 'SHA-256 hex string (optional)',
    `size` BIGINT NOT NULL COMMENT 'File size (bytes)',
    `object_name` VARCHAR(255) NOT NULL COMMENT 'Object storage key',
    `content_type` VARCHAR(128) NULL COMMENT 'MIME type',
    `uploaded` TINYINT NOT NULL DEFAULT 0 COMMENT '0 pending, 1 uploaded',
    `uploaded_time` DATETIME NULL COMMENT 'Uploaded time (null if not uploaded)',
    `create_time` DATETIME NOT NULL COMMENT 'Create time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_info_share_key` (`share_key`),
    KEY `idx_file_info_sha256` (`sha256`),
    KEY `idx_file_info_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL COMMENT 'User id',
    `username` VARCHAR(64) NOT NULL COMMENT 'Username',
    `avatar` BIGINT NOT NULL DEFAULT 0 COMMENT 'Avatar file id (0 means none)',
    `email` VARCHAR(255) NOT NULL COMMENT 'Email',
    `sex` INT NOT NULL DEFAULT 0 COMMENT '0 unknown, 1 male, 2 female',
    `brief` VARCHAR(255) NULL COMMENT 'Brief introduction',
    `birthday` DATETIME NULL COMMENT 'Birthday',
    `register_time` DATETIME NOT NULL COMMENT 'Register time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`email`),
    KEY `idx_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_token` (
    `id` BIGINT NOT NULL COMMENT 'Token record id',
    `uid` BIGINT NOT NULL COMMENT 'User id',
    `token` VARCHAR(255) NOT NULL COMMENT 'Token string',
    `expired_time` DATETIME NOT NULL COMMENT 'Expired time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_token_token` (`token`),
    KEY `idx_user_token_uid` (`uid`),
    KEY `idx_user_token_expired_time` (`expired_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `channel` (
    `id` BIGINT NOT NULL COMMENT 'Channel id',
    `name` VARCHAR(64) NOT NULL COMMENT 'Channel name',
    `owner` BIGINT NOT NULL COMMENT 'Owner user id (-1 means system channel)',
    `brief` VARCHAR(255) NULL COMMENT 'Channel brief',
    `avatar` BIGINT NOT NULL DEFAULT 0 COMMENT 'Avatar file id (0 means none)',
    `create_time` DATETIME NOT NULL COMMENT 'Create time',
    PRIMARY KEY (`id`),
    KEY `idx_channel_owner` (`owner`),
    KEY `idx_channel_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `channel_member` (
    `id` BIGINT NOT NULL COMMENT 'Channel member record id',
    `uid` BIGINT NOT NULL COMMENT 'User id',
    `cid` BIGINT NOT NULL COMMENT 'Channel id',
    `name` VARCHAR(64) NULL COMMENT 'Nickname in channel',
    `authority` INT NOT NULL DEFAULT 0 COMMENT '0 member, 1 admin',
    `join_time` DATETIME NOT NULL COMMENT 'Join time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_member_uid_cid` (`uid`, `cid`),
    KEY `idx_channel_member_cid` (`cid`),
    KEY `idx_channel_member_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `channel_application` (
    `id` BIGINT NOT NULL COMMENT 'Channel application id',
    `uid` BIGINT NOT NULL COMMENT 'Applicant user id',
    `cid` BIGINT NOT NULL COMMENT 'Channel id',
    `state` INT NOT NULL DEFAULT 0 COMMENT '0 pending, 1 approved, 2 rejected',
    `msg` VARCHAR(255) NULL COMMENT 'Application message',
    `apply_time` DATETIME NOT NULL COMMENT 'Apply time',
    PRIMARY KEY (`id`),
    KEY `idx_channel_application_uid_cid` (`uid`, `cid`),
    KEY `idx_channel_application_cid_apply_time` (`cid`, `apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `channel_ban` (
    `id` BIGINT NOT NULL COMMENT 'Channel ban record id',
    `cid` BIGINT NOT NULL COMMENT 'Channel id',
    `uid` BIGINT NOT NULL COMMENT 'Banned user id',
    `aid` BIGINT NOT NULL COMMENT 'Admin user id',
    `duration` INT NOT NULL COMMENT 'Duration (seconds, legacy)',
    `until_time` BIGINT NOT NULL DEFAULT 0 COMMENT 'Mute until (epoch millis, 0 means not set)',
    `reason` VARCHAR(255) NULL COMMENT 'Mute reason (optional)',
    `create_time` DATETIME NOT NULL COMMENT 'Create time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_ban_uid_cid` (`uid`, `cid`),
    KEY `idx_channel_ban_cid` (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL COMMENT 'Message id',
    `uid` BIGINT NOT NULL COMMENT 'Sender user id',
    `cid` BIGINT NOT NULL COMMENT 'Channel id',
    `domain` VARCHAR(64) NOT NULL COMMENT 'Domain:SubDomain',
    `domain_version` VARCHAR(16) NOT NULL DEFAULT '1.0.0' COMMENT 'Domain version (SemVer)',
    `reply_to_mid` BIGINT NOT NULL DEFAULT 0 COMMENT 'Reply target message id (0 means not a reply)',
    `data` TEXT NOT NULL COMMENT 'JSON string payload',
    `send_time` DATETIME NOT NULL COMMENT 'Send time',
    PRIMARY KEY (`id`),
    KEY `idx_message_cid_id` (`cid`, `id`),
    KEY `idx_message_cid_send_time` (`cid`, `send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `channel_read_state` (
    `id` BIGINT NOT NULL COMMENT 'Read state record id',
    `uid` BIGINT NOT NULL COMMENT 'User id',
    `cid` BIGINT NOT NULL COMMENT 'Channel id',
    `last_read_mid` BIGINT NOT NULL DEFAULT 0 COMMENT 'Last read message id (0 means never)',
    `last_read_time` BIGINT NOT NULL DEFAULT 0 COMMENT 'Epoch millis (0 means never)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel_read_state_uid_cid` (`uid`, `cid`),
    KEY `idx_channel_read_state_cid` (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

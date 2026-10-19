-- ============================================================================
-- 用户域表结构
-- 状态：草稿（待评审）
-- 责任范围：user_profile
-- 可重复执行：是（CREATE TABLE IF NOT EXISTS）
-- 来源：database-impl 的 UserProfileEntity
-- ============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_profile (
    account_id BIGINT       NOT NULL COMMENT '账号 ID，与 auth_account.id 一致',
    avatar_url VARCHAR(512) NULL COMMENT '头像地址',
    bio        VARCHAR(512) NULL COMMENT '个人简介',
    sex        BIGINT       NULL COMMENT '性别编码（沿用客户端枚举值）',
    birthday   BIGINT       NULL COMMENT '生日（epoch 秒或业务约定编码）',
    created_at DATETIME(3)  NOT NULL COMMENT '创建时间',
    updated_at DATETIME(3)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (account_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户资料';

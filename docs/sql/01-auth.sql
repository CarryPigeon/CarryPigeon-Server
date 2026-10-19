-- ============================================================================
-- 认证域表结构
-- 状态：草稿（待评审）
-- 责任范围：auth_account、auth_refresh_session
-- 可重复执行：是（CREATE TABLE IF NOT EXISTS）
-- 来源：database-impl 的 AuthAccountEntity、AuthRefreshSessionEntity
-- ============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS auth_account (
    id            BIGINT       NOT NULL COMMENT '账号 ID（雪花）',
    username      VARCHAR(64)  NOT NULL COMMENT '登录名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码摘要（Argon2）',
    email         VARCHAR(255) NOT NULL COMMENT '邮箱，用于验证码与找回',
    created_at    DATETIME(3)  NOT NULL COMMENT '创建时间',
    updated_at    DATETIME(3)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_account_username (username),
    UNIQUE KEY uk_auth_account_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '认证账号';

CREATE TABLE IF NOT EXISTS auth_refresh_session (
    id                 BIGINT       NOT NULL COMMENT '会话 ID（雪花）',
    account_id         BIGINT       NOT NULL COMMENT '所属账号',
    refresh_token_hash VARCHAR(255) NOT NULL COMMENT 'refresh token 摘要',
    expires_at         DATETIME(3)  NOT NULL COMMENT '过期时间',
    revoked            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已撤销',
    created_at         DATETIME(3)  NOT NULL COMMENT '创建时间',
    updated_at         DATETIME(3)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_auth_refresh_session_account (account_id),
    KEY idx_auth_refresh_session_expires (expires_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '认证刷新会话';

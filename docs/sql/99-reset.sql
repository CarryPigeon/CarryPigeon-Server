-- ============================================================================
-- 结构清理脚本（破坏性）
-- 状态：草稿（待评审）
-- 责任范围：删除本项目管理的全部表；不执行 DROP DATABASE，不动库本身
-- 可重复执行：是（IF EXISTS）
-- 使用场景：仅用于本地/测试库的污染重置；生产禁止执行
-- ============================================================================

SET NAMES utf8mb4;

-- 按依赖反向顺序删除（子表在前）
DROP TABLE IF EXISTS chat_message_idempotency;
DROP TABLE IF EXISTS chat_mention;
DROP TABLE IF EXISTS chat_message;

DROP TABLE IF EXISTS chat_channel_read_state;
DROP TABLE IF EXISTS chat_channel_ban;
DROP TABLE IF EXISTS chat_channel_application;
DROP TABLE IF EXISTS chat_channel_member;
DROP TABLE IF EXISTS chat_channel;

DROP TABLE IF EXISTS user_profile;

DROP TABLE IF EXISTS auth_refresh_session;
DROP TABLE IF EXISTS auth_account;

-- 插件迁移历史表由应用启动时自动创建（见 database-impl 的 JdbcPluginMigrationDatabaseService）
DROP TABLE IF EXISTS plugin_schema_history;

-- Destructive schema reset for CarryPigeon backend tables.
-- Purpose: remove polluted existing project tables before re-running 00-all-in-one.sql.
-- Scope: drops only tables managed by docs/sql.
--
-- WARNING: this script deletes all data in the listed project tables.
-- Do not run it in an environment where existing data must be preserved.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS plugin_schema_history;
DROP TABLE IF EXISTS chat_notification_channel_preference;
DROP TABLE IF EXISTS chat_notification_server_preference;
DROP TABLE IF EXISTS chat_mention;
DROP TABLE IF EXISTS chat_channel_pin;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS chat_channel_read_state;
DROP TABLE IF EXISTS chat_channel_audit_log;
DROP TABLE IF EXISTS chat_channel_ban;
DROP TABLE IF EXISTS chat_channel_invite;
DROP TABLE IF EXISTS chat_channel_member;
DROP TABLE IF EXISTS chat_channel;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS auth_refresh_session;
DROP TABLE IF EXISTS auth_account;

SET FOREIGN_KEY_CHECKS = 1;

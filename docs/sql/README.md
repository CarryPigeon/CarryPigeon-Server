# 数据库脚本

**状态：草稿（待评审）**
**读者：** 建库、升级、重置环境的人
**边界：** 本文只说明脚本集合与执行顺序。部署步骤、账号权限、字符集核对、备份恢复见 `docs/operations/数据库部署.md`；表设计约定见 `docs/standards/持久化与SQL.md`。

---

## 1. 脚本集合

| 顺序 | 文件 | 内容 |
|---|---|---|
| 1 | `01-auth.sql` | `auth_account`、`auth_refresh_session` |
| 2 | `02-user.sql` | `user_profile` |
| 3 | `03-channel.sql` | `chat_channel`、`chat_channel_member`、`chat_channel_application`、`chat_channel_ban`、`chat_channel_read_state`，以及唯一 `system` 频道种子 |
| 4 | `04-message.sql` | `chat_message`、`chat_mention` |
| — | `99-reset.sql` | 破坏性清理：删除上述全部表（仅在需要重置时先执行） |

## 2. 执行顺序

空库初始化：

```bash
mysql -h <host> -P <port> -u <user> -p <database> < docs/sql/01-auth.sql
mysql -h <host> -P <port> -u <user> -p <database> < docs/sql/02-user.sql
mysql -h <host> -P <port> -u <user> -p <database> < docs/sql/03-channel.sql
mysql -h <host> -P <port> -u <user> -p <database> < docs/sql/04-message.sql
```

已污染的环境：

```bash
mysql ... < docs/sql/99-reset.sql
# 再按上面的顺序重新执行
```

---

## 3. 约定

| 约定 | 说明 |
|---|---|
| 可重复执行 | 全部使用 `CREATE TABLE IF NOT EXISTS`；`03-channel.sql` 的种子插入用 `WHERE NOT EXISTS` 保证幂等 |
| 无拼接件 | 不提供 all-in-one 脚本：同一份 DDL 只保留一份来源 |
| 字符集 | 每张表显式 `utf8mb4` + `utf8mb4_unicode_ci` + InnoDB |
| 无外键 | 表之间不建外键约束，一致性由应用保证（与现有仓储实现一致） |
| 时间类型 | 统一使用 `DATETIME(3)` |
| 主键 | 业务 ID 由应用生成（雪花），不使用自增 |

---

## 4. 不存在但容易被找的脚本

| 曾经的脚本 | 现状 | 原因 |
|---|---|---|
| `00-all-in-one.sql` | 不提供 | 与"唯一事实来源"冲突；需要一次性执行时用 shell 顺序调用 |
| `06-plugin.sql` | 不需要 | `plugin_schema_history` 由应用启动时自动创建（`JdbcPluginMigrationDatabaseService.ensureHistoryStorage()`），手工建表反而可能与其定义漂移 |
| `90-dev-seed.sql`（开发种子） | 暂缓 | 种子账号需要与认证使用的密码哈希参数一致，不能手写常量；待明确种子数据口径后补 |

---

## 5. 待确认项

| # | 项 | 依据 |
|---|---|---|
| 1 | `chat_channel_read_state` 的主键 | 实体声明 `@TableId("channel_id")`，但运行期按 `channel_id` + `account_id` 定位；本脚本按复合主键 `(channel_id, account_id)` 定义，需与仓储实现对齐后确认 |
| 2 | 若干 VARCHAR 长度（`name`/`brief`/`avatar`/`reason`/`preview`/`data`） | 实体为 `String`，长度按字段语义与请求 DTO 的校验上限推断，未在实体中显式声明 |
| 3 | `user_profile.sex` / `birthday` 的取值范围与单位 | 实体为 `Long`，语义由客户端与服务端约定，本脚本只保证类型 |
| 4 | 索引集合 | 依据 Mapper/Service 里实际出现的过滤与排序条件推断；上线前应按真实查询计划复核 |

## 6. 已有库升级提示

本版 message 表结构与旧库存在以下差异，已有环境需要手工迁移：

```sql
ALTER TABLE chat_mention CHANGE COLUMN `read` is_read TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE chat_message DROP COLUMN domain_version;
DROP TABLE IF EXISTS chat_message_idempotency;
```

新库直接按本目录脚本初始化，不需要执行以上语句。

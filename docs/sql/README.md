# 领域 SQL

本文档目录提供当前仓库对外保留的数据库 SQL 入口，用于：

- 在空库中初始化 CarryPigeon Backend 的最终表结构
- 按领域阅读当前数据库设计
- 为本地开发和联调导入一组可重复执行的测试数据

## 文件说明

- `00-all-in-one.sql`
  - 一次性初始化整库结构的最短路径
- `01-auth.sql`
  - 认证与 refresh session 相关表
- `02-user.sql`
  - 用户资料相关表
- `03-channel.sql`
  - 频道、成员、邀请、封禁、审计、已读、置顶相关表与系统种子频道
- `04-message.sql`
  - 消息与 mention 相关表
- `05-notification.sql`
  - 服务级与频道级通知偏好相关表
- `10-test-data.sql`
  - 本地开发 / 接口联调用测试数据，假定结构已初始化
- `99-reset-schema.sql`
  - 破坏性结构清理脚本，用于污染库重新初始化前删除当前项目管理的全部表

## 推荐执行方式

若你要初始化一个空的 MySQL schema，推荐以下两种方式之一：

1. 最简单路径：直接执行 `00-all-in-one.sql`
2. 按领域执行：依次执行 `01-auth.sql`、`02-user.sql`、`03-channel.sql`、`04-message.sql`、`05-notification.sql`

若你还需要一组现成数据，再执行：

1. `10-test-data.sql`

若当前 schema 中已有旧结构或污染数据，且这些数据不需要保留，可先执行：

1. `99-reset-schema.sql`
2. `00-all-in-one.sql`
3. `10-test-data.sql`（可选）

`99-reset-schema.sql` 会删除当前项目管理表中的全部数据和表结构，不执行 `DROP DATABASE`，但仍不得用于需要保留业务数据的环境。

## 测试账号

`10-test-data.sql` 会写入 3 个测试账号：

- `carry-owner` / `carrypigeon123`
- `carry-admin` / `carry-admin-123`
- `carry-member` / `carry-member-123`

## 适用范围

- MySQL 8.x
- 空 schema 初始化
- 开源场景下的结构审查、手工部署与本地联调

当前仓库已经不再保留 `db/migration` 版本迁移目录。完整部署说明请同时阅读：

- `docs/operations/数据库部署手册.md`
- `docs/operations/部署手册.md`

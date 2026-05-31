任务名称：
基于 docs-t 的 Mentions Inbox 实现批次十

任务目标：
实现 `docs/t/SERVER_API.md` 中的：
- `GET /api/mentions`
- `PUT /api/mentions/{mention_id}/read`
- `PUT /api/mentions/read_state`

任务背景：
当前仓库仍无任何 mentions 数据面，而 mentions inbox 与已读能力是一组紧邻契约。为控制范围，本批不并行推进 mention 生成触发器，仅实现列表与已读写接口。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `application-starter`（迁移）

允许修改范围：
- `chat-domain/src/main/java/**/message/**`
- `chat-domain/src/main/java/**/shared/**`
- `infrastructure-service/database-api/src/main/java/**`
- `infrastructure-service/database-impl/src/main/java/**`
- `application-starter/src/main/resources/db/migration/**`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 本批不并行实现 message 发送链路中的 mention 生成触发器

依赖限制：
- 可复用消息、频道成员和用户公开资料数据面
- mentions 需要独立持久化表，不强塞到消息表或读状态表

配置限制：
- 不新增未来占位配置

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：
1. 新增 mention 持久化表与 database-api / impl 契约。
2. 新增 mention domain model / repository。
3. 暴露 `GET /api/mentions`。
4. 暴露 `PUT /api/mentions/{mention_id}/read` 与 `PUT /api/mentions/read_state`。
5. 补充 application / controller / persistence / database-impl 测试。
6. 执行定向 Maven 验证。

关键假设与依赖：
- 当前批次直接复用 `chat_mention.is_read` 承载已读状态，不新增 read-state 聚合表。
- mention 生成触发器（消息发送时识别 @）若需要，可先做最小基于文本的生成。

实现要求：
- 返回字段对齐 docs/t：`mention_id`、`cid`、`mid`、`from_uid`、`target`、`created_at`、`read`。
- 列表支持 `cursor`、`limit`、`unread_only`、`cid` 的最小语义。
- 支持单条已读与按 `before_mention_id` / `cid` 的批量已读。

测试要求：
- 覆盖列表成功路径。
- 覆盖 unread_only / cid 过滤。
- 覆盖单条已读、批量已读、空请求体批量已读与非法参数路径。

质量门禁：
- 定向 Maven 测试通过。

验收标准：
- `GET /api/mentions` 可返回 mentions inbox。
- `PUT /api/mentions/{mention_id}/read` 可标记当前用户单条提及已读。
- `PUT /api/mentions/read_state` 可按条件批量标记已读。

完成定义：
- 验收标准满足并完成验证。

实际结果：
- 已新增独立 mentions 持久化表 `chat_mention` 及迁移 `V13__create_chat_mention.sql`。
- 已新增 `MentionRecord` / `MentionDatabaseService` 及 MyBatis 实现。
- 已新增 mention domain model / repository / persistence adapter。
- 已新增 `MentionApplicationService` 与 `MentionController`，暴露 `GET /api/mentions`、`PUT /api/mentions/{mention_id}/read`、`PUT /api/mentions/read_state`。
- 已支持 `cursor`、`limit`、`unread_only`、`cid` 的最小语义，并返回 docs/t 要求字段。
- 已基于 `chat_mention.is_read` 实现单条已读和批量已读，无新增 read-state 表。
- 已补充 database-api、persistence、application、controller、database-impl、autoconfiguration 与 starter smoke 定向验证。

验证记录：
- `lsp_diagnostics`：新增主实现文件无错误；个别测试文件 LSP 仍存在环境级不稳定现象，但 Maven 编译与测试已通过。
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am -Dtest=MentionApplicationServiceTests,MentionControllerTests,DatabaseBackedMentionRepositoryTests,MentionDatabaseServiceContractTests,MybatisPlusMentionDatabaseServiceTests,DatabaseServiceAutoConfigurationTests,ApplicationStarterSmokeTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 当前批次尚未补 message 发送链路中的 mention 生成触发。
- 当前批次未实现 mention 相关 WS 事件推送。

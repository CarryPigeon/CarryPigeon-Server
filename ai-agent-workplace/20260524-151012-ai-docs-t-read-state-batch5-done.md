任务名称：
基于 docs-t 的 Read State / Unreads 实现批次五

任务目标：
实现 `docs/t/11-http-endpoints-v1.md` 中的：
- `PUT /api/channels/{cid}/read_state`
- `GET /api/unreads`

任务背景：
此前已完成 Files 批次。当前继续推进 docs/t 对齐，实现频道读状态与未读计数的最小可运行能力。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `application-starter`

允许修改范围：
- `chat-domain/src/main/java/**/channel/**`
- `infrastructure-service/database-api/src/main/java/**`
- `infrastructure-service/database-impl/src/main/java/**`
- `application-starter/src/main/resources/db/migration/**`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到 pins / mentions / applications

依赖限制：
- 基于现有频道成员、消息时间轴和数据库分层实现
- 不引入新的外部基础设施

配置限制：
- 不新增未来占位配置

文档依据：
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：
1. 新增 read-state 持久化模型与数据库契约。
2. 新增 `PUT /api/channels/{cid}/read_state`。
3. 新增 `GET /api/unreads`。
4. 补充 controller / application / database service 测试。
5. 执行定向 Maven 验证。

关键假设与依赖：
- 未读计数基于读锚和 `chat_message` 聚合推导。
- 本批不处理 `read_state.updated` WS 推送。

实现要求：
- `read_state` 只前进不后退。
- `unreads` 返回 docs/t 规定的字段形状。

测试要求：
- 覆盖更新已读的成功路径与回退保护。
- 覆盖获取未读的成功路径。
- 覆盖 database-api / impl 契约。

质量门禁：
- 相关定向 Maven 测试通过。

复审要求：
- 重点检查 docs/t 一致性、只前进语义、未读计数推导与模块边界。

文档要求：
- 默认不修改 docs/t。

验收标准：
- `PUT /api/channels/{cid}/read_state` 可用且满足只前进语义。
- `GET /api/unreads` 可返回当前用户各频道未读信息。

完成定义：
- 验收标准满足。
- 质量门禁执行并记录。

实际结果：
- 已新增 `chat_channel_read_state` 持久化表迁移。
- 已实现 `PUT /api/channels/{cid}/read_state`。
- 已实现 `GET /api/unreads`。
- 已补齐 `chat-domain`、`database-api`、`database-impl`、`application-starter` 的最小链路与回归测试。

验证记录：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am -Dtest=ChannelApplicationServiceTests,ChannelReadStateControllerTests,ChannelReadStateDatabaseServiceContractTests,MybatisPlusChannelReadStateDatabaseServiceTests,MessageAttachmentRegressionTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 仍未补 `read_state.updated` WebSocket 事件。
- 当前 unread 为数据库实时聚合，若后续需要高性能预聚合可再增强。

知识沉淀 / 是否回写 docs：
- 默认不回写 docs。

产物清理与保留说明：
- 已完成，状态改为 `done`。

补充说明：
- 本批次为 docs/t read-state / unreads 的真正实现批次。

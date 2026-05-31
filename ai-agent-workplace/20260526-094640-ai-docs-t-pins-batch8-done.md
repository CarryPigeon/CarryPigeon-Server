任务名称：
基于 docs-t 的 Message Pins 实现批次八

任务目标：
实现 `docs/t/SERVER_API.md` 中的：
- `POST /api/channels/{cid}/pins/{mid}`
- `DELETE /api/channels/{cid}/pins/{mid}`
- `GET /api/channels/{cid}/pins`

任务背景：
当前仓库已具备消息、频道、成员、治理和审计能力，但尚无任何置顶持久化与 API。Pins 是下一批最小但完整的消息增强能力。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `application-starter`（迁移）

允许修改范围：
- `chat-domain/src/main/java/**/message/**`
- `chat-domain/src/main/java/**/channel/**`
- `infrastructure-service/database-api/src/main/java/**`
- `infrastructure-service/database-impl/src/main/java/**`
- `application-starter/src/main/resources/db/migration/**`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到 mentions / forward

依赖限制：
- 复用现有 message/channel/member 治理规则
- 新增持久化表必须走 database-api / database-impl 分层

配置限制：
- 不新增未来占位配置

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：
1. 新增 pin 持久化表与数据库契约。
2. 新增 pin/unpin/list application use cases。
3. 暴露三条 pins 接口。
4. 补充 controller / application / database-impl 测试。
5. 执行定向 Maven 验证。

关键假设与依赖：
- pin list 采用 `items + next_cursor + has_more`，先走最小 cursor 模式。
- 置顶上限可先以内存常量或 application 常量表达。

实现要求：
- 必须校验频道成员资格与消息归属频道一致性。
- 列表字段对齐 docs/t：`cid`、`mid`、`pinned_by_uid`、`pinned_at`、`note`。

测试要求：
- 覆盖 pin / unpin / list 成功路径。
- 覆盖 pin 上限或冲突路径（若实现）。

质量门禁：
- 相关定向 Maven 测试通过。

复审要求：
- 重点检查 docs/t 一致性、消息归属校验与分页形状。

验收标准：
- 三条 pins 接口可用。

完成定义：
- 验收标准满足并完成验证。

实际结果：
- 已新增 `chat_channel_pin` 持久化表。
- 已补齐 channel pin 的 domain / database-api / database-impl 链路。
- 已实现 `POST /api/channels/{cid}/pins/{mid}`。
- 已实现 `DELETE /api/channels/{cid}/pins/{mid}`。
- 已实现 `GET /api/channels/{cid}/pins`。
- 已补充 pins 的 application / controller / database-impl 定向测试。

验证记录：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl -am -Dtest=MessageApplicationServicePinsTests,ChannelPinsControllerTests,ChannelPinDatabaseServiceContractTests,MybatisPlusChannelPinDatabaseServiceTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- pinned / unpinned 相关 WS 事件本批未实现。

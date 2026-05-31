任务名称：
基于 docs-t 的 Audit Logs 批次十五

任务目标：
实现 docs/t 中的审计日志接口：
- `GET /api/audit_logs`

任务背景：
remote discover 批次完成后，剩余 docs/t backlog 仅剩 audit logs。当前仓库已经有 `ChannelAuditLog` 相关数据面和写入逻辑，但还没有面向 docs/t 的正式审计查询 HTTP 接口，因此本批优先复用现有审计持久化能力补齐分页与筛选查询能力。

影响模块：
- `chat-domain`
- `database-api` / `database-impl`（若现有审计数据库服务缺少查询能力）

允许修改范围：
- `chat-domain/src/main/java/**/channel/**`
- `chat-domain/src/main/java/**/shared/**`
- 与审计日志查询直接相关的 `database-api` / `database-impl`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到新的业务 feature

依赖限制：
- 优先复用现有 `ChannelAuditLog` 模型、仓储与数据库表
- 若需新增查询接口，必须沿 `chat-domain -> database-api -> database-impl` 分层落地

配置限制：
- 不新增未来占位配置

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/14-pagination-and-cursor-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：
1. 阅读 docs/t 中 audit logs 契约。
2. 阅读当前 `ChannelAuditLog` domain / repository / database service / mapper 现状。
3. 设计最小分页与筛选查询方案。
4. 实现 `GET /api/audit_logs`。
5. 补 controller / application / persistence 定向测试。
6. 执行定向 Maven 验证。

关键假设与依赖：
- 当前审计写入 action 枚举与 docs/t action 枚举可能存在命名差异，本批需要明确映射策略。
- 若现有审计只覆盖频道级动作，则先按当前已存在动作返回，不虚构不存在的历史记录。

实现要求：
- 以 docs/t 为协议真源。
- 返回统一 `{ items, next_cursor, has_more }` 结构。
- 支持 `cursor` / `limit` / `cid` / `actor_uid` / `action` / `from_time` / `to_time` 最小语义。

测试要求：
- 覆盖无筛选列表成功路径。
- 覆盖按 `cid` / `actor_uid` / `action` 的最小筛选。
- 覆盖分页游标和非法 cursor 路径。

质量门禁：
- 定向 Maven 测试通过。
- 无新增明确编译错误。
- 对外返回字段、分页结构与筛选行为具备断言。

复审要求：
- 完成后复查 action 命名映射、details 暴露边界、分页语义与 docs/t 一致性。

文档要求：
- 若仅实现既有 docs/t 契约，不额外改 `docs/`

验收标准：
- `GET /api/audit_logs` 可按 docs/t 最小契约工作。

完成定义：
- 验收标准满足
- 定向验证通过并记录
- 任务单补齐实际结果 / 验证记录 / 残留风险后转 `done`

实际结果：
- 已实现 `GET /api/audit_logs`。
- 已为现有 `ChannelAuditLog` 数据面补齐读侧 record / repository / mapper / database service 查询能力。
- 已实现最小分页和筛选：`cursor` / `limit` / `cid` / `actor_uid` / `action` / `from_time` / `to_time`。
- 已对 docs/t action 枚举与现有审计动作名做最小映射。

验证记录：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl -am -Dtest=AuditLogApplicationServiceTests,AuditLogControllerTests,DatabaseBackedChannelAuditLogRepositoryTests,MybatisPlusChannelAuditLogDatabaseServiceTests,DatabaseServiceAutoConfigurationTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 当前只返回现有已记录的审计动作，不为历史上未记录的数据虚构结果。

知识沉淀 / 是否回写 docs：
- 暂不回写，除非发现 docs/t 本身冲突

产物清理与保留说明：
- 保留本任务单用于最终收口追溯

补充说明：
- 该批若收口完成，意味着当前 docs/t backlog 已全部实现完毕，随后需要做一次最终全量复盘与收尾。

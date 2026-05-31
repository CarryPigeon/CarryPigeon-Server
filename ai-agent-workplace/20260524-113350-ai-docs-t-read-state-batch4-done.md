任务名称：
基于 docs-t 的 Read State / Unreads 补齐批次四

任务目标：
实现 `docs/t/11-http-endpoints-v1.md` 中的：
- `PUT /api/channels/{cid}/read_state`
- `GET /api/unreads`

任务背景：
上一批已补齐 Files 接口。剩余 docs/t 能力中，读状态与未读计数是高频核心能力，并且可望复用现有频道/消息数据，因此作为 batch4 优先推进。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- 必要时 `application-starter`（如 starter 回归需要）

允许修改范围：
- `chat-domain/src/main/java/**/channel/**`
- `chat-domain/src/main/java/**/message/**`
- `chat-domain/src/main/java/**/shared/**`
- `infrastructure-service/database-api/src/main/java/**`
- `infrastructure-service/database-impl/src/main/java/**`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不把本批次扩大为 pins / mentions / applications 的并行实现
- 不擅自改写 docs/t 目标协议

依赖限制：
- 优先复用现有频道成员、消息时间轴和数据库分层
- 如需新持久化契约，必须走 `database-api` / `database-impl` 正常分层

配置限制：
- 不新增未来占位配置
- 不引入新的外部基础设施

文档依据：
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/测试规范.md`

任务分解 / 执行计划：
1. 梳理当前频道/消息数据链路中是否已有 read-state / unread-count 邻接能力。
2. 设计最小读状态持久化模型与应用服务。
3. 实现 `PUT /api/channels/{cid}/read_state`。
4. 实现 `GET /api/unreads`。
5. 补充 controller / application / repository / database service 测试。
6. 执行诊断与定向 Maven 验证。

关键假设与依赖：
- 当前仓库中未必已有读状态持久化模型，因此本批可能需要新增最小数据库契约。
- 未读计数可以基于消息时间轴和读状态锚点推导，无需引入复杂预聚合。

实现要求：
- `read_state` 必须满足“只前进不后退”的 docs/t 语义。
- `unreads` 输出字段必须与 docs/t 形状一致。
- 若需要错误 reason，应与现有统一错误模型保持一致。

测试要求：
- 覆盖更新已读的成功路径与回退保护
- 覆盖获取未读的成功路径
- 若新增数据库契约，补充 database-api / impl 测试

质量门禁：
- 改动文件无新增诊断错误
- 相关定向 Maven 测试通过
- 受影响模块编译通过

复审要求：
- 需要复审
- 重点检查 docs/t 一致性、只前进语义、未读计数推导与模块边界

文档要求：
- 默认不修改 docs/t，只改实现对齐

验收标准：
- `PUT /api/channels/{cid}/read_state` 可用且满足只前进语义
- `GET /api/unreads` 可返回当前用户各频道未读信息

完成定义：
- 验收标准满足
- 质量门禁执行并记录

实际结果：
- 已实现 `PUT /api/channels/{cid}/read_state`。
- 已实现 `GET /api/unreads`。
- 已新增 `chat_channel_read_state` 持久化表迁移。
- 已补齐 `chat-domain`、`database-api`、`database-impl` 三层的 read-state / unread 聚合数据链路。
- 已新增对应 controller、application、database service 契约测试。

验证记录：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am -Dtest=ChannelApplicationServiceTests,ChannelReadStateControllerTests,ChannelReadStateDatabaseServiceContractTests,MybatisPlusChannelReadStateDatabaseServiceTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 当前仍未补齐 `read_state.updated` WS 事件推送。
- 当前 `GET /api/unreads` 基于数据库聚合推导，后续如需高并发优化可再考虑缓存或预聚合。

知识沉淀 / 是否回写 docs：
- 默认不回写 docs

产物清理与保留说明：
- 完成后改名为 `done`

补充说明：
- 本任务单选择 Read State / Unreads 作为 batch4 推荐实现面

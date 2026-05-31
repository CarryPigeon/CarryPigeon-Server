任务名称：
基于 docs-t 的 Message Edit / Message Model / WS 对齐批次十一

任务目标：
实现并对齐 `docs/t/SERVER_API.md` 中与消息编辑相关的核心能力：
- `PATCH /api/messages/{mid}`
- 消息模型补齐 `edited_at`、`edit_version`、`mentions`、`forwarded_from`
- 对应关键 WS 事件补齐最小闭环（优先 `message.updated`）

任务背景：
当前仓库已完成消息查询、搜索、pins、forward baseline、mentions inbox 与 mentions read，但剩余 P0 缺口集中在消息编辑及消息模型扩展。该批次既影响 HTTP 协议，也影响 WS 推送与消息返回结构，是后续把 forward / mentions 真正闭环的前置批次。

影响模块：
- `chat-domain`
- `application-starter`（仅当回归装配或测试支撑需要时）
- 如需最小持久化字段扩展，则涉及 `infrastructure-service/database-api`
- 如需最小持久化字段扩展，则涉及 `infrastructure-service/database-impl`

允许修改范围：
- `chat-domain/src/main/java/**/message/**`
- `chat-domain/src/main/java/**/server/**`
- `chat-domain/src/main/java/**/shared/**`
- 与消息模型持久化直接相关的 `database-api` / `database-impl`
- 与上述改动直接相关的测试
- `application-starter` 下与该批回归测试直接相关的测试支撑

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到 notification preferences / audit logs / remote discover
- 本批不并行完成 mentions 自动生成触发器的复杂增强版，只做 docs/t 所需最小闭环

依赖限制：
- 优先复用现有消息发送、查询、realtime 发布链路
- 若需新增持久化字段，必须沿既有 `chat-domain -> database-api -> database-impl` 分层落地
- 不引入新的持久化表，除非现有消息表完全无法承载 docs/t 要求且有明确证据

配置限制：
- 不新增未来占位配置
- 不扩展与本批无关的 runtime 配置项

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：
1. 读取 docs/t 中消息编辑、消息模型、forwarded_from、mentions、WS 事件章节。
2. 盘点当前消息 controller / service / dto / realtime publisher / persistence 的真实状态。
3. 确认最小持久化改动方案与 message edit 语义边界。
4. 实现 `PATCH /api/messages/{mid}` 最小闭环。
5. 对齐消息返回模型字段：`edited_at`、`edit_version`、`mentions`、`forwarded_from`。
6. 补最小必要 WS 事件，优先 `message.updated`。
7. 补 application / controller / persistence / realtime 定向测试。
8. 执行定向 Maven 验证。

关键假设与依赖：
- 当前 forward baseline 已存在入口，但 `forwarded_from` 结构化字段仍待补齐。
- mentions inbox/read 已完成，但 message send/edit 链路中的 mentions 规范化仍可能需要最小实现。
- 编辑窗口、可编辑权限、冲突版本控制以 docs/t 为准，并优先贴合现有 `ProblemException` / 认证模式。

实现要求：
- 必须优先满足 docs/t 对 `PATCH /api/messages/{mid}` 的请求体、响应体和错误语义要求。
- 必须保持消息查询、搜索、上下文接口返回模型的一致性，避免 edit 后不同接口形状分裂。
- 若引入 `edit_version` 或 `edited_at`，必须明确其持久化来源与更新时机。
- 优先做最小可运行实现，不做额外架构演进。

测试要求：
- 覆盖编辑成功路径。
- 覆盖不可编辑、编辑窗口过期、冲突版本等失败路径。
- 覆盖消息响应模型新增字段的序列化结果。
- 覆盖最小 WS 事件推送行为。

质量门禁：
- 相关文件无新增可确认编译错误
- 相关定向 Maven 测试通过
- 受影响模块 reactor 构建通过
- 对外协议关键字段与状态码具备定向断言

复审要求：
- 该批涉及对外协议、消息模型与 WS 事件，完成后必须进行一次独立复核，重点检查协议一致性、消息模型兼容性、事件遗漏与安全边界。

文档要求：
- 若本批只是在实现既有 docs/t 契约，不新增长期规则，则不额外改 `docs/`
- 任务级决策与验证记录写入本任务单

验收标准：
- `PATCH /api/messages/{mid}` 可按 docs/t 最小契约运行
- 消息返回模型新增字段在受影响接口中一致可见
- 最小必要 WS 事件已补齐并可验证

完成定义：
- 验收标准满足
- 定向验证已执行并记录
- 本任务单补齐实际结果、验证记录与残留风险后再改为 `done`

实际结果：
- 已新增 `V14__extend_chat_message_for_edit_and_forward.sql`，为消息表补齐 `edited_at`、`edit_version`、`mentions`、`forwarded_from`。
- 已扩展 `ChannelMessage`、`MessageRecord`、`MessageEntity`、`ChannelMessageResult` 与相关 mapper/repository 映射。
- 已实现 `PATCH /api/messages/{mid}` 最小闭环。
- 已补 `message.updated` 事件，并让 message 响应模型对齐 `edited_at` / `edit_version` / `mentions` / `forwarded_from`。
- 已补 forward 的 `forwarded_from` 结构化持久化与响应输出。

验证记录：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am -Dtest=MessageApplicationServiceSendTests,MessageApplicationServiceForwardTests,ChannelMessageQueryControllerTests,MessageForwardControllerTests,NettyMessageRealtimePublisherTests,DatabaseBackedMessageRepositoryTests,MybatisPlusMessageDatabaseServiceTests,ApplicationStarterSmokeTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 当前仅做了 docs/t 要求的最小 edit 闭环，mentions 自动生成触发器仍独立于该批次推进。

知识沉淀 / 是否回写 docs：
- 暂不回写，除非发现 docs/t 本身存在冲突或长期规则缺口

产物清理与保留说明：
- 保留本任务单用于后续批次追溯

补充说明：
- 该批完成后，再进入 notification preferences / audit logs / remote discover 等剩余 backlog。

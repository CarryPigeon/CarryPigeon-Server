任务名称：
基于 docs-t 的 Notification Preferences 批次十三

任务目标：
实现 docs/t 中通知偏好相关 HTTP 能力：
- `GET /api/notification_preferences`
- `PUT /api/channels/{cid}/notification_preference`
- `PUT /api/notification_preferences/server`

任务背景：
在基础契约清理批次完成后，剩余 backlog 中 notification preferences 相对独立，且能复用当前用户、频道和配置主链路。该批优先实现最小可运行的通知偏好读写闭环，不并行扩散到 audit logs 与 remote discover。

影响模块：
- `chat-domain`
- 如需正式持久化则涉及 `database-api` / `database-impl`
- `application-starter`（若新增迁移或测试支撑）

允许修改范围：
- `chat-domain/src/main/java/**/server/**`
- `chat-domain/src/main/java/**/channel/**`
- `chat-domain/src/main/java/**/shared/**`
- 与通知偏好持久化直接相关的 `database-api` / `database-impl`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到 audit logs / remote discover
- 不重构已完成批次的 mentions / message edit / files / pins / applications

依赖限制：
- 优先复用现有用户鉴权、频道读取、基础配置与 persistence 分层
- 如果需要持久化，必须沿 `chat-domain -> database-api -> database-impl` 路径落地

配置限制：
- 不新增未来占位配置
- 不把“当前默认值”误写成长期 docs 规则

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：
1. 阅读 docs/t 中 notification preferences 三个接口的请求/响应模型。
2. 阅读当前 server/channel/controller/config/persistence 现状，确认是否已有可复用数据面。
3. 设计最小实现方案：明确默认值、枚举值、是否需要新表。
4. 实现 notification preferences 读接口。
5. 实现频道级与服务端级写接口。
6. 补 controller / application / persistence 定向测试。
7. 执行定向 Maven 验证。

关键假设与依赖：
- 若当前仓库完全无通知偏好持久化基础，本批可能需要新增最小持久化表。
- 若 docs/t 未要求跨端复杂联动，本批优先只做 HTTP 契约，不并行补 WS 事件。

实现要求：
- 以 docs/t 为协议真源。
- 默认值与枚举必须稳定、可测。
- 最小实现优先，但不能留下明显 placeholder 行为。

测试要求：
- 覆盖读取默认偏好。
- 覆盖频道级写入与服务端级写入。
- 覆盖非法 mode / 非法输入路径。

质量门禁：
- 相关定向 Maven 测试通过。
- 无新增明确编译错误。
- 对外响应字段与状态码有断言。

复审要求：
- 完成后复查默认值选择、枚举值映射与持久化边界。

文档要求：
- 若仅实现既有 docs/t 契约，不额外改 `docs/`

验收标准：
- notification preferences 三个 HTTP 接口可按 docs/t 最小契约工作

完成定义：
- 验收标准满足
- 定向验证通过并记录
- 任务单补齐实际结果 / 验证记录 / 残留风险后转 `done`

实际结果：
- 已新增通知偏好持久化表与 migration：`V15__create_notification_preferences.sql`。
- 已实现 `GET /api/notification_preferences`、`PUT /api/notification_preferences/server`、`PUT /api/channels/{cid}/notification_preference`。
- 已补齐 database-api / database-impl / domain / controller / auto-config 主链路。

验证记录：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am -Dtest=NotificationPreferenceApplicationServiceTests,NotificationPreferenceControllerTests,ChannelControllerTests,NotificationPreferenceDatabaseServiceContractTests,MybatisPlusNotificationPreferenceDatabaseServiceTests,DatabaseServiceAutoConfigurationTests,ApplicationStarterSmokeTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 当前按 docs/t 最小契约实现默认值与枚举，未扩展任何 WS 事件。

知识沉淀 / 是否回写 docs：
- 暂不回写，除非发现 docs/t 本身冲突

产物清理与保留说明：
- 保留本任务单用于后续追溯

补充说明：
- 本批完成后再进入 remote discover / audit logs。

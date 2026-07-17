# 请求处理链审计问题修复任务单

## 任务目标

根据 `ai-agent-workplace/20260705-141033-ai-request-chain-audit-done.md` 中记录的问题，按既有模块边界修复确定性缺陷，补充必要测试，并完成验证闭环。

## 任务类型

实现类任务。

## affected modules

- `chat-domain`
- `infrastructure-service/cache-api`
- `infrastructure-service/cache-impl`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `docs/sql`
- `ai-agent-workplace`

## 允许修改范围

- 与审计任务单中 P0/P1/P2 确定性问题直接相关的 Java 源码、测试、SQL 表定义与测试数据。
- 本任务单的计划、验证记录、复审记录和关闭状态。

## 禁止边界

- 不新增第三方依赖。
- 不改变 Maven 模块结构与依赖方向。
- 不把 `chat-domain` 连接到任何 `*-impl`。
- 不引入新的架构模式。
- 不修改与本次审计问题无关的既有未提交改动。

## governing docs

- `AGENTS.md`
- `docs/standards/AI协作开发规范.md`
- `docs/standards/变更审核清单.md`
- `docs/architecture/架构文档.md`
- `docs/architecture/包结构规范.md`
- `docs/architecture/依赖引入规范.md`
- `docs/standards/异常与错误码规范.md`
- `docs/standards/测试规范.md`
- `docs/standards/注释规范.md`

## 执行计划

1. 读取相关领域服务、Controller、仓储适配、数据库/缓存契约与测试，确认现有代码形态。
2. 修复权限与输入校验类缺陷：审计日志、私有频道可见性、消息转发源频道可见性、禁言时间、置顶分页、WS malformed frame。
3. 修复持久化一致性缺陷：入群申请 reason、消息删除依赖清理、验证码原子消费、用户资料分页/搜索。
4. 收敛协议/文档一致性问题：OpenAPI 描述、TLS discovery 默认语义、删除/撤回事件语义、必要 SQL 更新。
5. 补充或调整相关单元、契约、业务链路测试。
6. 运行相关 Maven 测试；如范围允许，运行更大范围 `mvn test -DskipTests=false`。
7. 按变更审核清单自检，记录验证证据与残留风险，将任务单关闭为 `done`。

## 质量门禁

- 相关测试通过，至少覆盖本次修改的关键业务失败路径。
- 跨模块改动不破坏依赖方向。
- SQL 与 Java 模型字段保持一致。
- 对外错误语义不把客户端输入错误映射为 500。
- 任务单记录实际改动、验证结果和残留风险。

## acceptance criteria

- P0 权限/删除阻断问题均有代码修复与回归测试。
- P1 中可在既有边界内落地的问题完成修复；需产品取舍的问题记录实际取舍。
- P2 文档/API 描述不再与当前实现明显冲突。
- 任务单从 `current` 归档为 `done`。

## implementation result

已按审计任务单修复确定性链路缺陷：

- 权限链路：审计日志查询要求指定频道并校验成员身份；private / system 频道按 ID 查询要求成员身份；消息转发前校验操作者对源频道与目标频道均具备成员可见性。
- 输入校验：禁言绝对时间拒绝过去时间；置顶列表入口增加 `limit` 约束并修正空页游标安全；WS 入站 envelope 的非法对象 / 数字字段统一转为 `ProblemException.validationFailed`。
- 持久化一致性：入群申请 `reason` 进入领域模型、database-api record、MyBatis entity / mapper / service 和 SQL；消息硬删除前清理 pin / mention 依赖；未读 SQL 排除 recalled 消息。
- 一次性凭证：`CacheService` 增加 `consumeIfEquals` 语义，Redis 实现使用 Lua 原子 compare-and-delete，内存验证码实现使用条件 remove，验证码空值输入返回校验错误。
- 事件与协议：硬删除下发 `message.deleted`，撤回下发 `message.recalled`；插件事件声明补齐 `message.updated` / `message.deleted`；内置 realtime discovery 默认直连地址改为 `ws://`。
- API 契约：认证 `device_id` 从必填降级为可选并修正文档语义；用户公开资料 OpenAPI 描述改为公开资料语义；用户资料分页 / 搜索领域逻辑改用仓储分页与搜索能力。
- SQL：同步 `docs/sql/00-all-in-one.sql`、`docs/sql/03-channel.sql`、`docs/sql/10-test-data.sql` 的 `chat_channel_invite.reason` 字段与测试数据。

本次继续阶段额外补齐的回归测试：

- `ChannelMessagePublishingDomainApiForwardTests.forwardChannelMessage_sourceNonMember_throwsForbiddenProblem`
- `RealtimeChannelHandlerMessageDispatchTests.channelRead_sendChannelMessageMalformedNumericField_returnsValidationError`
- `RedisCacheServiceTests.consumeIfEquals_*`
- `MybatisPlusChannelPinDatabaseServiceTests.deleteByMessageId_delegatesToMapper`
- `MybatisPlusMentionDatabaseServiceTests.deleteByMessageId_delegatesToMapper`

## validation record

- `mvn -pl chat-domain,infrastructure-service/cache-api,infrastructure-service/cache-impl,infrastructure-service/database-api,infrastructure-service/database-impl -am test -DskipTests=false`
  - 结果：通过。
  - 覆盖：`infrastructure-basic` 28 tests；`database-api` 51 tests；`storage-api` 2 tests；`cache-api` 1 test；`mail-api` 5 tests；`chat-domain` 342 tests；`database-impl` 99 tests（1 skipped env）；`cache-impl` 26 tests（1 skipped env）。
- `mvn -pl chat-domain,infrastructure-service/cache-impl,infrastructure-service/database-impl -am -Dtest=ChannelMessagePublishingDomainApiForwardTests,RealtimeChannelHandlerMessageDispatchTests,RedisCacheServiceTests,MybatisPlusChannelPinDatabaseServiceTests,MybatisPlusMentionDatabaseServiceTests -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`
  - 结果：通过。
  - 覆盖：新增转发权限、WS malformed、Redis 原子消费、pin / mention 删除委托测试。
- `mvn test -DskipTests=false`
  - 首次在沙箱内失败：`infrastructure-basic/target/classes/log4j2-spring.xml` 资源复制报只读文件系统，未进入测试失败。
  - 提权重跑结果：通过。
  - 覆盖：14 个 Maven 模块全部成功；`database-impl` / `cache-impl` / `storage-impl` / `mail-impl` 各有 1 个环境依赖测试按既有条件跳过；`application-starter` 有 1 个环境依赖测试跳过。

## self-check against change review checklist

- 架构与边界：通过。未新增 Maven 模块，未改变模块职责；`chat-domain` 仍只依赖 `infrastructure-service/*-api`，没有连接 `*-impl`。
- 依赖：通过。未新增第三方依赖；根级提权测试仅解析已有模块依赖。
- 配置：通过。未新增占位配置；realtime discovery 默认协议调整为当前内置 Netty 能力。
- 异常与错误码：通过。客户端输入错误收敛为 validation / forbidden 语义，避免 malformed WS 帧走 `internal_error`。
- 注释与文档：通过。新增测试方法均补充测试意图注释；无新增长期规则，因此未修改长期规范文档。
- 测试：通过。新增失败路径与基础设施委托测试，相关模块完整测试与根级测试均通过。
- AI 工作目录：通过。本任务记录在 `ai-agent-workplace/`，完成后归档为 `done`。

## residual risks

- 通知偏好未接入实时投递过滤仍为审计中记录的后续 P2 风险，本任务未扩展实现。
- 用户列表 / 搜索 HTTP 入口是否恢复仍属于产品取舍；本次只修正领域 API 与现有文档明显不一致处。
- 认证 `device_id` 采取“降级为可选契约”的实现取舍，尚未把设备 ID 纳入 refresh session 持久化与校验。
- WSS 能力仍依赖外部反向代理或未来 TLS pipeline；当前内置 Netty 直连能力按 `ws://` 暴露。

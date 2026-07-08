# 请求处理链复审任务单

## 任务目标

基于当前工作区代码，逐调用链复查 HTTP / WebSocket 请求从入口、鉴权、参数转换、领域服务、仓储端口、基础设施适配器到副作用事件的处理链是否完整，识别仍存在的断链、权限绕过、错误映射不一致、文档不一致和测试缺口。

## 任务类型

只读探索 / 审查任务。本任务不修改正式 Java 源码、测试、SQL、配置或正式 docs；仅允许更新本任务单。

## 影响模块

- `application-starter`
- `chat-domain`
- `infrastructure-service/*-api`
- `infrastructure-service/*-impl`
- `docs/`
- `ai-agent-workplace/`

## 允许修改范围

- 允许新增和更新本任务单。
- 允许在 `ai-agent-workplace/` 记录审查结果。

## 禁止修改范围

- 不修改正式源码、测试、SQL、配置或正式 docs。
- 不新增依赖。
- 不改变 Maven 模块结构。
- 不调整对外 API / WS 协议。
- 不修改数据库模型。

## 文档依据

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/API.md`
- `docs/t/*.md`
- `ai-agent-workplace/20260707-224015-ai-request-chain-audit-done.md`
- `ai-agent-workplace/20260707-231748-ai-request-chain-fixes-done.md`

## 执行计划

1. 盘点当前 HTTP Controller、WS Handler、领域 API、仓储适配与测试覆盖。
2. 按 Auth、User、File、Channel、Message、Server/WS 拆分审查入口到副作用事件的调用链。
3. 重点复查上轮修复项：消息转发发送治理、申请审批封禁复核、消息请求体校验、撤回 HTTP 入口、文件异常模型、WS 入站协议。
4. 交叉检查 docs/OpenAPI 与实际入口是否仍有不一致。
5. 搜索模块边界和直接 impl/starter 依赖。
6. 汇总问题、影响、证据与建议修复方向。
7. 记录验证方式和残留风险，归档任务单为 `done`。

## 质量门禁

- 结论必须有仓库内文件证据。
- 主要 feature 至少覆盖入口、领域服务、仓储/适配器和测试。
- 明确说明本任务为只读审查，未运行实现类测试门禁的原因。

## 验收标准

- 输出当前代码的逐调用链复审结论。
- 明确列出新发现或仍未解决的问题。
- 明确列出已确认闭合的上轮修复项。
- 任务单归档为 `done`。

## 审查结果

## 审查范围与方法

- 已按 HTTP / WS 入口盘点所有 `@(Get|Post|Put|Patch|Delete)Mapping`。
- 已按 Auth、Server/Gate/Catalog、User、File、Channel、Application、Audit、ReadState、Message、Mention、Pin、NotificationPreference、Realtime WS 分组阅读主要入口、领域 API、仓储端口/适配器和关键测试。
- 已执行模块边界搜索：
  - `chat-domain`、`infrastructure-basic`、`infrastructure-service/*-api` 未发现直接依赖 `starter` 或 `*-impl` 的命中。
  - `docs/t` 未发现旧 `docs/api/` 引用。
- 已执行请求体校验搜索，重点确认缺少 `@Valid/@NotNull` 的入口。
- 本任务仅更新任务单，不修改正式源码、测试、SQL、配置或正式 docs。

## 总体结论

当前主业务请求链路总体是闭合的：HTTP 入口普遍先通过 `RequestAuthenticationContext` 获取认证主体，再把协议 DTO 转换为领域 command/query，领域服务执行权限/状态校验，通过领域仓储端口读写，持久化适配器经 `infrastructure-service/database-api` 访问数据库服务；消息和频道变更在事务提交后进入 realtime publisher。

上轮高风险问题已确认闭合：

- 消息发送和转发目标频道均经 `requireSendableChannel(...)`，禁言/不可发送状态不会被转发绕过。
- 频道申请审批通过前会重新检查目标账号当前 active ban。
- 消息编辑和转发请求体已接入 `@Valid @NotNull`。
- 已存在 HTTP 撤回入口 `POST /api/channels/{channelId}/messages/{messageId}/recall`，并进入 `ChannelMessageLifecycleApi.recallChannelMessage(...)`。
- 文件账号 ID 校验已统一抛出 `ProblemException.validationFailed(...)`。
- WS `send_channel_message` 入站命令进入领域发送 API；发送成功依赖 `message.created` 事件确认。
- `docs/t` 不再引用旧 `docs/api/` 路径。

## 逐链路复审记录

### Auth

- 入口：`AuthController` 覆盖 `/api/auth/email_codes`、`/register`、`/login`、`/tokens`、`/refresh`、`/revoke`。
- 处理链：Controller `@Valid @RequestBody` -> `AuthAccountApi` / `AuthSessionApi` -> 验证码服务 / token 服务 / refresh session 仓储 -> database-api 适配器。
- 结论：链路闭合，未发现权限断链。验证码登录会先检查 required plugin gate。
- 测试：`AuthControllerTests`、`AuthAccessTokenInterceptorTests`、`AuthSessionDomainApi*Tests`、验证码服务测试覆盖成功和失败路径。

### Server / Gate / Catalog

- 入口：`ServerController`、`ServerGateController`、`ServerPluginCatalogController`、`ServerDomainCatalogController`。
- 处理链：公开 discovery / required gate / catalog 入口 -> `ServerEntranceApi` 或 catalog domain -> 配置/插件注册数据。
- 结论：公开链路闭合；发现文档的 realtime URL 说明存在文档不一致问题，见问题 R-004。

### User

- 入口：`UserProfileController` 覆盖 `/api/users/me`、`/{accountId}`、批量 `/api/users?ids=`、邮箱更新、资料 patch、背景图上传。
- 处理链：认证主体 -> `UserProfileApi` / `EmailVerificationCodeService` / `FileTransferApi` -> user/auth/file 仓储或对象存储端口。
- 结论：功能链路闭合；批量公开资料查询仍存在全表扫描风险，见问题 R-001；背景图返回相对路径风险见问题 R-003。

### File

- 入口：`FileController` 覆盖上传授权、同源 PUT 上传、share key 下载。
- 处理链：认证主体 -> `FileTransferApi` -> share key codec / object key resolver -> `ObjectStorageService`。
- 结论：链路闭合；服务端头像匿名下载、普通文件认证下载规则存在。

### Channel / Application / Governance / Audit / ReadState

- 入口：`ChannelController`、`ChannelApplicationController`、`AuditLogController`、`ChannelReadStateController`。
- 处理链：认证主体 -> channel lifecycle/query/governance/application/access API -> channel/member/ban/invite/audit/read-state 仓储 -> after-commit realtime publisher。
- 结论：
  - 成员、管理员、封禁、申请审批、频道资料、删除安全检查等链路均有领域权限校验。
  - read-state 会校验成员身份与消息归属频道，并只前进不后退。
  - `read_state.updated` 事件当前会被通知偏好过滤，是否符合“状态同步不应被静音影响”需要产品确认，见问题 R-005。

### Message / Mention / Pin

- 入口：`ChannelMessageController`、`MessageController`、`MentionController`、`ChannelPinsController`。
- 处理链：认证主体 -> message publishing/timeline/lifecycle/attachment/pin/mention API -> message/mention/pin 仓储与 channel boundary -> after-commit realtime publisher。
- 结论：
  - 历史、搜索要求频道成员。
  - HTTP 发送和 WS 发送要求可发送频道。
  - 编辑要求发送者、文本 domain、版本冲突校验。
  - 删除/撤回走 recall permission，删除会清理 pins 与 mentions。
  - 置顶要求 pin moderation permission，并校验消息归属频道。
  - mention 查询/已读按当前账号过滤。
  - HTTP 发送请求体缺少 `@NotNull`，边界输入 `null` 可能在 Controller 触发 NPE，见问题 R-002。
  - 已撤回消息再次撤回会重复发布 update/recalled 事件，当前可视为幂等但有事件噪声风险，见问题 R-006。

### Realtime WS

- 入站链路：Netty pipeline -> `RealtimeAccessTokenHandshakeHandler` 写入 MDC 上下文 -> `RealtimeChannelHandler` 首帧 auth/reauth -> session registry -> `RealtimeInboundMessageDispatcher` -> `ChannelMessageRealtimeInboundHandler` -> `ChannelMessagePublishingApi`。
- 出站链路：领域 after-commit publisher -> `NettyMessageRealtimePublisher` / `NettyChannelRealtimePublisher` -> `RealtimeNotificationPreferenceFilter` -> `RealtimeSessionRegistry.appendEvent(...)` -> 在线 channel 写出 `event` frame。
- 结论：
  - WS 默认开启，`application.yaml` 中 `cp.chat.server.realtime.enabled` 默认 `true`。
  - 内置 Netty 未装配 TLS handler，直接端口是明文 `ws://`；生产 WSS 需要网关/代理 TLS 终止。
  - runtime 通过 `SmartLifecycle` 自动启动，`enabled=false` 时跳过绑定。

## 问题清单

### R-001 用户批量公开资料查询仍为全表扫描

- 严重级别：中
- 证据：
  - `UserProfileDomainApi.getPublicUserProfiles(...)` 使用 `userProfileRepository.findAll().stream().filter(...)`。
  - `UserProfileRepository` / `UserProfileDatabaseService` 当前只提供 `findAll()`，没有 `findByAccountIds(...)` 批量查询契约。
- 影响：`GET /api/users?ids=...` 在用户量增大后会变成全表读取 + 内存过滤，影响延迟和数据库/应用内存压力。
- 建议：扩展 `UserProfileRepository` 与 `UserProfileDatabaseService` 批量查询契约，在 database-impl 中落地 `WHERE account_id IN (...)`。

### R-002 HTTP 发送消息请求体缺少 `@NotNull`

- 严重级别：中
- 证据：
  - `ChannelMessageController.sendChannelMessage(...)` 参数为 `@Validated @RequestBody SendChannelMessageRequest requestBody`，未声明 `@NotNull`。
  - 方法内直接访问 `requestBody.domain()`、`requestBody.domainVersion()`、`requestBody.data()`。
  - `MessageController.forwardMessage(...)` 和 `editMessage(...)` 已使用 `@Valid @NotNull`，说明当前发送入口与同类入口不一致。
- 影响：`Content-Type: application/json` 且 body 为 `null` 等边界请求可能绕过字段校验，在 Controller 先触发 NPE，导致错误模型不稳定。
- 建议：改为 `@Valid @NotNull(message = "request body must not be null") @RequestBody SendChannelMessageRequest requestBody`，补 controller 测试。

### R-003 背景图与默认头像 URL 使用无前导斜杠相对路径

- 严重级别：低
- 证据：
  - `UserProfileController.uploadCurrentUserBackground(...)` 返回 `"api/files/download/" + shareKey`。
  - `ServerEntranceDomainApi.DEFAULT_AVATAR` 为 `"api/files/download/server_avatar"`。
  - docs/t 也保留 `api/files/download/server_avatar`。
- 影响：客户端如果按当前页面相对路径解析，可能从 `/api/users/api/files/...` 一类错误路径发起请求；不同客户端对相对 URL 处理不一致。
- 建议：统一为 `/api/files/download/...` 或明确该字段是客户端拼接用 path，不应当直接作为相对 URL。

### R-004 discovery 文档与当前代码的 ws_url scheme 不一致

- 严重级别：中
- 证据：
  - `RealtimeDiscoverySettings.wsUrl()` 当前返回 `"ws://" + host + ":" + port + path`。
  - `docs/API.md` 仍写示例 `wss://127.0.0.1:18080/api/ws`，并说明“当前固定使用 `wss://` scheme”。
  - 代码侧未搜索到 Netty TLS / SSL handler 装配。
- 影响：导入文档或按 discovery 预期测试时，客户端可能尝试 WSS 连接内置明文端口失败；也会混淆“默认开启”和“默认 TLS”的边界。
- 建议：文档改为 `ws://` 或在 discovery 增加可配置外部公开 URL；若要求内置 TLS，需要新增明确 TLS 配置和 Netty `SslHandler` 装配。

### R-005 通知偏好过滤可能拦截状态同步事件

- 严重级别：中 / 待产品确认
- 证据：
  - `RealtimeNotificationPreferenceFilter.FILTERED_EVENT_TYPES` 包含 `read_state.updated` 和 `channel.changed`。
  - `NettyChannelRealtimePublisher.publishReadStateUpdated(...)` 通过 `publishEvent(...)` 进入通知偏好过滤。
  - `channels.changed` 特意走 `publishEventWithoutPreferenceFilter(...)`，说明部分结构同步事件已有绕过过滤的意图。
- 影响：账号在 muted 或 mentions_only 状态下可能收不到 read-state/channel changed 刷新提示，客户端状态同步可能滞后。
- 建议：明确事件分类：通知类事件受偏好过滤，状态同步类事件绕过过滤；补对应 publisher/filter 测试。

### R-006 重复撤回会重复发布 `message.recalled`

- 严重级别：低
- 证据：
  - `ChannelMessageLifecycleDomainApi.recallChannelMessage(...)` 在 `isRecalled(existingMessage)` 时仍构造 `PersistedMessage` 并调用 `publishMessageUpdatedAfterCommit(...)`。
- 影响：接口幂等性可接受，但重复调用会产生重复实时事件和事件缓存记录，客户端可能重复刷新。
- 建议：如果希望撤回幂等且无副作用，已撤回分支直接返回结果，不再发布事件；若当前语义允许重复提示，则在文档和测试中明确。

### R-007 部分路径变量缺少 Controller 层正数校验

- 严重级别：低
- 证据：
  - `ChannelApplicationController` 的 `channelId`、`applicationId` 使用裸 `long`。
  - `ChannelReadStateController.updateReadState(...)` 的 `channelId` 使用裸 `long`。
  - `ChannelPinsController` 的 `channelId`、`messageId` 使用裸 `long`。
  - 领域层会补正数校验，因此不是权限断链。
- 影响：协议层错误归因和 OpenAPI 参数约束不够一致，部分非法参数会到领域层才返回。
- 建议：统一补 `@Validated` 与 `@Positive`，并补 controller 级非法路径变量测试。

## 验证记录

已执行静态检查：

- `rg "infrastructure\\.service\\..*\\.impl|backend\\.starter|application\\.starter|team\\.carrypigeon\\.backend\\.starter" chat-domain/src/main/java chat-domain/src/test/java infrastructure-basic/src/main/java infrastructure-service/*-api/src/main/java -n`
  - 结果：无命中。
- `rg "docs/api/" docs/t -n`
  - 结果：无命中。
- `rg "@RequestBody ..."` 请求体扫描
  - 结果：确认 `NotificationPreferenceController.updateServerNotificationPreference(...)` 未使用 `@Valid/@NotNull`，`ChannelMessageController.sendChannelMessage(...)` 未使用 `@NotNull`。

已执行测试：

```bash
mvn -pl chat-domain -am -Dtest=AuthControllerTests,UserProfileControllerTests,FileControllerTests,ChannelControllerTests,ChannelApplicationControllerTests,ChannelReadStateControllerTests,ChannelMessageQueryControllerTests,MessageForwardControllerTests,ChannelMessageLifecycleDomainApiTests,MessageBusinessChainTests,NotificationPreferenceControllerTests,RealtimeChannelHandlerMessageDispatchTests,RealtimeNotificationPreferenceFilterTests -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：

- Tests run: 105
- Failures: 0
- Errors: 0
- Skipped: 0
- Reactor: SUCCESS

## 残留风险

- 本次为重点链路复审，没有运行全仓库 `mvn test`。
- 工作区已有大量未归档修改，本审查基于当前工作区状态，不代表主分支干净状态。
- R-004、R-005 涉及协议/产品语义，修复前需要确认“内置 ws 明文 + 网关 WSS”是否是正式部署策略，以及通知偏好是否应过滤状态同步事件。

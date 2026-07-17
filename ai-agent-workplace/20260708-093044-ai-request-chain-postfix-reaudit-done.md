# 请求处理链修复后复审任务单

## 任务目标

基于当前工作区，在完成请求链路问题修复后，再次逐调用链检查各 HTTP / WebSocket 请求处理链是否完整、是否存在新增断链、权限绕过、错误映射不一致、文档不一致或测试缺口。

## 任务类型

只读复审。本任务不修改正式源码、测试、SQL、配置或正式 docs；仅允许更新本任务单。

## 影响模块

- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `application-starter`
- `docs/`
- `ai-agent-workplace/`

## 允许修改范围

- 允许新增和更新本任务单。
- 允许归档本任务单为 `done`。

## 禁止修改范围

- 不修改正式源码、测试、SQL、配置或正式 docs。
- 不新增依赖。
- 不改变 Maven 模块结构。
- 不调整对外 API / WS 协议。

## 文档依据

- `AGENTS.md`
- `docs/standards/变更审核清单.md`
- `docs/architecture/架构文档.md`
- `docs/architecture/包结构规范.md`
- `docs/standards/测试规范.md`
- `ai-agent-workplace/20260708-085551-ai-request-chain-reaudit-done.md`
- `ai-agent-workplace/20260708-091045-ai-request-chain-reaudit-fixes-done.md`

## 执行计划

1. 复核刚修复的 R-001 到 R-007 处理链和测试证据。
2. 重新扫描 HTTP / WS 入口、请求体校验、模块依赖边界、旧路径/文档口径。
3. 按 Auth、User、File、Channel、Message、Server/WS 分组检查入口到领域、仓储、基础设施适配和副作用事件。
4. 运行与本轮修复相关的定向测试/编译门禁，记录结果。
5. 汇总是否仍有阻塞问题、非阻塞风险或测试缺口。

## 验收标准

- 每个主要 feature 都有调用链结论。
- R-001 到 R-007 有复核结果。
- 结论必须带文件证据或命令结果。
- 任务单归档为 `done`。

## 复审结果

结论：本轮只读复审未发现新的阻塞级请求处理链断点、权限绕过、模块边界违例或旧下载路径回退。此前 R-001 到 R-007 的修复在当前工作区仍成立。

### 修复项复核

- R-001 用户公开资料批量查询：`UserProfileDomainApi.getPublicUserProfiles(...)` 调用 `UserProfileRepository.findByAccountIds(...)`；`DatabaseBackedUserProfileRepository` 覆盖该方法并下推到 `UserProfileDatabaseService.findByAccountIds(...)`；`MybatisPlusUserProfileDatabaseService` 使用 `LambdaQueryWrapper.in(...)`。生产路径不再全表扫描。
- R-002 频道消息发送请求体：`ChannelMessageController.sendChannelMessage(...)` 已使用 `@Valid @NotNull @RequestBody`。
- R-003 下载路径：服务头像、用户背景、附件 payload 和 `FileShareKeyCodec.downloadPath(...)` 均输出 `/api/files/download/...`。
- R-004 WS 发现协议：`ServerEntranceDomainApi` 经 `RealtimeDiscoverySettings.wsUrl()` 输出内置 Netty 的 `ws://.../api/ws`；生产 TLS/WSS 仍由网关或代理侧承接。
- R-005 实时通知偏好过滤：`RealtimeNotificationPreferenceFilter.FILTERED_EVENT_TYPES` 仅覆盖消息/提及通知类事件，不覆盖 `read_state.updated`、`channel.changed`、`channels.changed`。
- R-006 重复撤回：`ChannelMessageLifecycleDomainApi.recallChannelMessage(...)` 对已撤回消息直接返回 `PersistedMessage`，不再追加审计或再次发布更新事件。
- R-007 正 ID 校验：`ChannelApplicationController`、`ChannelReadStateController`、`ChannelPinsController` 与主要频道/消息控制器均有 `@Validated` 和路径参数 `@Positive`。

### 静态扫描结果

- HTTP 入口扫描：`rg '@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)|@RequestMapping|@RestController' chat-domain/src/main/java/.../features -n`，入口覆盖 Auth、User、File、Channel、Message、Server/Notification、WS。
- 请求体扫描：`rg '@RequestBody' chat-domain/src/main/java/.../features -n -C 1`。强请求体均有 `@Valid`；消息发送、消息编辑、消息转发、服务通知偏好更新有 `@NotNull`；`MentionController.markMentionsRead(...)` 与 `ChannelPinsController.pinChannelMessage(...)` 为协议允许的可选 body，并有空值分支。
- 模块边界扫描：`rg "infrastructure\\.service\\..*\\.impl|backend\\.starter|application\\.starter|team\\.carrypigeon\\.backend\\.starter" chat-domain infrastructure-basic infrastructure-service/*-api -n` 无命中。
- 下载路径扫描：`rg -P '(?<!/api)/files/download|api/files/download/' chat-domain docs infrastructure-service -n` 仅发现 `/api/files/download/...` 口径。
- 批量查询扫描：`rg 'findByAccountIds|findAll\\(\\)' .../user .../database-api .../database-impl -n` 显示领域服务调用 `findByAccountIds`，生产 adapter 覆盖并下推到 database-api；默认 `findAll()` fallback 仅保留为兼容/测试路径。
- 实时事件扫描：`rg 'FILTERED_EVENT_TYPES|read_state\\.updated|channel\\.changed|channels\\.changed|message\\.recalled' chat-domain/src/main/java chat-domain/src/test/java -n` 显示同步类事件有独立发布和测试，未纳入偏好过滤集合。

### 分组调用链结论

- Auth：`AuthController` 的验证码、注册、登录、token 创建、刷新和撤销入口均使用 `@Valid @RequestBody`，业务进入 `AuthAccountApi` / `AuthSessionApi`。邮箱验证码 token 创建先执行 required plugins gate，再进入 `AuthSessionDomainApi`；新账户创建、用户资料初始化、默认频道成员初始化和 refresh session 写入在领域服务与 repository 端口内完成。
- User：`UserProfileController` 登录态读取当前资料、公开资料、批量公开资料、邮箱更新、资料更新和背景图上传。公开资料批量接口经 `UserProfileDomainApi -> UserProfileRepository.findByAccountIds -> UserProfileDatabaseService.findByAccountIds -> MyBatis IN`；背景图上传经 `FileTransferApi.uploadFile(...)`，返回 `/api/files/download/profile_bg_{accountId}`。
- File：`FileController` 上传申请、同源上传和下载均经 `FileTransferDomainApi`。`FileObjectKeyResolver` 对普通上传、服务头像、用户背景图、消息附件分别解析 object key；普通文件和用户背景要求本人访问，附件下载通过 `FileAttachmentAccessAuthorizer` 校验频道访问权，服务头像允许匿名访问。
- Channel：`ChannelController`、`ChannelApplicationController`、`ChannelReadStateController` 分别进入 `ChannelLifecycleApi`、`ChannelGovernanceApi`、`ChannelApplicationFlowApi`、`ChannelAccessApi`、`ChannelQueryApi`。写侧统一走 `TransactionRunner`，通过 channel repository 端口持久化，after-commit 发布 `channel.changed`、`channels.changed`、`read_state.updated`；查询侧对 private/system、成员列表、封禁列表、审计日志做成员或治理权限校验。
- Message：HTTP 发送、历史、搜索、附件上传、撤回、编辑、删除、转发、置顶、提及均经 message 领域 API。发送链路进入 `ChannelMessagePublishingDomainApi`，通过 `MessageChannelBoundary.requireSendableChannel(...)` 校验成员、禁言和发送权限，保存消息与 mention 后 after-commit 发布实时事件。编辑/撤回/删除经 `ChannelMessageLifecycleDomainApi` 校验作者或治理权限；置顶经 `ChannelPinDomainApi` 校验 pin 管理权限和上限；附件上传先校验可发送频道，再通过 storage-api 写入对象。
- Server/WS：`ServerController`、plugin/domain catalog、gate 和 notification preference 链路职责清晰。WS 握手处理器只准备上下文，首帧 `auth/reauth` 才注册 `RealtimeSessionRegistry`；未认证命令返回 unauthorized。WS `send_channel_message` 经 `RealtimeInboundMessageDispatcher -> ChannelMessageRealtimeInboundHandler -> ChannelMessagePublishingApi`，与 HTTP 发送共享领域校验和持久化链路。
- Realtime 发布：`NettyMessageRealtimePublisher` 和 `NettyChannelRealtimePublisher` 只负责事件封装、缓存和通道推送，不参与业务鉴权。通知偏好过滤只处理消息/提及通知事件，同步事件绕过滤器或因不在过滤集合中直接放行。

### 验证记录

本轮为只读复审，没有修改正式源码，未新增测试。本轮未重新执行 Maven 测试；采用当前源码静态证据和上一修复闭环的测试结果作为门禁证据。

上一修复闭环已通过：

- `mvn -pl chat-domain,infrastructure-service/database-impl -am -Dtest=DatabaseBackedUserProfileRepositoryTests,MybatisPlusUserProfileDatabaseServiceTests,UserProfileDomainApiTests,ChannelMessageQueryControllerTests,NotificationPreferenceControllerTests,ChannelMessageLifecycleDomainApiTests,RealtimeNotificationPreferenceFilterTests,ServerControllerTests,ServerEntranceDomainApiTests,MessageAttachmentPayloadResolverTests,NettyMessageRealtimePublisherTests -Dsurefire.failIfNoSpecifiedTests=false test`
  - 结果：`chat-domain` 67 tests passed，`database-impl` 12 tests passed，BUILD SUCCESS。
- `mvn -pl chat-domain,infrastructure-service/database-impl -am -Dtest=AuthControllerTests,UserProfileControllerTests,DatabaseBackedUserProfileRepositoryTests,UserProfileDomainApiTests,FileControllerTests,ChannelControllerTests,ChannelApplicationControllerTests,ChannelReadStateControllerTests,ChannelPinsControllerTests,ChannelMessageQueryControllerTests,MessageForwardControllerTests,ChannelMessageLifecycleDomainApiTests,MessageBusinessChainTests,NotificationPreferenceControllerTests,RealtimeChannelHandlerMessageDispatchTests,RealtimeNotificationPreferenceFilterTests,ServerControllerTests,ServerEntranceDomainApiTests,MessageAttachmentPayloadResolverTests,NettyMessageRealtimePublisherTests,MybatisPlusUserProfileDatabaseServiceTests -Dsurefire.failIfNoSpecifiedTests=false test`
  - 结果：`chat-domain` 149 tests passed，`database-impl` 12 tests passed，BUILD SUCCESS。
- `mvn -pl application-starter -am -DskipTests compile`
  - 结果：13 个 reactor modules 编译通过，BUILD SUCCESS。

### 非阻塞残留风险

- 本轮只读复审没有重新跑全量 `mvn test`，原因是未修改正式源码且上一闭环已运行覆盖当前修复点的定向和较宽核心链路测试。
- `UserProfileRepository.findByAccountIds(...)` 与 `UserProfileDatabaseService.findByAccountIds(...)` 默认实现仍使用 `findAll()` 作为兼容 fallback；生产适配器已覆盖，不构成本次请求链路阻塞。
- `MessageAttachmentPayloadResolver` 仍保留未参与当前输出路径的对象存储 provider 构造参数/字段，属于代码清理项，不影响当前附件输出和下载链路。

### 最终结论

通过，带非阻塞风险。当前工作区未发现需要立即修复的请求处理链问题；若后续继续提高质量，建议单独开任务清理无用依赖字段并执行全量 `mvn test`。

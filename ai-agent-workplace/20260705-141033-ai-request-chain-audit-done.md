# 请求处理链审计任务单

## 任务目标

逐调用链检查当前 HTTP 与 WebSocket 请求处理链是否完整、是否存在明显 bug、是否存在协议/持久化/鉴权/事务/测试覆盖缺口。

## 任务类型

探索 / 审查类任务。默认不修改正式业务代码；如发现确定性缺陷，先记录证据与影响，再由用户确认是否进入修复任务。

## affected modules

- `application-starter`
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `docs`
- `ai-agent-workplace`

## 允许修改范围

- 本任务单。
- 如仅为完成审计所需，可读取源码、测试、SQL、文档和 Maven 配置。

## 禁止边界

- 不直接修复业务代码。
- 不新增依赖。
- 不改变模块结构或架构规则。
- 不把审计结论直接写入长期 `docs/`，除非后续用户确认。

## governing docs

- `AGENTS.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/变更审核清单.md`
- `docs/AI协作开发规范.md`

## 审计方法

- 枚举 HTTP Controller 与 WS Handler。
- 按 feature 追踪：入口 DTO / 鉴权 / command-query / domain api / repository or external service / response / side effect。
- 对照测试覆盖、SQL 表结构与 database-api/impl 映射。
- 优先报告有源码证据的确定性问题，其次报告高风险缺口。

## acceptance criteria

- 按 feature 输出调用链覆盖概览。
- 列出确定性 bug 或风险，包含文件/行号证据。
- 明确说明未能完全证明无 bug 的边界。
- 记录是否运行编译或测试；若未运行，说明原因。

## audit result

### 覆盖范围

- HTTP：auth、user、file、channel、message、server / notification preference。
- WebSocket：Netty realtime runtime、首帧 auth / reauth、send_channel_message 入站命令、message/channel realtime publisher。
- 持久化：抽样核对 database-api、database-impl MyBatis service / mapper、docs/sql 表结构。

### 确定性问题

1. 审计日志读取没有频道权限校验。
   - 入口：`AuditLogController.listAuditLogs` 只取当前账号并直接调用 `channelQueryDomainApi.listAuditLogs`。
   - 领域：`ChannelQueryDomainApi.listAuditLogs` 只校验 accountId / cursor / limit / filter，不校验调用方是否为目标频道成员或管理员；未传 cid 时可读取全局审计日志。
   - 影响：任意登录用户可能读取任意频道或全局审计日志。

2. 私有频道按 ID 查询缺少成员可见性校验。
   - `ChannelQueryDomainApi.getChannelById` 只对 system 频道检查 membership，private 频道未校验 membership。
   - 影响：只要知道频道 ID，登录用户可读取 private 频道摘要信息。

3. 入群申请 reason 没有持久化，创建后查询 / 审批会丢失。
   - `ChannelApplicationController.createApplication` 接收 `request.reason()`。
   - `ChannelApplicationFlowDomainApi.createChannelApplication` 仅在当次响应 `toApplicationResult(persistedInvite, command.reason())` 中回显。
   - `listChannelApplications` / `decideChannelApplication` 均使用空 reason。
   - `ChannelInvite`、`ChannelInviteRecord`、`ChannelInviteEntity`、`ChannelInviteMapper`、`docs/sql/03-channel.sql` 的 `chat_channel_invite` 均没有 reason 字段。

4. discovery 默认返回 `wss://`，但内置 Netty pipeline 未装配 TLS。
   - `RealtimeDiscoverySettings.wsUrl()` 固定拼接 `wss://`。
   - `RealtimeChannelInitializer` pipeline 只有 HTTP codec、aggregator、handshake、idle、WebSocket handler、业务 handler，无 `SslHandler`。
   - 影响：直接连接内置 18080 端口时不是 TLS；真实 WSS 只能依赖前置代理 / 网关。

5. 消息转发未校验操作者是否可见源消息。
   - 入口：`MessageController.forwardMessage` 只把当前账号、源消息 ID、目标频道 ID 传入 `ForwardChannelMessageCommand`。
   - 领域：`ChannelMessagePublishingDomainApi.forwardChannelMessage` 先 `requireMessage(sourceMessageId)`，随后只对 `targetChannelId` 调用 `requireMemberChannel`，没有对 `sourceMessage.channelId()` 校验调用方成员身份或读取权限。
   - 文档：`docs/t/SERVER_API.md` 的转发错误表包含“不允许转发该消息”应返回 `403/message_forward_forbidden`。
   - 测试缺口：`MessageBusinessChainTests.forwardMessage_invalidSourceOrTarget_returnsExpectedErrors` 覆盖了源消息不存在与目标频道非成员；`ChannelMessagePublishingDomainApiForwardTests.forwardChannelMessage_createsNewTargetChannelMessage` 甚至构造了操作者不在源频道、但可转发成功的场景。
   - 影响：只要知道源消息 ID，登录用户可能把自己不可见频道的消息摘要转发到自己所在频道，泄漏 `preview`、源频道 ID、发送人 ID 等来源摘要。
   - 建议修复方向：转发前对源消息所属频道执行读取权限校验；若不允许，返回文档约定的 `message_forward_forbidden` 或统一的 forbidden reason，并补充源频道非成员测试。

6. 消息硬删除会被置顶 / mention 外键阻断，调用链没有先清理依赖。
   - 入口：`MessageController.deleteMessage` 调用 `ChannelMessageLifecycleDomainApi.deleteChannelMessage`。
   - 领域：`deleteChannelMessage` 校验权限后直接 `messageRepository.delete(existingMessage.messageId())`。
   - SQL：`docs/sql/04-message.sql` 中 `chat_mention.message_id` 外键引用 `chat_message(message_id)`，`chat_channel_pin.message_id` 外键也引用 `chat_message(message_id)`，均未声明 `ON DELETE CASCADE`。
   - Mapper：`MentionMapper` 没有按消息删除 mention 的方法；`ChannelPinMapper` 只有按 `channel_id + message_id` 删除单个 pin 的方法，但删除消息链路没有调用。
   - 影响：删除已经被 mention 或被置顶的消息时，数据库会因外键约束拒绝删除，最终通过全局异常处理映射为 500，而不是业务上可理解的删除成功、冲突或先解除关联。
   - 建议修复方向：优先明确“删除消息”语义是物理删除还是撤回式软删除；若保留物理删除，需要在事务内清理 pins / mentions 或调整外键级联，并补充真实持久化集成测试。

7. WebSocket 入站命令部分非法字段会被映射成 `internal_error`，不是客户端可修正的校验错误。
   - `RealtimeClientMessage.longValue` 对非数字字符串直接 `Long.parseLong`，例如 `channel_id:"abc"` 会抛 `NumberFormatException`。
   - `RealtimeClientMessage.payload` / `metadata` / `resume` 直接把 `data` 字段强转为 `Map<String,Object>`，当客户端传非对象 payload 时会抛 `ClassCastException`。
   - `RealtimeChannelHandler.channelRead0` 只把 `ProblemException` 映射成业务错误；这些运行时异常会进入兜底 `RuntimeException` 分支，返回 `internal_error`。
   - 影响：Apifox / 客户端调试 WS 推送与发消息时，字段类型错误会表现为服务端内部错误，错误码语义不稳定，也会污染服务端告警。
   - 建议修复方向：让 envelope 解析层把类型错误转换为 `ProblemException.validationFailed`，并补充 malformed `send_channel_message` 帧的 WS 契约测试。

8. 认证请求体的 `client.device_id` 被要求填写，但实现没有使用或持久化。
   - DTO：`CreateTokenSessionRequest.ClientRequest.deviceId`、`RefreshAccessTokenRequest.ClientRequest.deviceId`、`RevokeRefreshTokenRequest.ClientRequest.deviceId` 都是必填。
   - Controller：`AuthController.createTokenSession` 只用 `client.installedPlugins` 做 required plugin gate，调用 `CreateTokenSessionCommand(request.grantType(), request.email(), request.code())`；`refresh` 与 `revoke` 只传 refresh token。
   - Domain / persistence：`CreateTokenSessionCommand`、`RefreshTokenCommand`、`LogoutCommand`、`AuthRefreshSession`、`AuthRefreshSessionRecord`、`AuthRefreshSessionEntity` 和 `docs/sql/01-auth.sql` 的 `auth_refresh_session` 都没有 device id 字段。
   - 影响：当前协议文档注释写有“用于校验 refresh token 归属 / 定位会话”的语义，但实际只按 refresh token hash 与 session id 校验；客户端填错 device id 也不会影响刷新 / 撤销。
   - 建议修复方向：二选一收敛契约：要么删除 / 降级 device id 语义，只作为未来字段；要么把 device id 纳入 refresh session 模型、表结构、签发、刷新和撤销校验链路。

9. `GET /api/users/{accountId}` 的 OpenAPI 注释与实际公开资料语义不一致。
   - Controller 注释：`UserProfileController.getByAccountId` 的 `@Operation` 描述为“当前实现仅允许访问当前登录账户自己的资料”。
   - 实现：方法只调用 `authRequestContext.requirePrincipal(request)`，没有比较当前账号与 path `accountId`，随后直接查询任意账号公开资料。
   - 文档 / 测试：`docs/t/11-http-endpoints-v1.md` 和 `docs/API.md` 都描述为获取用户公开资料；`UserProfileControllerTests.getByAccountId_authenticatedRequest_returnsPublicProfile` 也按公开资料查询验证。
   - 影响：导入 OpenAPI 后会误导客户端和测试人员；若产品语义是“只能查自己”，当前实现存在隐私越权；若产品语义是“公开资料”，则 OpenAPI 描述错误。
   - 建议修复方向：先确认产品语义；若是公开资料，修正文档和 OpenAPI 注释；若是私有资料，补权限校验和反向测试。

10. 置顶列表入口对非法 `limit` 缺少入口层约束，分页切片对空页不安全。
   - 入口：`ChannelPinsController.listChannelPins` 直接接收 `@RequestParam(defaultValue = "20") int limit`，没有 `@Min/@Max` 或本地 normalize；随后使用 `items.size() > limit`、`items.subList(0, limit)` 和 `pageItems.get(pageItems.size() - 1)` 组装游标。
   - 领域：`ChannelPinDomainApi.listChannelPins` 会校验 `limit <= 0 || limit > 50`，真实调用通常能兜底返回 422。
   - 不一致点：同类 `MentionController` 在 Controller 层执行 `normalizeLimit` 并有 `limit=0` 的 422 测试；`ChannelPinsControllerTests` 只覆盖正常 `limit=20` 和游标。
   - 风险：Controller 单测 mock 或未来 API 实现返回非空结果时，`limit=0` 会让 `hasMore=true`、`pageItems` 为空，再取最后元素导致 500；协议层错误体验也与其它分页接口不一致。
   - 建议修复方向：在 Controller 层统一 `limit` validation/normalize，或者把分页组装封装成空页安全的共享方法，并补充 `limit=0` / `limit=51` 契约测试。

11. 按绝对时间禁言时，过去的 `until` 会被静默转换为 1 秒禁言。
   - DTO：`BanChannelMemberV1Request.until` 只有 `@Positive`，不能表达“必须晚于当前时间”。
   - 领域：`ChannelGovernanceDomainApi.banChannelMemberUntil` 使用 `Math.max(1L, (until - timeProvider.nowMillis()) / 1000L)`，过去时间或非常接近当前时间都会变成 1 秒 duration。
   - 测试：`ChannelDomainApiGovernanceTests.banChannelMemberUntil_epochMillis_usesTimeProvider` 只覆盖未来时间转换。
   - 文档：`docs/t/11-http-endpoints-v1.md` 只说明 `until` 是 epoch 毫秒，没有说明过去时间会被当成 1 秒禁言。
   - 影响：客户端传错时间不会得到明确校验错误，管理员以为设置了指定截止时间，实际效果变成短暂禁言。
   - 建议修复方向：在领域中校验 `until > now`，或在文档和响应中明确过去时间的折算规则；优先建议作为校验错误处理。

12. 未读统计 SQL 会把已撤回消息继续计为未读，语义与“撤回后内容不可见/不再搜索”边界不清。
   - SQL：`ChannelReadStateMapper.listUnreadsByAccountId` 只按 `message_id > last_read_message_id` 和 `sender_id != account_id` 计数，没有排除 `status = 'recalled'`。
   - 领域：`ChannelMessageLifecycleDomainApi.recallChannelMessage` 会把消息更新为 `recalled`，`AbstractMessageDomainSupport.toRecalledMessage` 清空可搜索内容并保留原 `message_id`。
   - 文档：`docs/产品需求文档.md` 明确撤回采用软撤回、保留 `message_id`、使用占位文案、不再参与搜索；`GET /api/unreads` 文档只给出未读数量示例，未定义撤回消息是否继续计未读。
   - 影响：如果业务语义是“撤回后不应制造未读”，当前 SQL 会让被撤回但未读的消息仍出现在未读数中；如果语义是“撤回占位也需要同步给用户”，则文档和测试需要明确。
   - 建议修复方向：先确认产品语义；若撤回不计未读，SQL 增加 `m.status != 'recalled'` 并补充 MySQL/Mapper 集成测试；若继续计未读，在协议文档写明。

13. 删除 / 撤回消息的 HTTP 语义、实时事件和插件事件声明不一致。
   - HTTP 文档：`docs/t/11-http-endpoints-v1.md` 把 `DELETE /api/messages/{mid}` 定义为“硬删除=消失”，并要求推送 `message.deleted`。
   - WS 文档：`docs/t/12-ws-events-v1.md` 把 `message.deleted` 定义为“消息被硬删除（删除=消失）”。
   - 实现：`ChannelMessageLifecycleDomainApi.deleteChannelMessage` 物理删除记录后构造 `toRecalledMessage(existingMessage)` 交给 `publishMessageUpdatedAfterCommit`；`NettyMessageRealtimePublisher.publishUpdate` 对任何 `status = recalled` 都下发 `message.deleted`。
   - 撤回实现：`recallChannelMessage` 也是把消息状态改为 `recalled` 后通过同一 `publishUpdate` 分支下发 `message.deleted`。
   - 插件声明：`MessageTypePluginRegistrationSupport` 声明支持事件包含 `message.recalled`，但当前 realtime publisher 不会下发该事件；测试名写“updated event type”，断言却是 `message.deleted`。
   - 影响：客户端 / Apifox WS 自动化无法仅凭事件类型区分硬删除和软撤回；插件目录暴露的事件能力与真实 WS 事件不一致。
   - 建议修复方向：统一协议枚举：要么撤回改发 `message.recalled` / `message.updated` 并保留硬删除的 `message.deleted`，要么文档明确 `message.deleted` 同时覆盖“本地移除/隐藏”的撤回语义，并修正插件事件声明。

14. 邮箱验证码成功校验后的消费不是原子操作，存在并发重复使用窗口。
   - 缓存实现：`CacheBackedEmailVerificationCodeService.verifyCode` 先 `cacheService.get(cacheKey(email))`，匹配后再 `cacheService.delete(cacheKey(email))`。
   - 内存实现：`InMemoryEmailVerificationCodeService.verifyCode` 先从 `ConcurrentHashMap` 读取 entry，匹配后再 `issuedCodes.remove(normalizedEmail)`。
   - 缓存端口：`CacheService` 只暴露 `get` / `set` / `delete` / `exists`，没有 get-and-delete 或 compare-and-delete 语义，Redis 实现无法在当前端口下表达一次性验证码的原子消费。
   - 调用链：`AuthSessionDomainApi.createTokenSession` 在创建账号 / refresh session 事务前调用 `emailVerificationCodeService.verifyCode`；`UserProfileController.updateCurrentUserEmail` 更新邮箱前也直接调用该校验。
   - 影响：同一邮箱和验证码的并发请求可能同时通过读取校验，再分别进入创建 token session 或邮箱更新流程；这破坏“一次性验证码只能成功一次”的安全语义。
   - 建议修复方向：在 cache-api 增加原子消费能力，Redis 侧用 Lua / GETDEL / compare-and-delete 表达；内存实现用 `compute` / 条件 remove 合并校验与删除；补充并发双请求契约测试。

15. 用户资料分页 / 搜索领域 API 没有使用已有仓储分页 / 搜索能力，且文档仍保留未暴露的旧 HTTP 接口。
   - 领域实现：`UserProfileDomainApi.getUserProfiles` 只调用 `userProfileRepository.findByAccountId(query.accountId())`，再对当前账号资料做游标过滤；不会返回其它用户资料。
   - 搜索实现：`UserProfileDomainApi.searchUserProfiles` 同样只查询当前账号资料，再在内存中匹配关键字。
   - 仓储能力：`UserProfileRepository` 已定义 `findByAccountIdBefore` 和 `searchByKeyword`；`DatabaseBackedUserProfileRepository` 也已接入 `UserProfileDatabaseService.findByAccountIdBefore` / `searchByKeyword`。
   - 测试现状：`UserProfileDomainApiTests.getUserProfiles_withCursor_returnsPageResult` 和 `searchUserProfiles_keyword_returnsPageResult` 构造了多个用户资料，但断言只返回当前账号，测试固化了当前错误行为。
   - HTTP 暴露：当前 `UserProfileController` 只暴露 `/api/users/me`、`/api/users/{accountId}`、`GET /api/users?ids=...`、邮箱/资料/背景更新；没有 `GET /api/users/page` 和 `GET /api/users/search`。
   - 文档不一致：`docs/API.md` 仍写“以下旧接口当前仍保留”：`GET /api/users/page`、`GET /api/users/search`、`PUT /api/users/me`，其中分页和搜索端点在当前 Controller 中不存在。
   - 影响：如果后续重新挂载分页 / 搜索 HTTP 入口，会直接得到只返回当前用户的错误分页 / 搜索结果；当前文档也会误导 Apifox / 客户端按不存在接口测试。
   - 建议修复方向：先确认是否保留用户列表 / 搜索能力；若保留，领域层改用仓储分页 / 搜索方法并恢复 HTTP/OpenAPI 契约；若不保留，删除领域 API 和文档中的过渡接口说明，避免形成假能力。

### 高风险缺口

- 通知偏好当前只支持落库和查询，未接入 `NettyMessageRealtimePublisher` / `NettyChannelRealtimePublisher` 的实时事件投递过滤；静音用户仍会收到 realtime event。
- 文件通用上传强依赖 `Content-Length` 且要求与 share key 声明大小完全一致；chunked / unknown length 上传会失败。
- refresh token 轮换基于 refresh token claims 构造账号对象签发新 token，没有回读当前账号状态；当前无禁用账号模型时不是现有 bug，但后续若引入账号禁用 / 删除会形成绕过风险。
- HTTP access token 和 WS access token 都是纯 stateless 校验，不感知 refresh session revocation；这是当前边界而非实现 bug。
- 批量用户公开资料查询 `UserProfileDomainApi.getPublicUserProfiles` 通过 `userProfileRepository.findAll()` 后在内存中过滤，数据量增大后会形成明显性能风险；建议 repository / database-api 提供按 account id 集合查询。
- 频道删除安全检查没有显式检查 `chat_channel_read_state`。当前如果频道无消息但存在读状态，会在实际删除时被外键阻断并统一转成 `channel_delete_blocked`，业务结果可接受但错误定位不精确；建议把 read state 纳入删除安全检查或明确外键级联策略。
- 部分 Controller 的 `@RequestBody` 未统一使用 `@Valid`，例如 `MessageController.forwardMessage` / `editMessage`、`NotificationPreferenceController.updateServerNotificationPreference`。多数缺口当前由领域校验兜底，但协议层字段错误明细不一致，OpenAPI / Apifox 自动化测试体验会受影响。
- `ChannelController.discoverChannels`、`AuditLogController.listAuditLogs` 和 `ChannelPinsController.listChannelPins` 都采用“领域返回 limit+1，Controller 按原始 limit 切片并取最后元素生成 cursor”的模式。领域层当前有 limit 校验兜底，但 Controller 层没有统一 Bean Validation；建议后续统一分页入口约束与空页安全处理。

### 再次补扫新增记录（20260705-160535）

- 已补扫 Maven / Java import 的模块依赖方向，未发现明确的 `chat-domain -> *-impl`、`*-api -> chat-domain`、`infrastructure-basic -> chat-domain` 等硬依赖违规。
- 已补扫文件下载 / 分享 key 链路：普通文件 key 绑定上传者，消息附件 key 绑定频道成员可见性，`server_avatar` 匿名读取属于显式例外；本轮未新增确定性问题。
- 已补扫消息插件构建链路：text / file / voice 插件均有基本 payload、object key 和对象存在性校验；本轮未新增确定性问题。
- 已补扫置顶列表、频道发现、审计日志的分页入口；新增记录 `ChannelPinsController` 的 limit 入口层不一致问题，其余同类入口由领域校验兜底但仍建议统一协议层约束。
- 已补扫频道治理禁言链路；新增记录过去 `until` 被折算为 1 秒禁言的问题。
- 已补扫未读统计 Mapper 与消息撤回状态；新增记录撤回消息是否计入未读的语义/SQL 缺口。
- 已补扫删除 / 撤回消息的 HTTP 文档、WS 文档、realtime publisher 和插件事件声明；新增记录事件命名与语义不一致问题。

### 再次补扫新增记录（20260705-163440）

- 已补扫认证会话与验证码实现：`AuthSessionDomainApi.createTokenSession`、`UserProfileController.updateCurrentUserEmail`、`CacheBackedEmailVerificationCodeService`、`InMemoryEmailVerificationCodeService`、`CacheService`。新增记录验证码消费非原子导致并发重复使用窗口。
- 已补扫用户资料领域 API、仓储端口、database-api 适配器、Controller 暴露面和 `docs/API.md`。新增记录用户资料分页 / 搜索领域实现绕过已有分页搜索仓储能力，且文档保留未暴露旧接口。
- 已补扫 realtime registry / publisher / inbound dispatcher：`RealtimeChannelHandler`、`RealtimeSessionRegistry`、`NettyMessageRealtimePublisher`、`NettyChannelRealtimePublisher`、`ChannelMessageRealtimeInboundHandler`。除已记录 malformed frame 映射、TLS discovery 和删除 / 撤回事件语义问题外，本轮未发现新的确定性 realtime 调用链缺陷。
- 已补扫事务后置发布相关入口：消息发送 / 更新 / 删除和频道读状态发布均通过 `TransactionRunner` after-commit 触发，未发现新的“事务回滚后仍推送”确定性问题。

### 本轮补扫记录

- 已重新枚举 16 个 HTTP Controller：
  - `AuthController`
  - `AuditLogController`
  - `ChannelApplicationController`
  - `ChannelController`
  - `ChannelReadStateController`
  - `FileController`
  - `ChannelMessageController`
  - `ChannelPinsController`
  - `MentionController`
  - `MessageController`
  - `NotificationPreferenceController`
  - `ServerController`
  - `ServerDomainCatalogController`
  - `ServerGateController`
  - `ServerPluginCatalogController`
  - `UserProfileController`
- 已补扫认证会话链路：public auth route 放行、access token 拦截器、refresh / revoke command、refresh session model / SQL。
- 已补扫消息持久化链路：send / edit / recall / delete / forward、mention 生成、pin 生成、message / mention / pin SQL 外键。
- 已补扫 WS 入站链路：`RealtimeChannelHandler`、`RealtimeClientMessage`、`ChannelMessageRealtimeInboundHandler`、现有 WS dispatch 测试。
- 已补扫异常映射：`GlobalExceptionHandler` 对 `ProblemException`、Bean Validation、不可读请求体和未捕获异常的 HTTP 映射。
- 已反查相关测试：消息转发、WS dispatch、用户资料 Controller、认证 token flow、持久化 contract tests。

### 优先级建议

1. P0：修复审计日志权限、私有频道可见性、消息转发源频道权限。
2. P0：明确消息删除语义，处理 mention / pin 外键阻断。
3. P1：修复入群申请 reason 持久化。
4. P1：收敛 WS discovery TLS 语义，明确直连 `ws://` 或引入 TLS / 代理配置。
5. P1：修复 WS malformed frame 错误映射。
6. P1：收敛认证 `device_id` 契约与实现。
7. P1：统一删除 / 撤回消息的 WS 事件语义，修正插件事件声明或事件下发实现。
8. P1：修复禁言 `until` 过去时间的静默折算问题。
9. P1：修复邮箱验证码原子消费，避免一次性验证码并发重复使用。
10. P2：通知偏好接入实时投递过滤。
11. P2：用户公开资料接口文档 / OpenAPI 语义对齐。
12. P2：收敛用户资料分页 / 搜索领域能力与 HTTP / 文档暴露面。
13. P2：批量用户资料查询与 Controller validation 一致性优化。
14. P2：确认撤回消息是否计入未读，并按确认结果调整 SQL / 文档 / 测试。

### 测试记录

- 首次运行 `mvn -pl chat-domain -Dtest='ChannelDomainApiApplicationFlowTests,AuditLogDomainApiTests,ChannelDomainApiAccessTests,RealtimeChannelHandlerMessageDispatchTests' -DskipTests=false test` 失败：未带 `-am`，依赖模块未解析。
- 第二次运行 `mvn -pl chat-domain -am -Dtest='ChannelDomainApiApplicationFlowTests,AuditLogDomainApiTests,ChannelDomainApiAccessTests,RealtimeChannelHandlerMessageDispatchTests' -DskipTests=false test` 失败：依赖模块没有匹配测试，Surefire 默认报错。
- 最终运行 `mvn -pl chat-domain -am -Dtest='ChannelDomainApiApplicationFlowTests,AuditLogDomainApiTests,ChannelDomainApiAccessTests,RealtimeChannelHandlerMessageDispatchTests' -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false test` 成功：26 tests, 0 failures, 0 errors。
- 补扫阶段未新增运行测试；本轮目标是问题整理落档，未改业务代码。

### 未覆盖边界

- 未运行全量 `mvn test`。
- 未对真实 MySQL / Redis / MinIO 环境执行端到端请求。
- 本轮为只读审计，未修复业务代码。
- 未证明所有第三方客户端协议文档均与当前 OpenAPI 一致；只抽查了与本轮问题相关的 `docs/t/11-http-endpoints-v1.md`、`docs/t/SERVER_API.md`、`docs/API.md`。

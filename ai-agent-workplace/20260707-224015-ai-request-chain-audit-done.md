# 请求处理链逐链路审查任务单

## 任务目标

逐调用链检查当前项目各类 HTTP / WebSocket 请求从入口、鉴权、参数转换、领域服务、仓储端口、基础设施适配器到副作用事件的处理链是否完整，识别断链、漏鉴权、漏持久化、漏事件、错误响应不一致、模块边界越界和测试缺口。

## 任务类型

只读探索 / 审查任务。本任务默认不修改正式代码；若发现问题，先记录证据和影响，不直接扩展为实现类任务。

## 影响模块

- `application-starter`
- `chat-domain`
- `infrastructure-service/*-api`
- `infrastructure-service/*-impl`
- `docs/`
- `ai-agent-workplace/`

## 允许修改范围

- 允许新增和更新本任务单。
- 允许在 `ai-agent-workplace/` 追加审查记录。

## 禁止修改范围

- 不修改正式 Java 源码、测试、SQL、配置或正式 docs。
- 不新增依赖。
- 不改变 Maven 模块结构。
- 不改变对外 API / WS 协议。
- 不修改数据库模型。

## 文档依据

- `AGENTS.md`
- `docs/standards/AI协作开发规范.md`
- `docs/standards/变更审核清单.md`
- `docs/architecture/架构文档.md`
- `docs/architecture/包结构规范.md`
- `docs/architecture/依赖引入规范.md`
- `docs/standards/异常与错误码规范.md`
- `docs/standards/测试规范.md`
- `docs/api/API.md`
- `docs/api/*.md`
- `docs/t/*.md`

## 执行计划

1. 盘点 HTTP Controller、WS Handler、OpenAPI / docs API 清单和测试覆盖面。
2. 按 feature 拆分调用链：auth、user、channel、message、file、server/realtime。
3. 对每条链路检查入口鉴权、请求 DTO、领域命令、领域服务、仓储端口、基础设施适配器、副作用事件和测试。
4. 交叉检查 API 文档 / OpenAPI 与实际入口是否一致。
5. 交叉检查 `chat-domain` 是否直接依赖 impl / starter，`application-starter` 是否承担业务逻辑。
6. 汇总问题，按阻塞 / 高 / 中 / 低分类，记录证据文件和建议修复方向。
7. 记录只读审查验证方式和残留风险，归档任务单。

## 质量门禁

- 审查结论必须有仓库内文件证据支撑。
- 每个主要 feature 至少覆盖入口、领域服务、仓储/适配器和测试四个层面。
- 明确说明本任务无正式代码改动，因此不执行实现类测试门禁。

## 验收标准

- 给出逐调用链审查结论。
- 明确列出发现的问题、影响和建议修复优先级。
- 明确说明未发现问题的链路和审查依据。
- 任务单归档为 `done`。

## 审查结果

### 逐链路覆盖

- Auth 链路：覆盖 `AuthController`、会话/令牌领域 API、访问令牌拦截器、全局异常映射和 required gate 相关测试。未发现新的断链问题。
- User 链路：覆盖 `UserProfileController`、`UserProfileDomainApi`、用户资料仓储适配器和控制器/领域测试。发现批量公开资料查询的性能风险。
- File 链路：覆盖 `FileController`、`FileTransferDomainApi`、对象存储端口和文件上传/下载测试。发现领域校验异常类型不一致风险。
- Channel 链路：覆盖频道资料、成员、治理、申请/邀请、已读、审计日志、频道通知偏好入口，及对应领域服务、仓储适配器和测试。发现申请审批时缺少审批瞬间的封禁复核。
- Message 链路：覆盖历史/搜索/发送/附件/编辑/删除/转发/置顶/mention 入口，消息发布/生命周期/时间线/置顶/mention 领域服务，频道边界适配器、持久化适配器和业务链路测试。发现转发绕过发送治理、部分请求体校验缺口、撤回 HTTP 入口文档不一致。
- Server / WS 链路：覆盖服务发现、插件/领域目录、通知偏好、Netty WS 首帧鉴权、ping、resume、入站命令分发、消息/频道实时发布器和通知偏好过滤器。实现链路整体可闭合；发现 WS 文档引用路径错误，入站 `send_channel_message` 命令缺少明确文档化协议。
- 模块边界：执行 `rg "infrastructure\\.service\\..*\\.impl|backend\\.starter|application\\.starter|team\\.carrypigeon\\.backend\\.starter"` 覆盖 `chat-domain/src/main/java`、`chat-domain/src/test/java`、`infrastructure-basic/src/main/java`、`infrastructure-service/*-api/src/main/java`，未发现 `chat-domain` 直依 impl / starter 或 api 反向依赖 starter 的文本命中。

### Findings

#### 高：转发消息绕过目标频道发送治理

- 链路：`POST /api/messages/{messageId}/forward` -> `MessageController.forwardMessage` -> `ChannelMessagePublishingDomainApi.forwardChannelMessage` -> `MessageChannelBoundary`。
- 证据：普通发送在 `ChannelMessagePublishingDomainApi` 第 80、210、283 行都使用 `requireSendableChannel(...)`，会进入频道治理策略；转发链路第 166-167 行只检查源频道和目标频道 membership，并未调用 `requireSendableChannel(...)`。
- 影响：被禁言成员无法通过普通 HTTP / WS 发送消息，但仍可能通过转发接口向目标私有频道创建新消息，形成权限绕过。
- 建议：目标频道改为 `requireSendableChannel(command.targetChannelId(), command.accountId())`，并增加“muted member forward returns forbidden / user_muted”的领域或业务链路测试。

#### 高：入群申请审批未复核被审批人的当前封禁状态

- 链路：`POST /api/channels/{channelId}/applications/{applicationId}/decision` -> `ChannelApplicationController` -> `ChannelApplicationFlowDomainApi.decideChannelApplication` -> 成员仓储。
- 证据：创建邀请/申请路径在 `ChannelApplicationFlowDomainApi` 第 86-89 行等位置调用 `channelGovernancePolicy.requireBanInactive(...)`；审批路径第 247-290 行在 approve 时直接保存成员，第 268-270 行未对 `invite.inviteeAccountId()` 当前封禁状态进行复核。
- 影响：用户申请后若被封禁，管理员后续批准旧申请仍可把该用户加入频道，绕过频道封禁策略。
- 建议：approve 分支保存成员前按 `channelId + inviteeAccountId` 查询 ban 并调用 `requireBanInactive(...)`，补充“申请后被封禁再审批应失败”的测试。

#### 中：消息转发和编辑入口请求体缺少入口级校验

- 链路：`POST /api/messages/{messageId}/forward`、`PATCH /api/messages/{messageId}` -> `MessageController`。
- 证据：`MessageController` 第 99 行、125 行均为裸 `@RequestBody`，未使用 `@Valid` / `@Validated` / `@NotNull`；第 106-107 行、第 132-136 行直接访问 `body` 字段。DTO 内虽有 `@NotBlank`，但入口未触发 Bean Validation。
- 影响：空 JSON、`null` 请求体或缺字段可能以 NPE 进入全局兜底异常，返回 500；非法字段也可能绕过标准 `validation_failed` 细节。
- 建议：为两个参数增加 `@Valid` 和显式非空处理，或在控制器首行统一判空抛 `ProblemException.validationFailed(...)`；补充空体、`target_cid` 空、`domain/domain_version` 空的 MockMvc 测试。

#### 中：文档声明撤回 HTTP 接口，但当前没有 Controller 映射

- 链路：文档 `/api/channels/{cid}/messages/{mid}/recall` -> 实际 HTTP Controller。
- 证据：`docs/api/API.md` 第 650-656 行声明 `POST /api/channels/{cid}/messages/{mid}/recall`；源码搜索 `recall|/recall|RecallChannelMessage` 仅发现领域服务 `ChannelMessageLifecycleDomainApi.recallChannelMessage(...)` 和领域测试，没有任何 `@PostMapping` / Controller 映射。
- 影响：按文档或 Apifox 导入测试会认为撤回接口存在，但实际调用会 404；业务层能力无法经 HTTP 暴露。
- 建议：二选一：补 HTTP 入口并走 `RecallChannelMessageCommand`，或从公开 API 文档移除过渡接口说明。

#### 中：文件领域服务正数校验异常类型与全局错误模型不一致

- 链路：`POST /api/files/uploads` / `PUT /api/files/uploads/{shareKey}` -> `FileController` -> `FileTransferDomainApi`。
- 证据：`FileTransferDomainApi.createUploadGrant(...)` 第 61 行调用 `requirePositive(accountId, ...)`；第 155-158 行抛 `IllegalArgumentException`，而同类业务校验大多抛 `ProblemException.validationFailed(...)`。
- 影响：若调用方复用领域 API 或认证上下文异常导致非法 accountId，会被 `GlobalExceptionHandler` 兜底为 500，而不是稳定的 422 `validation_failed`。
- 建议：改为 `ProblemException.validationFailed(...)`，并补领域服务非法 accountId 测试。

#### 低：批量公开用户资料查询使用全表读取后内存过滤

- 链路：消息响应 sender 补全 / 用户公开资料批量查询 -> `UserProfileDomainApi.getPublicUserProfiles` -> `UserProfileRepository.findAll`。
- 证据：`UserProfileDomainApi` 第 102-109 行对入参列表查询时调用 `userProfileRepository.findAll()` 后 `contains` 过滤。
- 影响：数据量增长后会造成无谓全表扫描、内存占用和响应延迟；返回顺序也不保证与请求 accountIds 一致。
- 建议：仓储端补 `findByAccountIds(Collection<Long>)` 或使用数据库 API 批量查询，并定义返回顺序契约。

#### 低：WS 文档仍引用不存在的 `docs/api` 路径

- 链路：WS / HTTP 协议文档引用。
- 证据：`docs/t/12-ws-events-v1.md` 第 9、51 行引用 `docs/api/...`；`docs/t/SERVER_API.md` 第 7-15 行也声明遵循 `docs/api/`，但当前仓库规范文件实际在 `docs/t/` 下。
- 影响：读文档或自动化校验时会遇到死链，不影响运行时但影响集成测试准备和协议确认。
- 建议：统一改为 `docs/t/...`，或补齐真实 `docs/api/` 目录。

#### 低：WS 入站 `send_channel_message` 命令缺少正式协议文档

- 链路：WS 客户端命令 -> `RealtimeChannelHandler` -> `RealtimeInboundMessageDispatcher` -> `ChannelMessageRealtimeInboundHandler` -> `ChannelMessagePublishingApi`。
- 证据：`ChannelMessageRealtimeInboundHandler` 第 33-50 行支持 `send_channel_message`；`docs/t/10-http-ws-protocol-v1.md`、`docs/t/12-ws-events-v1.md`、`docs/t/SERVER_API.md` 中只搜索到 auth / subscribe 命令示例，没有 `send_channel_message` 协议字段说明。测试 `RealtimeChannelHandlerMessageDispatchTests` 第 122-129 行验证该命令，但使用测试替身返回旧 `channel_message` 帧。
- 影响：Apifox 或手工 WS 集成测试无法从文档直接构造命令；测试替身事件格式与生产 `NettyMessageRealtimePublisher` 的 v1 `event/message.created` envelope 不完全一致，容易误导。
- 建议：补 WS 入站命令文档，明确成功响应是否仅依赖 `message.created` event，或增加 `send_channel_message.ok` ack；测试替身尽量改为生产 envelope。

### 未发现新问题的重点链路

- Required gate：控制器、领域异常和全局异常映射覆盖 `required_plugin_missing -> HTTP 412`，与现有测试一致。
- 消息附件：上传链路经 `ChannelMessageAttachmentDomainApi` 的可发送频道检查，发送 file / voice 时由 `MessageHttpPayloadParser` 解析 `share_key` 或 `object_key`，业务链路测试覆盖越权 object_key 和对象不存在。
- 置顶/取消置顶：入口、频道治理权限、pin 仓储和实时事件均有业务链路测试覆盖。
- Mention：发送、查询、单条已读和批量已读链路有业务链路测试覆盖。
- 通知偏好：服务级入口位于 `NotificationPreferenceController`，频道级入口位于 `ChannelController`，文档和测试均覆盖。
- WS 首帧鉴权：握手期只记录上下文，认证由 `auth` / `reauth` 首帧承担；生命周期、resume、unauthorized 和 malformed numeric 字段均有测试覆盖。

### 验证方式

- 本次为只读审查，没有修改正式源码、测试、SQL 或正式文档。
- 未运行 Maven 测试；结论基于源码、测试和文档的静态逐链路阅读与 `rg` 交叉搜索。
- 已执行模块边界文本搜索，未发现新的 impl / starter 越界命中。

### 残留风险

- 未启动真实 MySQL / Redis / MinIO / Netty 服务做端到端请求验证。
- 未对 OpenAPI 生成结果逐接口比对，只对源码入口、docs API 文档和相关测试做静态比对。
- 工作区存在大量用户/历史改动，本审查基于当前文件内容，不判断这些改动是否均已提交或是否属于同一任务。

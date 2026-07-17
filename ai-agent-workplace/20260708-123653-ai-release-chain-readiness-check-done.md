# 上线前请求链路确认任务单

## 任务目标

基于当前工作区，通过请求处理链检查和可执行门禁验证，确认当前代码是否已经达到上线标准，或明确指出仍未满足的上线阻塞项。

## 任务类型

只读确认。本任务不修改正式源码、测试、SQL、配置或正式 docs；仅允许更新并归档本任务单。

## 影响模块

- `application-starter`
- `chat-domain`
- `infrastructure-basic`
- `infrastructure-service`
- `distribution`
- `docs/`
- `ai-agent-workplace/`

## 允许修改范围

- 允许新增和更新本任务单。
- 允许归档本任务单为 `done`。

## 禁止修改范围

- 不修改正式源码、测试、SQL、配置或正式 docs。
- 不新增依赖。
- 不改变 Maven 模块结构。
- 不调整对外 HTTP / WS 协议。
- 不回退或清理用户/其他任务产生的既有改动。

## 文档依据

- `AGENTS.md`
- `docs/standards/变更审核清单.md`
- `docs/architecture/架构文档.md`
- `docs/architecture/包结构规范.md`
- `docs/standards/测试规范.md`
- `ai-agent-workplace/20260708-093044-ai-request-chain-postfix-reaudit-done.md`

## 执行计划

1. 静态扫描 HTTP / WS 入口、请求体校验、权限入口、模块边界和下载路径口径。
2. 按 Auth、User、File、Channel、Message、Server/WS、Realtime 分组抽查入口到领域 API、repository port、基础设施 adapter、副作用的完整链路。
3. 运行全量测试或可替代的 Maven 门禁，记录结果。
4. 检查工作区状态是否满足上线前可审计要求。
5. 给出是否达到上线标准的明确结论。

## 验收标准

- 有静态链路扫描证据。
- 有关键分组调用链结论。
- 有实际测试/构建命令结果。
- 明确给出“可上线 / 不可上线 / 可提测但不可上线”的判断。
- 任务单归档为 `done`。

## 检查结果

结论：当前代码不能确认达到上线标准。链路检查未发现新的阻塞级业务处理链断点，但上线门禁中的全量测试失败，且工作区存在大量未提交/未跟踪改动，不能作为可上线状态确认。

### 静态链路扫描

- HTTP 入口扫描：
  - 命令：`rg '@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)|@RequestMapping|@RestController' chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features -n`
  - 结果：入口覆盖 Auth、User、File、Channel、Message、Server/Notification 等 HTTP 分组。
- 请求体扫描：
  - 命令：`rg '@RequestBody' chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features -n -C 1`
  - 结果：强请求体均带 `@Valid`；消息发送、消息编辑、消息转发、服务级通知偏好更新带 `@NotNull`；`MentionController.markMentionsRead(...)` 和 `ChannelPinsController.pinChannelMessage(...)` 为协议允许的可选 body，代码有空值处理。
- 模块边界扫描：
  - 命令：`rg "infrastructure\\.service\\..*\\.impl|backend\\.starter|application\\.starter|team\\.carrypigeon\\.backend\\.starter" chat-domain infrastructure-basic infrastructure-service/*-api -n`
  - 结果：无命中。未发现 `chat-domain` / `infrastructure-basic` / `*-api` 直接依赖 forbidden implementation 或 starter 包。
- 下载路径扫描：
  - 命令：`rg -P '(?<!/api)/files/download|api/files/download/' chat-domain docs infrastructure-service -n`
  - 结果：命中均为 `/api/files/download/...` 口径，未发现旧下载路径回退。

### 分组调用链确认

- Auth：HTTP DTO 校验后进入 `AuthAccountApi` / `AuthSessionApi`；token 创建先检查 required plugins，再由领域服务完成账号、资料、默认频道成员和 refresh session 写入。
- User：当前用户、公开资料、批量公开资料、邮箱更新、资料更新和背景图上传链路完整；批量公开资料经 repository/database-api 批量查询下推，背景图上传走 `FileTransferApi`。
- File：上传申请、同源上传、下载均经 `FileTransferDomainApi`；普通文件、用户背景图、消息附件和服务头像的 object key 解析与访问约束分支完整。
- Channel：频道生命周期、治理、申请流、读状态和查询均经 channel domain API；写侧通过 `TransactionRunner` 和 repository port，after-commit 发布实时同步事件。
- Message：HTTP/WS 发送最终均进入 `ChannelMessagePublishingApi`；发送权限、禁言、成员、持久化、mention、after-commit realtime 发布链路完整；编辑、撤回、删除、置顶、提及读取/已读链路有对应领域服务和权限校验。
- Server/WS/Realtime：WS 握手只准备上下文，首帧 `auth/reauth` 后注册会话；未认证命令返回 unauthorized。通知偏好过滤只作用于消息/提及通知类事件，同步类事件不被过滤。

### 测试与构建门禁

- 全量测试：
  - 命令：`mvn test -DskipTests=false`
  - 结果：失败。
  - 失败模块：`chat-domain`
  - 失败测试：`NettyChannelRealtimePublisherTests.publishChannelChanged_mutedChannel_skipsEvent`
  - 失败原因：测试第 117 行期望 `channel.changed` 在频道静音时不下发；当前实现中 `RealtimeNotificationPreferenceFilter.FILTERED_EVENT_TYPES` 不包含 `channel.changed`，同步类事件会放行，因此实际收到 `TextWebSocketFrame`。
  - 判断：从链路设计看，当前实现符合“同步事件不被通知偏好过滤”的目标；失败更像是测试期望未同步更新。但只要全量测试失败，上线门禁就是不通过。
- 编译门禁：
  - 命令：`mvn -pl application-starter -am -DskipTests compile`
  - 结果：13 个 reactor modules 编译通过，BUILD SUCCESS。

### 工作区状态

- 命令：`git status --short`
- 结果：当前工作区存在大量 `M` 和 `??` 文件，包括源码、测试、配置、SQL、docs 和多个任务单。
- 判断：即使修复测试，正式上线前仍需要人工确认这些改动是否都属于本次发布范围，并清理或归档无关 `current` 任务单。

### 最终判断

当前状态：不可直接上线。

原因：

1. 全量 `mvn test -DskipTests=false` 失败。
2. 失败点虽疑似测试期望过期，但仍属于上线测试门禁未通过。
3. 工作区改动范围很大，未达到上线前可审计的干净状态。

可确认的正向结果：

1. 主要请求处理链未发现新的业务链路断点。
2. 模块依赖边界扫描无违例。
3. 下载路径和请求体校验扫描结果符合当前修复目标。
4. `application-starter` 及其依赖模块编译通过。

下一步建议单独修正或同步 `NettyChannelRealtimePublisherTests.publishChannelChanged_mutedChannel_skipsEvent` 的期望，再重新跑全量测试和完整构建。

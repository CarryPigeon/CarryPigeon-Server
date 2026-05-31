任务名称：
客户端基准 API 重写前差距梳理

任务目标：
在不修改正式代码与正式文档的前提下，继续下钻当前代码、测试与数据库契约，形成“现状实现 vs `docs/t` 基准协议”的可执行差距图，为后续 API 重写设计提供依据。

任务背景：
用户要求继续阅读理解项目；已完成第一轮整体理解，当前需要更具体地识别哪些 API family 已对齐、哪些仍混合、哪些存在设计层阻塞。

任务类型：
只读探索 / 审查任务

影响模块：
- `chat-domain`
- `application-starter`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `docs/t/`

允许修改范围：
- 仅允许在 `ai-agent-workplace/` 新增或更新分析任务单与分析记录

禁止修改范围：
- 不修改正式源码、正式测试、正式配置、正式文档

依赖限制：
- 不新增任何依赖

配置限制：
- 不修改任何运行时配置

文档依据：
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/架构文档.md`
- `docs/API.md`
- `docs/t/SERVER_API.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：
1. 抽取主要 HTTP / WS family 的测试入口，识别实际被锁定的协议语义。
2. 阅读 `database-api` 与 `database-impl` 的服务契约落点，判断协议重写对持久化层的影响方式。
3. 形成按 family 分类的差距图：已基本对齐、部分对齐、明显偏离。
4. 输出后续 API 重写设计需要优先处理的阻塞点与切入顺序建议。

关键假设与依赖：
- 当前探索仍为只读任务。
- `docs/t` 继续作为未来协议标准，而不是回到 `docs/API.md` 兼容现状。

实现要求：
- 结论必须有代码、测试或文档证据。
- 不把“已有控制器路径”误判为“协议已完成对齐”。

测试要求：
- 本任务为只读探索 / 审查任务，不运行实现类测试。

质量门禁：
- 已阅读关键测试与数据库契约代码。
- 输出明确指出协议锁定点、混合点、阻塞点。
- 明确说明本次无正式代码改动、无测试执行。

复审要求：
- 对外协议重写设计与实现前仍需用户确认边界。

文档要求：
- 本次不修改 `docs/`。

验收标准：
- 能给出面向 API 重写的具体差距清单和建议切入顺序。

完成定义：
- 完成只读下钻与输出总结。

实际结果：
- 已阅读代表性控制器契约测试、统一异常测试、WS 协议测试，以及 `database-api` 代表性服务契约。
- 已确认测试层与实现层共同体现出三种状态：
  1. 已基本对齐 `docs/t` 的 family：`auth` 主登录流、`server discovery`、`required gate`、`message query/send/forward/edit`、`mentions`、`pins`、`notification_preferences`、`audit_logs`。
  2. 部分对齐但仍保留历史兼容/混合响应的 family：`channel`、`user`、`server presence`、`well-known`、`message recall`。
  3. WS 外壳对齐但命令模型仍偏旧实现：`auth/ping/resume` 已具备 v1 壳，入站发送仍依赖 `send_channel_message` 旧命令名。
- 已确认错误模型已被 `GlobalExceptionHandlerTests` 与各控制器测试稳定锁定为标准 `{ error: ... }` 结构；后续 API 重写不应再回到 `CPResponse` 错误包装。
- 已确认成功模型未统一：
  - `AuthControllerTests`、`ChannelMessageQueryControllerTests`、`ServerControllerTests` 同时断言“新资源响应”与“旧 `CPResponse` 包装”。
  - `ChannelControllerTests`、`UserProfileControllerTests` 仍显著保留旧包装断言。
- 已确认 `database-api` 当前承载的是“最小数据库交互契约”，而非前端协议模型；例如 `MessageDatabaseService` / `MessageRecord` 已能支撑编辑、转发、搜索、mentions 等字段，不构成 API 重写的主要阻塞点。
- 已确认真正的设计阻塞主要在协议层而非持久化层：
  - 旧 `CPResponse` 成功包装残留。
  - 同一 family 内新旧路径并存。
  - WS 连接模型与 `docs/t` 在入口形态上接近，但命令语义与部署模型仍未完全标准化。

差距图：

- 已基本对齐
  - `POST /api/auth/email_codes`
  - `POST /api/auth/tokens`
  - `POST /api/auth/refresh`
  - `POST /api/auth/revoke`
  - `GET /api/server`
  - `POST /api/gates/required/check`
  - `GET /api/plugins/catalog`
  - `GET /api/domains/catalog`
  - `GET /api/channels/{cid}/messages`
  - `GET /api/channels/{cid}/messages/search`
  - `POST /api/channels/{cid}/messages`
  - `PATCH /api/messages/{mid}`
  - `POST /api/messages/{mid}/forward`
  - `POST/DELETE/GET /api/channels/{cid}/pins...`
  - `GET/PUT /api/mentions...`
  - `GET /api/audit_logs`
  - `GET/PUT /api/notification_preferences...`

- 部分对齐 / 混合态
  - `channel` family：`/api/channels` 下新旧响应混用，`default/system/private/invites/...` 大量残留 `CPResponse`
  - `user` family：`/api/users/me` 等新旧形态并存，分页/搜索仍走 `CPResponse<CursorPageResponse<...>>`
  - `server` family：`GET /api/server` 已新，`/.well-known/carrypigeon-server` 与 `/api/server/presence/me` 仍旧包装
  - `message` family：主查询/发送/编辑/转发已新，但 `recall` 仍旧包装

- 明显偏离 / 后续需要设计决策
  - WS 入站命令：当前测试仍验证 `send_channel_message` 旧命令，而不是一套完全按 `docs/t` 固化的命令集
  - WS 部署模型：当前为 feature 内 Netty 独立端口，可关闭；是否继续保留该模型需要在 API 重写时一并确认
  - `ServerApplicationService` 的公开能力描述仍带旧叙事（如 `username_password` 登录能力），与当前邮箱验证码会话主流程不完全一致

建议切入顺序：
1. 先冻结目标协议：明确“以 `docs/t` 为唯一成功响应标准”，并确认哪些历史兼容端点直接删除、哪些保留过渡期。
2. 先统一 HTTP 成功模型，再清理 family 内旧路径。
3. 之后统一 WS 命令模型与事件模型，最后再做文档/OpenAPI 与测试全面收口。
4. `database-api` 只做被动适配，不应先动它；除非重写方案新增了当前持久化契约无法表达的字段。

验证记录：
- 不适用。本任务为只读探索 / 审查任务，本次无代码改动，因此不适用实现类测试 / 构建门禁。

残留风险：
- 当前差距图基于代表性测试与入口代码，仍未逐条核验所有次级 DTO 字段与所有 database-impl SQL 细节。
- 后续若决定“兼容迁移”而不是“直接切到 `docs/t` 标准”，任务拆分和测试策略会显著不同。

知识沉淀 / 是否回写 docs：
- 暂不回写 `docs/`。

产物清理与保留说明：
- 保留本任务单与后续分析记录，作为 API 重写前置材料。

补充说明：
- 若后续转入实现类任务，应新开实现类任务单并确认协议切换策略。

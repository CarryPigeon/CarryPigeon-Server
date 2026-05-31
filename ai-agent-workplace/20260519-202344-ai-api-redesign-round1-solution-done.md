任务名称：

第 1 轮：API 重写方案与迁移边界确认

任务目标：

基于 `docs/t` 和当前实现现状，产出服务端 API 重写方案，明确协议切换范围、迁移策略、模块影响面与实施顺序，并形成待确认决策点。

任务背景：

当前服务端 API 与客户端基准 API 存在系统性差异，直接编码风险过高。需要先完成方案层收敛。

影响模块：

- `ai-agent-workplace/`
- 只读涉及 `chat-domain`
- 只读涉及 `application-starter`
- 只读涉及 `infrastructure-basic`

允许修改范围：

- 仅允许修改 `ai-agent-workplace/`

禁止修改范围：

- 不修改正式源码
- 不修改正式测试
- 不修改正式配置

依赖限制：

- 不新增依赖

配置限制：

- 不修改配置

文档依据：

- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/API.md`
- `docs/t/*`

任务分解 / 执行计划：

1. 列出现有协议与客户端协议的结构性差距。
2. 判断哪些差距属于必须一次收敛，哪些可阶段兼容。
3. 输出建议的迁移策略、模块改造顺序与风险点。
4. 列出需要你确认的决策项。

关键假设与依赖：

- 本轮仍为只读方案任务。
- 本轮完成并得到确认后，后续实现轮次才可开始。

实现要求：

- 方案必须覆盖 HTTP、错误模型、鉴权、分页、ID/时间编码与 WS。
- 必须明确“兼容迁移”或“直接切换”两种路径的取舍。

测试要求：

- 无代码测试要求

质量门禁：

- 已形成方案文稿。
- 已列出明确决策点。
- 已说明不进入实现的原因与边界。

复审要求：

- 需要你确认方案和迁移策略。

文档要求：

- 任务产物保留在 `ai-agent-workplace/`

验收标准：

- 用户可依据本轮产物决定后续实现策略。

完成定义：

- 方案已输出。
- 决策项已列出。
- 等待确认后转入下一轮。

实际结果：

- 已完成只读方案收敛，形成当前服务端 API 与 `docs/t` 客户端基准 API 的结构性差距清单。
- 已给出迁移策略建议：对外协议优先采用“直接切换到 v1 新协议”，不承诺旧协议与新协议长期并行兼容。
- 已给出后续实施顺序建议：先统一协议基础层，再重写 server/auth，再重写业务 HTTP，最后重写 WS 与补齐测试文档。
- 已列出进入实现轮次前必须确认的决策项。

验证记录：

- 文档阅读：`docs/API.md`
- 文档阅读：`docs/t/SERVER_API.md`
- 文档阅读：`docs/t/10-http-ws-protocol-v1.md`
- 文档阅读：`docs/t/11-http-endpoints-v1.md`
- 文档阅读：`docs/t/12-ws-events-v1.md`
- 文档阅读：`docs/t/13-error-model-and-reasons-v1.md`
- 文档阅读：`docs/t/14-pagination-and-cursor-v1.md`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/shared/controller/advice/GlobalExceptionHandler.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/controller/support/AuthAccessTokenInterceptor.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/controller/http/AuthController.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/user/controller/http/UserProfileController.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/channel/controller/http/ChannelController.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/message/controller/http/ChannelMessageController.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/controller/http/ServerController.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/controller/http/ServerWellKnownController.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/controller/ws/RealtimeAccessTokenHandshakeHandler.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/controller/ws/RealtimeChannelHandler.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/controller/ws/RealtimeClientMessage.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/controller/ws/RealtimeServerMessage.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/shared/controller/CPResponse.java`
- 代码阅读：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/shared/controller/CursorPageResponse.java`
- 代码阅读：`infrastructure-basic/src/main/java/team/carrypigeon/backend/infrastructure/basic/json/JacksonAutoConfiguration.java`
- 本轮为只读方案任务，无正式代码改动，因此不适用实现类测试、构建与启动门禁。

残留风险：

- 若继续尝试“旧 HTTP/WS 协议 + 新 v1 协议”长期并行，容易同时背负两套错误模型、鉴权模型、分页模型和 WS 语义，导致实现复杂度与回归风险显著上升。
- 若产品侧最终不接受 `docs/t` 中的邮箱验证码登录、required gate 或 `/api/ws` 同源入口，则后续实现轮次需要先回退并收窄目标协议。
- 在未确认 ID、时间、字段命名和错误结构是否一次切换前，任何局部编码都可能形成二次返工。

知识沉淀 / 是否回写 docs：

- 暂不回写 `docs/`

产物清理与保留说明：

- 保留方案任务单及其分析材料。

补充说明：

- 本轮结束后，若结论属于长期规则变更，再决定是否回写正式文档。
-
- 方案结论摘要：
- 1. 当前服务端协议与目标 v1 协议属于系统性差异，而非少量字段补丁。
- 2. 当前实现仍以 `CPResponse<T>` 成功/失败包装和 `100/200/300/404/500` 业务码为主；目标 v1 以真实 HTTP 状态码 + `error.reason` 为客户端稳定分支条件。
- 3. 当前 HTTP 鉴权与 auth API 仍围绕 `register/login/refresh/logout + username/password`；目标 v1 以 `GET /api/server -> required gate -> POST /api/auth/tokens -> WS auth` 为主流程，并要求 `device_id` 与 `installed_plugins` 参与会话创建。
- 4. 当前分页虽然已有 `items + next_cursor + has_more` 外壳，但游标语义仍偏向账号 ID / 消息 ID 数值游标；目标 v1 要求对外统一为不透明字符串 cursor。
- 5. 当前 WS 仍是握手阶段 Bearer 鉴权、最小 welcome/problem 命令结构；目标 v1 需要首帧 `auth`、`reauth`、统一事件 envelope、`event_id` 和 `resume` 恢复语义。
- 6. 因此 round2+ 建议采用“对外协议直接切换、内部实现分轮推进”的策略，而不是承诺旧新协议并行兼容。
-
- 结构性差距清单：
- 1. HTTP 成功/失败模型差距：当前成功与大多数失败都围绕统一业务包装；目标 v1 要求标准 HTTP 状态码与统一 `error` 对象。
- 2. 鉴权与登录差距：当前是用户名密码 + refresh token；目标 v1 是邮箱验证码创建会话，并带 required gate。
- 3. Server 探测差距：当前公开权威入口偏向 `/.well-known/carrypigeon-server` 与最小 `/api/server/echo`；目标 v1 强依赖 `GET /api/server` 返回 `server_id/api_version/ws_url/required_plugins/capabilities/server_time`。
- 4. 用户/频道/消息资源模型差距：当前路径与响应对象命名仍以现有服务端语义为主，未完全对齐 `uid/cid/mid/domain/domain_version/data/preview` 等客户端基准模型。
- 5. 时间与 ID 编码差距：目标 v1 要求雪花 ID 统一以十进制字符串暴露，时间统一为 epoch 毫秒；该点需确认当前全部对外 DTO 是否一次切换。
- 6. 字段命名差距：基础设施层已经统一了 snake_case 序列化，但当前对外 API 文档、DTO 语义与客户端基准仍未整体对齐，需在实现轮次作为显式验收点。
-
- 推荐实施顺序：
- 1. round2：统一错误模型、协议响应基础、cursor/时间/ID 外部编码约定。
- 2. round3：重写 `/api/server`、required gate、auth token 流程，并确认 `/.well-known` 的保留策略。
- 3. round4：重写 users/channels/messages 等 HTTP 业务端点，对齐资源命名与分页搜索契约。
- 4. round5：重写 `/api/ws` 握手、首帧 auth、事件 envelope、resume/reauth 与最小事件集。
- 5. round6：补充契约测试、回归测试、文档同步与联调核对。
-
- 进入实现前需确认的决策项：
- 1. 是否明确采用“直接切换到 v1 对外协议”，而不是兼容旧协议长期并行。
- 2. 是否接受 auth 产品语义切换到邮箱验证码会话模型；若不接受，需要先修订 `docs/t` 或明确本项目的 auth 差异边界。
- 3. required gate 是否纳入当前阶段范围；若纳入，必须在 auth 前补齐 `GET /api/server` 与 `POST /api/gates/required/check`。
- 4. WS 是否必须迁移到同源 `/api/ws`，并从握手 Bearer 改为首帧 `auth` + `resume` 模式。
- 5. `/.well-known/carrypigeon-server` 是否继续保留为匿名发现补充入口，还是由 `/api/server` 成为唯一客户端主探测入口。
- 6. 旧外部契约中的 `CPResponse` 是否只保留为内部历史兼容实现细节，而不再继续作为新客户端协议的一部分。

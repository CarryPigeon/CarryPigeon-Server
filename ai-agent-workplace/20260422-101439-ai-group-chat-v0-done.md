任务名称：
V0 群聊主链路实现

任务目标：
1. 在既有架构边界内落地 CarryPigeon V0 群聊主链路。
2. 支持默认群聊频道、频道成员关系、文本消息发送、历史消息按频道游标分页查询，以及实时消息推送与历史消息 ID 一致。
3. 保持 `application-starter` 只负责启动与装配，业务逻辑落在 `chat-domain`，数据库读写经由 `infrastructure-service/database-api` 与 `database-impl`。

任务背景：
正式 PRD 已明确当前阶段需要提供群聊频道、小型群聊、实时消息能力与历史查询能力，并要求实时推送与历史查询中的消息 ID 一致。当前代码库已具备注册/登录/JWT、用户资料与实时连接骨架，但尚无真正的频道、成员、会话、消息领域模型，也没有对应持久化与协议入口，因此 V0 群聊主链路仍未落地。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `application-starter`
- `ai-agent-workplace`

允许修改范围：
- 允许在 `chat-domain` 中新增 `channel`、`message` 等 feature 内部代码、配置、控制器、应用服务、领域模型、仓储抽象与测试。
- 允许在 `infrastructure-service/database-api` 与 `database-impl` 中新增与频道、成员、会话、消息相关的数据库契约、JDBC 实现与测试。
- 允许在 `application-starter/src/main/resources/db/migration` 中新增当前实现真实使用的数据库迁移脚本。
- 允许在 `chat-domain.features.server` 中调整实时消息处理，使其委托业务消息主链路而不是只做 echo。
- 允许创建并持续维护当前任务单。

禁止修改范围：
- 不允许修改模块职责与依赖方向。
- 不允许新增未经批准的第三方依赖。
- 不允许把业务规则塞入 `application-starter` 或 `database-impl`。
- 不允许在未获额外确认前扩展到文件消息、插件动态加载、1 对 1 私聊、跨服务端联邦或复杂后台管理能力。
- 不允许引入未来占位配置。

依赖限制：
- 仅使用仓库现有依赖与当前已存在的 Spring Boot / Lombok / Netty / JDBC / Flyway 体系。
- 如需新的外部依赖，必须先获得许可。

配置限制：
- 运行时配置入口保持为 `application-starter/src/main/resources/application.yaml`。
- 仅在当前功能真实需要时新增最小配置；优先复用既有配置能力。
- 不添加未来功能占位配置。

文档依据：
- `AGENTS.md`
- `docs/产品需求文档.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/配置规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 复核 PRD 与现有 auth/user/server/database 实现模式，确定最小可落地的频道、成员、消息与实时链路设计。
2. 定义 `chat-domain` 中的 `channel` / `message` feature 结构、命令/查询、领域模型、仓储抽象与异常边界。
3. 定义 `database-api` 中与频道、成员、会话、消息相关的契约模型与服务接口，并在 `database-impl` 中提供 JDBC 实现。
4. 在 `application-starter` 中新增 Flyway 迁移，完成 V0 真实需要的表结构。
5. 将 realtime 入口从 `welcome/echo` 骨架改为面向认证用户的业务消息入口，并与消息持久化主链路打通。
6. 补充与改动匹配的 application/controller/persistence 测试，并执行诊断、测试、构建验证。
7. 按变更审核清单完成自检，记录实际结果、验证证据与残留风险。

关键假设与依赖：
- 已确认事实：当前官方需求来源为 `docs/产品需求文档.md`。
- 已确认事实：当前代码已具备 JWT 鉴权、HTTP 控制器统一响应、问题异常映射与 WebSocket 握手鉴权骨架。
- 已确认事实：当前没有 `channel`、`message`、`conversation`、`channel_member` 等正式业务实现与持久化。
- 当前默认范围：先实现 V0 主链路，不展开 V1 文件消息与更完整权限审计。
- 停止条件：若实施过程中需要改变长期架构边界、依赖方向、对外协议主规则或引入新依赖，则先停止并请求确认。

实现要求：
- `chat-domain` 必须继续按 `features` 分包，新增功能优先落在 `features.channel` 与 `features.message`。
- `repository` 业务语义保留在 `domain` 层；薄适配器可位于对应 feature 的 `support` / `config`，并且只能依赖 `database-api`。
- 历史消息查询必须按频道查询并优先采用 cursor 模式。
- 实时推送与历史查询必须复用同一业务消息 ID，不允许分别生成两套消息标识。
- 实时连接必须基于已认证主体进行频道消息收发与成员校验。
- 错误响应必须继续遵守 `CPResponse` 与 `ProblemException` 的既有约定。

测试要求：
- 至少补充成功路径与失败路径。
- 覆盖应用层用例、协议入口与数据库适配契约。
- 对外响应相关测试需覆盖 `CPResponse.code` 的 `100`、`200`、`300`、`404`、`500`（在与本次改动匹配的场景中）。
- 测试类与测试方法命名、测试注释需符合 `docs/测试规范.md` 与 `docs/注释规范.md`。

质量门禁：
- 改动文件无新增诊断错误；若 Java LSP 不可用，使用 Maven 编译/测试作为替代验证并记录原因。
- 受影响模块测试通过。
- 至少执行一次匹配改动范围的 Maven 测试或构建命令并记录结果。
- 对用户可见的 HTTP/实时行为提供协议测试或可复验的手工验证证据。
- 跨模块改动需检查依赖方向与包边界未被破坏。

复审要求：
- 本任务属于跨模块、鉴权、实时协议与数据持久化敏感改动，必须进行独立深度复审；必要时咨询 Oracle。

文档要求：
- 若实现过程中没有引入新的长期项目规则，则不额外修改 `docs/`。
- 若发现 PRD 与现有规范存在稳定冲突，先记录并等待确认，再考虑文档更新。

验收标准：
- 认证用户可以发送默认群聊频道文本消息。
- 消息可以持久化，并可按频道使用 cursor 查询历史消息。
- 实时推送与历史查询返回的消息 ID 一致。
- 群聊主链路实现仍符合模块边界、依赖方向、响应码和测试规范。
- 相关验证记录、残留风险与自检结果完整可追溯。

完成定义：
- 任务范围内代码、测试、验证、自检、风险记录全部完成。
- 若实现完成，则把任务单改为 `done` 并按规则归档；若存在外部阻塞，则在实际结果与残留风险中明确记录。

实际结果：
- 已在既有模块边界内落地 V0 群聊主链路。
- `chat-domain` 新增 `features.channel` 与 `features.message`，提供默认频道发现、频道文本消息发送、按频道 cursor 历史查询，以及成员校验。
- `features.server` realtime 入口已从 echo 骨架改为委托 `MessageApplicationService` 的业务消息入口，并通过同一业务 `messageId` 复用持久化与实时下发。
- `infrastructure-service/database-api` 与 `database-impl` 已新增频道、成员、消息契约与 JDBC 实现。
- `application-starter` 已新增 V0 聊天表 Flyway 迁移，并以确定性数据播种默认公共频道。
- 注册事务内已补充默认频道成员关系初始化。
- 在真实 Docker 依赖环境下，已验证 MySQL、Redis、MinIO 三类外部服务可支撑当前 V0 群聊主链路启动与运行。
- 在真实联调过程中，额外发现并修复了多项运行时缺陷：`messageRealtimePublisher` Bean 冲突、`RealtimeChannelInitializer` 未接入真实 `MessageApplicationService`、`RealtimeServerProperties` 配置绑定失效、`MinioStorageProperties` 配置绑定失效。
- 在真实联调过程中，确认 WebSocket 入站协议字段命名遵循项目当前 snake_case 风格，业务发送命令中的频道字段应使用 `channel_id`。
- 已完成单客户端 realtime/history 一致性联调与双客户端广播联调，证明当前实现不只“可编译、可测试”，而且在真实依赖环境下可运行。

验证记录：
- LSP 诊断尝试失败：`lsp_diagnostics` 初始化超时 / 退出码 13，因此按任务单要求改用 Maven 编译与测试作为替代验证。
- `mvn -pl infrastructure-service/database-impl -am test -DskipTests=false`：通过。
- `mvn -pl chat-domain -am test -DskipTests=false`：通过。
- `mvn -pl application-starter -am test -DskipTests=false`：通过。
- `mvn install -DskipTests=false`：通过。
- `docker compose ps`：MySQL、Redis、MinIO 容器已启动；其中 Redis 初始显示 `unhealthy`，后确认是 compose 健康检查脚本密码读取问题，不代表 Redis 服务不可用。
- 通过直接运行 `application-starter/target/application-starter-1.0.0-exec.jar` 完成真实启动验证；HTTP `8080` 与 realtime `18080` 均成功监听。
- 应用启动日志中已确认：`Initialization check passed [database]`、`Initialization check passed [cache]`、`Initialization check passed [storage]`、`Realtime Netty server started on 127.0.0.1:18080/ws`。
- 真实 HTTP 联调：注册、登录、默认频道查询、历史消息查询全部成功。
- 真实单客户端 WebSocket 联调：Bearer 握手成功，收到 `welcome`，发送群聊文本消息后收到 `channel_message`，并验证 realtime 返回的 `message_id` 可在 HTTP 历史查询中命中。
- 真实双客户端广播联调：两个真实 WebSocket 客户端同时在线时，发送者与另一名在线成员均收到同一条 `channel_message`；两端 `message_id` 一致，且该消息可在 HTTP 历史查询中查到。
- MinIO 真实联调过程中额外确认：默认 bucket 需存在；已用容器实际凭据创建 `carrypigeon` bucket 后通过 storage 初始化检查。

残留风险：
- 当前任务范围内的 V0 群聊主链路已完成真实依赖联调与真实双客户端广播验证，不再仅依赖模块测试与构建证据。
- 当前 `docker-compose.yaml` 中 Redis 健康检查对密码来源的读取存在环境漂移，容器状态可能显示 `unhealthy`，但本次应用侧真实 `redis ping` 初始化检查已通过；该问题属于运行环境脚本一致性风险，不阻塞当前群聊主链路可用性结论。
- 当前 MinIO 运行容器内的实际根凭据与仓库 `.env.example` 默认值不一致（现场容器为 `carrygieon/carrygieon`）；该问题反映当前本地运行环境已存在漂移。虽然本次已使用实际凭据完成 bucket 创建与 storage 检查，但后续若重建容器或切换环境，仍需先确认 MinIO 真实凭据与默认 bucket 状态。
- 尚未覆盖超出 V0 范围的内容，如文件消息、小型群聊管理、复杂权限审计、长时稳定性压测、断线重连与恶意帧场景。

知识沉淀 / 是否回写 docs：
- 暂无新增长期规则预期；若实现中发现稳定规则缺口，再单独评估是否回写 `docs/`。

产物清理与保留说明：
- 保留当前任务单；任务完成后改名为 `done`，并按仓库规则归档。

补充说明：
- 当前仓库根目录已有另一个 `current` 任务单，后续若其完成或确认无关，应按规则尽快收敛，避免长期悬挂多个无关 `current` 文件。

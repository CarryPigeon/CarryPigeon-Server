任务名称：

群聊权限与消息撤回完善

任务目标：

在不突破现有模块职责与依赖边界的前提下，把群聊能力提升到可持续演进的成熟开源项目水准，补齐明确、可测试的群权限治理、成员管理、管理员治理、消息撤回与审计增强能力，并形成完整任务闭环。

任务背景：

- 当前群聊主链路已具备：注册登录、JWT 鉴权、默认频道接入、HTTP 历史查询、WebSocket 实时消息、文件/语音消息、统一异常映射。
- 当前权限语义仍停留在“已认证账号 + 频道成员”两层，没有频道管理员、细粒度消息权限、消息撤回或审计留痕能力。
- `docs/产品需求文档.md` 已明确：
  - 当前服务端负责“群聊频道与小型群聊、实时消息收发、历史消息查询、插件接口、事件、权限声明和运行边界”；
  - V1 目标包含“支持基础权限和审计增强”；
  - 聊天场景覆盖 `public` / `private` / `system` 频道，其中小型群聊通过 `private channel` 表达。
- 用户已确认本次按“完整群管理版”推进：在消息治理基础上，继续覆盖 mute / kick / invite / ban / owner-admin 管理等群管理能力，而不是停在最小 moderation 切片。

影响模块：

- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `application-starter`（仅在需要新增或调整数据库迁移时）
- `ai-agent-workplace`

允许修改范围：

- 允许在 `chat-domain` 的 `features/channel`、`features/message`、`features/server` 内补充权限与撤回相关领域模型、应用服务、控制器、实时处理链路、测试与必要注释。
- 允许在 `infrastructure-service/database-api` / `database-impl` 中补充与权限、消息撤回、查询过滤相匹配的数据库契约与实现。
- 允许在 `application-starter` 中新增必要的数据库迁移脚本。
- 允许在 `ai-agent-workplace` 中维护本任务单。

禁止修改范围：

- 不允许修改模块依赖方向。
- 不允许引入新的重量级依赖或中间件。
- 不允许修改全局异常体系主规则、统一响应码语义或全局配置体系。
- 不允许把核心业务规则塞进 `application-starter`。
- 不允许在本任务中扩展为完整后台管理系统或泛化 ACL/RBAC 框架。

依赖限制：

- 继续使用现有 Spring Boot、Lombok、MyBatis Plus、既有数据库 API 与对象存储 API。
- 若发现需要新依赖，必须先重新确认。

配置限制：

- 默认不新增全局配置项。
- 若权限或撤回必须依赖新的运行时配置（如撤回时间窗口），需先确认后再扩展。

文档依据：

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/任务单模板.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/异常与错误码规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/产品需求文档.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：

1. 固化本次任务边界，确认“成熟开源项目水准”在本仓库中的最小可行实现范围。
2. 基于既有 `Channel` / `ChannelMember` / `ChannelMessage` 与实时链路，确定角色模型、成员状态模型、邀请流程、禁言/封禁/移除规则、消息撤回与审计模型。
3. 在任务边界确认后，实现领域模型、应用服务、HTTP/WS 接入、数据库读写与必要迁移。
4. 补充成功/失败路径测试，覆盖角色变更、邀请加入、禁言/封禁/移除、权限拒绝、资源不存在、撤回后历史/搜索/实时一致性。
5. 执行 LSP/测试/构建验证，完成独立复审与任务单收口。

关键假设与依赖：

- 已确认事实：当前代码中不存在现成的频道管理员/拥有者/角色/权限矩阵模型。
- 已确认事实：当前消息模型与数据库契约中已经存在 `status` 和 `metadata` 字段，可作为消息撤回的扩展落点。
- 已确认范围：本次任务不是只做消息治理，而是补齐完整群管理版所需的角色、成员管理与审核治理能力。
- 已确认假设：本次消息撤回优先采用软撤回（保留 `message_id`，更新状态并保留审计信息）。
- 已确认假设：频道管理员可撤回他人消息，普通成员只能撤回自己的消息。
- 已确认设计：频道角色固定为 `OWNER` / `ADMIN` / `MEMBER`，不引入通用 ACL 或可配置权限矩阵。
- 已确认设计：`ChannelMember` 继续表示“活跃成员投影”，仅承载活跃成员需要的治理字段（如角色、禁言信息）；`invite` / `ban` 不与活跃成员状态强行混为一个大状态机。
- 已确认设计：消息撤回属于 `features/message` 的消息生命周期变更；群治理授权规则集中在 `features/channel`；`features/server` 仅负责实时协议转发与事件下发。
- 默认假设：`private` 频道优先具备完整 owner/admin 治理能力；`public` 频道采用更保守的直接加入 / 直接管理规则；`system` 频道不进入完整群治理流程。

实现要求：

- 必须优先复用现有 `features/channel`、`features/message`、`features/server` 结构，不发明新架构。
- 权限校验必须落在应用服务或领域规则可复用位置，不能只放在 controller。
- 权限失败必须继续使用 `ProblemException.forbidden(...)`，并保持 `CPResponse.code = 300` 映射稳定。
- 消息撤回若落地，必须保持历史与实时链路上的 `message_id` 一致性。
- 审计增强优先做最小必要留痕，不直接扩展为独立审计子系统。
- 群管理能力应至少覆盖：成员邀请/加入、成员移除、成员禁言、成员封禁、管理员治理、所有者治理边界。
- 若 OWNER / ADMIN / MEMBER 之外还需要更多角色，必须证明是当前实现所必需，而不是为未来预留。
- invite / ban / audit 的数据表达应采用最小可持续设计，避免为了“一张表全包”而破坏当前按活跃成员查询与实时广播的清晰语义。

测试要求：

- 至少覆盖成功路径与失败路径。
- 至少覆盖：成员权限不足、管理员权限通过、所有者专属操作、邀请/移除/禁言/封禁规则、非成员拒绝、资源不存在、撤回后历史查询表现、撤回后搜索表现、撤回实时通知或更新表现。
- 至少覆盖：活跃成员查询与实时广播不会误包含被 ban / 未接受 invite 的账号。
- 新增或修改的测试必须遵守现有注释与分级规则。

质量门禁：

- 受影响文件无新增 LSP/编译错误。
- `chat-domain` 相关测试通过。
- `infrastructure-service/database-impl` 相关测试通过。
- 若涉及数据库迁移或 starter 装配，受影响 Maven reactor 验证通过。
- 对外接口与实时协议的成功/失败路径有可执行验证证据。

复审要求：

- 由于本任务涉及权限、审计、数据持久化和用户可见行为，完成实现后必须做独立复审 / Oracle 复审。

文档要求：

- 若本次沉淀出新的长期稳定权限/撤回规则，需要同步回写 `docs/`。
- 仅任务局部决策保留在任务单中，不把任务局部假设误写成长期规范。

验收标准：

- 任务边界已被明确，不存在“权限管理”范围不清导致的无控制扩张。
- 实现后的群聊权限、成员管理与撤回能力满足已确认的产品规则，并与现有 public/private 频道模型兼容。
- 权限失败、资源不存在、成功路径的响应码与异常语义符合项目规范。
- 相关测试、构建与复审全部通过。

完成定义：

- 用户已确认本次功能边界中的关键产品决策。
- 验收标准已满足。
- 质量门禁已执行并记录。
- 本任务单已补齐“实际结果 / 验证记录 / 残留风险”，并从 `current` 改为 `done`。

实际结果：

- 已完成“群治理持久化基础切片”：
  - `ChannelMember` 继续作为活跃成员投影，但已补充固定角色 `OWNER / ADMIN / MEMBER` 与 `mutedUntil` 持久化字段。
  - 新增独立 invite / ban / audit log 领域模型、仓储抽象、database-api 契约、database-impl 实现与 Flyway 迁移。
  - 现有注册默认入群、消息发送、历史查询、搜索与实时广播链路保持兼容。
- 已完成“频道治理应用层切片”：
  - 已支持 `private` 频道创建、成员邀请、接受邀请、成员列表查询。
  - 已集中落地 `ChannelGovernancePolicy`，作为 `private` 频道治理的统一授权规则组件。
  - 创建 `private` 频道的用户会自动成为该频道 `OWNER`。
- 已完成“群管理动作切片”：
  - 已支持成员提升为 `ADMIN`、管理员降级为 `MEMBER`、所有权转移、禁言、解除禁言、移除成员、封禁、解封。
  - 已把这些动作统一接入 channel audit log，并修正为 `metadata` 非空写入，避免与持久化约束冲突。
  - 已把消息发送链路接入治理规则，`private` 频道被禁言成员无法继续发送消息。
- 已完成“消息撤回与消息治理收口切片”：
  - 在 `features/message` 内新增 HTTP 撤回入口，应用服务通过 `ChannelGovernancePolicy` 统一编排撤回授权，不在 controller 重复 OWNER / ADMIN / MEMBER 规则。
  - 撤回采用软撤回：保持原 `messageId`，把 `status` 更新为 `recalled`，并将 `body` / `previewText` 重写为撤回占位文案，清空 `payload`、清空 `metadata`，并将 `searchableText` 重写为空字符串，保证历史身份稳定且搜索不再命中原内容，同时满足当前表结构约束。
  - 为撤回补充最小持久化接缝：`MessageRepository` / `MessageDatabaseService` 新增按 `messageId` 查询与更新能力，`database-impl` 补齐 mapper / service 实现。
  - realtime 出站沿用现有 publisher 层，新增 `channel_message_updated` 事件承载撤回后的稳定消息投影，不新增 inbound WS recall 或通用事件总线。
  - 撤回动作已补充 channel audit 留痕，避免敏感治理动作无追踪记录。
- 已完成文档沉淀：
  - `docs/产品需求文档.md` 已补充当前稳定的频道治理规则和消息撤回规则，避免长期规则只停留在代码与任务单中。

验证记录：

- 已尝试对受影响目录执行 `lsp_diagnostics`，但当前环境中的 Java LSP 服务初始化失败 / 超时，未能产出可用诊断结果；已使用 Maven 编译与全量受影响 reactor 验证作为替代证据。
- 第一轮全量验证：`mvn -pl application-starter -am verify -DskipTests=false` -> 通过。
  - `chat-domain`：115 个测试通过。
  - `database-impl`：41 个测试通过。
  - `application-starter`：11 个测试通过。
- 频道治理与消息撤回切片定向验证：
  - `mvn -pl chat-domain -am -Dtest=MessageApplicationServiceSendTests,MessageApplicationServiceQueryTests,ChannelMessageQueryControllerTests,DatabaseBackedMessageRepositoryTests,NettyMessageRealtimePublisherTests test -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false` -> 通过。
  - `mvn -pl infrastructure-service/database-impl -am -Dtest=MybatisPlusMessageDatabaseServiceTests test -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false` -> 通过。
- 群管理动作接入后的全量验证：`mvn -pl application-starter -am verify -DskipTests=false` -> 通过。
  - `chat-domain`：153 个测试通过。
  - `database-impl`：49 个测试通过。
  - `application-starter`：11 个测试通过。
- 修复 Oracle 终审指出的阻塞项后再次全量验证：`mvn -pl application-starter -am verify -DskipTests=false` -> 通过。
  - `chat-domain`：153 个测试通过，JaCoCo 门禁通过。
  - `database-impl`：49 个测试通过，JaCoCo 门禁通过。
  - `application-starter`：11 个测试通过，JaCoCo 门禁通过。
- 本次收口过程中的真实问题与修复：
  - 修复了 `ChannelControllerTests` 中重复测试方法导致的编译失败。
  - 修复了 starter 测试装配缺失 `ChannelGovernancePolicy` bean 的问题。
  - 修复了 recall 与 audit 非空约束相关问题：
    - recalled 消息的 `searchableText` 改为 `""` 而不是 `null`。
    - channel audit `metadata` 改为默认写入 `"{}"` 或显式 JSON。
    - recall 动作补充审计留痕。

残留风险：

- `chat_channel_ban` 当前采用单条当前记录 + `revoked_at` 的最小可持续表达；若后续需要多次封禁历史明细检索，可能仍需进一步演化查询模型，但这不阻塞当前功能闭环。
- 真实数据库约束目前主要通过 schema 规则推导 + 全量 Maven 验证 + 定向仓储测试进行保障，尚未引入单独的 embedded / containerized 数据库集成测试基座；当前不阻塞关闭，但后续若数据库交互继续复杂化，建议补齐。
- Java LSP 诊断未能在当前环境成功初始化，后续如环境恢复，应补跑 LSP 诊断以补齐该项门禁证据。

知识沉淀 / 是否回写 docs：

- 已回写 `docs/产品需求文档.md`：补充当前频道治理规则与消息撤回规则。

产物清理与保留说明：

- 当前保留本任务单为 `current`。
- 完成后改为 `done`，并根据仓库规则决定是否归档。

补充说明：

- 用户已明确要求按“完整群管理版”推进，因此本任务需要覆盖消息治理之外的成员治理能力。
- 具体实现仍需遵守“最小可持续设计”原则，避免直接膨胀成通用 ACL 平台或完整后台管理系统。
- 当前计划阶段已明确采用“固定角色 + 活跃成员投影 + 独立 invite/ban 记录 + 消息层软撤回”的实现方向，后续编码按此执行；若发现边界扩大，再先更新任务单。

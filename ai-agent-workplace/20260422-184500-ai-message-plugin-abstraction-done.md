任务名称：
消息插件化抽象设计与落地

任务目标：
在不破坏当前 text 消息主链路、不过度扩展数据库契约与运行时协议的前提下，为 `chat-domain` 的消息能力建立可插件扩展的抽象边界，使后续语音消息、文件消息、富文本消息、特殊消息可以通过新增插件而不是重写主链路的方式接入。

任务背景：
当前消息链路虽然在数据结构上已经具备 `messageType`、`content`、`payload`、`metadata` 四类字段，但真实运行流程仍然对 text 消息强耦合：realtime 入站命令写死为 `send_channel_text_message`，应用层发送用例写死为 `SendChannelTextMessageCommand`，消息构造逻辑集中在 `MessageApplicationService` 中，尚未形成按消息类型扩展的处理模型。用户希望把消息进行设计抽象，使消息能力能够插件化拓展到语音消息、文件消息、富文本消息、特殊消息等类型。

影响模块：
- `chat-domain`
- `ai-agent-workplace`

允许修改范围：
- 允许修改 `chat-domain/features/message/**`
- 允许修改 `chat-domain/features/server/**` 中与 realtime 入站消息分发直接相关的代码
- 允许新增或调整 `chat-domain` 内 message feature 的 `config`、`support`、`application`、`domain` 下局部抽象与装配
- 允许补充或改写 `chat-domain` 相关测试
- 允许更新任务单记录本次设计、实现与验证结果

禁止修改范围：
- 不允许修改模块依赖方向
- 不允许让 `chat-domain` 直接依赖任何 `*-impl`
- 不允许本轮直接扩展 `database-api` 或 `database-impl` 的消息契约与表结构，除非实现中发现现有约束无法支撑 text 兼容迁移且必须先回到任务单重新界定
- 不允许一次性落地语音 / 文件 / 富文本 / 特殊消息的完整业务链路实现
- 不允许引入新的全局插件框架、脚手架系统或与当前仓库风格不一致的基础设施层插件机制

依赖限制：
- 继续使用现有 Spring Boot / Jackson / 项目内基础设施能力
- 不新增第三方依赖，除非出现当前确有必要且边界明确的最小需求

配置限制：
- 不新增面向未来占位的运行时配置
- 保持现有应用配置和协议默认值最小化

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 结合当前消息链路与仓库既有扩展模式，确定 repo-aligned 的消息插件抽象设计。
2. 在 `chat-domain/features/message` 内引入消息草稿、消息插件契约、插件注册器等最小抽象。
3. 将当前 text 消息发送逻辑改造为通过插件驱动生成最终 `ChannelMessage`，同时保留兼容入口 `sendChannelTextMessage(...)`。
4. 将 realtime 入站从硬编码文本消息分支改为基于 handler/dispatcher 的可扩展分发，但继续兼容现有 `send_channel_text_message` 协议。
5. 补充测试，验证 text 消息行为未回归，且插件/dispatcher 机制能正确选择处理器。
6. 执行定向验证并记录结果、残留风险与后续扩展建议。

关键假设与依赖：
- 已确认 `ChannelMessage` / `MessageRecord` / `MessageEntity` 当前已经具备 `messageType`、`content`、`payload`、`metadata` 字段，可作为插件化扩展的兼容承载结构。
- 已确认当前仓库最接近的扩展模式是“稳定契约 + Spring 注入的 `List<实现>` + feature 内局部装配”，而不是全局插件框架。
- 已确认现有消息发送主入口是 realtime，HTTP 当前仅提供历史查询，不提供消息发送入口。
- 已确认本轮默认目标是“先抽象 text，并建立扩展骨架”，不是“同时完整支持多消息类型”。

实现要求：
- 新抽象应优先落在 `chat-domain/features/message` 内部，表达为 message feature 自身能力，而不是额外创建跨模块总线式框架
- 现有 `sendChannelTextMessage(...)` 必须继续可用，并委托到新的插件化发送主链路
- realtime 入站必须继续兼容当前 `send_channel_text_message` 协议
- 历史消息查询与 realtime 下发的对外结构在 text 消息场景下应保持兼容
- 插件注册器应对重复注册、未知消息类型等情况有明确行为
- 类注释与测试注释需符合仓库注释规范

测试要求：
- 至少覆盖 text 插件成功路径与失败路径
- 至少覆盖 dispatcher/registry 的路由选择逻辑
- 至少覆盖 legacy realtime 文本命令兼容行为
- 至少覆盖 text 消息通过新抽象后仍保持既有 `messageType/content/payload/metadata/status` 语义

质量门禁：
- 受影响 `chat-domain` 文件无新增编译错误
- 相关单测通过
- 受影响 Maven 模块定向验证通过
- 不引入新的模块边界违规

复审要求：
- 重点复审：抽象是否过度、是否符合当前仓库模式、是否保持 text 主链路兼容、是否为后续扩展提供真实可用的接入点

文档要求：
- 若本次没有产生新的长期仓库规则，则不修改 `docs/`
- 任务过程与结果记录在本任务单中

验收标准：
- text 消息发送主链路已迁移到可插件扩展的内部抽象上
- legacy realtime text 命令仍可工作
- 后续新增消息类型时，可通过新增插件/handler 而不是重写主链路方式接入
- 当前 text 消息历史查询与 realtime 下发未回归

完成定义：
- 任务范围内代码、测试、验证、自检全部完成
- 任务单已补充实际结果、验证记录与残留风险
- 当前任务单可从 `current` 改为 `done`

实际结果：
- 已在 `chat-domain/features/message` 内引入最小可用的消息插件化抽象：
  - `SendChannelMessageCommand`
  - `ChannelMessageDraft`
  - `TextChannelMessageDraft`
  - `ChannelMessagePlugin`
  - `ChannelMessagePluginRegistry`
  - `TextChannelMessagePlugin`
- 已将 `MessageApplicationService` 从“硬编码 text 消息构造”改为“通过插件注册器按消息类型构造最终 `ChannelMessage`”，同时保留 `sendChannelTextMessage(...)` 兼容入口。
- 已在 `chat-domain/features/server` 内引入最小 realtime 入站可扩展机制：
  - `RealtimeInboundMessageHandler`
  - `RealtimeInboundMessageDispatcher`
  - `SendChannelMessageRealtimeHandler`
  - `RealtimeMessageHandlingConfiguration`
- 已将 `RealtimeChannelHandler` 从硬编码 `send_channel_text_message` 分支改为通过 dispatcher 分发处理；当前仍兼容 legacy 命令 `send_channel_text_message`，并新增统一命令入口 `send_channel_message`（当前仅内建 `text` 类型）。
- 已扩展 `RealtimeClientMessage` 协议结构，增加 `messageType`、`payload`、`metadata` 字段，但保持当前 text 场景兼容。
- 当前 text 消息的对外语义保持不变：仍使用 `messageType="text"`，仍保持 `payload=null`、`metadata=null`、`status="sent"`，历史查询与 realtime 下发未发生行为回归。

验证记录：
- 已执行：`mvn -f chat-domain/pom.xml test -DskipTests=false`
  - 结果：通过，`chat-domain` 共 78 个测试通过。
- 已执行：`mvn -pl chat-domain,application-starter -am -DskipTests package`
  - 结果：通过。
- 已补充并验证以下关键测试：
  - `MessageApplicationServiceTests`
    - legacy `sendChannelTextMessage(...)` 继续可用
    - 通用 `sendChannelMessage(TextChannelMessageDraft)` 保持 text 语义不变
  - `RealtimeChannelHandlerTests`
    - legacy `send_channel_text_message` 继续可用
    - 新统一命令 `send_channel_message` + `messageType=text` 可用
    - 未支持的统一消息类型会返回 `problem`

残留风险：
- 本轮只把 text 消息迁移到插件化抽象，并建立扩展骨架；语音 / 文件 / 富文本 / 特殊消息的真实业务规则、存储语义、协议细节尚未实现。
- 当前消息表仍受既有 `content TEXT NOT NULL` 约束限制，因此未来非 text 消息若要脱离“兼容 content 字段”的做法，可能需要单独任务调整数据库契约。
- 当前统一实时命令 `send_channel_message` 只内建 `text` 类型；未来新增其它消息类型时，需要继续补对应 draft / plugin / realtime handler 映射与测试。

知识沉淀 / 是否回写 docs：
- 本次未引入新的长期仓库规则，暂不回写 `docs/`。

产物清理与保留说明：
- 保留当前任务单作为本次消息抽象重构的追溯记录。

补充说明：
- 本轮默认不修改消息表结构，因此未来非 text 插件必须暂时适配现有 `content TEXT NOT NULL` 约束；若后续新增消息类型需要突破该约束，应另行建立新的边界任务。

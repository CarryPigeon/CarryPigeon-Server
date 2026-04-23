任务名称：
文件消息与语音消息落地

任务目标：
在现有消息插件化骨架之上，继续增强通用性，并正式落地文件消息与语音消息两种类型，使其能够通过统一的消息插件机制接入，同时支持通用的消息预览文本与可检索文本能力。

任务背景：
当前项目已经完成消息骨架从 `content` 单字段模型升级为 `body / previewText / searchableText` 三投影模型，并具备消息插件注册、realtime 分发和最小搜索能力。但目前只有 text 消息真正接入这套骨架。用户明确要求两件事同时推进：
1. 继续做骨架增强，保证通用性与可扩展性；
2. 实际落地语音消息与文件消息。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `application-starter`
- 可能涉及 `infrastructure-service/storage-api` / `storage-impl` 的既有使用方式
- `ai-agent-workplace`

允许修改范围：
- 允许修改 `chat-domain/features/message/**`
- 允许修改 `chat-domain/features/server/**` 中与消息入站 / realtime 下发直接相关的代码
- 允许修改 `database-api` / `database-impl` 中与消息持久化契约直接相关的代码
- 允许新增 Flyway 迁移
- 允许在不突破模块边界的前提下接入既有 `storage-api` 能力
- 允许补充或改写相关测试

禁止修改范围：
- 不允许修改模块依赖方向
- 不允许让 `chat-domain` 直接依赖任何 `*-impl`
- 不允许引入新的中间件、全文检索系统或新的第三方依赖
- 不允许一次性实现超出 file / voice 之外的其它具体消息类型
- 不允许把本次实现演化成仓库级全局插件框架

依赖限制：
- 继续使用现有 Spring Boot / Jackson / MyBatis-Plus / 项目基础设施能力
- 文件与语音消息若需要引用对象存储，只能通过既有 `storage-api` 契约完成，不得直接接入 `storage-impl`

配置限制：
- 不新增未来占位配置
- 不修改全局配置体系，除非现有运行时确有必要且在任务范围内

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 继续探索当前消息骨架、文件/语音相关切点、storage 接入模式和仓库原生扩展方式。
2. 在现有消息插件体系上补齐更通用的 file / voice 元信息表达能力。
3. 落地文件消息插件与语音消息插件。
4. 让 realtime 入站命令与消息发送主链路支持 file / voice 两类消息。
5. 同步持久化契约、数据库实体、SQL 与必要迁移。
6. 补充历史查询 / 搜索 / realtime 下发相关测试，并做定向验证。

关键假设与依赖：
- 已确认本轮可以继续沿用 `Draft + Plugin + Registry + Realtime Dispatcher` 模式。
- 已确认 text 消息结构不再需要向前兼容旧 `content` 语义，因此 file / voice 可直接基于新骨架建模。
- 已确认消息搜索当前是数据库最小实现，本轮只需让 file / voice 能正确纳入搜索文本，而不升级为复杂全文检索。
- 待确认事实：仓库中是否已存在可直接复用的 file 元信息 / storage object 引用模式，以及语音消息是否应附带时长、mimeType、转写摘要等字段。

实现要求：
- file / voice 必须通过消息插件机制接入，而不是在 `MessageApplicationService` 内再加 `if/else` 硬编码
- 每种消息类型都必须明确：
  - 核心 payload 结构
  - previewText 生成规则
  - searchableText 生成规则
- file / voice 的对外历史消息与 realtime 下发应走同一通用消息结构
- 若需要依赖对象存储对象引用，必须优先采用 repo 当前既有契约风格（例如 objectKey / presigned url / metadata），不得临时发明跨模块耦合结构

测试要求：
- 覆盖 file / voice 插件成功路径与失败路径
- 覆盖 file / voice 的 previewText 和 searchableText 生成
- 覆盖 realtime 入站分发到 file / voice 类型
- 覆盖历史查询与搜索返回结果中 file / voice 消息的结构
- 覆盖数据库映射与搜索 SQL 行为

质量门禁：
- 受影响 Java 文件无新增编译错误
- 相关单测通过
- 受影响 Maven reactor 构建通过
- 不引入新的模块边界违规
- 若实现涉及真实启动/链路验证，应记录实际命令与结果

复审要求：
- 重点复审：file / voice 是否真的通过统一骨架接入、storage 引用是否符合模块边界、preview/search 是否合理、是否引入新的结构性技术债

文档要求：
- 若未形成新的长期仓库规则，则不修改 `docs/`
- 任务过程与结果记录在本任务单

验收标准：
- file / voice 两种消息类型已经可以通过统一消息骨架进入发送、持久化、历史查询、实时下发链路
- file / voice 都具备合理的 previewText 和 searchableText
- text / file / voice 三者共享同一通用消息模型与插件机制

完成定义：
- 任务范围内代码、测试、验证、自检全部完成
- 任务单已补充实际结果、验证记录与残留风险
- 当前任务单可从 `current` 改为 `done`

实际结果：
- 待实现。

验证记录：
- 待实现。

残留风险：
- 待实现。

知识沉淀 / 是否回写 docs：
- 待实现后复核。

产物清理与保留说明：
- 保留当前任务单作为本轮 file / voice 消息实现的追溯记录。

补充说明：
- 本轮以“先用 repo 现有 storage 能力支撑消息引用”为默认方向；若探索结果表明需要先引入独立文件业务 feature 或新的对外协议边界，应先更新任务单再继续。

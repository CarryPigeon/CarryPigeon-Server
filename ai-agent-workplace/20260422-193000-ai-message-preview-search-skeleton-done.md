任务名称：
消息预览与检索通用骨架迭代

任务目标：
在不实现具体新消息类型的前提下，将当前消息插件化骨架继续升级为支持“消息内容本体、消息预览文本、消息可检索文本”三类通用投影的设计，并让消息能够纳入最小可用的搜索链路中，为后续文件、语音、富文本、特殊消息等类型提供统一扩展接口。

任务背景：
上一轮已经在 `chat-domain` 内建立了 `Draft + Plugin + Registry + Realtime Dispatcher` 的最小消息插件化骨架，但当前消息模型仍然把 `content` 同时承担“正文内容、用户可见预览、搜索文本”三种角色，导致通用性不足。用户已明确要求本轮只做骨架，不落具体消息类型，同时要支持预览（例如 `[文件消息]`）和消息检索，并明确允许 text 消息结构按新模型调整，不要求向前兼容旧结构。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `application-starter`
- `ai-agent-workplace`

允许修改范围：
- 允许修改 `chat-domain/features/message/**`
- 允许修改 `chat-domain/features/server/**` 中与消息入站 / realtime 出站直接相关的代码
- 允许修改 `infrastructure-service/database-api` 与 `database-impl` 的消息契约/实体/mapper/服务实现
- 允许新增 Flyway 迁移以演进消息表结构
- 允许补充或改写相关测试
- 允许更新任务单记录本次设计、实现与验证结果

禁止修改范围：
- 不允许修改模块依赖方向
- 不允许把消息插件框架做成跨模块全局基础设施框架
- 不允许本轮直接实现语音 / 文件 / 富文本 / 特殊消息的具体业务插件
- 不允许引入新的第三方依赖或搜索中间件

依赖限制：
- 继续使用现有 Spring Boot / Jackson / MyBatis-Plus / 项目内基础设施能力
- 搜索能力仅基于当前数据库实现做最小落地，不引入额外全文检索组件

配置限制：
- 不新增未来占位配置
- 保持当前配置最小化

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
1. 将消息核心模型从 `content` 单字段升级为“正文 body + previewText + searchableText”的通用结构。
2. 将消息插件契约升级为负责产出完整消息投影，而不只是基础 `content/payload/metadata`。
3. 同步演进 `database-api` / `database-impl` / Flyway，让 preview/search 投影可持久化。
4. 在 `message` feature 内增加最小消息搜索链路（query + repository + database service + SQL + application service），必要时补充 HTTP 搜索入口。
5. 调整当前 text 插件与 realtime 入站，使其基于新结构工作。
6. 补充测试并执行定向验证。

关键假设与依赖：
- 已确认当前仓库最贴近的模式是 `Plugin + Registry` 与 `Handler + Dispatcher`，应继续沿用。
- 已确认本轮允许调整 text 消息结构，因此可以放弃旧 `content` 兼容压力，直接切到新通用骨架。
- 已确认当前数据库中消息表仍是 `content TEXT NOT NULL`，因此需要通过新迁移演进表结构。

实现要求：
- 消息插件应统一产出正文、预览文本、可检索文本三类投影
- 新增搜索能力必须是 message feature 内部的一部分，而不是外部搜索框架占位
- 不实现具体新消息类型，但新骨架必须能自然承接后续消息类型扩展
- text 插件应成为第一种使用新骨架的实现

测试要求：
- 覆盖 text 插件在新结构下的成功/失败路径
- 覆盖消息搜索主链路的成功/失败路径
- 覆盖 HTTP/realtime 返回结构的关键字段
- 覆盖数据库消息映射与搜索 SQL 行为

质量门禁：
- 受影响 Java 文件无新增编译错误
- 相关测试通过
- 受影响 Maven 模块打包通过
- 不引入新的模块边界违规

复审要求：
- 重点复审：通用性是否真实提升、是否仍只停留在 text 特化、搜索能力是否形成真实闭环、表结构演进是否最小且合理

文档要求：
- 若未形成新的长期项目规则，则不修改 `docs/`
- 任务过程与结果记录在本任务单

验收标准：
- 消息主模型已支持正文 / 预览 / 检索三类通用投影
- text 消息已迁移到新结构
- message feature 已具备最小可用搜索能力
- 后续新消息类型只需新增插件即可参与预览与搜索

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
- 保留当前任务单作为本次消息骨架迭代的追溯记录。

补充说明：
- 本轮允许 text 消息结构随通用骨架同步调整，因此不再把旧 `content` 字段结构兼容作为硬目标。

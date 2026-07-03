任务名称：

chat-domain 领域 API 接口注释全量补充与注释规范完善

任务目标：

对 `chat-domain` 下所有 `domain/api` 对外领域能力接口补充规范、完整、稳定的 JavaDoc 注释，并完善项目注释规范文档，使领域 API 接口具备强制性的类级与方法级注释要求。

任务背景：

当前 `chat-domain` 已完成领域 API 拆分与语义提纯，API 接口成为 feature 对外暴露领域能力的稳定边界。为了让 API 边界具备成熟开源项目级别的可读性、可维护性与 AI 协作可理解性，需要对接口注释做全量补强，并将“领域 API 接口必须有规范完整注释”的要求沉淀到 `docs/注释规范.md`。

影响模块：

- `chat-domain`
- `docs`
- `ai-agent-workplace`

允许修改范围：

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/**/domain/api/*.java`
- `docs/注释规范.md`
- 必要时可补充 `docs/包结构规范.md` 中关于 `domain/api` 的注释要求引用，但仅限规则补充，不做架构变更。
- 当前任务单。

禁止修改范围：

- 不修改任何业务行为、接口方法签名、返回类型、异常语义、响应字段或错误码。
- 不修改 API 拆分结构，不新增或删除领域 API 接口。
- 不修改 `domain/service` 实现代码，除非后续发现接口注释必须引用的术语存在明显错字；如需修改实现注释，应先更新任务单并重新确认。
- 不新增第三方依赖。
- 不修改 Maven 模块结构。
- 不做无关格式化或全仓换行整理。
- 不回滚工作区内既有无关改动。

依赖限制：

- 不引入新依赖。
- 只使用 JavaDoc 与现有文档规范完成任务。

配置限制：

- 不新增、不修改运行时配置。

文档依据：

- `AGENTS.md`
- `docs/注释规范.md`
- `docs/包结构规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：

1. 全量扫描 `chat-domain` 的 `domain/api` 接口清单，确认实际文件范围。
2. 审查每个 API 接口现有类级注释与方法级注释缺口。
3. 为每个 API 接口补充类级 JavaDoc，至少包含：
   - 职责
   - 边界
   - 输入/输出模型类型
   - 失败语义归属
   - 调用方约束
4. 为每个公开方法补充方法级 JavaDoc，至少包含：
   - 业务语义
   - `@param` 参数业务含义
   - `@return` 返回结果业务含义；`void` 方法说明副作用
   - 失败语义或权限/校验约束
5. 完善 `docs/注释规范.md`，新增或强化 `domain/api` 接口注释强制规则。
6. 如 `docs/包结构规范.md` 已描述 `domain/api` 包职责，则仅补充对 `docs/注释规范.md` 的引用或简短规则，不重复长篇说明。
7. 运行编译检查，确认注释补充不影响源码编译。
8. 运行结构扫描，确认没有业务代码改动范围外的残留。
9. 根据 `docs/变更审核清单.md` 自检并归档任务单。

关键假设与依赖：

- 本次只补注释与文档规则，不改变任何代码行为。
- `domain/api` 接口是领域能力对外暴露边界，不直接表达 HTTP/WS 协议细节。
- 方法注释应描述业务契约和失败语义，不重复实现细节。
- 注释语言使用中文，保持当前项目文档与代码注释风格一致。

当前确认的 API 文件范围：

- `auth/domain/api/AuthAccountApi.java`
- `auth/domain/api/AuthSessionApi.java`
- `channel/domain/api/ChannelAccessApi.java`
- `channel/domain/api/ChannelApplicationFlowApi.java`
- `channel/domain/api/ChannelGovernanceApi.java`
- `channel/domain/api/ChannelLifecycleApi.java`
- `channel/domain/api/ChannelQueryApi.java`
- `file/domain/api/FileTransferApi.java`
- `message/domain/api/ChannelMessageAttachmentApi.java`
- `message/domain/api/ChannelMessageLifecycleApi.java`
- `message/domain/api/ChannelMessagePublishingApi.java`
- `message/domain/api/ChannelMessageTimelineApi.java`
- `message/domain/api/ChannelPinApi.java`
- `message/domain/api/MentionApi.java`
- `message/domain/api/MessagePluginCatalogApi.java`
- `server/domain/api/NotificationPreferenceApi.java`
- `server/domain/api/ServerEntranceApi.java`
- `user/domain/api/UserProfileApi.java`

实现要求：

- API 接口类级注释必须说明“这是领域 API，而不是 controller、service facade 或基础设施端口”。
- API 方法注释不得只复述方法名，必须说明业务语义和边界。
- `@param` 不写“command 参数”这类空泛描述，应说明命令/查询代表的业务输入。
- `@return` 不写“返回结果”这类空泛描述，应说明投影或处理结果的业务含义。
- 对可能产生校验失败、权限失败、资源不存在、冲突的 API，应在 JavaDoc 中说明失败语义归属为领域问题异常，不枚举实现细节栈。
- 注释不引入未来功能承诺，不写 TODO 占位。
- 注释必须和当前方法签名、命令、查询、投影类型一致。

测试要求：

- 本任务原则上不新增测试，因为不改行为。
- 必须运行 `mvn -pl chat-domain -am test-compile`。
- 如实际改动触及非注释内容，必须运行 `mvn -pl chat-domain -am test -DskipTests=false` 并在任务单记录原因。

质量门禁：

- 所有 `domain/api/*.java` 文件具备类级 JavaDoc。
- 所有领域 API 公开方法具备方法级 JavaDoc。
- `docs/注释规范.md` 明确写入 `domain/api` 接口强制注释规则。
- `mvn -pl chat-domain -am test-compile` 通过。
- 结构扫描确认 API 文件范围无遗漏。
- 变更审查确认没有业务行为改动。

复审要求：

- 逐个 API 文件检查注释是否准确表达职责、边界、输入、输出和失败语义。
- 检查文档规则是否是长期规范，而不是任务局部说明。
- 检查注释没有把实现类、数据库、HTTP/WS 协议细节泄漏到领域 API 边界中。

文档要求：

- 必须更新 `docs/注释规范.md`。
- 仅当 `docs/包结构规范.md` 中缺少 `domain/api` 注释边界引用时，才做最小补充。
- 不修改其它文档。

验收标准：

- 18 个 `chat-domain` 领域 API 接口均有规范类级注释。
- 18 个接口中的所有公开 API 方法均有规范方法级 JavaDoc。
- 文档明确要求领域 API 接口必须具备注释，且说明注释内容标准。
- 编译验证通过。
- 任务单更新实际结果、验证记录、残留风险后归档为 `done`。

完成定义：

- 代码注释补充完成。
- 文档规则补充完成。
- 验证命令通过。
- 自检记录完整。
- 任务单归档。

实际结果：

- 已为 `chat-domain` 下 18 个 `domain/api` 接口补充规范类级 JavaDoc。
- 已为 18 个接口中的 72 个公开 API 方法补充方法级 JavaDoc。
- 注释覆盖内容包括职责、边界、输入、输出、副作用、约束、失败语义和调用方边界。
- 已更新 `docs/注释规范.md`，新增“领域 API 接口注释”强制规则。
- 已更新 `docs/包结构规范.md`，在 `domain/api` 包规则中引用领域 API 注释强制要求。
- 未修改 API 方法签名、返回类型、业务行为、配置或依赖。

验证记录：

- 覆盖扫描通过：
  - API 文件数：18
  - API 方法数：72
  - 类级 JavaDoc 缺失：0
  - 方法级 JavaDoc 缺失：0
- `mvn -pl chat-domain -am test-compile` 通过。
- 本任务只修改注释与文档，未运行完整测试；此前同一工作树业务测试已通过，本次验证以编译和注释覆盖扫描为门禁。

残留风险：

- 注释基于当前命令、查询、投影和现有业务语义编写；如后续 API 方法语义变化，必须同步维护 JavaDoc。
- 当前工作树中 `domain/api` 目录属于既有未跟踪重构产物，本任务在这些文件上补充注释，不改变其跟踪状态。

知识沉淀 / 是否回写 docs：

- 已回写 `docs/注释规范.md`，形成长期强制规则。
- 已最小补充 `docs/包结构规范.md`，避免 API 包结构规则与注释规则割裂。

产物清理与保留说明：

- 当前任务单将归档为 `done`，作为本次注释规范化工作的可追踪记录。

补充说明：

本任务属于注释与规范完善任务，不是领域 API 结构拆分任务。若执行中发现 API 命名、拆分或方法签名问题，应另起任务单处理。

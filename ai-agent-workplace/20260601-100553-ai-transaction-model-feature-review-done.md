任务名称：

事务模型 feature 适配审查与文档收口

任务目标：

基于当前项目代码，明确各 feature 对统一事务模型的适配状态，收口 `channel/message`、`auth/user`、`file/server` 在 `TransactionRunner` 与 `afterCommit` 语义上的边界，并回写长期有效的架构文档。

任务背景：

当前 `channel` 与 `message` 已完成事务后广播收口，但其它模块是否应统一到同一事务模型仍需按代码事实进一步判断，避免把数据库事务语义错误扩展到对象存储、验证码或 realtime 运行时等不具备同构事务边界的场景。

影响模块：

- `chat-domain`
- `docs`

允许修改范围：

- 允许只读审查 `chat-domain` 相关 feature
- 允许更新 `docs/架构文档.md`
- 允许在 `ai-agent-workplace/` 记录任务与审查结果

禁止修改范围：

- 不修改业务代码
- 不新增依赖
- 不引入 outbox、Saga 或新事务框架
- 不修改模块依赖方向

依赖限制：

- 仅依据现有代码与现有文档给出结论

配置限制：

- 不新增配置

文档依据：

- `docs/架构文档.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：

1. 审核各 feature 的事务边界与副作用类型
2. 固化 `channel/message` 作为事务后广播基线
3. 固化 `auth/user` 当前仅需普通数据库事务的结论
4. 固化 `file/server` 不应直接套用 after-commit 事务模型的边界
5. 补充 future trigger 清单，说明何时应把 `auth/user` 迁到 `afterCommit`

关键假设与依赖：

- 当前判断以仓库内主干代码为准，不推测未来需求
- 只有出现“数据库提交成功后才允许执行”的副作用时，才需要 after-commit

实现要求：

- 结论必须能在源码中定位到证据
- 文档新增内容必须是长期规则，不写任务局部口径

测试要求：

- 本任务属于只读审查 + 文档收口，不适用实现类测试

质量门禁：

- 审查结论与源码证据一致
- 文档表述不引入超出现有实现的新承诺
- `git diff --check` 通过

复审要求：

- 重点复审是否把外部 IO 错误描述为数据库事务一致性能力

文档要求：

- 将长期有效的事务模型适配边界回写到 `docs/架构文档.md`

验收标准：

- 文档中明确哪些 feature 已适配 after-commit
- 文档中明确哪些 feature 仅适合普通事务
- 文档中明确哪些 feature 不应直接套用该模型
- 文档中明确 `auth/user` 迁移到 after-commit 的触发条件

完成定义：

- 审查、文档收口、自检与任务留痕完成

实际结果：

- 已按 feature 审查当前事务模型适配边界
- 已确认 `channel` / `message` 为 `数据库事务 + afterCommit` 基线
- 已确认 `auth` / `user` 当前只需普通数据库事务
- 已确认 `file` / `server` 不应直接套用当前 after-commit 模型
- 已将长期有效的适配结论与迁移触发条件回写到 `docs/架构文档.md`

验证记录：

- 已检索 `chat-domain` 各 feature 的 `TransactionRunner`、realtime 发布与外部 IO 用法作为证据链
- `git diff --check -- docs/架构文档.md ai-agent-workplace/20260601-100553-ai-transaction-model-feature-review-current.md` 通过
- 本任务属于只读审查 + 文档收口，不适用实现类测试

残留风险：

- 本次仅固化当前代码事实；若未来 `auth` / `user` 新增提交后副作用，仍需按触发条件再次评审并落地代码迁移

知识沉淀 / 是否回写 docs：

- 是，回写 `docs/架构文档.md`

产物清理与保留说明：

- 保留任务单作为审查留痕

补充说明：

- 本任务不改变代码行为，只收口架构边界描述

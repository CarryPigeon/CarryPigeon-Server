任务名称：

AI 工作目录清理与自主执行工作流优化

任务目标：

清理 `ai-agent-workplace/` 中已完成但仍堆积在根目录的历史工作文件，删除明显冗余占位文件，并补强 AI 协作流程，使 AI 能在明确边界内自主规划、执行、验证、修复，直到质量门禁通过或形成明确阻塞后才停止。

任务背景：

当前 `ai-agent-workplace/` 已积累多轮历史任务单、PRD 草稿、总结和修复记录。现有 `docs/AI协作开发规范.md`、`docs/任务单模板.md` 与 `docs/变更审核清单.md` 已能约束边界和命名，但对自主执行循环、质量门禁、清理保留策略、长期知识沉淀的规定仍不充分。

影响模块：

- `ai-agent-workplace`
- `docs`

允许修改范围：

- 允许在 `ai-agent-workplace/` 下新增归档目录并移动历史任务单。
- 允许删除经审计确认的冗余 AI 工作文件。
- 允许更新 `ai-agent-workplace/README.md`。
- 允许更新 `docs/AI协作开发规范.md`、`docs/任务单模板.md`、`docs/变更审核清单.md`。

禁止修改范围：

- 不修改正式业务代码。
- 不修改 Maven 依赖与运行配置。
- 不删除仍有唯一追溯价值的任务单内容。
- 不把长期规则只写在 `ai-agent-workplace/` 中替代 `docs/`。

依赖限制：

- 不新增依赖。
- 不引入外部工具链要求。

配置限制：

- 不新增运行时配置。

文档依据：

- `docs/AI协作开发规范.md`
- `docs/任务单模板.md`
- `docs/变更审核清单.md`
- `ai-agent-workplace/README.md`

实现要求：

- 保留 `ai-agent-workplace/README.md` 在根目录。
- 删除明显冗余的最终总结任务壳文件。
- 将已完成的历史任务单归档到 `ai-agent-workplace/archive/`。
- 在正式文档中新增自主执行循环、停止条件、质量门禁、任务单生命周期、清理保留策略和知识沉淀规则。
- 当前任务单完成后归档为 `done`。

测试要求：

- 本任务不修改业务代码，不要求 Maven 测试。
- 需要检查 `ai-agent-workplace/` 根目录是否只保留 README、当前任务单和必要活跃文件。
- 需要检查不存在孤立 `current` 文件。

文档要求：

- 因本任务新增长期 AI 工作流规则，必须回写 `docs/`。

验收标准：

- `ai-agent-workplace/` 根目录不再堆积已完成历史任务单。
- 明显冗余文件已删除或被合并说明覆盖。
- `docs/AI协作开发规范.md` 明确 AI 自主执行循环与停止条件。
- `docs/任务单模板.md` 明确执行计划、质量门禁、验证记录、残留风险、知识沉淀与清理说明字段。
- `docs/变更审核清单.md` 明确执行闭环、质量门禁、复审门禁与 AI 工作目录清理检查。

补充说明：

- 外部实践参考包括 OpenAI Codex best practices、GitHub Copilot coding agent 文档、Anthropic Claude Code best practices 等；本任务只吸收通用工作流原则，不引入外部产品依赖。

实际结果：

- 已删除冗余最终总结任务壳文件：`20260421-112500-ai-final-remediation-summary-done.md`。
- 已将历史 `done` 任务单和历史草稿移入 `ai-agent-workplace/archive/`。
- `ai-agent-workplace/` 根目录当前仅保留：`README.md`、`archive/`、本任务单。
- 已更新 `docs/AI协作开发规范.md`，新增自主执行闭环、停止条件、质量门禁、任务单生命周期、清理保留策略和知识沉淀触发规则。
- 已更新 `docs/任务单模板.md`，新增执行计划、关键假设、质量门禁、复审要求、完成定义、实际结果、验证记录、残留风险、知识沉淀和产物清理字段。
- 已更新 `docs/变更审核清单.md`，新增执行闭环检查、质量门禁检查、复审门禁检查，并扩展 AI 工作目录检查。
- 已更新 `ai-agent-workplace/README.md`，明确文件类型、清理规则和与正式 `docs/` 的关系。

验证记录：

- 已读取 `ai-agent-workplace/` 目录，确认根目录只剩 `README.md`、`archive/` 和当前任务单。
- 已检查 `ai-agent-workplace/**/*current*.md`，仅存在当前治理任务单。
- 已检查 `docs/AI协作开发规范.md`、`docs/变更审核清单.md` 的新增章节编号，未发现本次新增章节编号冲突。
- 已执行 `mvn -pl application-starter -am test -DskipTests=false`，结果为 `BUILD SUCCESS`。

残留风险：

- 本任务主要修改文档和 AI 工作目录，不涉及业务代码变更。
- `archive/` 中保留了历史任务单，后续如需进一步压缩，可在有明确保留期限规则后再做二次归档或删除。

知识沉淀 / 是否回写 docs：

- 已回写 `docs/AI协作开发规范.md`、`docs/任务单模板.md`、`docs/变更审核清单.md`。
- 本任务产生的长期规则不只保存在 `ai-agent-workplace/`。

产物清理与保留说明：

- 保留 `ai-agent-workplace/README.md` 作为工作目录操作说明。
- 保留 `ai-agent-workplace/archive/` 作为历史追溯目录。
- 当前任务单完成后改名为 `done` 并保留在根目录，作为本次治理的最新索引。

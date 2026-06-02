任务名称：
数据库设计阅读与迭代优化方案评估

任务目标：
基于当前仓库文档、Flyway 迁移脚本、database-api/database-impl 与 chat-domain 持久化适配代码，形成对现状数据库设计的可追溯理解，并给出分阶段迭代优化方案。

任务背景：
用户要求先完整理解当前数据库设计，再提出后续优化方向。本次任务属于探索 / 审查类任务，不以修改正式代码为目标。

影响模块：
- `docs/`
- `application-starter`
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `ai-agent-workplace`

允许修改范围：
- 允许在 `ai-agent-workplace/` 新增任务单与分析记录

禁止修改范围：
- 不修改正式源码、测试、配置、迁移脚本与 `docs/`
- 不改变模块职责、依赖方向、数据库 schema 或对外协议

依赖限制：
- 仅基于仓库内现有代码与文档分析

配置限制：
- 不新增或修改任何运行配置

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/基建文档.md`
- `docs/AI协作开发规范.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 阅读架构、包结构、AI 协作文档，确认数据库边界与探索任务门禁。
2. 阅读 Flyway 迁移脚本，梳理表结构、约束、索引与演进路径。
3. 阅读 `database-api`、`database-impl` 与 `chat-domain` 仓储适配代码，建立“领域语义 -> 数据库契约 -> mapper/sql”的映射。
4. 汇总当前数据库设计优点、缺口、风险与不一致点。
5. 输出分阶段迭代优化方案，并记录本次探索结论。

关键假设与依赖：
- 当前仓库中的 Flyway 脚本是数据库设计的主事实来源。
- `database-api` 对 `chat-domain` 暴露的是最小数据库契约，而不是完整领域模型。
- 若发现 schema 与实现不一致，本次仅记录为风险与优化建议，不直接修复。

实现要求：
- 本次不做正式实现。
- 结论必须有仓库内证据支撑。

测试要求：
- 本任务为只读探索 / 审查任务，本次无代码改动，因此不适用实现类测试要求。

质量门禁：
- 已完成必要文档与代码阅读。
- 结论可回溯到仓库内文件。
- 明确说明本次无正式代码改动。

复审要求：
- 重点复核数据库事实来源、持久化边界与 schema/代码一致性判断是否准确。

文档要求：
- 不新增长期规则，不回写 `docs/`。

验收标准：
- 能清楚说明当前数据库表结构分层与持久化职责分配。
- 能指出主要结构性问题、性能风险和一致性风险。
- 能给出可执行的分阶段优化路径。

完成定义：
- 已形成可对外输出的数据库设计理解与优化方案。
- 已记录本次分析过程与结论。

实际结果：
- 已完成对架构文档、Flyway 迁移脚本、`database-api`、`database-impl`、`chat-domain` 持久化适配层与相关历史任务单的只读审查。
- 已确认当前数据库设计主轴为：
  - `auth_account` / `auth_refresh_session` / `user_profile` 构成账户与资料基础表。
  - `chat_channel` / `chat_channel_member` / `chat_channel_invite` / `chat_channel_ban` / `chat_channel_audit_log` / `chat_channel_read_state` / `chat_channel_pin` 构成频道治理与协作数据面。
  - `chat_message` 作为宽表承载消息主投影，并通过 `mentions` / `forwarded_from` 等 JSON 字段继续扩展。
  - `chat_mention`、`chat_notification_*` 作为消息侧与通知侧独立能力表补充主链路。
  - `chat-domain` 通过 repository 抽象依赖 `database-api`，`database-impl` 使用 MyBatis / MyBatis-Plus 承接实际 SQL。
- 已识别出一批高优先级一致性风险与设计债务，主要包括：
  - 迁移脚本与 mapper / entity 存在字段不一致。
  - 某些用户可见字段是查询投影或协议适配字段，而非数据库稳定事实字段。
  - 搜索、mentions、pins、未读统计的索引与查询条件未完全对齐。
  - system channel、pin、mention、message edit 等能力存在数据库约束不足或实现遗漏风险。
- 已形成后续分阶段优化建议，重点先做 schema/实现一致性修复，再做索引与读模型优化，最后再考虑拆分宽表和补充更强一致性机制。

验证记录：
- 本任务为只读探索 / 审查任务，本次无代码改动，因此未执行测试或构建验证。
- 实际执行的验证方式为：
  - 阅读并交叉核对 `docs/`、迁移脚本、`database-api`、`database-impl`、`chat-domain` 持久化适配代码。
  - 对关键判断补充文件级证据定位。

残留风险：
- 本次未执行真实启动或数据库集成验证，因此对“运行时是否已被其它未检视迁移补丁兜底”不做假设。
- 已识别问题中，部分属于明显的代码 / schema 不一致；但在未做实现任务前，本任务不直接下结论为“线上必现”，而是作为高优先级修复候选。

知识沉淀 / 是否回写 docs：
- 目前无。

产物清理与保留说明：
- 保留本任务单作为本次探索任务追溯记录。

补充说明：
- 若后续转入 schema 修复、索引调整或持久化重构，应基于本任务结论另起实现类任务单。

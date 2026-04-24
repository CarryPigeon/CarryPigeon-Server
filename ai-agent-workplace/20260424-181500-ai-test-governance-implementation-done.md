任务名称：

测试治理方案实施

任务目标：

将已确认的测试治理审查结论落实到仓库，使测试规范、注释规则、审核清单、Maven 测试门禁与首批测试分级试点进入可执行状态，并形成完整验证记录。

任务背景：

仓库当前已经有基础测试规范和较稳定的测试代码风格，但缺少测试分级、状态标记、覆盖率报告/门禁和统一执行入口。本次任务在不改变既有架构边界的前提下，落地这些治理能力。

影响模块：

- `docs/测试规范.md`
- `docs/注释规范.md`
- `docs/变更审核清单.md`
- 根 `pom.xml`
- 首批试点测试类
- `ai-agent-workplace/`

允许修改范围：

- 允许更新测试相关规范文档。
- 允许更新根 `pom.xml` 中的测试治理配置。
- 允许为首批试点测试增加分级标记与必要的一致性补充。
- 允许维护本任务单直到完成并改为 `done`。

禁止修改范围：

- 不修改模块依赖方向与架构边界。
- 不新增重量级测试依赖或真实外部服务集成方案。
- 不为追求覆盖率补充无语义测试。
- 不进行无关业务逻辑重构。

依赖限制：

- 继续使用 Spring Boot Test、JUnit 5、JaCoCo 与 Maven Surefire。
- 不默认引入 Failsafe 或额外测试框架，除非确有当前必要。

配置限制：

- 配置集中于根 `pom.xml`。
- 不新增未来占位型配置。

文档依据：

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `docs/测试规范.md`
- `docs/注释规范.md`

任务分解 / 执行计划：

1. 回读相关文档与构建配置，确认本轮落地边界。
2. 更新 `docs/测试规范.md`，补充分级、状态标记、覆盖率和执行入口规则。
3. 更新 `docs/注释规范.md` 与 `docs/变更审核清单.md`，补齐对应治理要求。
4. 更新根 `pom.xml`，增加 JaCoCo 报告/检查与 Tag 筛选入口。
5. 为首批代表性测试类增加 `@Tag`，形成试点闭环。
6. 运行诊断、测试、覆盖率与标签筛选命令，记录结果。
7. 更新任务单实际结果、验证记录、残留风险，并在完成后改为 `done`。

关键假设与依赖：

- 当前仓库现有测试数量和覆盖面足以支撑低风险的首批分级试点。
- Maven Surefire 的 JUnit 5 tag 选择能力可满足当前测试分级执行入口。
- 覆盖率门禁应采用保守阈值，避免与“契约优先”原则冲突。

实现要求：

- 改动必须最小、清晰、可验证。
- 文档规则与构建入口必须一致，不允许文档写法与命令行为脱节。
- 首批试点测试应选择边界清晰、分类明确的类。

测试要求：

- 若修改测试类，必须保持现有命名与注释规范。
- 首批分级至少覆盖 `unit`、`contract`、`smoke`、`regression` 中的代表性测试。

质量门禁：

- 相关文档内容自洽。
- 相关 Java 文件无新增诊断错误。
- `mvn test` 可通过。
- 覆盖率报告可生成。
- 至少一个 Tag 筛选命令可执行并选中对应测试。

复审要求：

- 完成后进行深度自检与 Oracle 复审。

文档要求：

- 长期规则必须落入 `docs/`。

验收标准：

- 测试分级、状态标记、覆盖率与执行入口规则已进入正式文档。
- 根 `pom.xml` 具备与文档一致的最小可执行能力。
- 首批测试分级试点已落地并通过验证。
- 任务单包含实际结果与验证证据。

完成定义：

- 所有计划项完成并验证通过。
- 任务单更新为 `done`。

实际结果：

- 已更新 `docs/测试规范.md`，补充测试分级定义、`@Tag` 使用规则、`@Disabled` 约束、测试状态注释规则、覆盖率原则与 Maven 执行入口。
- 已更新 `docs/注释规范.md`，补充测试状态标记与 `@Tag` 注释边界要求。
- 已更新 `docs/变更审核清单.md`，补充测试分级标签、禁用原因、测试状态注释、筛选命令与覆盖率证据检查项。
- 已更新根 `pom.xml`，增加：
  - `test.groups` / `test.excludedGroups` 入口；
  - Surefire 标签筛选能力；
  - `@{argLine}` 组合，保留 JaCoCo agent 注入；
  - JaCoCo `report` 与 `check` 绑定到 `verify`；
  - 基于当前基线的非零覆盖率门禁：line `0.30`、branch `0.30`。
- 已为首批四个代表性测试类增加主分级标签：
  - `JsonsTests` -> `unit`
  - `GlobalExceptionHandlerTests` -> `contract`
  - `ApplicationStarterSmokeTests` -> `smoke`
  - `MessageAttachmentRegressionTests` -> `regression`
- 已根据 Oracle 复审补齐两处治理一致性缺口：
  - 为 `GlobalExceptionHandlerTests` 增加 validation -> `200` 的契约测试；
  - 为 `JsonsTests` 的失败路径增加异常 message 与 cause 断言。

验证记录：

- `mvn -pl infrastructure-basic -am test -DskipTests=false -Dtest.groups=unit` -> 通过。
- `mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=contract` -> 通过。
- `mvn -pl application-starter -am test -DskipTests=false -Dtest.groups=smoke` -> 通过。
- `mvn -pl application-starter -am test -DskipTests=false -Dtest.groups=regression` -> 通过。
- `mvn test -DskipTests=false` -> 通过。
- `mvn clean verify -DskipTests=false` -> 通过，并生成各模块 JaCoCo 报告。
- 基于生成的 `target/site/jacoco/jacoco.csv` 计算覆盖率基线后，将门禁从 `0.00 / 0.00` 收紧到 `0.30 / 0.30`。
- `mvn verify -DskipTests=false`（最终阈值） -> 通过。
- LSP 诊断在本环境初始化超时，因此以实际 Maven 编译、测试和 `verify` 结果作为最终代码正确性证据。
- 说明：早期并行运行 `smoke` 与 `regression` 命令、以及并行运行 `test` / `verify` 时，分别触发了 Surefire 输出目录争用和 JaCoCo 执行数据污染；随后改为串行重跑，结果全部通过，判定为验证方式造成的伪失败，而非仓库配置缺陷。

残留风险：

- 当前覆盖率门禁是保守起步的非零底线，只能作为最低防线，不能替代对测试质量的人工判断。
- 当前仅完成首批试点标签，不代表全仓测试已经完成全面分级；后续仍需按模块逐步补齐。
- `ApplicationStarterSmokeTests` 当前验证的是最小装配图而非完整启动流程；这符合本轮试点目标，但不应被误读为对所有自动装配漂移的全面覆盖。

知识沉淀 / 是否回写 docs：

- 本任务本身即为长期规则回写任务。

产物清理与保留说明：

- 本任务单已关闭为 `done`，保留为本次测试治理落地的可追溯记录。
- 前一份审查型任务单 `20260424-173500-ai-test-governance-review-done.md` 保留作为方案审查依据；本文件作为实施闭环记录。

补充说明：

- 本次只落地当前确认过的测试治理能力，不扩展到大规模集成测试体系。

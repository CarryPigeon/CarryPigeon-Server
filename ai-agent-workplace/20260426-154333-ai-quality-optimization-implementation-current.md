任务名称：
代码质量优化路线图实施

任务目标：
基于已完成的代码质量/设计深度审查结果，按低风险高收益优先原则，逐步落地核心代码质量优化，包括应用服务瘦身、适配层重复样板收敛、关键测试补强与相关验证，最终形成一轮可验证、可继续演进的质量提升结果。

任务背景：
仓库已经完成一轮深度代码质量与设计审查，结论是：整体架构合理，可继续优化迭代，但 `chat-domain` 大应用服务、adapter / database service 重复代码、控制器失败路径覆盖与部分集成边界测试仍存在明显演进债务。用户明确要求先把优化路线图转成任务单，再依次落地直到完成。

影响模块：
- `chat-domain`
- `application-starter`
- `infrastructure-service/database-impl`
- `infrastructure-basic`
- 根 `pom.xml`
- `docs`（若形成长期规则或说明需要同步）
- `ai-agent-workplace`

允许修改范围：
- 在现有架构内对正式代码、测试、构建门禁做小步优化
- 在 `ai-agent-workplace/` 持续更新任务单与验证记录
- 若需要明确长期规则或说明，可有限更新 `docs/`

禁止修改范围：
- 不改变既有模块职责与依赖方向
- 不把质量优化演变为架构重写
- 不在未建立足够保护前重写 realtime / websocket 核心路径
- 不用 suppress / 临时跳过方式掩盖类型、编译或测试问题

依赖限制：
- 优先使用现有 Spring Boot、Lombok、JUnit、Mockito、MyBatis-Plus 等基线能力
- 不新增新依赖，除非能证明它直接服务于本轮质量提升且放在正确模块

配置限制：
- 保持现有运行配置语义不变
- 不新增未来占位配置
- 所有调整都必须符合现有 `cp.*` 配置规则

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- 已有审查结论（聊天上下文内）

任务分解 / 执行计划：
1. 根据路线图确定第一批低风险高收益优化目标。
2. 优先收敛 `chat-domain` 大应用服务中的重复校验、字符串常量与审计拼装逻辑。
3. 收敛 `DatabaseBacked*Repository` 与 `MybatisPlus*DatabaseService` 层的重复样板与异常翻译。
4. 补强最关键的测试保护层，确保 refactor 有回归保护。
5. 运行定向测试与全仓验证；在回归失败时修复并复验。
6. 完成 Oracle 复审后关闭任务单。

关键假设与依赖：
- 当前架构总体合理，优化应以局部瘦身与去重为主，而非重构模块边界。
- 业务层已有一定测试保护，可支撑第一轮低风险重构。
- 对外部集成边界（database/cache/storage/realtime）要更保守，避免在没有额外测试的情况下大改。

实现要求：
- 每一处改动都必须能够映射到明确的质量问题或路线图项。
- 优先做不改变对外行为的内部重构与去重。
- 先增强保护，再碰高复杂度路径。

测试要求：
- 所有重构都要有对应测试保障，至少包括定向测试与全仓 Maven 验证。
- 若抽取重复逻辑，需确认原有行为与响应码语义不变。

质量门禁：
- 变更后 `mvn test -DskipTests=false` 通过
- 变更后 `mvn clean install -DskipTests=false` 通过
- 改动文件编译通过
- Oracle 最终复审无阻塞问题

复审要求：
- 完成后进行 Oracle 复审，重点检查是否引入行为漂移或把局部优化演变为架构变化。

文档要求：
- 若形成本轮长期适用的质量规则或脚手架变化，再决定是否回写 `docs/`
- 否则至少在任务单中记录实际结果和残留风险

验收标准：
- 第一轮低风险高收益优化完成并通过验证
- 核心重复样板减少，服务类可读性提升，测试保护层增强
- 没有破坏当前分发、启动与既有架构边界

完成定义：
- 任务单记录实际修改、验证记录、残留风险与后续项
- 任务单改为 `done`

实际结果：
- 已完成第一轮低风险高收益代码质量优化，聚焦 message 垂直切片。
- `chat-domain/.../MessageApplicationService.java`：
  - 收敛了重复的正整数参数校验逻辑。
  - 提取了 `system/file/voice/recalled` 等重复字符串常量。
  - 统一了 `idGenerator.nextLongId()` / `timeProvider.nowInstant()` 的调用入口，减少重复样板。
- `infrastructure-service/database-impl/.../MybatisPlusMessageDatabaseService.java`：
  - 将重复的 `try/catch + DatabaseServiceException` 包装逻辑收敛为统一执行辅助方法。
- `infrastructure-service/database-impl/.../MybatisPlusMessageDatabaseServiceTests.java`：
  - 从粗粒度 `any()` 验证升级为 `ArgumentCaptor` 字段级断言，增强映射行为保护。
- 本轮没有改动架构边界，也没有进入 realtime 核心路径重构。

验证记录：
- `mvn -pl chat-domain test -DskipTests=false -Dtest=MessageApplicationServiceSendTests,MessageApplicationServiceQueryTests,MessageApplicationServiceAttachmentTests,DatabaseBackedMessageRepositoryTests,ChannelMessageQueryControllerTests,ChannelMessageAttachmentControllerTests`：成功。
- `mvn -pl infrastructure-service/database-impl test -DskipTests=false -Dtest=MybatisPlusMessageDatabaseServiceTests`：成功。
- `mvn -pl chat-domain -am compile -DskipTests=true`：成功。
- `mvn -pl infrastructure-service/database-impl -am compile -DskipTests=true`：成功。
- `mvn test -DskipTests=false`：成功。
- `mvn clean install -DskipTests=false`：成功。

残留风险：
- 当前仅完成第一轮优化，尚未扩展到 `AuthApplicationService`、`ChannelApplicationService` 与更多 adapter/service 去重。
- Oracle 审查中标出的更高风险区域（realtime / 外部集成边界）本轮仍保持保守，不建议在缺少额外保护前做大改。

知识沉淀 / 是否回写 docs：
- 本轮未形成新的长期项目规则，暂不回写 `docs/`

产物清理与保留说明：
- 当前任务单使用 `current`
- 完成后改为 `done`

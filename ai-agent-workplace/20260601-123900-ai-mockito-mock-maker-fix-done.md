任务名称：
Mockito mock maker 环境修复

任务目标：
修复 `chat-domain` controller 测试在当前 JDK/WSL 环境下因 Mockito inline self-attach 失败而无法执行的问题。

任务背景：
当前 `spring-boot-starter-test` 引入 Mockito 5，默认 mock maker 为 inline。该模式依赖 Byte Buddy agent 自附着，但当前测试 JVM 不支持，导致多个 controller 测试在 `mock(...)` 初始化阶段直接失败。

影响模块：
- `chat-domain`

允许修改范围：
- 允许新增/修改 `chat-domain` 测试资源配置
- 允许补充任务单
- 允许运行定向测试验证

禁止修改范围：
- 不修改生产代码
- 不新增第三方依赖
- 不调整模块依赖方向

依赖限制：
- 仅使用现有 Mockito 能力与测试资源配置

配置限制：
- 仅允许增加测试期 mock maker 配置，不新增运行时配置

文档依据：
- `docs/测试规范.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：
1. 增加 Mockito mock maker 测试资源配置，切换为 subclass 模式。
2. 运行受影响 controller 测试验证不再发生自附着失败。
3. 回写任务单结果并收尾。

关键假设与依赖：
- 当前 controller 测试不依赖 mock final/static/constructor 的 inline 能力。
- `mock-maker-subclass` 足以覆盖现有测试需求。

实现要求：
- 改动应局限在测试目录。
- 修复应对 `chat-domain` 下同类 controller 测试普遍生效。

测试要求：
- 至少执行已知失败的 `ChannelApplicationControllerTests`
- 优先补充同类 controller 测试抽样回归

质量门禁：
- 无生产代码改动
- 定向测试通过

复审要求：
- 检查是否引入对 inline 特性的隐藏回归

文档要求：
- 本次无需更新 `docs/`

验收标准：
- controller 测试不再因 Mockito agent 自附着失败报错
- 定向测试通过

完成定义：
- 配置落地、测试通过、任务单转 `done`

实际结果：
- 已新增 `chat-domain/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- 已将 `chat-domain` 测试默认 mock maker 固定为 `mock-maker-subclass`
- 之前报错的 controller 测试已恢复可执行并通过

验证记录：
- 通过：`mvn -pl chat-domain -am -Dtest=ChannelApplicationControllerTests,ChannelControllerTests,ChannelDiscoverControllerTests,ChannelReadStateControllerTests,AuditLogControllerTests,ChannelBansControllerTests,FileControllerTests,AuthControllerTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：`Tests run: 40, Failures: 0, Errors: 0, Skipped: 0`
- 说明：测试日志中仍出现一条 Mockito self-attaching 提示，但未再导致失败

残留风险：
- 若后续新增依赖 mock final/static/constructor 的测试，需要按测试范围局部恢复 inline 能力或显式配置 agent

知识沉淀 / 是否回写 docs：
- 不回写 docs

产物清理与保留说明：
- 保留任务单

补充说明：
- 本次以最小测试配置修复环境兼容问题。

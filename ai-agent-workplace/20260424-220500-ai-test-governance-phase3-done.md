任务名称：

测试治理第三阶段：处理暂缓测试类并完成分级收口

任务目标：

消化第二阶段中明确暂缓的 4 个混合边界测试类，通过拆分、重组或最小调整让它们满足单一主分级原则，并完成测试治理的最终收口。

任务背景：

第二阶段已将全仓 54 个 `*Tests.java` 中的 50 个完成主分级标签化，仅保留 4 个测试类因边界混合而暂缓。这 4 个类分别混合了应用服务跨用例编排、controller 协议与附件链路、配置装配与内部断言、以及 WebSocket/Netty 实时通道编排。若继续停在暂缓状态，测试治理仍然存在明确尾项，因此需要进入第三阶段做最后收口。

影响模块：

- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/message/application/service/MessageApplicationServiceTests.java`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/message/controller/http/ChannelMessageControllerTests.java`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/config/RealtimeServerConfigurationTests.java`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/controller/ws/RealtimeChannelHandlerTests.java`
- 可能新增或调整的同目录测试类
- `ai-agent-workplace/`

允许修改范围：

- 允许在上述测试类所在目录内拆分或重组测试类。
- 允许新增测试类文件以承接更清晰的主分级职责。
- 允许为拆分后的测试类补充正确的 `@Tag`。
- 允许维护本阶段任务单并在完成后关闭为 `done`。

禁止修改范围：

- 不修改生产业务逻辑，除非仅为测试可访问性所需且不改变行为。
- 不新增新的测试分级标签。
- 不以删除测试覆盖为代价换取分级整洁。
- 不把多个混合关注点继续硬塞回单个测试类。

依赖限制：

- 继续使用现有 JUnit 5 / Surefire / JaCoCo 方案。
- 不新增测试依赖。

配置限制：

- 本阶段默认不修改 `pom.xml` 和正式测试规范文档，除非出现真实阻塞。

文档依据：

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `docs/测试规范.md`
- `docs/注释规范.md`

任务分解 / 执行计划：

1. 深入阅读 4 个暂缓测试类与相关依赖，确定真实混合边界。
2. 形成最小拆分方案，使每个测试类只承担单一主分级职责。
3. 实施测试拆分、文件重命名或新建测试类，并保持原有测试覆盖不回退。
4. 为拆分后的测试类补充 `@Tag`，让原 4 个暂缓点不再处于未分类状态。
5. 运行针对性命令与全量 `verify`。
6. 进行深度自检与 Oracle 复审，关闭任务单为 `done`。

关键假设与依赖：

- 这 4 个类的混合关注点可以在测试层拆分，而不需要修改生产架构。
- 若个别类最终仍无法拆清，需要在本阶段明确给出强约束理由；但默认目标是完成收口而非继续延期。

实现要求：

- 优先做最小拆分，不做与分级无关的测试美化。
- 拆分后命名、注释、断言风格仍需符合既有规范。
- 保持测试覆盖面不缩减。

测试要求：

- 原 4 个暂缓点在本阶段结束时，应被拆分为可准确归类的测试类，或形成极强且已验证的保留理由。
- 新增测试类必须有类级注释和明确分级。

质量门禁：

- 原 4 个暂缓测试类的状态在本阶段得到实质性解决。
- 新增/调整后的测试命令与全量 `verify` 均通过。
- Oracle 复审无阻塞结论。

复审要求：

- 完成后进行深度自检与 Oracle 复审。

文档要求：

- 本阶段以实施收口为主；若出现新的稳定规则再决定是否回写 `docs/`。

验收标准：

- 第二阶段留下的 4 个暂缓点已被拆分并完成分级，或已被严格论证为唯一合理保留项。
- 所有新增/调整测试在现有治理体系下可稳定执行。
- 任务单包含完整证据并关闭为 `done`。

完成定义：

- 所有计划项完成并复验通过。
- 任务单改名为 `done`。

实际结果：

- 已处理第二阶段遗留的 4 个暂缓测试类，并通过拆分完成最终分级收口。
- `MessageApplicationServiceTests` 已拆分为：
  - `MessageApplicationServiceSendTests` -> `contract`
  - `MessageApplicationServiceAttachmentTests` -> `contract`
  - `MessageApplicationServiceQueryTests` -> `contract`
  - `MessageApplicationServiceTestSupport` -> 测试支持类（非 `*Tests.java`）
- `ChannelMessageControllerTests` 已拆分为：
  - `ChannelMessageQueryControllerTests` -> `contract`
  - `ChannelMessageAttachmentControllerTests` -> `contract`
- `RealtimeServerConfigurationTests` 已拆分为：
  - `RealtimeServerConfigurationContextTests` -> `contract`
  - `RealtimeServerConfigurationPublisherFactoryTests` -> `unit`
- `RealtimeChannelHandlerTests` 已拆分为：
  - `RealtimeChannelHandlerLifecycleTests` -> `contract`
  - `RealtimeChannelHandlerMessageDispatchTests` -> `contract`
  - `RealtimeChannelHandlerTestSupport` -> 测试支持类（非 `*Tests.java`）
- 原第二阶段暂缓的 4 个 `*Tests.java` 已全部移除，不再存在未分类的测试类。
- 当前仓库共有 59 个 `*Tests.java`，全部具备单一主分级标签。

验证记录：

- `mvn -pl chat-domain -am test -DskipTests=false -Dtest=MessageApplicationServiceSendTests,MessageApplicationServiceAttachmentTests,MessageApplicationServiceQueryTests,ChannelMessageQueryControllerTests,ChannelMessageAttachmentControllerTests,RealtimeServerConfigurationContextTests,RealtimeServerConfigurationPublisherFactoryTests,RealtimeChannelHandlerLifecycleTests,RealtimeChannelHandlerMessageDispatchTests -Dsurefire.failIfNoSpecifiedTests=false` -> 通过。
- `mvn verify -DskipTests=false` -> 通过。
- 统计复核：当前 59 个 `*Tests.java` 中，0 个未打标签。
- 第三阶段中途出现 1 个明确编译错误：`RealtimeChannelHandlerTestSupport` 中缺失 `pluginRegistry` 局部变量。已修复后重跑验证，全部通过。
- Oracle 复审结论：测试治理任务已在功能上完成，第三阶段唯一待补的是任务单闭环；在补齐本文件并改为 `done` 后，可视为整体任务全部完成。

残留风险：

- `RealtimeServerConfigurationPublisherFactoryTests` 仍带少量实现感知（反射读取字段），但其边界已比旧类清晰，不构成治理闭环阻塞。
- 后续若 `chat-domain` 新增更复杂的 WebSocket 或消息编排测试，应继续遵守“先拆边界，再打单一主标签”的规则，避免重新回到混合测试类状态。

知识沉淀 / 是否回写 docs：

- 预计以实施收口为主；如形成稳定拆分经验，再决定是否补文档。

产物清理与保留说明：

- 本任务单已关闭为 `done`，保留为测试治理第三阶段最终收口记录。
- 第一、第二阶段任务单继续保留，用于串联完整治理闭环。

补充说明：

- 第三阶段目标是收口，不再满足于“记录暂缓”。

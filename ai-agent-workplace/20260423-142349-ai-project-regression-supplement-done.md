任务名称：
项目级回归测试补强

任务目标：
在当前 file / voice 消息链路已经完成真实可用化与附件链路优化的基础上，补齐项目级回归/集成测试中最有价值的缺口，使本轮改动具备更强的跨层回归保障与启动级回归保障。

任务背景：
当前整仓 `mvn test -DskipTests=false` 已通过，但测试分布审查显示：
1. 现有覆盖以单元测试、controller 协议测试、config 装配测试为主；
2. 缺少 `application-starter` 级别的真实启动烟雾回归测试；
3. 缺少围绕消息附件链路的跨层回归测试；
4. websocket 相关目前有 handler / EmbeddedChannel 级测试，但是否需要进一步做认证后整链集成测试，需要在补完前两项后再复评。

影响模块：
- `application-starter`
- `ai-agent-workplace`

允许修改范围：
- 允许在 `application-starter/src/test/java/**` 中新增或修改测试
- 允许新增共享测试支撑配置或内存替身
- 允许更新本任务单记录结论与验证结果

禁止修改范围：
- 不允许修改生产代码行为，除非测试暴露真实缺陷且修复是当前任务范围内必需项
- 不允许新增第三方依赖、Testcontainers 或外部服务依赖
- 不允许为了测试通过而放宽现有业务边界或协议语义

依赖限制：
- 继续使用现有 Spring Boot test、JUnit5、MockMvc、ApplicationContextRunner 等仓库已存在能力
- 优先使用内存替身与 `@TestConfiguration`，避免真实外部服务耦合

配置限制：
- 不新增项目长期配置项
- 测试级覆盖可使用局部测试属性，但不得污染正式 `application.yaml`

文档依据：
- `AGENTS.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 在 `application-starter` 下新增共享测试运行时配置，提供 file / voice 链路回归所需的内存替身。
2. 新增 `application-starter` 启动烟雾测试，验证关键装配在本地可启动条件下可被 Spring 上下文正确装配。
3. 新增消息附件跨层回归测试，至少覆盖：注册/鉴权（若必要）、附件上传、file/voice 发送、历史查询中 `access_url` 派生等关键路径。
4. 重新评估 websocket 认证后整链集成测试是否仍是当前缺口；若前两项已足够覆盖本轮改动，则在任务单中记录“不继续扩张”的理由。
5. 运行整仓测试门禁并记录结果。

关键假设与依赖：
- 当前更高价值的是“可运行的 Spring 上下文级回归测试”，而不是依赖真实外部服务的重型集成测试。
- 本轮可以通过 `application-starter` 下的共享测试配置解决 repository / transaction / storage 等依赖替身问题。
- websocket 认证后整链集成测试只有在前两项后仍存在明显覆盖缺口时才继续扩展。

实现要求：
- 测试新增必须围绕最近两轮消息附件改动的实际回归风险。
- 启动烟雾测试必须落在 `application-starter`，体现项目最终装配入口视角。
- 跨层回归测试必须覆盖真实 Spring Bean 装配下的消息附件关键链路，而不是仅复用已有 isolated unit 测试。

测试要求：
- 启动烟雾测试应验证关键 Bean 与上下文可用性。
- 跨层回归测试应验证 canonical objectKey、发送链路与 history/search 出站 `access_url` 派生。
- 若决定不补 websocket 认证后整链测试，必须在任务单中说明依据。

质量门禁：
- 新增测试通过
- `mvn test -DskipTests=false` 通过
- 不引入新的模块边界违规

复审要求：
- 重点复审新增测试是否真正提升项目级回归能力
- 重点复审共享测试替身是否可维护、可读、不过度耦合实现细节
- 重点复审“不继续扩张 websocket 集成测试”的结论是否有充分依据

文档要求：
- 不修改 `docs/`
- 过程与结论记录在本任务单

验收标准：
- `application-starter` 下存在启动烟雾回归测试
- `application-starter` 下存在 file / voice 附件链路跨层回归测试
- 已明确说明 websocket 认证后整链集成测试在本轮是否需要继续补
- 全项目测试通过

完成定义：
- 任务范围内测试、验证、自检全部完成
- 任务单已补充实际结果、验证记录与残留风险
- 当前任务单可从 `current` 改为 `done`

实际结果：
- 在 `application-starter` 下新增了 starter 级启动烟雾回归测试：`ApplicationStarterSmokeTests`。
- 在 `application-starter` 下新增了 starter 级消息附件跨层回归测试：`MessageAttachmentRegressionTests`。
- 为避免 `@SpringBootTest` 路径受测试类扫描污染，本轮采用 `ApplicationContextRunner + StarterRegressionConfiguration + StarterTestRuntimeConfiguration` 方式，在 `application-starter` 视角下装配真实消息/鉴权 Bean 与内存替身。
- 新增共享测试支撑配置：
  - `StarterRegressionConfiguration`
  - `StarterTestRuntimeConfiguration`
- `InitializationCheckConfigurationTests` 的内部测试专用配置已改为 `@TestConfiguration`，避免污染后续 starter 级回归上下文。
- 已完成 websocket 认证后整链集成测试复评：当前不继续扩张该测试。原因是 websocket 侧已具备 handshake / channel handler / realtime handler / publisher 四层覆盖，本轮新增的 starter 跨层回归已经补足最近消息附件改动的主要项目级风险。

验证记录：
- starter 定向回归：
  - `mvn -pl application-starter -am -Dtest=ApplicationStarterSmokeTests,MessageAttachmentRegressionTests,InitializationCheckConfigurationTests -Dsurefire.failIfNoSpecifiedTests=false test` ✅
- 全项目门禁：
  - `mvn test -DskipTests=false` ✅
  - 结果：全仓测试通过

残留风险：
- 当前未发现本轮范围内残留风险。
- websocket 认证后整链集成测试本轮未新增，但基于现有握手、channel、handler、publisher 测试与新增 starter 跨层回归，当前覆盖已足够支撑最近两轮消息附件改动。

知识沉淀 / 是否回写 docs：
- 本轮未形成新的长期仓库规则，不回写 `docs/`。
- 可复用经验：当 `application-starter` 级回归需要真实 Spring 装配但又不适合依赖外部服务时，优先采用共享测试装配配置与内存替身，而不是引入重型环境依赖。

产物清理与保留说明：
- 保留当前任务单作为本轮项目级回归补强追溯记录。

补充说明：
- 若实现中发现必须引入外部服务容器、Testcontainers 或新的测试依赖，需先更新任务单再继续。

任务名称：
chat-domain 移除应用服务兼容 facade

任务目标：
在不再要求向前兼容的前提下，移除 `ChannelApplicationService` 与 `MessageApplicationService` 兼容 facade，让 controller、realtime 与测试直接依赖最小能力服务，进一步提升可读性与可拓展性。

任务背景：
上一轮已把 channel/message 的命令侧职责拆分为多个能力服务，但仍保留了 facade 与手工构造兼容路径。现在兼容约束已解除，继续保留 facade 会维持伪边界和重复装配成本，阻碍后续结构继续收敛。

影响模块：
- chat-domain
- application-starter
- ai-agent-workplace

允许修改范围：
- 允许修改 chat-domain 主代码与测试代码
- 允许修改 application-starter 测试装配代码
- 允许删除兼容 facade 与兼容构造
- 允许在 ai-agent-workplace 记录任务单

禁止修改范围：
- 不修改 Maven 模块依赖方向
- 不引入新第三方依赖
- 不扩大到 infrastructure-* 主体重构
- 不修改 docs 作为本轮主目标

依赖限制：
- 仅使用当前仓库已有 Spring Boot / Lombok / 测试依赖

配置限制：
- 不新增运行时配置
- 不为过渡保留占位配置

文档依据：
- docs/架构文档.md
- docs/包结构规范.md
- docs/AI协作开发规范.md
- docs/变更审核清单.md
- docs/测试规范.md

任务分解 / 执行计划：
1. 识别 controller、realtime、starter 测试和应用层测试对 facade 的真实依赖面。
2. 将调用点改为直接依赖最小能力服务。
3. 调整测试支撑与手工装配代码，去掉 facade 构造入口。
4. 删除旧 facade 或将其完全退出主调用链。
5. 运行受影响定向测试与 chat-domain / application-starter 相关回归。
6. 回填任务单并归档。

关键假设与依赖：
- 用户已明确确认“不需要向前兼容”。
- 当前能力服务边界已经足以覆盖 controller / realtime 的真实调用需求，不需要引入新的过渡 facade。

实现要求：
- controller 和 realtime 只依赖其实际使用的能力服务。
- 不保留新的 facade 替代旧 facade。
- 测试支撑应围绕能力服务组织，而不是继续围绕旧 facade。

测试要求：
- 至少覆盖受影响的 channel/message controller、realtime、application service 测试。
- 最终运行 chat-domain 离线回归。

质量门禁：
- 相关模块编译通过
- 受影响定向测试通过
- chat-domain 离线回归通过

复审要求：
- 重点检查是否还存在“假 facade”或仅重命名未拆边界的情况。

文档要求：
- 如无新增长期规则，不修改 docs。

验收标准：
- controller / realtime / 测试不再依赖 `ChannelApplicationService` 与 `MessageApplicationService`。
- 旧 facade 被删除或不再承担实际装配入口。
- 受影响测试通过。

完成定义：
- 代码、测试、任务单与归档均完成。

实际结果：
- 已移除 `ChannelApplicationService` 与 `MessageApplicationService` 主代码 facade。
- `chat-domain` 的 controller、realtime 与测试支撑已直接依赖拆分后的最小能力服务。
- `application-starter` 测试装配已改为显式装配拆分后的能力服务，并补齐 `AuthController`、`FileApplicationService` 等新构造参数。
- starter 附件回归测试已按当前文件下载契约更新为校验 canonical object key 下载地址，而不是过时的 share_key 直出地址。
- 已清理测试注释中遗留的旧 facade 名称，避免继续传递过时边界认知。

验证记录：
- 2026-06-25：在 `/tmp/carrypigeon-backend-build-0625b/src` 执行
  `mvn -o -Dmaven.repo.local=/tmp/carrypigeon-m2/repository -pl chat-domain -am -DskipTests install`
  通过，验证 `chat-domain` 主代码与测试代码编译、并确认 facade 删除后模块可安装。
- 2026-06-25：在 `/tmp/carrypigeon-backend-build-0625b/src` 执行
  `mvn -o -Dmaven.repo.local=/tmp/carrypigeon-m2/repository -pl chat-domain -DskipTests=false test`
  通过，结果为 `Tests run: 343, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-06-25：先执行
  `mvn -o -Dmaven.repo.local=/tmp/carrypigeon-m2/repository -pl infrastructure-service/database-impl,infrastructure-service/cache-impl,infrastructure-service/storage-impl,infrastructure-service/mail-impl -am -DskipTests install`
  安装 `application-starter` 所需依赖产物，再执行
  `mvn -o -Dmaven.repo.local=/tmp/carrypigeon-m2/repository -pl application-starter -DskipTests=false test`
  通过，结果为 `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`。
- 2026-06-25：执行
  `rg -n --glob '*.java' '\b(ChannelApplicationService|MessageApplicationService)\b' chat-domain/src/main/java chat-domain/src/test/java application-starter/src/test/java`
  返回空，确认运行时与测试代码已无旧 facade 类型或注释残留。

残留风险：
- `application-starter` 仅验证了模块自身测试。若执行 `mvn -pl application-starter -am test`，当前工作树中仍会先触发 `database-impl` 的既有失败
  `DatabaseServiceAutoConfigurationTests.autoConfiguration_enabled_registersDatabaseBeans`，该问题不属于本任务改动面，但会影响全链路 reactor 回归。
- 测试文件名与类名仍保留部分历史命名（如 `ChannelApplicationService*Tests`），虽然不再依赖旧 facade，但若后续继续提升可读性，可再做一次纯命名收敛。

知识沉淀 / 是否回写 docs：
- 暂不回写，除非形成新的长期架构规则。

产物清理与保留说明：
- 任务单保留在 ai-agent-workplace。

补充说明：
- 本轮目标是“去除兼容壳”，不是继续引入新抽象层。

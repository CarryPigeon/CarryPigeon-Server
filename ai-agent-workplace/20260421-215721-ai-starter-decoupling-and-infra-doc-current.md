任务名称：
application-starter 解耦与 infrastructure-basic AI 文档补强

任务目标：
1. 让 `application-starter` 只负责 Spring Boot 启动、运行时装配入口与初始化检查执行。
2. 评估并在已确认边界内迁出当前位于 starter 的仓储适配与 realtime 运行时职责。
3. 补强 `infrastructure-basic` 文档，使 AI 在多数场景下可先读文档理解通用基建能力、入口、配置点与边界，而不是先读源码。

任务背景：
当前 `application-starter` 中除了 `ApplicationStarter` 外，还存在 auth/user 持久化装配类与 Netty realtime 运行时装配类；这与“starter 只做启动与装配”的目标存在张力。与此同时，`infrastructure-basic` 已形成稳定能力，但现有文档对 AI 的阅读顺序、主要入口、跨模块使用模式和限制说明仍不充分。

影响模块：
- `application-starter`
- `chat-domain`
- `infrastructure-basic`
- `infrastructure-service`
- `docs`
- `ai-agent-workplace`

允许修改范围：
- 允许创建并维护当前任务单。
- 允许补充或改进 `docs/` 下与本任务直接相关的架构/基建文档。
- 允许在**不突破已确认架构边界**的前提下修改 `application-starter`、`chat-domain`、`infrastructure-basic`、`infrastructure-service` 的相关代码、测试和自动配置。
- 允许补充与本次重构直接相关的初始化检查契约、装配和验证测试。

禁止修改范围：
- 不允许引入新的第三方依赖。
- 不允许绕过模块依赖方向约束做临时实现。
- 不允许把可替换外部服务实现塞入 `infrastructure-basic`。
- 不允许在未解决文档/规则冲突前，直接实施会改变模块职责或依赖方向的代码迁移。
- 不允许在未获额外确认前，随意新增长期模块或引入新的高层架构模式。

依赖限制：
- 仅使用仓库当前已有依赖与 Spring Boot/Lombok/Hutool/已存在 Netty 体系。
- 不新增未获许可依赖。

配置限制：
- 保持 `application-starter/src/main/resources/application.yaml` 作为运行时配置入口。
- 不新增未来占位配置。
- 若新增初始化检查相关配置，必须证明当前代码真实使用；优先避免新增配置。

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/配置规范.md`
- `docs/基建文档.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 阅读并交叉验证架构、依赖、配置、AI 协作与审核清单文档。
2. 盘点 `application-starter` 当前超出启动边界的职责，以及 `infrastructure-basic` 的稳定能力与对外入口。
3. 识别阻塞：确认仓储适配迁移、realtime 迁移与 checker 契约落点是否与现有规则冲突。
4. 若需要改变模块职责、依赖方向或文档中的长期边界，先更新文档并记录冲突解法，再进入代码实现。
5. 在已确认边界内实施 starter 解耦与 checker 执行改造。
6. 补强 `infrastructure-basic` 文档，使其具备 AI 先读文档即可建立心智模型的能力。
7. 运行与改动匹配的诊断、测试、构建和自检，记录证据与残留风险。

关键假设与依赖：
- 已确认事实：`application-starter` 当前包含 `AuthPersistenceConfiguration`、`UserProfilePersistenceConfiguration`、`StarterAuthAccountRepository`、`StarterAuthRefreshSessionRepository`、`StarterUserProfileRepository`、`RealtimeServerConfiguration`、`RealtimeServerRuntime`。
- 已确认事实：`infrastructure-service` 已存在 `DatabaseHealthService`、`CacheHealthService`、`StorageHealthService` 及对应实现。
- 已确认事实：用户已明确认可“仓储装配可以通过 Spring DI 在 `chat-domain` 内基于 `database-api` 完成”的方向。
- 当前实现方向：将 starter 中的仓储适配与 feature 运行时装配迁回 `chat-domain`，而不是强行迁往 `infrastructure-service/*-impl`。
- 停止条件：若需要修改模块职责、依赖方向或长期规则且未形成自洽文档落点，则不得继续做破坏性代码迁移。

实现要求：
- `application-starter` 最终只保留启动入口、运行时装配入口与初始化检查执行，不继续承载业务语义转换适配器与独立服务运行时。
- 初始化检查应通过共享契约驱动，starter 仅负责编排执行，不直接硬编码数据库/缓存/存储健康检查细节。
- 文档必须明确说明 `infrastructure-basic` 是固定全局基建，不是外部服务实现承载点。

测试要求：
- 补充或调整与 starter 装配边界、checker 执行边界、文档相关变更匹配的测试。
- 涉及 Spring 自动配置的改动，应至少有 `ApplicationContextRunner` 级别验证。

质量门禁：
- 相关改动无新增 LSP/编译错误。
- 受影响模块测试通过。
- 至少执行一次匹配改动范围的 Maven 构建或测试命令并记录结果。
- 跨模块改动需检查依赖方向与包边界是否仍符合更新后的文档。

复审要求：
- 本任务属于跨模块、架构敏感改动，必须进行独立深度自检；必要时咨询 Oracle。

文档要求：
- 若本次任务修正了长期架构冲突，必须同步回写 `docs/`。
- 必须补强 `docs/基建文档.md` 或相关文档，使 AI 首轮阅读即可理解通用基建边界与入口。

验收标准：
- `application-starter` 的职责与代码落点与目标边界一致，或明确记录被文档规则阻塞的原因。
- 初始化检查机制有清晰契约、装配点与验证证据。
- `infrastructure-basic` 文档可直接回答“它提供什么、从哪里用、配置点在哪、什么不能放进去”。
- 验证记录完整可追溯。

完成定义：
- 任务相关改动、验证、自检、风险记录完成。
- 若实现完成，则将本任务单改为 `done` 并按规则归档；若因架构确认或规则冲突阻塞，则在实际结果与残留风险中明确记录。

实际结果：
- 已完成任务单创建、相关文档阅读、starter 越界职责识别与 Oracle 咨询。
- 已确认当前直接迁移 starter 中仓储适配到 `infrastructure-service/*-impl` 会与现行书面规则冲突；后续改为按用户确认方向，将仓储装配迁入 `chat-domain` 并通过 `database-api` 完成 Spring DI。
- 已在 `infrastructure-basic` 新增共享初始化检查契约、结果模型与失败异常。
- 已在 `database-impl`、`cache-impl`、`storage-impl` 中新增基于既有 health service 的初始化检查适配器，并接入各自自动配置。
- 已在 `application-starter` 新增初始化检查执行器与相关测试，使 starter 可以统一发现并执行共享检查契约。
- 已补强 `docs/架构文档.md`、`docs/包结构规范.md`、`docs/基建文档.md`，使 AI 可先从文档理解 starter 边界、共享启动检查契约和 `infrastructure-basic` 的能力地图；当前继续按用户确认方向收尾 starter 解耦。
- 已将 auth/user 仓储装配与薄适配器迁入 `chat-domain`，并通过 `database-api` 完成 Spring DI。
- 已将 realtime 配置与运行时宿主迁入 `chat-domain.features.server.config`，不再由 starter 承担该 feature 的运行时装配。
- 已将初始化检查执行器改为 `SmartInitializingSingleton`，使必需检查早于 `SmartLifecycle` 类型运行时执行。
- 已为 WebSocket 握手添加 `Authorization: Bearer <access-token>` 校验，并将认证主体绑定到 Netty channel。
- 已将 realtime 默认配置收紧为 `enabled=false`、`host=127.0.0.1`，并收紧 JWT secret 强度要求。

验证记录：
- 已完成文档与代码阅读校验，待补充实现后的诊断、测试与构建记录。
- `lsp_diagnostics`：尝试执行，但当前环境下 Java LSP 初始化失败/超时，未能形成可用诊断结果；已使用 Maven 编译、测试与整包构建作为替代验证。
- `mvn -pl application-starter -am test -DskipTests=false`：通过。
- `mvn clean install -DskipTests=false`：通过。
- `mvn -s "../maven-settings.xml" -Dmaven.repo.local=/tmp/carrypigeon-m2/repository spring-boot:run -Dspring-boot.run.arguments="--cp.infrastructure.service.database.enabled=false --cp.infrastructure.service.cache.enabled=false --cp.infrastructure.service.storage.enabled=false --cp.chat.server.realtime.enabled=false --cp.chat.auth.jwt.secret=test-secret"`（在 `application-starter` 模块下执行）：启动失败，原因是关闭 database service 后无法提供 `AuthAccountRepository` Bean；该现象暴露了当前应用对数据库仓储装配的既有依赖，不是本轮初始化检查契约新增逻辑导致的编译或测试失败。
- 在按用户确认方向完成仓储与 realtime 迁移后，再次执行 `mvn -pl application-starter -am test -DskipTests=false`：通过。
- 再次执行 `mvn clean install -DskipTests=false`：通过。
- 执行 `mvn -s "../maven-settings.xml" -Dmaven.repo.local=/tmp/carrypigeon-m2/repository spring-boot:run -Dspring-boot.run.arguments="--cp.chat.auth.jwt.secret=test-secret --cp.chat.server.realtime.enabled=false"`（在 `application-starter` 模块下执行）：应用完成到 Tomcat 初始化与 chat-domain Bean 装配阶段，但因本地 MySQL/Flyway 凭据不可用而失败，报错为 `Access denied for user 'root'@'localhost' (using password: NO)`；该失败反映本地环境外部依赖不可用，不是 starter 解耦后的 Bean 丢失。
- 已完成 5 路 post-implementation review：Goal review = FAIL；QA = PASS（带环境限制）；Code quality = FAIL；Security = FAIL（Medium）；Context mining = PASS。
- 在修复初始化检查时序、WebSocket 握手鉴权、默认监听暴露面和 JWT secret 强度后，再次执行 `mvn -pl application-starter -am test -DskipTests=false`：通过。
- 在上述修复后再次执行 `mvn clean install -DskipTests=false`：通过。
- 执行 `mvn -s "../maven-settings.xml" -Dmaven.repo.local=/tmp/carrypigeon-m2/repository spring-boot:run -Dspring-boot.run.arguments="--cp.chat.auth.jwt.secret=test-secret-test-secret-test-secret"`（在 `application-starter` 模块下执行）：Spring 进入 Tomcat 初始化并加载 chat-domain 仓储装配，但仍因本地 MySQL/Flyway 凭据不可用失败；当前未出现因握手鉴权、初始化检查时序或 JWT 强度校验导致的新增启动异常。

 残留风险：
- 当前 live run 仍依赖本地 MySQL/Flyway 可用性，无法在本环境下完成完整正向启动验证。
- 复审修复后仍需再次执行最终 review-work，确认先前的 FAIL 项是否全部关闭。

知识沉淀 / 是否回写 docs：
- 需要。至少涉及 starter 边界、checker 契约落点和 infrastructure-basic AI 阅读入口。

产物清理与保留说明：
- 保留当前任务单；完成后改名为 `done`，并按仓库规则归档。

补充说明：
- Oracle 已给出建议：checker 共享契约更适合放在共享模块（按当前规则更偏向 `infrastructure-basic`），而不是继续放在 starter 中；但仓储适配迁移前必须先解决文档规则冲突。
- 用户后续已明确确认：仓储装配可通过 Spring DI 在 `chat-domain` 内基于 `database-api` 完成，因此本轮已按该方向实现。

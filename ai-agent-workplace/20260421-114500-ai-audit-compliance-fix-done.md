# 审核不合规项纠正任务单

任务名称：

audit-compliance-fix

任务目标：

在不引入新架构、不过度扩散修改范围的前提下，纠正本轮审核中已确认的不合规项，使 auth 持久化实现重新符合模块与包结构边界，并收敛运行配置中的默认敏感信息表达方式。

任务背景：

本轮审核已确认：`chat-domain` 中存在具体 auth 仓储实现，违反“repository 的实现不在 chat-domain”的项目规则；同时 `application-starter/src/main/resources/application.yaml` 中存在本地数据库凭据与 JWT secret 默认值，存在配置合规与安全表达风险。用户已明确要求继续并对不合规项进行纠正。

影响模块：

- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `application-starter`
- `ai-agent-workplace`

允许修改范围：

- 调整 auth 持久化实现的落点与相关 Spring 装配
- 在必要范围内调整 `database-api` 与 `database-impl` 的契约或实现承载方式
- 删除 `chat-domain` 中不合规的 persistence 实现文件
- 修改 `application-starter/src/main/resources/application.yaml` 以收敛默认敏感配置表达
- 补充或调整与本次修正直接相关的测试
- 在 `ai-agent-workplace/` 记录任务单与最终自检结果

禁止修改范围：

- 不修改既有模块职责定义
- 不新增未获批准的新依赖
- 不扩展 auth 业务能力范围
- 不顺带重构无关 feature
- 不修改 `docs/` 中长期规则，除非发现本次修正必须依赖新的已确认长期规则

依赖限制：

- 保持既有模块依赖方向不变或更收敛
- 不让 `chat-domain` 依赖任何 `*-impl`
- 不把可替换外部服务实现放入 `infrastructure-basic`

配置限制：

- 保持 `application-starter` 为最终运行配置入口
- 只做当前真实使用且能提升合规性的最小配置修正
- 不新增未来占位配置

文档依据：

- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/配置规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

实现要求：

- `chat-domain` 仅保留 auth 领域仓储抽象，不保留其具体 persistence 实现
- auth 持久化实现应落入符合规则的 `infrastructure-service` 模块中
- 修正后仍保持现有应用层与控制层契约稳定
- 所有新落点类与边界方法补齐职责/边界注释

测试要求：

- 补充或调整本次修正直接影响的测试
- 至少完成与 auth 持久化装配相关的最小验证
- 执行与本次修正相关的 Maven 测试/构建验证

文档要求：

- 本任务执行中使用 `current` 状态
- 完成后重命名为 `done`
- 若未引入新的长期规则，不修改 `docs/`

验收标准：

- `chat-domain` 中不再存在 auth 持久化实现类
- auth 持久化实现落点符合 `infrastructure-service` 边界
- 相关 Spring 装配与测试通过
- `application.yaml` 中默认敏感配置表达方式得到收敛且不违反当前配置规则
- 能清晰说明改动内容、影响范围、测试结果与残留风险

补充说明：

- 本任务基于用户在当前会话中的明确修正指令执行
- 本任务以“纠正已确认不合规项”为边界，不做额外开放性优化
- 2026-04-21 进一步确认采用最小合规修正路径：auth 仓储实现不下沉到 `database-impl`，而是放入 `application-starter` 作为运行时装配适配器，以同时满足“实现不在 chat-domain”与“*-impl 不依赖 chat-domain”两项边界

## 实际结果

- 已删除 `chat-domain` 中不合规的 auth 持久化实现：`AuthAccountRepositoryImpl`、`AuthRefreshSessionRepositoryImpl`
- 已在 `application-starter` 新增 `AuthPersistenceConfiguration`，由启动层装配 auth 仓储抽象到 `database-api` 服务契约之间的运行时适配器
- 已新增 `StarterAuthAccountRepository` 与 `StarterAuthRefreshSessionRepository` 作为最小运行时持久化适配器
- 已新增 `AuthPersistenceConfigurationTests` 验证数据库服务启用/禁用时的仓储装配边界
- 已将 `application.yaml` 中数据库用户名/密码、Redis 密码、MinIO access-key/secret-key、JWT secret 收敛为环境变量占位符表达

## 实际影响文件

- `application-starter/src/main/java/team/carrypigeon/backend/starter/config/AuthPersistenceConfiguration.java`
- `application-starter/src/main/java/team/carrypigeon/backend/starter/config/StarterAuthAccountRepository.java`
- `application-starter/src/main/java/team/carrypigeon/backend/starter/config/StarterAuthRefreshSessionRepository.java`
- `application-starter/src/test/java/team/carrypigeon/backend/starter/config/AuthPersistenceConfigurationTests.java`
- `application-starter/src/main/resources/application.yaml`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/support/persistence/AuthAccountRepositoryImpl.java`（已删除）
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/support/persistence/AuthRefreshSessionRepositoryImpl.java`（已删除）
- `ai-agent-workplace/20260421-114500-ai-audit-compliance-fix-done.md`

## 自检与验收记录

- 架构边界：已修正。`chat-domain` 中不再保留 auth 持久化实现，运行时适配器改由 `application-starter` 装配，未引入 `*-impl -> chat-domain` 违规依赖
- 配置边界：已修正。敏感运行值改为环境变量占位符表达，仍保持 `application-starter` 为最终运行配置入口
- 测试验证：已执行 `mvn -pl application-starter -am test -DskipTests=false`，结果通过
- 启动验证：已执行 `SPRING_MAIN_LAZY_INITIALIZATION=true CP_CHAT_AUTH_JWT_SECRET=test-secret mvn -pl application-starter spring-boot:run -Dspring-boot.run.arguments=--cp.chat.server.realtime.enabled=false`，Spring Boot 成功启动并进入运行态；命令因长期运行超过工具超时被终止，不属于启动失败
- 诊断验证：当前环境缺少 `jdtls`，无法执行 Java LSP 诊断；已以 Maven 编译、测试与实际启动结果作为替代证据

## 残留风险与未完成项

- `AuthJwtProperties` 仍保留本地开发默认 secret 回退逻辑，虽然配置文件已不再提交具体 secret，但运行时若未显式提供变量仍会回退到本地开发值
- 本轮仅纠正已确认不合规项，未进一步收敛其它仓库内历史默认凭据策略

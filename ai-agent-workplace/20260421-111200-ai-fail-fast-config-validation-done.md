# 配置失败前置校验加固任务单

任务名称：

fail-fast-config-validation

任务目标：

在不重构整体配置体系的前提下，为当前已识别的关键敏感配置补充更清晰的失败前置校验，使缺失 JWT secret 或 MinIO 凭据时能以可读、可定位的配置异常尽早失败，而不是等到更深层 Bean 初始化时报通用错误。

任务背景：

上一轮已去除 runtime 路径上的固定敏感默认值，当前应用在缺失关键敏感配置时会进入 fail-fast，但主要依赖后续 Bean 创建时报错，错误语义仍可进一步收紧为“配置缺失”级别。用户继续要求推进这一步。

影响模块：

- `chat-domain`
- `infrastructure-service/storage-impl`
- `ai-agent-workplace`

允许修改范围：

- 调整 `AuthJwtProperties` 的构造校验逻辑
- 调整 `MinioStorageProperties` 的构造校验逻辑或相关最小装配校验
- 补充或调整与本次失败前置校验直接相关的测试
- 在 `ai-agent-workplace/` 记录任务过程与结果

禁止修改范围：

- 不重构整体配置体系
- 不扩展业务能力
- 不新增未获批准的新依赖
- 不顺带修改无关配置项

依赖限制：

- 保持既有模块依赖方向不变
- 不引入新的校验框架或第三方依赖

配置限制：

- 只对当前已识别的关键敏感配置做更明确的失败前置校验
- 不新增未来占位配置

文档依据：

- `docs/配置规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

实现要求：

- 缺失 JWT secret 时应报出明确可读的配置错误
- 启用对象存储实现但缺失 MinIO access-key / secret-key 时应报出明确可读的配置错误
- 尽量复用现有 record 构造器与最小测试体系，不扩张实现面

测试要求：

- 补充或调整本次校验增强直接影响的测试
- 执行受影响模块相关的 Maven 测试与最小启动验证

文档要求：

- 本任务执行中使用 `current` 状态
- 完成后改为 `done`
- 若未引入新的长期规则，不修改 `docs/`

验收标准：

- 缺失关键敏感配置时能得到更清晰的失败信息
- 相关测试通过
- 启动验证仍可在显式提供环境变量的情况下通过

补充说明：

- 本任务基于用户在当前会话中的继续整改指令执行
- 本轮目标是“更清晰的 fail-fast”，不是再做一轮开放性巡检

## 实际结果

- 已在 `AuthJwtProperties` 中补充明确前置校验：缺失 `cp.chat.auth.jwt.secret` 时直接抛出可读配置异常
- 已在 `MinioStorageProperties` 中补充明确前置校验：启用对象存储实现但缺失 `access-key` 或 `secret-key` 时直接抛出可读配置异常
- 已将 `MinioStorageProperties` 的默认构造调整为默认关闭对象存储实现，避免无凭据默认启用导致的隐式失败路径
- 已新增 `AuthJwtPropertiesTests`，并扩充 `MinioStoragePropertiesTests`，覆盖新的 fail-fast 语义

## 实际影响文件

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/config/AuthJwtProperties.java`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/auth/config/AuthJwtPropertiesTests.java`
- `infrastructure-service/storage-impl/src/main/java/team/carrypigeon/backend/infrastructure/service/storage/impl/config/MinioStorageProperties.java`
- `infrastructure-service/storage-impl/src/test/java/team/carrypigeon/backend/infrastructure/service/storage/impl/config/MinioStoragePropertiesTests.java`
- `infrastructure-service/storage-impl/src/test/java/team/carrypigeon/backend/infrastructure/service/storage/impl/config/StorageServiceAutoConfigurationTests.java`
- `ai-agent-workplace/20260421-111200-ai-fail-fast-config-validation-done.md`

## 自检与验收记录

- fail-fast 目标：已满足。缺失关键敏感配置时不再依赖更深层 Bean 初始化异常，而是在配置对象阶段报出更明确的错误
- 测试验证：已执行 `mvn -pl application-starter -am test -DskipTests=false`，结果通过
- 启动验证：已执行 `MYSQL_USERNAME=carrypigeon MYSQL_PASSWORD=carrypigeon123 REDIS_PASSWORD=carrypigeon123 MINIO_ROOT_USER=carrypigeon MINIO_ROOT_PASSWORD=carrypigeon123 CP_CHAT_AUTH_JWT_SECRET=test-secret SPRING_MAIN_LAZY_INITIALIZATION=true mvn -pl application-starter spring-boot:run -Dspring-boot.run.arguments=--cp.chat.server.realtime.enabled=false`，Spring Boot 成功启动并进入运行态；命令因服务长期运行超过工具超时被终止，不属于启动失败
- 诊断验证：当前环境缺少 `jdtls`，无法执行 Java LSP 诊断；已以 Maven 测试和实际启动结果作为替代证据

## 残留风险与未完成项

- 当前数据库用户名/密码与 Redis/MinIO 凭据仍主要依赖外部环境变量提供，但已不再有仓库内 live runtime fallback；本轮未继续引入更重的统一配置校验层
- 本轮保持最小改动原则，未进一步调整 Docker 引导模板中的本地默认值

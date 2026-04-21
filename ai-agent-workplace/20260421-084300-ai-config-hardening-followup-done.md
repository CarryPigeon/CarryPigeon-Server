# 第二轮配置合规加固任务单

任务名称：

config-hardening-followup

任务目标：

在不扩展业务范围、不引入新依赖的前提下，继续纠正第二轮巡检中剩余的配置与密钥回退问题，避免应用在缺失环境变量时回退到仓库内固定敏感值。

任务背景：

上一轮整改已移除 `chat-domain` 中不合规的 auth 持久化实现，并将 `application.yaml` 中多项敏感运行值改为环境变量占位符。但第二轮巡检确认仍存在代码级与配置级的默认敏感值回退，包括 JWT secret、MinIO 访问密钥以及部分运行时配置中的弱默认凭据表达。

影响模块：

- `chat-domain`
- `infrastructure-service/storage-impl`
- `application-starter`
- `ai-agent-workplace`

允许修改范围：

- 调整 `AuthJwtProperties` 的 secret 默认回退策略
- 调整 `MinioStorageProperties` 的 access-key / secret-key 默认回退策略
- 调整 `application-starter/src/main/resources/application.yaml` 中相关敏感配置的 fallback 表达
- 补充或调整与本次加固直接相关的测试
- 在 `ai-agent-workplace/` 记录本轮任务与自检结果

禁止修改范围：

- 不修改模块职责与依赖方向
- 不扩展 auth 或 storage 业务能力
- 不新增未获批准的新依赖
- 不顺带重构无关配置体系

依赖限制：

- 保持既有模块依赖方向不变
- 不让 `chat-domain` 依赖任何 `*-impl`
- 不为配置加固引入额外第三方库

配置限制：

- 保持 `application-starter` 为最终运行配置入口
- 只处理当前已确认存在风险的敏感回退项
- 不新增未来占位配置

文档依据：

- `docs/配置规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

实现要求：

- 应用在缺失关键敏感配置时不得再静默回退到仓库内固定 secret 或固定访问凭据
- 代码级默认值与 `application.yaml` fallback 表达应保持一致且可解释
- 对本次调整涉及的配置类与测试补齐必要注释

测试要求：

- 补充或调整本次配置加固直接影响的测试
- 执行与受影响模块相关的 Maven 测试与最小启动验证

文档要求：

- 本任务执行中使用 `current` 状态
- 完成后改为 `done`
- 若未引入新的长期规则，不修改 `docs/`

验收标准：

- JWT secret 不再回退到仓库内固定值
- MinIO access-key / secret-key 不再回退到仓库内固定值
- `application.yaml` 中相关敏感值 fallback 策略得到收敛
- 受影响测试与启动验证通过
- 能清晰说明本轮改动、验证方式与残留风险

补充说明：

- 本任务基于用户在当前会话中的继续整改指令执行
- 本轮以“配置与密钥回退加固”为边界，不再展开新一轮开放式巡检

## 实际结果

- 已调整 `AuthJwtProperties`，去除仓库内固定 JWT secret 回退，改为缺失时保持空值，由运行时进入 fail-fast 路径
- 已调整 `MinioStorageProperties`，去除 access-key / secret-key 的固定默认值，仅保留 endpoint 与 bucket 的非敏感默认值
- 已调整 `application-starter/src/main/resources/application.yaml`，移除数据库用户名/密码、Redis 密码、MinIO access-key / secret-key 的弱 fallback 默认值
- 已同步修正 `MinioStoragePropertiesTests` 与 `StorageServiceAutoConfigurationTests`，使测试显式提供所需凭据，而不再依赖隐式敏感默认值

## 实际影响文件

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/config/AuthJwtProperties.java`
- `infrastructure-service/storage-impl/src/main/java/team/carrypigeon/backend/infrastructure/service/storage/impl/config/MinioStorageProperties.java`
- `infrastructure-service/storage-impl/src/test/java/team/carrypigeon/backend/infrastructure/service/storage/impl/config/MinioStoragePropertiesTests.java`
- `infrastructure-service/storage-impl/src/test/java/team/carrypigeon/backend/infrastructure/service/storage/impl/config/StorageServiceAutoConfigurationTests.java`
- `application-starter/src/main/resources/application.yaml`
- `ai-agent-workplace/20260421-084300-ai-config-hardening-followup-done.md`

## 自检与验收记录

- 配置加固目标：已满足。运行时不再静默回退到仓库内固定 JWT secret、固定 MinIO 凭据或 `application.yaml` 中的弱默认凭据
- 测试验证：已执行 `mvn -pl application-starter -am test -DskipTests=false`，结果通过
- 启动验证：已执行 `MYSQL_USERNAME=carrypigeon MYSQL_PASSWORD=carrypigeon123 REDIS_PASSWORD=carrypigeon123 MINIO_ROOT_USER=carrypigeon MINIO_ROOT_PASSWORD=carrypigeon123 CP_CHAT_AUTH_JWT_SECRET=test-secret SPRING_MAIN_LAZY_INITIALIZATION=true mvn -pl application-starter spring-boot:run -Dspring-boot.run.arguments=--cp.chat.server.realtime.enabled=false`，Spring Boot 成功启动并进入运行态；命令因服务长期运行超过工具超时被终止，不属于启动失败
- 诊断验证：当前环境缺少 `jdtls`，无法执行 Java LSP 诊断；已以 Maven 测试和实际启动结果作为替代证据

## 残留风险与未完成项

- `docker-compose.yaml` 与 `.env.example` 仍保留本地开发默认凭据模板，这是当前有意保留的本地引导材料，不属于本轮 live runtime fallback 加固范围
- 当前缺失敏感配置时主要依赖运行时 Bean 创建失败实现 fail-fast，尚未额外补充更显式的配置绑定校验提示

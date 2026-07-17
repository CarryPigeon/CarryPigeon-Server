# 真实环境测试补齐任务单

## 任务类型

实现类任务。继续补齐三层测试体系中的真实环境测试场景。

## 任务目标

- 在不改变生产代码和模块依赖方向的前提下，补充可显式运行的 `env-*` 测试本体。
- 真实环境测试必须提前注入本用例数据，并在结束后清理，避免因人工数据缺失导致失败。
- 没有真实环境配置时，测试必须通过 JUnit assumptions 显式跳过，不能伪装成 mock 测试。
- 同步更新测试规范或命令说明中必要的真实环境执行入口。

## 受影响模块

- `infrastructure-service/database-impl/src/test/java`
- `infrastructure-service/cache-impl/src/test/java`（如能稳定补齐）
- `infrastructure-service/storage-impl/src/test/java`（如能稳定补齐）
- `infrastructure-service/mail-impl/src/test/java`（如能稳定补齐）
- `application-starter/src/test/java`（修复旧测试引用，并补齐可显式运行的最小 `env-app` 入口）
- `infrastructure-service/cache-impl/src/main/java`（仅限修复阻断全量验证的既有缓存契约缺陷）
- `infrastructure-service/storage-impl/src/main/java`（仅限修复阻断全量验证的既有对象存储契约缺陷）
- `docs/standards/测试规范.md`（如命令或环境变量约定需要补充）
- `ai-agent-workplace/` 当前任务单

## 允许修改范围

- `env-*` 测试类、测试辅助类、测试资源。
- 测试规范中的真实环境执行命令与环境变量说明。
- `application-starter` 测试侧配置、烟雾测试、回归测试和真实应用装配测试。
- 阻断测试全量验证的极小生产缺陷修复，必须有既有测试契约支撑。
- 不新增生产依赖，不修改生产业务逻辑。

## 禁止边界

- 不让 `chat-domain` 依赖或直接使用 `infrastructure-service/*-impl`。
- 不把真实环境测试放进 `chat-domain`。
- 不修改生产代码行为来迁就测试。
- 不为新增测试任意改变生产行为；仅允许修复既有测试已明确定义的契约缺陷。
- 不新增 Testcontainers 或其它测试依赖，除非再次确认。
- 不处理与本任务无关的既有脏状态。

## 依据文档

- `docs/standards/测试规范.md`
- `docs/operations/Docker配置.md`
- `docs/operations/数据库部署手册.md`
- `docs/sql/README.md`
- `docs/standards/变更审核清单.md`

## 执行计划

1. 梳理 Docker、SQL、配置和现有 external service impl API。
2. 优先补齐 `database-impl` 的 `env-db` 测试，使用真实数据库配置门禁、主动插入测试数据、验证读写链路、反向清理。
3. 评估并补齐 Redis/MinIO/SMTP 的最小真实环境测试，如缺少稳定配置则记录未完成风险。
4. 修复 `application-starter` 旧测试对已删除 application 层类的引用，改为当前 domain API 装配。
5. 补齐 `application-starter` 的最小 `env-app` 入口，默认跳过，显式开启后验证真实装配前置门禁。
6. 运行新增测试的无环境门禁验证，确保默认环境不会误失败。
7. 运行现有 `business` 和 `chat-domain` 全量测试，确认未破坏已有分层。
8. 按审核清单记录结果并归档任务单。

## 验收标准

- 至少新增一个真实环境测试本体，并使用 `@Tag("env")` 和具体 `@Tag("env-*")`。
- 真实环境测试有明确环境变量门禁。
- 真实环境测试用例主动创建并清理数据。
- 无真实环境配置时，相关测试跳过而不是失败。
- 已执行新增测试命令、`business` 命令和 `chat-domain` 全量命令。
- `application-starter` 测试编译不再被旧包引用阻塞，`env-app` 命令有明确结果。

## 完成记录

- 已补齐 `database-impl`、`cache-impl`、`storage-impl`、`mail-impl` 的真实环境测试入口，分别使用 `env-db`、`env-cache`、`env-storage`、`env-mail` 标签。
- 已补齐 `application-starter` 的 `env-app` 测试入口，默认跳过，显式开关后验证真实 Spring Boot 装配与数据库、缓存、对象存储健康 Bean。
- 已为真实数据库测试补充 `EnvTestDataScope`，用于唯一命名空间和反向清理动作管理。
- 已修复 `application-starter` 旧测试对已删除 application 层包的引用，改为当前 domain API 装配。
- 已更新 `docs/standards/测试规范.md`，补充三层测试模型、真实环境标签、环境变量开关和推荐命令。
- 已在阻断全量验证时做最小生产契约修复：
  - `RedisCacheService.exists` 将 Redis `hasKey` 的 `null` 返回映射为 `false`。
  - `MinioObjectStorageService.get` 明确返回元数据视图，不隐式拉取对象内容。

## 验证结果

- `mvn -pl application-starter -am test-compile -DskipTests=false`：通过。
- `mvn -pl application-starter -am test -DskipTests=false -Dtest=ApplicationStarterEnvTests -Dsurefire.failIfNoSpecifiedTests=false`：通过，`env-app` 默认跳过 1 个。
- `mvn -pl application-starter -am test -DskipTests=false -Dtest=ApplicationStarterSmokeTests,MessageAttachmentRegressionTests -Dsurefire.failIfNoSpecifiedTests=false`：通过 4 个。
- `mvn -pl application-starter -am test -DskipTests=false -Dtest='*EnvTests' -Dtest.groups=env-app -Dsurefire.failIfNoSpecifiedTests=false`：通过，`env-app` 默认跳过 1 个。
- `mvn -pl infrastructure-service/cache-impl -am test -DskipTests=false -Dtest=RedisCacheServiceTests -Dsurefire.failIfNoSpecifiedTests=false`：通过 10 个。
- `mvn -pl infrastructure-service/storage-impl -am test -DskipTests=false -Dtest=MinioObjectStorageServiceTests,MinioObjectStorageServiceEnvTests -Dsurefire.failIfNoSpecifiedTests=false`：通过 6 个，其中 `env-storage` 默认跳过 1 个。
- `mvn -pl infrastructure-service/storage-impl -am test -DskipTests=false -Dtest='*EnvTests' -Dtest.groups=env-storage -Dsurefire.failIfNoSpecifiedTests=false`：通过，`env-storage` 默认跳过 1 个。
- `mvn -pl chat-domain -am test -DskipTests=false -Dtest.groups=business`：通过 38 个。
- `mvn -pl chat-domain -am test -DskipTests=false`：通过 340 个。
- `mvn -pl application-starter -am test -DskipTests=false`：通过，13 个 reactor 模块全部成功；其中真实环境测试默认跳过 5 个，符合显式开关设计。

## 剩余风险

- `env-*` 测试默认只验证跳过门禁；若要验证真实链路，需要准备 Docker 或目标测试环境并打开对应 `CP_ENV_*_TEST_ENABLED` 开关。
- `env-mail` 当前只验证 SMTP 健康连接，不发送真实邮件；原因是项目还没有稳定测试邮箱和可清理邮件投递契约。
- `env-app` 验证应用真实装配与外部服务健康 Bean，服务级数据读写闭环由各 `infrastructure-service/*-impl` 的 `env-*` 测试覆盖。
- 对象存储 `get` 当前契约为元数据读取；内容读取应通过预签名 URL 或后续显式内容读取 API 扩展。

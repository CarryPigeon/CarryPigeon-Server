# infrastructure-service 服务框架任务单

任务名称：

infrastructure-service 第一阶段服务框架落地

任务目标：

在不修改 `chat-domain` 业务结构的前提下，完成 `database/cache/storage` 三类外部服务的 `api` 与 `impl` 模块骨架建设，建立稳定契约、自动配置入口、实现层最小适配和运行配置承接方式。

任务背景：

当前项目处于重写式重构阶段，`infrastructure-service` 仍只有父聚合模块，尚未形成可替换外部服务的标准落地结构。本轮目标是先把服务框架与依赖边界稳定下来，不提前落业务数据服务，不把实现细节泄露到 `chat-domain`。

影响模块：

- `pom.xml`
- `infrastructure-service`
- `application-starter`
- `infrastructure-basic`
- `ai-agent-workplace`

允许修改范围：

- 调整根 `pom.xml` 的版本管理与内部模块声明
- 调整 `infrastructure-service/pom.xml`
- 新增 `database-api`
- 新增 `database-impl`
- 新增 `cache-api`
- 新增 `cache-impl`
- 新增 `storage-api`
- 新增 `storage-impl`
- 修改 `application-starter/pom.xml`
- 修改 `application-starter/src/main/resources/application.yaml`
- 为新增模块补充测试
- 在 `ai-agent-workplace/` 记录任务过程与自检结果

禁止修改范围：

- 不修改 `chat-domain` 业务结构
- 不新增业务 repository
- 不新增 user/channel/message 等业务数据服务
- 不改变既有模块职责
- 不让 `chat-domain` 依赖任何 `*-impl`
- 不把外部服务实现放入 `infrastructure-basic`
- 不引入 MyBatis
- 不引入 MyBatis-Plus
- 不引入 Redisson
- 不引入 Testcontainers
- 不容器化应用本身

依赖限制：

- `database-api` 不引入 JDBC、MySQL、Flyway
- `cache-api` 不引入 Redis、Lettuce
- `storage-api` 不引入 MinIO SDK
- `database-impl` 允许引入 `spring-boot-starter-jdbc`、`mysql-connector-j`、`flyway-core`、`flyway-mysql`
- `cache-impl` 允许引入 `spring-boot-starter-data-redis`
- `storage-impl` 允许引入 `io.minio:minio`
- 根 `pom.xml` 只管理版本，不直接承载具体实现依赖
- `application-starter` 只引入三个 `*-impl` 完成运行时装配

配置限制：

- 外部服务配置类归属各自 `*-impl`
- 最终运行配置值统一写入 `application-starter/src/main/resources/application.yaml`
- 保持最小真实配置，不添加未来占位配置
- 项目自定义配置前缀保持 `cp`
- Spring 标准配置使用 `spring.datasource`、`spring.data.redis`、`spring.flyway`

文档依据：

- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/配置规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/注释规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

实现要求：

- 统一依赖方向为 `chat-domain -> *-api -> *-impl`
- `application-starter -> *-impl`
- `*-api` 只放抽象接口、数据契约、异常语义、健康检查契约
- `*-impl` 只放自动配置、配置类、具体客户端适配、最小实现
- 自动配置只允许存在于 `*-impl`
- 第一阶段只落服务框架，不落业务数据服务
- 缓存第一阶段只支持字符串缓存
- 对象存储抽象名使用 `storage`，实现层使用 `MinIO`

测试要求：

- 为新增 API 值对象与配置类补充模块内契约测试
- 为 `*-impl` 补充自动配置边界测试
- 测试类命名使用 `<Name>Tests`
- 测试方法命名使用 `methodName_condition_expected()`
- 测试代码补齐职责、边界、输入与期望结果注释
- 完成后执行 `mvn -q test` 验证

文档要求：

- AI 过程材料进入 `ai-agent-workplace/`
- 任务单文件命名遵守 `{{time}}-{{author}}-{{task}}-{{state}}.md`
- 本轮若未引入新的长期项目规则，则不回写 `docs/`
- 若后续把当前框架沉淀为长期统一规则，再视需要回写 `docs/`

验收标准：

- `infrastructure-service` 下成功新增 6 个 Maven 子模块
- 模块依赖方向符合既定架构约束
- `chat-domain` 未依赖任何 `*-impl`
- `*-api` 未泄露 JDBC、Redis、MinIO 实现类型
- `*-impl` 具备自动配置、配置类和最小健康检查/服务适配
- `application-starter` 已引入三个 `*-impl`
- `application.yaml` 已承接最小真实外部服务配置
- 新增测试通过
- 全项目 `mvn -q test` 通过

补充说明：

- 2026-04-20 已获得用户确认，允许按本任务单落地
- 本轮新增依赖、模块结构和运行配置，已在获批边界内执行
- 本轮不新增长期架构规则，因此未修改 `docs/`
- 当前文件已按命名规范使用 `done` 状态标记本轮任务已完成

## 实际结果

- 已新增 `database-api`、`database-impl`、`cache-api`、`cache-impl`、`storage-api`、`storage-impl`
- 已更新根 `pom.xml`、`infrastructure-service/pom.xml`、`application-starter/pom.xml`
- 已更新 `application-starter/src/main/resources/application.yaml`
- 已为 `infrastructure-basic/pom.xml` 补齐测试依赖，避免父 POM 依赖下放后现有测试失效
- 已补充 API 契约测试、配置默认值测试和自动配置边界测试
- 已执行 `mvn -q test`，结果通过

## 实际影响文件

- `pom.xml`
- `infrastructure-service/pom.xml`
- `infrastructure-service/database-api/**`
- `infrastructure-service/database-impl/**`
- `infrastructure-service/cache-api/**`
- `infrastructure-service/cache-impl/**`
- `infrastructure-service/storage-api/**`
- `infrastructure-service/storage-impl/**`
- `application-starter/pom.xml`
- `application-starter/src/main/resources/application.yaml`
- `infrastructure-basic/pom.xml`
- `AGENTS.md`
- `.gitignore`
- `ai-agent-workplace/20260420-132451-ai-infrastructure-service-framework-done.md`

## 自检结论

- 已明确任务目标、影响模块、允许修改范围、禁止边界、文档依据与验收标准
- AI 过程文件已落入 `ai-agent-workplace/`
- 正式代码与测试已落入对应模块源码目录
- 当前任务单已满足项目对 AI 任务单、命名规范和过程留痕的要求

## 残留风险与未完成项

- 当前仅落服务框架，未落业务数据服务
- 当前未提供 Flyway 迁移脚本
- 当前未在 `chat-domain` 建立对 `database-api/cache-api/storage-api` 的实际业务适配
- 对象存储、缓存和数据库的真实连通性依赖本地外部服务环境，不在本轮任务单内展开

任务名称：
database-impl 引入 MyBatis-Plus 简化数据库操作

任务目标：
在不改变 `database-api` 契约、不改变模块依赖方向、不修改 `chat-domain` 业务语义的前提下，将 `infrastructure-service/database-impl` 当前基于 `JdbcClient` 的数据库实现迁移为以 MyBatis-Plus 为主的持久化实现，降低样板 SQL/映射代码数量，并保持现有对外行为、异常语义与事务边界稳定。

任务背景：
当前 `database-impl` 的六个数据库服务实现均直接使用 `JdbcClient` 内联 SQL 与行映射，重复样板代码较多，维护成本偏高。用户已明确要求后续工作专注于 `database-impl`，并引入 MyBatis-Plus 简化数据库操作。本次任务属于依赖变更与持久化层内部重构，必须先明确边界与验收标准，再进行编码。

影响模块：
- `infrastructure-service/database-impl`
- `application-starter`（仅限直接受 `database-impl` 自动装配影响的测试或最小运行时扫描装配）

允许修改范围：
- 允许修改 `infrastructure-service/database-impl` 的 `pom.xml`
- 允许在 `database-impl` 内新增 MyBatis-Plus 所需的 entity、mapper、xml、config、support、service 实现
- 允许调整 `DatabaseServiceAutoConfiguration` 的具体装配方式
- 允许替换当前 `Jdbc*DatabaseService` 实现类
- 允许补充或改写 `database-impl` 相关测试
- 允许最小化修改 `application-starter` 中直接依赖 `DatabaseServiceAutoConfiguration` 装配前提的测试或启动扫描配置

禁止修改范围：
- 不允许修改模块依赖方向
- 不允许让 `database-api`、`chat-domain`、`infrastructure-basic` 直接依赖 MyBatis-Plus
- 不允许改变 `database-api` 中现有 service interface、record、exception、transaction abstraction 的公开契约
- 不允许引入超出本次真实需要的 ORM/代码生成/分页等额外能力
- 不允许借机重构业务逻辑、接口协议、响应码或群聊链路实现
- 不允许修改 Flyway schema 作为迁移前提，除非为修复本次重构直接引入的问题且有明确必要性

依赖限制：
- 新依赖必须严格归属 `infrastructure-service/database-impl`
- 首选 `com.baomidou:mybatis-plus-spring-boot3-starter`，因为项目当前基于 Spring Boot 3.5.3 / Java 21
- 仅在测试确有需要时再引入最小测试依赖
- 若当前需求未使用分页，则默认不引入 `mybatis-plus-jsqlparser`
- 保留现有 `spring-boot-starter-jdbc` 仅限 datasource/bootstrap/健康检查或实际仍有必要的最小低层能力，不得形成双套业务持久化实现长期并存

配置限制：
- 保持 `cp.infrastructure.service.database.*` 现有配置契约稳定
- 不新增面向业务的 MyBatis-Plus 配置项
- 若需要 mapper 扫描或 mapper-locations，优先放在 `database-impl` 内部固定配置或最小装配中完成

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`

任务分解 / 执行计划：
1. 完成 `database-impl` 当前 JDBC 实现、测试、schema、自动装配面摸底，确认真实边界与重复模式。
2. 在任务单中冻结依赖引入理由、允许范围、禁止范围、验收标准。
3. 在 `database-impl` 中引入 MyBatis-Plus 最小依赖与内部目录结构。
4. 逐个迁移六个数据库服务实现，保持 `database-api` 契约、异常消息与事务边界不变。
5. 调整自动装配与必要测试，移除业务路径对 `JdbcClient` 的依赖。
6. 运行受影响模块测试与构建验证，并在任务单补充实际结果、验证记录与残留风险。

关键假设与依赖：
- 已确认项目当前使用 Spring Boot 3.5.3、Java 21，适配 Boot 3 的 MyBatis-Plus starter 是正确选择。
- 已确认 `database-impl` 当前业务持久化边界主要集中在 6 个 `Jdbc*DatabaseService` 类与 `DatabaseServiceAutoConfiguration`。
- 已确认 `database-api` 的 record 与 service interface 是对外稳定契约，不能在本次任务中破坏。
- 已确认 `chat_channel_member` 为复合主键表，`chat_message` 历史查询包含 cursor + limit，自定义 SQL 仍可能需要保留在 mapper 层。
- 若实施中发现必须修改更多模块、更多配置或 schema，需先回写任务单再继续。

实现要求：
- MyBatis-Plus 相关类型仅允许落在 `database-impl` 内
- service 对外仍实现 `database-api` 中的原接口
- 持续保持 `DatabaseServiceException` 的异常边界与关键 message 文本稳定
- `TransactionRunner` 继续由 Spring 事务抽象提供，不将事务语义泄漏到上层
- 对简单单表查询可用 MyBatis-Plus 简化，对复合查询/特殊查询可使用自定义 mapper SQL，但仍必须收敛在 `database-impl`
- 代码风格、注释要求、命名稳定性需继续符合仓库规范

测试要求：
- 至少覆盖当前已存在的关键成功/失败路径
- 自动装配测试需继续验证启用/禁用 database service 时 Bean 注册边界
- 持久化服务测试需继续验证稳定异常消息与关键字段映射
- 对 `user_profile update` 的“0 行受影响”行为必须保留测试
- 对 `channel message` cursor 查询与 `channel member` 复合键相关逻辑应保留或补足测试

质量门禁：
- 受影响 Java 文件的 LSP/编译诊断无新增错误
- `mvn -pl infrastructure-service/database-impl -am test -DskipTests=false` 通过
- 若 `application-starter` 装配测试受影响，则对应模块测试通过
- 不出现新的模块边界违规或依赖方向违规
- 任务单中的实际结果、验证记录、残留风险完整可追溯

复审要求：
- 本任务属于依赖变更 + 数据持久化实现重构，完成后需重点复审：依赖归属是否正确、自动装配是否稳定、异常语义是否漂移、测试是否足以覆盖原行为。

文档要求：
- 若未引入新的长期项目规则，则不修改 `docs/`
- 任务过程与结果必须记录在本任务单

验收标准：
- `database-impl` 业务持久化主路径已从 `JdbcClient` 风格迁移为以 MyBatis-Plus 为主的实现
- `database-api` 的现有调用方无需修改即可继续工作
- 关键对外行为保持一致：查询结果、写入行为、异常边界、事务边界、启停装配边界
- 相关测试和构建验证通过

完成定义：
- 任务范围内代码与测试已完成
- 质量门禁已执行并记录
- 任务单已补充实际结果、验证记录、残留风险
- 文件状态可从 `current` 改为 `done`

实际结果：
- 已在 `infrastructure-service/database-impl` 中引入 `com.baomidou:mybatis-plus-spring-boot3-starter:3.5.9`。
- 已新增 `impl.mybatis.entity`、`impl.mybatis.mapper`、`impl.mybatis.service` 内部实现，并将六个业务数据库服务从 `JdbcClient` 迁移为 MyBatis-Plus / MyBatis Mapper 驱动实现。
- 已删除原六个 `Jdbc*DatabaseService` 业务实现与对应 JDBC 细节单测，保留 `JdbcClientSupport` 与 `JdbcDatabaseHealthService` 继续支撑 `cp.infrastructure.service.database.health-query` 的 JDBC 健康检查路径。
- 已更新 `DatabaseServiceAutoConfiguration`：业务服务继续只暴露 `database-api` 契约；真实运行时的 MyBatis mapper 扫描改由 `application-starter` 启动入口显式声明，避免打包运行时因为扫描范围不包含 `database-impl` 而丢失 mapper Bean。
- 已更新 `database-impl` 与 `application-starter` 相关装配测试，使其验证 MyBatis-Plus 迁移后的 Bean 装配边界与稳定行为，而不再依赖旧 `JdbcClient` 业务实现细节。
- 在复验过程中额外发现：`application-starter` 的两个持久化装配测试若直接引用 `database-impl` 内部 mapper，会破坏模块独立测试运行时的类发现；该问题已修正为仅 mock `database-api` 服务契约，测试边界与架构边界现已一致。
- 针对深度复审中发现的阻塞问题，已额外完成以下修正：
  - `chat_channel_member` 不再伪装为单主键 MyBatis-Plus 实体；现已改为显式 SQL mapper，直接按 `(channel_id, account_id)` 复合键语义插入与查询。
  - `AuthRefreshSessionMapper.revokeById(...)` 已恢复数据库侧 `CURRENT_TIMESTAMP(6)` 更新时间语义，不再使用 JVM `Instant.now()`。
  - 已补充 `MybatisPlusAuthAccountDatabaseServiceTests` 与 `MybatisPlusAuthRefreshSessionDatabaseServiceTests`，覆盖 auth 持久化服务的成功/失败路径与稳定异常消息。
- 已保留以下关键行为：`database-api` 契约不变、`DatabaseServiceException` message 文本稳定、`TransactionRunner` 保持 Spring 事务抽象、`user profile update affected no rows` 语义稳定、`chat_message` cursor 查询语义稳定、`chat_channel_member` 复合键写入/查询语义稳定。
- 已完成真实依赖环境下的 HTTP + WebSocket 联调，确认本次 MyBatis-Plus 重构后的 `database-impl` 不是仅在 mock/单测下成立，而是在真实 MySQL/Redis/MinIO + 应用打包启动场景中可工作。

验证记录：
- 已执行：`mvn -f infrastructure-service/database-impl/pom.xml test -DskipTests=false`
  - 结果：最终通过，`database-impl` 共 29 个测试通过。
- 已执行：`mvn -f application-starter/pom.xml -Dtest=AuthPersistenceConfigurationTests,UserProfilePersistenceConfigurationTests test -DskipTests=false`
  - 结果：通过，两个受影响 starter 测试共 4 个测试通过。
- 已执行：`mvn -pl infrastructure-service/database-impl,application-starter -am -DskipTests package`
  - 结果：通过。
- 已执行多轮深度复审：目标/约束复核、安全审查、上下文挖掘、代码质量复审；期间曾发现并修复两类阻塞问题：
  - `chat_channel_member` 复合键建模不当。
  - auth 持久化新实现缺少直接单测。
- 已执行真实打包启动验证：
  - 启动命令：`java -jar target/application-starter-1.0.0-exec.jar ... --cp.chat.server.realtime.enabled=true`
  - 结果：应用成功启动，Tomcat `8080` 与 realtime `18080` 均成功监听。
  - 启动日志确认：Flyway 校验通过、database/cache/storage 初始化检查通过、Netty realtime 服务启动成功。
- 已执行真实 HTTP 链路验证：
  - 注册：成功，返回新 `account_id`。
  - 登录：成功，拿到 `access_token` 与 `refresh_token`。
  - `/api/auth/me`：成功。
  - `/api/users/me` 查询：成功，命中注册后默认资料。
  - `/api/users/me` 更新：成功；再次查询可见 nickname / avatar / bio 已落库。
  - `/api/channels/default`：成功，命中默认频道。
  - `/api/channels/{channelId}/messages`：成功，返回历史消息列表。
- 已执行真实 WebSocket 链路验证：
  - Bearer 握手成功，收到 `welcome`。
  - 发送 `send_channel_text_message` 命令成功，收到 `channel_message` 事件。
  - 该 realtime 返回的 `message_id` 可在 HTTP 历史消息查询中命中，证明消息写入持久化与历史读取链路一致。
- 已执行 refresh / logout / revoke 真实链路验证：
  - `/api/auth/refresh`：成功。
  - `/api/auth/logout`：成功。
  - 对已注销 refresh token 再次调用 `/api/auth/refresh`：返回 `code=300`、`message=refresh token is invalid`，证明 refresh session 撤销已真实生效。
- 已多次尝试对受影响 Java 文件执行 LSP diagnostics，但当前工作区 Java LSP 初始化持续超时；最终以 Maven 编译、受影响模块测试、打包结果与真实链路联调作为本次验证依据。

残留风险：
- 当前已完成真实 MySQL/Flyway 环境下的主链路验证，因此“仅 mock 验证”的风险已显著下降；但仍未补充自动化数据库集成测试切片，后续若继续演进 mapper/实体映射，建议增加 `@MybatisTest` 或等效集成测试。
- 真实运行依赖当前 `application-starter` 明确声明的 mapper 扫描范围；后续若调整启动层包扫描或替换启动装配方式，需要同步复核 `database-impl` mapper 是否仍会被运行时发现。
- 当前未执行“全 reactor 且所有模块测试全开”的最终验证；一次早期尝试在到达目标模块前被上游模块测试发现流程阻断，后续已改为对受影响模块做定向测试、打包验证与真实链路联调。

知识沉淀 / 是否回写 docs：
- 本次未引入新的长期项目规则，暂不回写 `docs/`。

产物清理与保留说明：
- 保留本任务单作为本次依赖变更与持久化重构的追溯记录。

补充说明：
- 当前默认策略不是把所有 SQL 都“魔法化”；若某些查询使用 MyBatis-Plus wrapper 反而更绕，应保留为 mapper 自定义 SQL，但边界仍限定在 `database-impl` 内。

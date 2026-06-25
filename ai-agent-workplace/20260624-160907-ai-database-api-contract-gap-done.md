任务名称：
database-api 缺失 contract 补测

任务目标：
补齐 `database-api` 中尚未覆盖的公共契约测试，锁定默认方法语义、列表 fallback 语义和抽象写入扩展语义。

任务背景：
在继续审查 `database-api` 测试结构时发现，当前更大的缺口不是目录样式，而是 `ChannelAuditLogDatabaseService`、`ChannelBanDatabaseService`、`ChannelInviteDatabaseService`、`MessageDatabaseService` 这几类 public contract 仍无对应 contract 测试。

任务类型：
实现类任务

影响模块：
- `infrastructure-service/database-api`
- `ai-agent-workplace/`

允许修改范围：
- 允许新增 `database-api` contract 测试
- 允许新增本轮任务单

禁止修改范围：
- 不修改正式业务逻辑
- 不修改模块依赖方向
- 不新增第三方依赖

依赖限制：
- 仅使用现有测试依赖

配置限制：
- 不新增配置

文档依据：
- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `docs/测试规范.md`
- `docs/注释规范.md`

任务分解 / 执行计划：
1. 盘点 `database-api/service` 中未覆盖的 public contract。
2. 按现有 contract 风格补充缺失测试，优先锁定 default/fallback 语义与抽象扩展语义。
3. 执行本地可行验证并记录环境阻塞。
4. 关闭任务单。

关键假设与依赖：
- 现有 `service` contract 测试风格可继续复用，不需要引入第二套测试模式。
- 若 Maven 依赖环境仍阻塞，只能以 `testCompile`/`git diff --check` 等本地可行验证为主。

实现要求：
- 新增测试必须使用 `@Tag("contract")`
- 测试类和方法命名遵守既有规范
- 断言直接对应接口 contract，不引入实现细节

测试要求：
- 覆盖 `ChannelAuditLogDatabaseService`
- 覆盖 `ChannelBanDatabaseService`
- 覆盖 `ChannelInviteDatabaseService`
- 覆盖 `MessageDatabaseService`

质量门禁：
- 缺失 contract 测试已补齐
- 无新增格式问题
- 验证命令或阻塞原因记录完整

复审要求：
- 自检 default 方法语义、列表 fallback 语义与 delegate 断言是否准确

文档要求：
- 默认不改 `docs/`

验收标准：
- 四份缺失 contract 测试已新增
- 本轮验证结果清晰可追踪

完成定义：
- 用户获得更完整的 `database-api` public contract 测试面

实际结果：
- 已为以下 public contract 新增测试：
  - `ChannelAuditLogDatabaseServiceContractTests`
  - `ChannelBanDatabaseServiceContractTests`
  - `ChannelInviteDatabaseServiceContractTests`
  - `MessageDatabaseServiceContractTests`
- 已补齐 `database-api/service` 当前缺失的四份 contract 测试

验证记录：
- `git diff --check -- infrastructure-service/database-api/src/test/java ai-agent-workplace/20260624-160907-ai-database-api-contract-gap-current.md`
  - 结果：通过，无新增空白与补丁格式问题
- `mvn -o -pl infrastructure-service/database-api -DskipTests test-compile`
  - 结果：通过
  - 说明：`database-api` 主源码与测试源码均完成离线编译
- `mvn -o -pl infrastructure-service/database-api -Dtest=ChannelAuditLogDatabaseServiceContractTests,ChannelBanDatabaseServiceContractTests,ChannelInviteDatabaseServiceContractTests,MessageDatabaseServiceContractTests test`
  - 结果：失败
  - 原因：离线环境缺少 `maven-surefire-plugin:3.5.4` 相关依赖，无法执行 surefire

残留风险：
- Maven 离线依赖问题仍阻塞真实 `test` 阶段执行

知识沉淀 / 是否回写 docs：
- 暂不回写

产物清理与保留说明：
- 保留本任务单作为本轮补测记录

补充说明：
- 本轮优先补 contract 缺口，不处理纯目录样式问题。

# docs/t API 模型一致性第 3 项任务单

## 任务目标

仅处理用户指定的第 3 项：对照 `docs/t` 补齐或裁剪用户资料、WebSocket、响应模型差异。

## affected modules

- `chat-domain`
- `application-starter`
- `ai-agent-workplace`

## 允许修改范围

- 用户资料相关 request/response DTO、Controller、领域命令和最小必要领域模型字段。
- 用户资料数据库表定义、测试数据、重置脚本、database-api 记录模型、database-impl 持久化映射。
- HTTP 响应模型与 OpenAPI 示例中明显偏离 `docs/t` 的字段。
- WebSocket 差异仅允许做配置/文档化自检或最小元数据修正；如需引入新实时通道架构，必须先停止并说明。
- 对应测试。
- 当前任务单文件。

## 禁止边界

- 不处理路径参数名规范化。
- 不处理 `PUT /api/files/uploads/{shareKey}` 是否正式写入 `docs/t` 的问题。
- 不新增依赖。
- 不重构模块结构。
- 数据库结构只允许围绕用户已确认的 `sex`、`birthday` 用户资料字段扩展。
- 不扩展新的实时通信架构。

## 依据文档

- `AGENTS.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/SERVER_API.md`
- `docs/standards/AI协作开发规范.md`
- `docs/standards/变更审核清单.md`
- `docs/standards/测试规范.md`

## 验收标准

- 明确列出第 3 项下可安全修正与不可直接修正的差异。
- 对可安全修正的 HTTP 模型差异完成代码与测试。
- `PATCH /api/users/me` 的 `sex`、`birthday` 能进入领域模型并通过数据库服务持久化。
- 对 WebSocket 差异如涉及架构或未实现能力，给出准确边界，不擅自扩展。
- 相关 Maven 测试通过。

## 实际结果

- 已将 `sex`、`birthday` 接入 `PATCH /api/users/me` 请求到领域命令、领域模型、结果投影、仓储适配、`database-api` 记录模型和 MyBatis 实体映射。
- 已在 `docs/sql/02-user.sql` 与 `docs/sql/00-all-in-one.sql` 的 `user_profile` 表中补齐 `sex BIGINT NOT NULL DEFAULT 0`、`birthday BIGINT NOT NULL DEFAULT 0`。
- 已在 `docs/sql/10-test-data.sql` 的 `user_profile` 测试数据中补齐 `sex`、`birthday` 字段值。
- 已补充或调整用户资料 controller、领域服务、仓储适配、database-api 契约、database-impl 映射相关测试断言。

## 验证记录

- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am test -DskipTests=false '-Dtest=UserProfile*Tests,DatabaseBackedUserProfileRepositoryTests,MybatisPlusUserProfileDatabaseServiceTests,OpenApiConfigurationTests' -Dsurefire.failIfNoSpecifiedTests=false`
  - 结果：`BUILD SUCCESS`
  - 关键结果：`chat-domain` 用户资料相关测试 26 个通过；`database-api` 用户资料契约测试 5 个通过；`database-impl` 用户资料 MyBatis 测试 10 个通过；`application-starter` OpenAPI 与用户资料持久化配置测试 6 个通过。

## 自检结论

- 模块边界：通过。`chat-domain` 仍只依赖 `database-api`，没有依赖 `database-impl`。
- 依赖：通过。未新增依赖。
- 配置：通过。未新增运行时配置。
- SQL：通过。表定义和测试数据已补齐新增字段；清理脚本只负责 drop 表，无需按字段调整。
- 测试：通过。相关 Maven reactor 测试已通过。
- 残留风险：已有数据库需要执行重置脚本或自行 `ALTER TABLE user_profile ADD COLUMN ...` 后再导入测试数据；本项目当前未引入迁移框架。

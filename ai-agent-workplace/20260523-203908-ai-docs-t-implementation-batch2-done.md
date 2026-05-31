任务名称：
基于 docs-t 的 API 实现补齐批次二

任务目标：
在上一轮“显式拒绝协议漂移”的基础上，继续把 `docs/t` 中优先级最高的真实行为补齐，重点实现：
- 消息上下文查询 `around_mid`
- 消息搜索高级过滤与分页参数的真实下沉
- 频道资料更新 `PATCH /api/channels/{cid}` 的真实更新语义

任务背景：
当前代码已消除“文档说支持、运行时静默忽略”的明显漂移，但仍有多处行为只是显式未实现。用户要求继续以 `docs/t` 为基准改进代码，因此本批次从消息查询和频道更新两条最明确的数据链路入手，推进真实实现。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- 必要时 `application-starter`（若测试或装配需要）

允许修改范围：
- `chat-domain/src/main/java/**/message/**`
- `chat-domain/src/main/java/**/channel/**`
- `infrastructure-service/database-api/src/main/java/**/Message*`
- `infrastructure-service/database-api/src/main/java/**/Channel*`
- `infrastructure-service/database-impl/src/main/java/**/message/**`
- `infrastructure-service/database-impl/src/main/java/**/channel/**`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不在本批次内扩大到 pins / mentions / files / unreads 全量功能面
- 不擅自改写 docs/t 目标协议

依赖限制：
- 维持现有 Spring Boot / Lombok / MyBatis 基线
- 持久化扩展必须走既有 `database-api` / `database-impl` 分层，不得绕过模块边界

配置限制：
- 不新增未来占位配置
- 不引入新的全局协议开关

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/测试规范.md`

任务分解 / 执行计划：
1. 梳理消息查询现有 controller → application → repository → database → mapper 数据链路。
2. 为 `around_mid`、高级过滤、搜索分页补充最小必要的 query/repository/database 契约。
3. 梳理频道 patch 的 controller → application → repository → database 数据链路。
4. 为频道资料更新补齐最小必要的应用与持久化支持。
5. 补充/更新契约测试、应用层测试、持久化测试。
6. 执行诊断与定向 Maven 验证。

关键假设与依赖：
- 消息查询与频道更新的真实实现需要扩大到数据库契约层，这是本批允许范围。
- 若某一子功能实现会明显扩展到跨领域设计重构，则应先收敛为最小可运行实现。
- `docs/t` 仍然是目标基线，当前批次只实现其中最优先且链路清晰的部分。

实现要求：
- 不允许继续存在“参数已暴露但未参与真实查询”的行为。
- 新增数据库契约必须保持语义清晰、命名稳定，不造模糊 util 接口。
- 频道 patch 需表现为真实更新，而不是重读或伪成功。

测试要求：
- 补充消息上下文与高级过滤的成功/失败路径测试。
- 补充频道资料更新成功/失败路径测试。
- 若扩展到数据库 API / impl，补充对应单元或契约测试。

质量门禁：
- 改动文件无新增诊断错误
- 相关定向 Maven 测试通过
- 受影响模块编译通过

复审要求：
- 需要复审
- 重点检查 docs/t 一致性、查询语义真实性、持久化边界与测试覆盖

文档要求：
- 本批默认不改 docs/t，只改实现对齐

验收标准：
- `around_mid` 不再显式未支持，而具备真实行为
- 消息搜索高级参数不再静默忽略或统一拒绝，而具备真实下沉语义（或在 docs/t 明确允许的范围内落地）
- `PATCH /api/channels/{cid}` 具备真实更新行为

完成定义：
- 验收标准满足
- 质量门禁执行并记录

实际结果：
- 已实现 `around_mid` 消息上下文查询，支持 `before/after` 上下文窗口。
- 已实现消息搜索高级过滤参数下沉：`cursor`、`sender_uid`、`domain`、`before_mid`、`after_mid`。
- 已补齐频道资料更新真实行为：`PATCH /api/channels/{cid}` 现在执行实际更新并返回 `204 No Content`。
- 已补齐频道资料模型与持久化字段：`brief`、`avatar` 已进入 channel domain / database contract / MyBatis entity。
- `GET /api/channels` 与 `GET /api/channels/{cid}` 的 `brief` / `avatar` / `owner_uid` 不再全部来自占位空串，而由真实数据或成员关系推导。
- `POST /api/channels` 已改为使用真实 `brief/avatar` 输入创建频道。
- 已新增数据库迁移 `V5__add_channel_profile_fields.sql`。

验证记录：
- 文件级 `lsp_diagnostics` 对核心改动文件执行通过。
- 目录级 `lsp_diagnostics` 在当前环境初始化失败/超时，属工具环境问题；最终以 Maven 编译与测试结果作为验收依据。
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am -Dtest=ChannelControllerTests,ChannelApplicationServiceTests,DatabaseBackedChannelRepositoryTests,ChannelDatabaseServiceContractTests,MybatisPlusChannelDatabaseServiceTests,ChannelMessageQueryControllerTests,MessageApplicationServiceQueryTests,DatabaseBackedMessageRepositoryTests,MybatisPlusMessageDatabaseServiceTests,MessageApplicationServiceAttachmentTests,MessageApplicationServiceSendTests,MessageAttachmentRegressionTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 本批已补齐消息上下文、搜索过滤和频道 patch/create 的关键链路，但 docs/t 其它未实现端点（pins / mentions / files / unreads 等）仍需后续继续推进。
- 频道 `avatar` 目前已入库，但本批未补充独立的头像上传/文件下载协议面。

知识沉淀 / 是否回写 docs：
- 默认不回写 docs

产物清理与保留说明：
- 完成后改名为 `done`

补充说明：
- 本任务单延续上一轮 docs/t follow-up remediation 的实现阶段

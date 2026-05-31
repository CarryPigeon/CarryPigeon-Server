任务名称：
基于 docs-t 的 API 后续整改批次

任务目标：
修复上一轮深度自检中已确认的高优先级 docs/t 不一致点，使当前代码至少消除“文档公开、运行时不公开”和“控制器接受参数但实现静默忽略”这两类明显协议漂移。

任务背景：
上一轮自检已确认当前 API 与 `docs/t` 仍非完全一致，尤其集中在：
- `/api/plugins/catalog`、`/api/domains/catalog` 运行时仍被鉴权拦截
- 消息搜索接口接受 `sender_uid/domain/before_mid/after_mid` 等参数但未真正进入应用层查询模型
- `PATCH /api/channels/{cid}` 仍为占位实现

影响模块：
- `chat-domain`
- `application-starter`（如需补充 OpenAPI / 配置测试）

允许修改范围：
- `chat-domain/src/main/java/**/auth/config/**`
- `chat-domain/src/main/java/**/message/**`
- `chat-domain/src/main/java/**/channel/**`
- 与上述改动直接相关的测试类

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不进行无边界的大规模 API 重构
- 不擅自实现整套 `docs/t` 未确认优先级的大功能面（如 pins / mentions / files 全量）

依赖限制：
- 维持现有 Spring Boot / Lombok / Springdoc / MyBatis 基线
- 不引入新的协议中间层或网关

配置限制：
- 保持最小配置原则
- 不新增未来占位配置

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`
- `docs/架构文档.md`
- `docs/测试规范.md`

任务分解 / 执行计划：
1. 修复匿名公开目录接口的运行时鉴权边界，使其与 `docs/t` 和 OpenAPI 保持一致。
2. 收敛消息搜索接口：要么把已暴露参数真正落到应用层查询模型，要么在控制器层明确拒绝当前不支持的参数，避免静默忽略。
3. 评估并最小化处理 `PATCH /api/channels/{cid}` 占位问题；如无法在本批完整实现，则至少改为明确失败而不是伪装成功。
4. 补充/更新相关契约测试。
5. 执行诊断与定向 Maven 测试验证。

关键假设与依赖：
- 本批次以“消除明确协议漂移”为目标，不承诺一次性补齐 `docs/t` 全量端点。
- 若消息搜索高级参数落地需要扩大到数据库契约层，则允许在既有模块边界内最小扩展相关 query/repository/service。
- 若频道 patch 真实实现需要超出本批边界，则优先改为显式未实现失败。

实现要求：
- 不允许继续存在“控制器接受参数但实现静默忽略”的行为。
- 对外匿名接口必须在运行时真实匿名可访问。
- 不能用 `CPResponse` 风格伪装成功来掩盖未实现逻辑。

测试要求：
- 至少覆盖匿名目录接口的鉴权边界
- 至少覆盖消息搜索高级参数的行为
- 至少覆盖频道 patch 的新行为

质量门禁：
- 改动文件无新增诊断错误
- 相关契约测试通过
- 受影响 Maven reactor 构建通过

复审要求：
- 需要复审
- 重点关注 docs/t 一致性、运行时鉴权边界、参数语义与伪实现清理

文档要求：
- 若本批只做局部修复且仍未完全对齐 `docs/t`，不修改 `docs/t` 目标协议本身

验收标准：
- `/api/plugins/catalog` 与 `/api/domains/catalog` 可匿名访问
- 消息搜索接口不再静默忽略已公开参数
- `PATCH /api/channels/{cid}` 不再表现为“假更新成功”

完成定义：
- 验收标准满足
- 质量门禁执行并记录

实际结果：
- `AuthWebMvcConfiguration` 已放行 `/api/plugins/catalog` 与 `/api/domains/catalog`，修复 docs/t、OpenAPI 与运行时鉴权边界不一致问题。
- `GET /api/channels/{cid}/messages/search` 对 `cursor/sender_uid/domain/before_mid/after_mid` 改为显式 `422` 拒绝，避免静默忽略已公开参数。
- `GET /api/channels/{cid}/messages` 对 `around_mid/before/after` 改为显式 `422` 拒绝，避免伪装支持上下文查询语义。
- `PATCH /api/channels/{cid}` 改为显式 `422 channel_update_not_implemented`，不再返回伪成功结果。
- 已补充并更新对应契约测试。

验证记录：
- 相关改动文件在编辑后进行了 LSP 诊断检查；后续以 Maven 编译与测试为准完成最终验证。
- `mvn -pl chat-domain -am -Dtest=AuthWebMvcConfigurationTests,ChannelMessageQueryControllerTests,ChannelControllerTests,ServerPluginCatalogControllerTests,ServerDomainCatalogControllerTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 本批通过“显式拒绝”消除了协议漂移，但尚未补齐 `around_mid` 与消息搜索高级过滤的真实实现。
- docs/t 中其它未实现端点（如 pins / mentions / files / unreads 等）仍需后续分批推进。

知识沉淀 / 是否回写 docs：
- 默认不回写 docs

产物清理与保留说明：
- 任务完成后改名为 `done`

补充说明：
- 本任务单为上一轮 docs/t 深度自检后的后续整改批次

任务名称：

第 4 轮：用户、频道、消息 HTTP 业务接口重写

任务目标：

按客户端基准 API 重写用户、频道、消息相关 HTTP 接口与消息数据模型。

任务背景：

业务接口面最大，且受协议基础设施与 auth/server 接口影响，需放在中后段单独推进。

影响模块：

- `chat-domain`
- 可能涉及 `infrastructure-service/*-api`

允许修改范围：

- `user` feature
- `channel` feature
- `message` feature
- 对应 DTO、应用服务、仓储适配器与测试

禁止修改范围：

- 不在本轮重写 WS 协议
- 不引入未经确认的新架构

依赖限制：

- 不得破坏既有模块依赖方向

配置限制：

- 不新增与当前业务无关的配置

文档依据：

- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：

1. 对照客户端基准 API 列出业务端点映射表。
2. 优先重写消息相关 HTTP 接口。
3. 再重写频道相关 HTTP 接口。
4. 最后补齐用户相关 HTTP 接口。
5. 补齐分页、错误与边界测试。

关键假设与依赖：

- 依赖第 2 轮和第 3 轮完成。

实现要求：

- 消息对象必须收敛到客户端基准字段语义。
- 分页返回必须统一。

测试要求：

- 业务成功与失败路径都要覆盖。

质量门禁：

- 受影响 controller/application 测试通过。
- 分页与错误模型符合新协议。

复审要求：

- 重点复审字段语义是否仍混入旧协议概念。

文档要求：

- 最终收口时更新正式 API 文档。

验收标准：

- 客户端主要 HTTP 业务接口可按基准 API 访问。

完成定义：

- HTTP 业务端点重写完成并验证通过。

实际结果：

- 已完成第一个业务切片：用户资源 HTTP 面按 v1 语义收敛。
- 已将 `GET /api/users/me` 改为直接返回 `uid / email / nickname / avatar`，不再沿用旧的全量资料成功 envelope。
- 已将 `GET /api/users/{uid}` 改为返回最小公开资料 `uid / nickname / avatar`，移除了“仅允许查看自己资料”的旧限制。
- 已将 `GET /api/users?ids=...` 改为返回 `items[]` 外壳，作为批量公开资料查询入口。
- 已新增 `PUT /api/users/me/email` 与 `PATCH /api/users/me` 的 v1 路径与请求模型，并复用现有资料更新能力承接昵称/头像/简介字段。
- 为支撑邮箱更新，已补充 `AuthAccountRepository` 与 `AuthAccountDatabaseService` 的 `findById / update` 能力，并打通 database-impl 适配实现。
- 已完成第二个业务切片：频道 HTTP 资源开始按 v1 资源语义收敛。
- 已新增 `GET /api/channels`、`GET /api/channels/{cid}`、`POST /api/channels` 以及 `PUT / DELETE /api/channels/{cid}/admins/{uid}` 等更接近 v1 的资源路径，同时保留旧的过渡性接口以降低改动爆炸半径。
- 已将 `GET /api/channels/{cid}/members` 收敛到 `items[]` 外壳，并把成员字段命名切到 `uid / role / nickname / avatar / join_time` 语义。
- 已完成第三个业务切片：消息 HTTP 资源开始按 v1 成功对象收敛。
- 已将 `GET /api/channels/{cid}/messages` 与 `GET /api/channels/{cid}/messages/search` 改为直接返回 v1 风格的 `items + next_cursor + has_more`，消息对象字段切到 `mid / cid / uid / sender / send_time / domain / domain_version / data / preview`。
- 已新增 `POST /api/channels/{cid}/messages`，当前按最小可用范围只支持 `Core:Text@1.0.0` 文本消息发送。
- 已新增 `DELETE /api/messages/{mid}` 的硬删除入口，并补齐 message repository / database service / mybatis service 的删除能力。
- 已保留旧的附件上传与撤回接口作为过渡能力，并同步修正其测试与 starter 回归装配，确保 round4 改造不破坏既有附件链路。

验证记录：

- 测试命令：`mvn -q -pl chat-domain,infrastructure-service/database-impl,infrastructure-service/database-api -am -Dtest=UserProfileControllerTests,UserProfileApplicationServiceTests,MybatisPlusAuthAccountDatabaseServiceTests,AuthApplicationServiceTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.user.controller.http.UserProfileControllerTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.user.application.service.UserProfileApplicationServiceTests.txt`
- surefire 结果：`infrastructure-service/database-impl/target/surefire-reports/team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusAuthAccountDatabaseServiceTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.auth.application.service.AuthApplicationServiceTests.txt`
- 测试命令：`mvn -q -pl chat-domain,infrastructure-service/database-impl,infrastructure-service/database-api -am -Dtest=UserProfileControllerTests,UserProfileApplicationServiceTests,ChannelControllerTests,MybatisPlusAuthAccountDatabaseServiceTests,AuthApplicationServiceTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.channel.controller.http.ChannelControllerTests.txt`
- 测试命令：`mvn -q -pl chat-domain,infrastructure-service/database-impl,infrastructure-service/database-api -am -Dtest=ChannelMessageQueryControllerTests,ChannelMessageAttachmentControllerTests,MessageApplicationServiceQueryTests,MessageApplicationServiceSendTests,DatabaseBackedMessageRepositoryTests,MybatisPlusMessageDatabaseServiceTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageQueryControllerTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.message.controller.http.ChannelMessageAttachmentControllerTests.txt`
- surefire 结果：`chat-domain/target/surefire-reports/team.carrypigeon.backend.chat.domain.features.message.support.persistence.DatabaseBackedMessageRepositoryTests.txt`
- surefire 结果：`infrastructure-service/database-impl/target/surefire-reports/team.carrypigeon.backend.infrastructure.service.database.impl.mybatis.service.MybatisPlusMessageDatabaseServiceTests.txt`
- 测试命令：`mvn -q -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl,application-starter -am -Dtest=UserProfileControllerTests,UserProfileApplicationServiceTests,ChannelControllerTests,ChannelMessageQueryControllerTests,ChannelMessageAttachmentControllerTests,MessageApplicationServiceQueryTests,MessageApplicationServiceSendTests,DatabaseBackedMessageRepositoryTests,MybatisPlusAuthAccountDatabaseServiceTests,MybatisPlusMessageDatabaseServiceTests,AuthApplicationServiceTests,MessageAttachmentRegressionTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- surefire 结果：`application-starter/target/surefire-reports/team.carrypigeon.backend.starter.MessageAttachmentRegressionTests.txt`

残留风险：

- 若 message 模型未彻底切换，WS 轮次也会受阻。
- 用户层虽然已经有 v1 主路径，但旧的 `/api/users/page`、`/api/users/search` 与 `PUT /api/users/me` 过渡接口仍保留在代码中；最终是否移除，需要在后续收口轮次统一决定。
- 频道层虽然已新增 v1 风格资源路径，但 `default/system/private/invites/...` 旧动作式路径仍保留以承接当前内部业务能力；后续若要完全对外收敛，还需决定哪些旧路径正式下线。
- 消息层当前仅把 HTTP 发送入口最小收敛到 `Core:Text@1.0.0`，其它 domain 仍通过旧内部 messageType/plugin 语义承接；若后续需要完整域目录与 schema 约束，还需在后续轮次继续推进。
- `message.deleted` 等实时事件的对外协议面尚未在本轮处理，仍需 round5 统一完成，与新的 HTTP 删除语义配套。

知识沉淀 / 是否回写 docs：

- 待收口轮次统一处理

产物清理与保留说明：

- 保留接口映射与差异分析草稿

任务名称：
基于 docs-t 的 Channel Applications 实现批次六

任务目标：
实现 `docs/t/11-http-endpoints-v1.md` 中的：
- `POST /api/channels/{cid}/applications`
- `GET /api/channels/{cid}/applications`
- `POST /api/channels/{cid}/applications/{application_id}/decisions`

任务背景：
当前仓库已具备 `ChannelInvite` 模型、邀请仓储和成员治理规则。相比新建一整套 applications 表，沿现有邀请链路扩展为 docs/t 语义是更小更稳的实现路径。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`

允许修改范围：
- `chat-domain/src/main/java/**/channel/**`
- `infrastructure-service/database-api/src/main/java/**/ChannelInvite*`
- `infrastructure-service/database-impl/src/main/java/**/ChannelInvite*`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到 pins / mentions / bans list

依赖限制：
- 优先复用现有 `ChannelInvite` 作为 applications 语义载体
- 通过 channel governance 保持 owner/admin/member 规则一致

配置限制：
- 不新增未来占位配置

文档依据：
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/测试规范.md`

任务分解 / 执行计划：
1. 扩展 `ChannelInvite` 数据链路以支持按频道列出申请。
2. 新增 applications 相关 command / dto / controller。
3. 实现审批 decision（approve/reject）并复用成员加入逻辑。
4. 补充 controller / application / database service 测试。
5. 执行定向 Maven 验证。

关键假设与依赖：
- 申请加入可视为“无 inviter 的 invite”或复用 invite 行为建模。
- `application_id` 可以在当前批次中最小映射为被申请用户 ID 或扩展现有 invite 标识方式，但必须保证 docs/t 对外字段稳定。

实现要求：
- 接口字段必须保持 docs/t 的 `snake_case`。
- 重复审批需要返回可识别错误。

测试要求：
- 覆盖申请创建成功路径
- 覆盖申请列表成功路径
- 覆盖 approve / reject 成功与重复审批路径

质量门禁：
- 相关定向 Maven 测试通过

复审要求：
- 重点检查 docs/t 一致性、语义复用是否合理、审批幂等性与权限规则

文档要求：
- 默认不修改 docs/t

验收标准：
- 三条 applications 接口可用

完成定义：
- 验收标准满足
- 质量门禁执行并记录

实际结果：
- 已沿现有 `ChannelInvite` 模型补齐 applications 语义。
- 已为 `chat_channel_invite` 增加 `application_id` 支撑对外稳定标识。
- 已扩展 invite repository / database-api / database-impl，支持按频道列申请、按 `application_id` 查询。
- 已实现 `POST /api/channels/{cid}/applications`。
- 已实现 `GET /api/channels/{cid}/applications`。
- 已实现 `POST /api/channels/{cid}/applications/{application_id}/decisions`。
- 已补充 applications 相关 application / controller / persistence / database-impl 测试。

验证记录：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl -am -Dtest=ChannelApplicationServiceTests,ChannelApplicationControllerTests,DatabaseBackedChannelInviteRepositoryTests,MybatisPlusChannelInviteDatabaseServiceTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 当前 applications 仍复用 invite 持久化模型；若后续产品需要邀请与申请严格拆模，可能需要再分离。

知识沉淀 / 是否回写 docs：
- 默认不回写 docs

产物清理与保留说明：
- 完成后改名为 `done`

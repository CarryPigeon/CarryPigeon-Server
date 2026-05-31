任务名称：
基于 docs-t 的 Channel Bans List 实现批次七

任务目标：
实现 `docs/t/11-http-endpoints-v1.md` 中的：
- `GET /api/channels/{cid}/bans`

任务背景：
当前仓库已具备禁言/解除禁言写链路与 `chat_channel_ban` 持久化表，缺口主要在按频道读取封禁列表的读能力，因此本批选择以最小爆炸半径补齐 bans list。

影响模块：
- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`

允许修改范围：
- `chat-domain/src/main/java/**/channel/**`
- `infrastructure-service/database-api/src/main/java/**/ChannelBan*`
- `infrastructure-service/database-impl/src/main/java/**/ChannelBan*`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到 pins / mentions

依赖限制：
- 复用现有 `ChannelBan` 表和治理规则
- 不新增新表

配置限制：
- 不新增未来占位配置

文档依据：
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`

任务分解 / 执行计划：
1. 扩展 ban repository / database-api / mapper 支持按频道读取封禁列表。
2. 在 channel application service 补 bans list 用例。
3. 暴露 `GET /api/channels/{cid}/bans`。
4. 补充 controller / application / persistence / database-impl 测试。
5. 执行定向 Maven 验证。

关键假设与依赖：
- bans list 只读取现有封禁记录，不引入分页。
- 返回字段以 docs/t 当前示例为准：`cid`、`uid`、`until`、`reason`、`create_time`。

实现要求：
- 维持 owner/admin 权限语义。
- 响应字段使用 `snake_case`。

测试要求：
- 覆盖 bans list 成功路径。
- 覆盖无权限或频道不存在的失败路径（如现有测试体系适用）。

质量门禁：
- 定向 Maven 测试通过。

复审要求：
- 重点检查 docs/t 一致性与最小读能力实现。

验收标准：
- `GET /api/channels/{cid}/bans` 可返回封禁列表。

完成定义：
- 验收标准满足并完成验证。

实际结果：
- 已补齐 ban repository / database-api / mapper / database service 的按频道列表读取能力。
- 已实现 `GET /api/channels/{cid}/bans`。
- 已补充 application / controller / database-impl 定向测试。

验证记录：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl -am -Dtest=ChannelApplicationServiceTests,ChannelBansControllerTests,MybatisPlusChannelBanDatabaseServiceTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- bans list 暂不分页，后续若数据量增长可再增强。

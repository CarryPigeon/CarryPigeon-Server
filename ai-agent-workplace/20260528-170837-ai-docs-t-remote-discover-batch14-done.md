任务名称：
基于 docs-t 的 Remote Discover 批次十四

任务目标：
实现 docs/t 中的远端频道发现接口：
- `GET /api/channels/discover`

任务背景：
notification preferences 批次完成后，剩余 docs/t backlog 中更适合优先推进的是 remote discover。该能力主要是读接口和分页协议，对现有 channel 数据面侵入较小，可先基于当前频道数据做最小可运行实现。

影响模块：
- `chat-domain`
- 如需最小查询扩展则涉及 `database-api` / `database-impl`

允许修改范围：
- `chat-domain/src/main/java/**/channel/**`
- `chat-domain/src/main/java/**/shared/**`
- 与发现查询直接相关的 `database-api` / `database-impl`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到 audit logs
- 不在本批并行重做已完成 channel list/profile/applications/bans 能力

依赖限制：
- 优先复用现有 channel/profile/member/application 数据面
- 最小实现优先；若 docs/t 字段无现成来源，可在当前阶段用明确可解释的近似规则，但不能返回 placeholder/null 壳

配置限制：
- 不新增未来占位配置

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/14-pagination-and-cursor-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：
1. 阅读 docs/t 中 remote discover 契约。
2. 阅读当前 channel list/profile/persistence 现状，确认可复用字段与缺口。
3. 设计最小 discover 查询方案与分页语义。
4. 实现 `GET /api/channels/discover`。
5. 补 controller / application / persistence 定向测试。
6. 执行定向 Maven 验证。

关键假设与依赖：
- 当前批次优先基于现有频道表做 discover，不引入复杂推荐逻辑。
- `requires_application` 可先按频道类型/现有规则计算，若 docs/t 未给更细规则则以最小稳定语义落地。

实现要求：
- 以 docs/t 为协议真源。
- 返回统一 `{ items, next_cursor, has_more }` 分页结构。
- 支持 `q` / `cursor` / `limit` / `type` 最小语义。

测试要求：
- 覆盖默认 discover 列表。
- 覆盖按 `q` 搜索。
- 覆盖按 `type` 筛选。
- 覆盖分页参数与非法 cursor 路径。

质量门禁：
- 定向 Maven 测试通过。
- 无新增明确编译错误。
- 对外返回字段与分页结构具备断言。

复审要求：
- 完成后复查 discover 字段来源是否稳定、分页语义是否与 docs/t 一致。

文档要求：
- 若仅实现既有 docs/t 契约，不额外改 `docs/`

验收标准：
- `GET /api/channels/discover` 可按 docs/t 最小契约工作。

完成定义：
- 验收标准满足
- 定向验证通过并记录
- 任务单补齐实际结果 / 验证记录 / 残留风险后转 `done`

实际结果：
- 已实现 `GET /api/channels/discover`。
- 已扩展 channel 查询链路，使 `ChannelRecord` / `Channel` 支持 `member_count` 与 `requires_application`。
- 已补齐 discover 的 database-api / database-impl / domain / controller 主链路。

验证记录：
- `mvn -pl chat-domain,infrastructure-service/database-api,infrastructure-service/database-impl -am -Dtest=ChannelDiscoverApplicationServiceTests,ChannelDiscoverControllerTests,ChannelControllerTests,DatabaseServiceAutoConfigurationTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 当前 discover 采用最小稳定实现，不含复杂推荐逻辑。

知识沉淀 / 是否回写 docs：
- 暂不回写，除非发现 docs/t 本身冲突

产物清理与保留说明：
- 保留本任务单用于后续追溯

补充说明：
- 本批完成后再进入 audit logs。

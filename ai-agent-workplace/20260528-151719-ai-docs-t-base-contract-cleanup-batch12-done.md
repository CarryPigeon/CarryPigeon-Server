任务名称：
基于 docs-t 的 Base Contract Cleanup 批次十二

任务目标：
对齐当前剩余的基础 HTTP 契约差距：
- channel ban 路径 / 请求体 / 语义
- kick member 返回 `204`
- `POST /api/users/me/background`

任务背景：
在 message edit / message model / WS 核心批次收口后，剩余差距里最靠前的是若干已有功能但对外契约仍与 docs/t 不一致的基础接口。该批优先收口这些“路径/响应码/入口缺失”问题，避免继续积累兼容层和文档漂移。

影响模块：
- `chat-domain`
- `application-starter`（仅当测试装配需要时）

允许修改范围：
- `chat-domain/src/main/java/**/channel/**`
- `chat-domain/src/main/java/**/user/**`
- `chat-domain/src/main/java/**/shared/**`
- 与上述改动直接相关的测试
- `application-starter` 下与本批测试直接相关的支撑

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到 notification preferences / audit logs / remote discover
- 不在本批并行重构已稳定通过验证的 mentions / files / pins / applications 能力

依赖限制：
- 优先复用现有 channel governance、user profile、file 上传/对象存储主链路
- 若旧路径仍需兼容，优先保留运行时兼容并新增 docs/t 正式路径

配置限制：
- 不新增未来占位配置

文档依据：
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/SERVER_API.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：
1. 阅读 docs/t 中 kick、ban、user background upload 契约。
2. 阅读现有 `ChannelController` / `UserProfileController` / 相关 DTO / service。
3. 设计最小兼容改法：优先新增正式路径，必要时保留旧路径运行时兼容。
4. 实现 channel ban 契约对齐。
5. 实现 kick `204` 对齐。
6. 实现 `POST /api/users/me/background`。
7. 补 controller / application 定向测试并执行 Maven 验证。

关键假设与依赖：
- 现有 ban/kick 业务规则基本可复用，主要差距在 HTTP 契约层。
- user background upload 可沿用户资料或对象存储既有能力实现最小闭环。

实现要求：
- 以 docs/t 为协议真源。
- 优先做最小闭环，不做额外能力泛化。
- 对外响应码、路径、字段命名必须明确对齐 docs/t。

测试要求：
- 覆盖 ban 新路径成功/失败路径。
- 覆盖 kick 返回 `204`。
- 覆盖 user background upload 成功路径及必要失败路径。

质量门禁：
- 相关定向 Maven 测试通过。
- 无新增明确编译错误。
- 关键 HTTP 合同字段与状态码具备断言。

复审要求：
- 该批涉及正式 HTTP 路径与响应码调整，完成后需复查兼容性和 docs/t 一致性。

文档要求：
- 若只是在实现既有 docs/t 契约，不额外改 `docs/`

验收标准：
- channel ban 正式路径与请求体对齐 docs/t
- kick member 返回 `204`
- `POST /api/users/me/background` 可工作

完成定义：
- 验收标准满足
- 定向验证通过并记录
- 任务单补齐实际结果 / 验证记录 / 残留风险后转 `done`

实际结果：
- 已补齐 channel ban 的 v1 路径与请求体：`PUT /api/channels/{cid}/bans/{uid}`。
- 已将 kick member 正式返回码对齐为 `204`。
- 已实现 `POST /api/users/me/background` 最小上传闭环，并返回 `background_url`。

验证记录：
- `mvn -pl chat-domain -am -Dtest=ChannelControllerTests,ChannelBansControllerTests,UserProfileControllerTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- background upload 当前采用最小固定 share_key 方案，后续若产品需要多版本背景图策略，可再扩展。

知识沉淀 / 是否回写 docs：
- 暂不回写，除非发现 docs/t 本身有冲突

产物清理与保留说明：
- 保留本任务单用于后续批次追溯

补充说明：
- 本批完成后再进入 notification preferences / audit logs / remote discover。

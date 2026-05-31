任务名称：

第 2 轮：统一协议基础设施改造

任务目标：

在确认的迁移策略下，先落地新的协议基础设施，为后续端点改造提供统一支撑。

任务背景：

当前 `CPResponse`、错误返回、ID/时间编码与分页模型均不符合客户端基准 API，若不先统一底座，业务端点会重复返工。

影响模块：

- `chat-domain`
- 可能涉及 `application-starter`
- 可能涉及 `infrastructure-basic`

允许修改范围：

- 与协议基础设施直接相关的响应模型、异常映射、JSON 序列化约定、分页模型及其测试

禁止修改范围：

- 不在本轮直接重写大批业务端点
- 不在本轮直接重写 WS 业务事件链路
- 不新增无关业务能力

依赖限制：

- 原则上不新增依赖；如必须新增，需单独确认

配置限制：

- 不扩大全局配置体系

文档依据：

- `docs/异常与错误码规范.md`
- `docs/API.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：

1. 引入新的 HTTP 错误模型与异常映射。
2. 统一雪花 ID 的字符串编码与时间毫秒输出规则。
3. 统一分页 envelope 与 cursor 约定。
4. 补齐基础层测试。

关键假设与依赖：

- 依赖第 1 轮完成并确认。
- 若涉及对现有 `CPResponse` 的完全替换，必须按确认策略执行。

实现要求：

- 改造要能被后续各 controller 直接复用。

测试要求：

- 相关单元测试和协议映射测试必须补齐。

质量门禁：

- 新基础协议能力具备测试覆盖。
- 不产生额外架构越界。

复审要求：

- 需要针对异常模型、序列化与分页规则做专项自检。

文档要求：

- 若形成新的长期对外协议规则，后续轮次收口时评估是否回写 `docs/`

验收标准：

- 后续业务端点可在统一底座上继续改造。

完成定义：

- 基础协议能力落地并验证通过。

实际结果：

- 已完成第一个落地切片：新增标准 HTTP 错误响应模型 `ApiError` / `ApiErrorResponse`。
- 已将 `GlobalExceptionHandler` 从旧 `CPResponse` 失败包装切换为真实 HTTP 状态码 + 标准错误包。
- 已保留成功响应暂不切换，避免本轮第一步改动范围过大。
- 已补充 `ProblemException.validationFailed(reason, message)` 重载，为后续更细粒度 reason 预留基础能力。
- 已更新 `GlobalExceptionHandlerTests`，覆盖 401 / 404 / 422 / 500 与 `request_id` 写回契约。
- 已完成第二个落地切片：调整全局 Jackson 时间序列化策略，统一 `Instant` 输出为 epoch 毫秒。
- 已新增 `JacksonAutoConfigurationTests`，锁定 `snake_case` 字段命名与 `Instant` 毫秒输出契约。
- 已完成第三个落地切片的首个试点：新增通用 `CursorPageResponse<T>` 分页外壳。
- 已将用户分页接口 `/api/users/page` 与 `/api/users/search` 收敛到 `items + next_cursor + has_more` 结构。
- 已移除旧的 `UserProfilePageResponse`，避免同一业务同时维护两套分页协议。
- 已将同一通用分页 envelope 扩展到消息历史分页接口 `/api/channels/{channelId}/messages`。
- 已移除旧的 `ChannelMessageHistoryResponse`，统一分页协议出口。
- 已完成第四个落地切片：将雪花 ID 十进制字符串编码规则显式收拢到 `Ids` 工具语义。
- 已将认证、用户、频道、消息控制器以及通用分页外壳中的对外 ID 转换从分散的 `Long.toString(...)` 收敛为统一 `Ids.toString(...)`。
- 已新增 `IdsTests` 并扩展 `SnowflakeIdGeneratorTests`，锁定“生成的字符串 ID 必须为十进制数字串且可被统一工具无损解析回写”的基础契约。

验证记录：

- 测试命令：`mvn -q -pl chat-domain -am -Dtest=GlobalExceptionHandlerTests,HttpRequestMdcFilterTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- 说明：当前仅验证了异常出口相关最小契约测试，尚未执行完整模块测试，也尚未扩展到 ID/时间/分页协议改造
- 测试命令：`mvn -q -pl infrastructure-basic,chat-domain -am -Dtest=JacksonAutoConfigurationTests,JsonProviderTests,JsonsTests,GlobalExceptionHandlerTests,HttpRequestMdcFilterTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- 说明：已确认时间序列化基础改造与前一切片兼容，当前仍未切入分页 envelope 与雪花 ID 字符串编码
- 测试命令：`mvn -q -pl chat-domain -am -Dtest=UserProfileControllerTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- 说明：已确认新的通用分页 envelope 能在用户分页接口族上落地，并与前序错误模型改造兼容
- 测试命令：`mvn -q -pl chat-domain -am -Dtest=ChannelMessageQueryControllerTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- 说明：已确认通用分页 envelope 可扩展到消息历史分页接口，且与消息查询控制器上的真实 HTTP 错误状态码断言兼容
- 测试命令：`mvn -q -pl infrastructure-basic -am -Dtest=IdsTests,SnowflakeIdGeneratorTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- 说明：已确认统一 ID 工具与生成器字符串出口满足十进制字符串契约
- 测试命令：`mvn -q -pl chat-domain -am -Dtest=UserProfileControllerTests,ChannelMessageQueryControllerTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- 说明：已确认受影响控制器在收拢 ID 字符串编码后仍保持既有协议契约
- 测试命令：`mvn -q -pl infrastructure-basic,chat-domain -am -Dtest=IdsTests,SnowflakeIdGeneratorTests,JacksonAutoConfigurationTests,GlobalExceptionHandlerTests,UserProfileControllerTests,ChannelMessageQueryControllerTests -Dsurefire.failIfNoSpecifiedTests=false test`
- 结果：通过
- 说明：已确认错误模型、时间毫秒输出、分页 envelope 与统一 ID 字符串编码可以共同工作

残留风险：

- 若基础协议层改造不完整，后续业务端点会重复改动。
- 当前仍保留成功响应 `CPResponse<T>` 作为对外成功 envelope；后续若 round3/4 要完全切入 `docs/t` 资源模型，仍需继续评估成功响应外壳是否在后续轮次进一步收敛或替换。

知识沉淀 / 是否回写 docs：

- 当前轮次新增的“统一使用 `Ids` 作为对外雪花 ID 十进制字符串编码入口”已经具备稳定规则特征；是否回写 `docs/`，建议在 round6 文档收口时与成功响应外壳策略一起统一评估。

产物清理与保留说明：

- 相关分析草稿留在 `ai-agent-workplace/`

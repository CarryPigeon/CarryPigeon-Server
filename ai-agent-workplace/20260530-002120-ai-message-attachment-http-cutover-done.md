任务名称：
docs-t Public API Alignment

任务目标：
以 `docs/t` 下客户端基准 API 为唯一对外标准，持续重写当前项目的 HTTP / WebSocket 公共协议；不保留旧端点与旧入站协议，同时保留当前 Netty WebSocket 运行模型。

任务背景：
当前仓库处于重写阶段，已有一部分公开接口切向 `docs/t`，但仍残留旧 HTTP 端点、旧 WS 入站命令、错误 `reason`、公开响应字段不一致以及实时事件缺失等问题。用户要求在彻底对齐前持续自循环实现，不做旧协议兼容。

任务类型：
实现类任务

影响模块：
- `chat-domain`
- `application-starter`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `ai-agent-workplace/`

允许修改范围：
- 按 `docs/t` 调整公开 HTTP 端点、DTO、错误模型与 WS 事件
- 删除旧公开端点与旧公开 WS 入站命令
- 补齐 mention / pin / channel read-state / channel change 等客户端已依赖的实时语义
- 为对齐所需的持久化契约与测试替身做最小改造
- 同步补充任务记录

禁止修改范围：
- 不修改长期 `docs/`
- 不引入兼容旧端点
- 不变更 Netty WS 模型

文档依据：
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/SERVER_API.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：
1. 以 `docs/t` 盘点公开 HTTP 端点、错误 reason、分页 cursor、WS 事件与入站命令。
2. 删除旧附件公开端点，并把 file / voice / HTTP / WS 出站统一到 `share_key + download_path + Core:* domain`。
3. 修正公开基础语义，包括 `server_id`、plugin catalog 可见性、`users/me`、opaque cursor、公开错误映射。
4. 删除旧公开 WS 入站命令兼容，仅保留基准协议。
5. 补齐消息相关 P0 行为：消息编辑、转发、pins、mentions 以及对应实时事件。
6. 补齐频道侧客户端同步依赖的实时事件：`read_state.updated`、`channel.changed`、`channels.changed`。
7. 持续运行定向编译 / 测试并继续扫描剩余协议偏差。

关键假设与依赖：
- `docs/t` 是当前唯一公开协议基线；若与旧服务实现冲突，以 `docs/t` 为准。
- 内部仍可保留对象存储键、数据库字段或旧领域模型，但不能泄漏到公开协议。
- 频道侧实时事件以“最小刷新提示”形式对齐 `docs/t`，不额外引入新公共事件。

完成定义：
- 公开 HTTP 端点、WS 事件、错误 reason、分页 cursor 与 `docs/t` 对齐
- 旧公开端点 / 旧公开 WS 入站命令已删除
- 关键定向测试通过，任务单补全验证结果与残留风险

实际结果：
- 已完成 `docs/t` 公开协议对齐收口。
- HTTP / WebSocket 公开协议已统一到 `docs/t` 基线，不再保留旧公开端点与旧公开入站命令。
- file / voice 公开输出已统一为 `share_key + download_path`，公开 `domain` 统一为 `Core:*`，`server_id`、plugin catalog、`users/me`、opaque cursor、公开错误映射均已对齐。
- 频道与消息侧客户端依赖的实时语义已补齐，包括 mention、pin / unpin、`read_state.updated`、`channel.changed`、`channels.changed`。
- 收尾阶段补齐了 `application-starter` 回归测试装配偏差：
  - `RealtimeServerProperties.path` 从旧 `"/ws"` 更正为 `"/api/ws"`
  - `StarterRegressionConfiguration` 中的 `MentionRepository` 改为缺省装配，避免与运行时测试替身重复注册

验证记录：
- 已完成一轮定向测试：
  - `mvn -q -pl chat-domain -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MessageApplicationServiceSendTests,MessageApplicationServiceForwardTests,MessageApplicationServicePinsTests,MentionApplicationServiceTests,DatabaseBackedMentionRepositoryTests,NettyMessageRealtimePublisherTests,RealtimeChannelHandlerMessageDispatchTests,UserProfileControllerTests,AuthControllerTests test`
  - 结果：通过
- 已完成频道侧 realtime 事件补齐后的编译验证：
  - `mvn -pl chat-domain -am -DskipTests test-compile -Dstyle.color=never`
  - 结果：通过
- 已完成频道 / 审计 / realtime 定向验证：
  - `mvn -q -pl chat-domain -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ChannelApplicationServiceTests,ChannelDiscoverApplicationServiceTests,AuditLogApplicationServiceTests,MessageApplicationServiceSendTests,MessageApplicationServicePinsTests,NettyMessageRealtimePublisherTests,NettyChannelRealtimePublisherTests test`
  - 结果：通过
- 已完成 starter 模块编译验证：
  - `mvn -pl application-starter -am -DskipTests test-compile -Dstyle.color=never`
  - 结果：通过
- 已完成最终公开协议回归验证：
  - `mvn -pl chat-domain,application-starter -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ServerControllerTests,MessageAttachmentRegressionTests,RealtimeChannelHandlerMessageDispatchTests test -Dstyle.color=never`
  - 结果：通过
- 已完成公开协议残差扫描：
  - `rg -n "send_channel_message|legacy|compat|兼容历史|过渡|占位|/ws\\\"|object_key\\\"|objectKey\\\"" chat-domain/src/main/java chat-domain/src/test/java application-starter/src/main/java application-starter/src/test/java -g '!**/target/**'`
  - 结果：未发现旧公开端点、旧公开 WS 入站命令或错误的 `/ws` 公开路径残留；`object_key` 仅剩内部实现与测试场景

残留风险：
- `object_key` / `objectKey` 仍保留在内部附件 payload 解析与部分测试中，用于内部 canonical payload 兼容与测试断言；当前没有继续泄漏到公开 HTTP / WS 协议。
- 本次未新增长期项目规则，因此未修改 `docs/`。

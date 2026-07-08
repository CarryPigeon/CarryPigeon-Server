# 实时推送通知偏好完善任务单

## 任务目标

在不改变模块结构和外部服务依赖的前提下，让实时消息 / 频道事件推送尊重当前已有通知偏好模型，避免用户关闭或静音后仍收到对应 realtime event。

## 任务类型

实现类任务。

## affected modules

- `chat-domain`
- `ai-agent-workplace`

## 允许修改范围

- `chat-domain` 内实时发布器、通知偏好领域模型 / 仓储端口相关的最小代码与测试。
- 本任务单的记录与归档。

## 禁止边界

- 不新增第三方依赖。
- 不改变 Maven 模块结构。
- 不让 `chat-domain` 依赖任何 `infrastructure-service/*-impl`。
- 不修改数据库表结构，优先使用当前已存在的通知偏好仓储能力。
- 不实现 `device_id` 持久化校验或内置 WSS/TLS，这些需要单独产品 / 部署确认。

## governing docs

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/注释规范.md`

## 执行计划

1. 读取通知偏好模型、仓储端口、领域服务、实时发布器和现有测试。
2. 明确当前偏好字段对 realtime event 的过滤语义。
3. 在既有边界内接入过滤逻辑，避免实时发布器绕过偏好。
4. 补充单元 / 契约测试覆盖全局关闭、频道静音和默认允许。
5. 运行相关 Maven 测试；视影响范围运行 `chat-domain` 测试。
6. 按变更审核清单记录结果并归档任务单。

## acceptance criteria

- 关闭全局通知的用户不会收到 message / mention / pin / read-state 等普通实时事件。
- 频道静音用户不会收到对应频道的普通消息 / 频道事件。
- `channels.changed` 属于账号可见频道集合的结构同步事件，不按通知偏好过滤，避免静音后客户端无法刷新频道列表。
- 系统级认证 / ping / replay 等连接协议不受通知偏好过滤影响。
- 相关测试通过，任务单归档为 `done`。

## implementation result

- 新增 `RealtimeNotificationPreferenceFilter`，在实时事件写入 `RealtimeSessionRegistry` 和 Netty 帧推送前按通知偏好筛选接收账号。
- `NettyMessageRealtimePublisher` 已接入过滤：
  - `message.created`
  - `message.updated`
  - `message.recalled`
  - `message.deleted`
  - `message.pinned`
  - `message.unpinned`
  - `mention.created`
- `NettyChannelRealtimePublisher` 已接入过滤：
  - `read_state.updated`
  - `channel.changed`
- `channels.changed` 保持不过滤，作为账号频道集合结构同步事件继续下发。
- `RealtimeServerConfiguration` 新增过滤器 Bean，并注入消息 / 频道 realtime 发布器。
- 未新增依赖，未修改 Maven 模块结构，未让 `chat-domain` 依赖任何 `infrastructure-service/*-impl`。

## tests

- `mvn -pl chat-domain -am -Dtest=RealtimeNotificationPreferenceFilterTests,NettyMessageRealtimePublisherTests,NettyChannelRealtimePublisherTests,RealtimeServerConfigurationContextTests,RealtimeServerConfigurationPublisherFactoryTests -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`
  - 结果：通过，22 个测试，0 失败，0 错误。
- `mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：通过，351 个测试，0 失败，0 错误。
- `mvn test -DskipTests=false`
  - 结果：通过，完整 reactor 成功；存在既有环境类测试跳过，未由本次变更引入。
- `mvn -pl chat-domain -am -Dtest=NettyMessageRealtimePublisherTests,NettyChannelRealtimePublisherTests,RealtimeNotificationPreferenceFilterTests -Dsurefire.failIfNoSpecifiedTests=false test -DskipTests=false`
  - 结果：通过，19 个测试，0 失败，0 错误。

## self check

- 架构边界：通过。新代码位于 `chat-domain/features/server/support/realtime` 与 `config`，复用既有 `NotificationPreferenceRepository` 端口。
- 依赖方向：通过。未新增第三方依赖，未引入 starter 或 impl 包依赖。
- 配置影响：通过。仅新增实际使用的 Spring Bean，无新增外部配置项。
- 注释规范：通过。新增关键类、Bean 方法和测试均有职责 / 边界说明。
- 测试覆盖：通过。覆盖全局静音、频道静音、仅提及模式、过期静音、频道覆盖、结构同步不过滤和发布器跳过推送。

## unresolved risks

- 当前过滤器为每个接收账号读取一次服务端偏好和频道偏好列表，符合“不扩展数据库 API”的任务边界；若频道大规模群发性能不足，应单独设计批量查询或缓存策略。
- `device_id` 级别偏好、WSS/TLS 部署校验仍不在本任务范围内，需要单独任务确认。

# 上线门禁实时事件测试修正任务单

## 任务目标

修正全量测试中暴露的 `NettyChannelRealtimePublisherTests.publishChannelChanged_mutedChannel_skipsEvent` 过期测试期望，并再次执行链路自检与测试门禁，确认是否仍存在阻塞 bug。

## 影响模块

- `chat-domain`
- `ai-agent-workplace/`

## 允许修改范围

- 允许修改 `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/support/realtime/NettyChannelRealtimePublisherTests.java`
- 允许更新并归档本任务单

## 禁止边界

- 不修改生产源码
- 不修改 HTTP / WS 协议
- 不新增依赖
- 不改变 Maven 模块结构
- 不回退或清理用户/其他任务产生的既有改动

## 文档依据

- `AGENTS.md`
- `docs/standards/变更审核清单.md`
- `docs/standards/测试规范.md`
- `ai-agent-workplace/20260708-123653-ai-release-chain-readiness-check-done.md`

## 验收标准

- 测试期望与当前实时同步事件设计一致：`channel.changed` 不受通知偏好静音过滤。
- 相关定向测试通过。
- 全量 `mvn test -DskipTests=false` 通过，或记录新的阻塞原因。
- 执行边界扫描与结果记录。
- 任务单归档为 `done`。

## 执行记录

### 修正内容

- 修改文件：`chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/support/realtime/NettyChannelRealtimePublisherTests.java`
- 修正点：将 `publishChannelChanged_mutedChannel_skipsEvent` 的旧期望修正为 `publishChannelChanged_mutedChannel_emitsSyncEvent`。
- 原因：`channel.changed` 是频道结构同步事件，不是消息/提及通知事件。当前 `RealtimeNotificationPreferenceFilter.FILTERED_EVENT_TYPES` 不包含 `channel.changed`，该事件应在频道静音时继续下发，避免客户端结构状态不同步。
- 断言：静音频道接收人仍收到 `channel.changed`，并校验 `cid`、`scope`、`hint`。

### 自检结果

- 旧失败断言清理：
  - 命令：`rg 'publishChannelChanged_mutedChannel_skipsEvent|assertNull' chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/support/realtime/NettyChannelRealtimePublisherTests.java -n`
  - 结果：无命中。
- 模块边界扫描：
  - 命令：`rg "infrastructure\\.service\\..*\\.impl|backend\\.starter|application\\.starter|team\\.carrypigeon\\.backend\\.starter" chat-domain infrastructure-basic infrastructure-service/*-api -n`
  - 结果：无命中。
- 实时事件语义扫描：
  - 命令：`rg 'FILTERED_EVENT_TYPES|channel\\.changed|read_state\\.updated|channels\\.changed|mention\\.created|message\\.created' chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/support/realtime chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/support/realtime -n`
  - 结果：通知类事件仍由过滤器覆盖；`read_state.updated`、`channel.changed`、`channels.changed` 有测试保护为同步事件。

### 测试记录

- 定向测试：
  - 命令：`mvn -pl chat-domain -am -Dtest=NettyChannelRealtimePublisherTests,RealtimeNotificationPreferenceFilterTests -Dsurefire.failIfNoSpecifiedTests=false test`
  - 结果：BUILD SUCCESS；`NettyChannelRealtimePublisherTests` 5 个测试通过，`RealtimeNotificationPreferenceFilterTests` 6 个测试通过。
- 全量测试：
  - 命令：`mvn test -DskipTests=false`
  - 结果：BUILD SUCCESS。
  - Reactor：14 个模块全部 SUCCESS。
  - 主要统计：`chat-domain` 362 个测试通过；`database-impl` 101 个测试通过、1 个环境测试跳过；`cache-impl` 26 个测试通过、1 个环境测试跳过；`storage-impl` 16 个测试通过、1 个环境测试跳过；`mail-impl` 11 个测试通过、1 个环境测试跳过；`application-starter` 18 个测试通过、1 个环境测试跳过。

### 复审结论

通过。当前已修正全量测试中的实时事件测试期望问题；本轮未修改生产源码，未发现新的模块边界问题或实时通知过滤回退问题。

### 残留风险

- 当前工作区仍存在大量既有未提交/未跟踪改动，这不是本任务引入，但上线前仍需要人工确认发布范围。
- 本轮未执行真实 Docker 外部服务启动和手工 HTTP/WS 联调；本轮验证范围为自动化测试和静态链路自检。

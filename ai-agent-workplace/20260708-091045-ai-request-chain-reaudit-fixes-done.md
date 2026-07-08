# 请求处理链复审问题修复任务单

## 任务目标

根据 `ai-agent-workplace/20260708-085551-ai-request-chain-reaudit-done.md` 的问题清单，修正当前请求处理链中的已确认缺陷和一致性问题。

## 影响模块

- `chat-domain`
- `infrastructure-service/database-api`
- `infrastructure-service/database-impl`
- `docs/`
- `ai-agent-workplace/`

## 允许修改范围

- 修复用户公开资料批量查询全表扫描问题。
- 修复 HTTP 消息发送请求体 null 校验问题。
- 统一文件下载路径前导 `/` 的响应与文档口径。
- 修正 discovery `ws_url` 文档与代码口径。
- 调整 realtime 通知偏好过滤，避免状态同步类事件被静音策略拦截。
- 避免重复撤回产生重复 realtime 事件。
- 补齐部分路径变量 Controller 层正数校验。
- 为上述修复补充或更新聚焦测试。

## 禁止边界

- 不新增外部依赖。
- 不改 Maven 模块结构。
- 不让 `chat-domain` 依赖任何 `*-impl`。
- 不引入新的数据库表。
- 不改变已有对外路径。

## 文档依据

- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/测试规范.md`
- `ai-agent-workplace/20260708-085551-ai-request-chain-reaudit-done.md`

## 验收标准

- 复审任务单中 R-001 到 R-007 有对应修复或明确保留说明。
- `chat-domain` 不出现直接依赖 database-impl/starter 的边界违规。
- 相关 Controller/domain/database-impl 测试通过。
- 任务完成后本任务单归档为 `done`。

## 实施记录

## 已完成修复

### R-001 用户批量公开资料查询全表扫描

- 在 `UserProfileRepository` 增加 `findByAccountIds(...)` 默认端口。
- 在 `UserProfileDatabaseService` 增加 `findByAccountIds(...)` 默认契约。
- `UserProfileDomainApi.getPublicUserProfiles(...)` 改为调用批量查询端口。
- `DatabaseBackedUserProfileRepository` 覆盖批量查询，转发到 database-api。
- `MybatisPlusUserProfileDatabaseService` 用 `LambdaQueryWrapper.in(...)` 下推数据库批量查询。
- 增加 `DatabaseBackedUserProfileRepositoryTests`、`MybatisPlusUserProfileDatabaseServiceTests` 覆盖批量查询。

### R-002 HTTP 发送消息请求体 null 校验

- `ChannelMessageController.sendChannelMessage(...)` 改为 `@Valid @NotNull @RequestBody`。
- 增加 Controller 测试覆盖 JSON `null` 请求体返回 422。

### R-003 文件下载 path 统一前导 `/`

- `UserProfileController.uploadCurrentUserBackground(...)` 返回 `/api/files/download/...`。
- `ServerEntranceDomainApi.DEFAULT_AVATAR` 改为 `/api/files/download/server_avatar`。
- `FileShareKeyCodec.downloadPath(...)` 改为 `/api/files/download/...`。
- 更新 schema 示例、测试断言和 `docs/API.md` / `docs/t` 示例。

### R-004 discovery ws_url 文档与代码口径不一致

- `ServerDiscoveryDocument` schema 示例改为 `ws://127.0.0.1:18080/api/ws`。
- `docs/API.md` 与 `docs/t/11-http-endpoints-v1.md` discovery 示例改为当前代码返回的 `ws://` 口径。
- 保留文档说明：内置 Netty 未装配 TLS，生产 WSS 需由前置网关/反向代理/负载均衡完成 TLS 终止。

### R-005 通知偏好过滤状态同步事件

- `RealtimeNotificationPreferenceFilter` 过滤集合移除 `read_state.updated`、`channel.changed`。
- 更新测试：静音仍拦截 `message.created` / `mention.created`，但不拦截 read-state/channel changed 同步事件。

### R-006 重复撤回重复发布事件

- `ChannelMessageLifecycleDomainApi.recallChannelMessage(...)` 对已撤回消息保持幂等返回，但不再发布 update/recalled 事件、不再新增审计日志、不再写更新。
- 增加领域测试覆盖重复撤回无副作用。

### R-007 Controller 层路径变量正数校验

- `ChannelApplicationController` 增加 `@Validated`，并对 `channelId`、`applicationId` 补 `@Positive`。
- `ChannelReadStateController` 增加 `@Validated`，并对 `channelId` 补 `@Positive`。
- `ChannelPinsController` 对 `channelId`、`messageId` 补 `@Positive`。

### 额外同步修复

- `NotificationPreferenceController.updateServerNotificationPreference(...)` 改为 `@Valid @NotNull @RequestBody`。
- `UpdateNotificationPreferenceRequest` 增加 `mode @NotBlank`、`mutedUntil @PositiveOrZero`。
- 增加通知偏好 JSON `null` 请求体测试。

## 验证记录

静态检查：

- `rg "findByAccountIds\\(|userProfileRepository\\.findAll" ...`
  - 结果：生产 `UserProfileDomainApi` 已走 `findByAccountIds(...)`，全表扫描只保留在仓储默认 fallback。
- `rg '"api/files/download/|`api/files/download/|value\\("api/files/download/|assertEquals\\("api/files/download/' ...`
  - 结果：无旧无前导 `/` 的返回路径断言或示例。
- `rg 'wss://127|wss://example|固定使用' docs/API.md docs/t/11-http-endpoints-v1.md ...`
  - 结果：无旧 discovery `wss://` 示例或“固定 wss”说明。
- `rg "infrastructure\\.service\\..*\\.impl|backend\\.starter|application\\.starter|team\\.carrypigeon\\.backend\\.starter" chat-domain/src/main/java chat-domain/src/test/java infrastructure-basic/src/main/java infrastructure-service/*-api/src/main/java -n`
  - 结果：无命中。

测试：

```bash
mvn -pl chat-domain,infrastructure-service/database-impl -am -Dtest=DatabaseBackedUserProfileRepositoryTests,MybatisPlusUserProfileDatabaseServiceTests,UserProfileDomainApiTests,ChannelMessageQueryControllerTests,NotificationPreferenceControllerTests,ChannelMessageLifecycleDomainApiTests,RealtimeNotificationPreferenceFilterTests,ServerControllerTests,ServerEntranceDomainApiTests,MessageAttachmentPayloadResolverTests,NettyMessageRealtimePublisherTests -Dsurefire.failIfNoSpecifiedTests=false test
```

- 结果：chat-domain 67 个测试通过，database-impl 12 个测试通过，BUILD SUCCESS。

```bash
mvn -pl chat-domain,infrastructure-service/database-impl -am -Dtest=AuthControllerTests,UserProfileControllerTests,DatabaseBackedUserProfileRepositoryTests,UserProfileDomainApiTests,FileControllerTests,ChannelControllerTests,ChannelApplicationControllerTests,ChannelReadStateControllerTests,ChannelPinsControllerTests,ChannelMessageQueryControllerTests,MessageForwardControllerTests,ChannelMessageLifecycleDomainApiTests,MessageBusinessChainTests,NotificationPreferenceControllerTests,RealtimeChannelHandlerMessageDispatchTests,RealtimeNotificationPreferenceFilterTests,ServerControllerTests,ServerEntranceDomainApiTests,MessageAttachmentPayloadResolverTests,NettyMessageRealtimePublisherTests,MybatisPlusUserProfileDatabaseServiceTests -Dsurefire.failIfNoSpecifiedTests=false test
```

- 结果：chat-domain 149 个测试通过，database-impl 12 个测试通过，BUILD SUCCESS。

```bash
mvn -pl application-starter -am -DskipTests compile
```

- 结果：13 个 reactor 模块编译通过，BUILD SUCCESS。

## 未完成项 / 风险

- 未运行全仓库 `mvn test`。
- `docs/t/10-http-ws-protocol-v1.md` 和 `docs/t/12-ws-events-v1.md` 仍保留生产/客户端协议视角的 `wss://{host}` 示例；本次只修正当前代码 discovery 示例和 `docs/API.md` 当前代码口径说明。
- `UserProfileRepository.findByAccountIds(...)` 保留默认 `findAll()` fallback，目的是避免大量测试替身被迫实现新方法；生产 `DatabaseBackedUserProfileRepository` 已覆盖为 database-api 批量查询。

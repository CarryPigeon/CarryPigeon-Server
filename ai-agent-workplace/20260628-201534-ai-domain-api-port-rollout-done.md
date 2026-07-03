任务名称：

domain api/port 结构规则跨 feature 落地

任务目标：

按已更新的 `docs/包结构规范.md`，将 auth 之外的 feature 也落地 `domain/api` 与 `domain/port`：

- `domain/api`：向 controller、server、其他 feature 暴露 domain 能力的入口接口。
- `domain/port`：domain service 依赖的非持久化能力端口。
- `domain/service`：只保留业务服务、业务用例实现、业务策略与协作对象。

任务背景：

上一任务已在 auth feature 落地新结构。本轮用户要求对其他 feature 也进行同层面的落地。初步扫描显示：

- `domain/service` 中仍有能力端口接口：
  - `channel`: `ChannelMessageBoundary`, `ChannelRealtimePublisher`
  - `file`: `FileAttachmentAccessAuthorizer`
  - `message`: `MessageChannelBoundary`, `ChannelMessagePlugin`, `MessagePayloadResolver`, `MessageRealtimePublisher`
- 多个 controller/server/support 仍直接依赖具体 `*DomainService`，需要通过 `domain/api` 暴露稳定入口接口。

影响模块：

- `chat-domain`
- `ai-agent-workplace`

允许修改范围：

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/channel/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/file/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/message/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/user/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/channel/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/file/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/message/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/server/**`
- `chat-domain/src/test/java/team/carrypigeon/backend/chat/domain/features/user/**`
- 当前任务单本身

禁止修改范围：

- 不修改业务行为、HTTP/WS 协议、错误码和响应字段。
- 不新增第三方依赖。
- 不修改 Maven 模块结构。
- 不迁移 repository 抽象；repository 仍保留在 `domain/repository`。
- 不修改正式 docs，除非发现上一轮规则需要修正并另行说明。
- 不把本任务扩张为业务服务拆分或控制器重写。

拟定落地范围：

1. 迁移能力端口到 `domain/port`：
   - channel: `ChannelMessageBoundary`, `ChannelRealtimePublisher`
   - file: `FileAttachmentAccessAuthorizer`
   - message: `MessageChannelBoundary`, `ChannelMessagePlugin`, `MessagePayloadResolver`, `MessageRealtimePublisher`
2. 新增对外 domain api：
   - channel：按现有领域服务粒度新增 `ChannelAccessApi`, `ChannelApplicationFlowApi`, `ChannelGovernanceApi`, `ChannelLifecycleApi`, `ChannelQueryApi`
   - file：新增 `FileApi`
   - message：新增 `MessageDeliveryApi`, `MessageModerationApi`, `MessageQueryApi`, `MentionApi`, `MessagePluginCatalogApi`
   - server：新增 `ServerApi`, `NotificationPreferenceApi`
   - user：新增 `UserProfileApi`
3. 让对应 `*DomainService` 实现 api 接口。
4. controller/server/support 对外调用点优先依赖 `domain/api`，不直接依赖具体 `*DomainService`。
5. support/config 中的技术适配实现依赖 `domain/port`。

实施步骤：

1. 迁移 port 接口文件并更新 main/test imports。
2. 新增 api 接口并让领域服务实现。
3. 更新 controller、server support、controller tests 的注入类型。
4. 扫描 `domain/service` 下是否仍有 `public interface`。
5. 扫描 controller/server/support 是否仍直接依赖具体 `*DomainService`。
6. 运行受影响目标测试。
7. 运行 `mvn -pl chat-domain -am test -DskipTests=false`。

验收标准：

- auth 之外的能力端口接口不再位于 `domain/service`。
- controller/server/support 对 domain 能力的外部调用优先依赖 `domain/api`。
- `domain/service` 中仅保留业务服务、业务协作实现或业务语义类。
- 受影响测试与 chat-domain 全量测试通过。
- 任务单记录实际结果、验证命令、残留风险，并关闭为 `done`。

执行前确认点：

- 本任务涉及跨 feature 包结构迁移和注入类型调整，需用户确认后再开始修改正式代码。

实际结果：

- 已在 auth 之外的 feature 落地 `domain/api`：
  - channel: `ChannelAccessApi`, `ChannelApplicationFlowApi`, `ChannelGovernanceApi`, `ChannelLifecycleApi`, `ChannelQueryApi`
  - file: `FileApi`
  - message: `MessageDeliveryApi`, `MessageModerationApi`, `MessageQueryApi`, `MentionApi`, `MessagePluginCatalogApi`
  - server: `ServerApi`, `NotificationPreferenceApi`
  - user: `UserProfileApi`
- 已将非持久化能力端口迁移到 `domain/port`：
  - channel: `ChannelMessageBoundary`, `ChannelRealtimePublisher`
  - file: `FileAttachmentAccessAuthorizer`
  - message: `MessageChannelBoundary`, `ChannelMessagePlugin`, `MessagePayloadResolver`, `MessageRealtimePublisher`
- 已将 `MessageSenderSnapshot` 移入 `message/domain/port`，因为它是 `MessageRealtimePublisher` 端口签名的一部分。
- 已让对应 `*DomainService` 实现 `domain/api` 接口。
- 已更新正式 controller、server config、server support/realtime 等外部调用点，避免直接依赖具体 `*DomainService`。
- 已保留 `ChannelMessagePluginRegistry`、`ChannelMessagePluginRegistration`、`ChannelMessagePluginDescriptor` 在 `message/domain/service`，因为它们是插件治理/装配语义对象，不是能力端口接口。
- 已同步修正 main/test imports 与测试 fixture 中的具体实现构造点。

验证记录：

- `mvn -pl chat-domain -am -DskipTests compile`
  - 结果：通过。
- `mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：通过。
  - `chat-domain` 测试：302 run, 0 failures, 0 errors, 0 skipped。
- 结构扫描：
  - `rg -n "public interface" chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/*/domain/service`
    - 结果：无命中，`domain/service` 下无接口残留。
  - 精确扫描旧端口路径 `domain.service.(ChannelMessageBoundary|ChannelRealtimePublisher|FileAttachmentAccessAuthorizer|MessageChannelBoundary|ChannelMessagePlugin|MessagePayloadResolver|MessageRealtimePublisher)`
    - 结果：无命中。
  - 正式 controller/server config/server support 直接导入具体 `*DomainService` 扫描
    - 结果：无命中。

残留风险：

- 未发现本任务范围内残留风险。
- `git status` 显示大量既有重构删除、移动与文档修改，其中相当部分来自前序已确认任务；本任务未尝试回滚或清理与当前目标无关的变更。

知识沉淀 / 是否回写 docs：

- 不回写 docs。本任务按上一轮已确认并写入的 `docs/包结构规范.md` 执行，没有新增长期规则。

产物清理与保留说明：

- 保留当前任务单作为 AI 协作追踪记录，并在完成后由 `current` 归档为 `done`。

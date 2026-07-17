任务名称：

domain api 一接口一实现类代码落地

任务目标：

按 `docs/architecture/包结构规范.md` 中已确认的规则，将 `chat-domain` 的正式 Java 实现调整为一个 `domain/api` 接口对应一个明确命名的 `*DomainApi` 实现类，避免 `domain/service` 直接实现对外 API，避免一个类实现多个 API。

任务背景：

现有代码中多个 `*DomainService` 直接 `implements *Api`，并存在 `AuthDomainService` 同时实现 `AuthAccountApi` 与 `AuthSessionApi`、`ChannelMessageDomainApi` 同时实现 `ChannelMessageApi`、`ChannelMessageAttachmentApi` 与 `ChannelPinApi` 的情况。用户已要求开始调整 Java 实现。

影响模块：

- `chat-domain`
- `ai-agent-workplace`

允许修改范围：

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/**/domain/service/**`
- 必要的 `chat-domain/src/test/java/**` 测试和测试支撑
- 当前任务单

禁止修改范围：

- 不改变 HTTP/WS 协议、响应字段、错误码和业务行为。
- 不新增第三方依赖。
- 不修改 Maven 模块结构。
- 不移动 repository/port/api 包边界。
- 不修改正式 docs，除非发现已写规则需要纠错。

实施方案：

1. 为每个 `domain/api` 接口创建一一对应的 `*DomainApi` 实现类。
2. 从内部 `*DomainService` 上移除 `implements *Api`，保留业务方法供 API 实现类委托。
3. 拆分现有多接口实现：
   - `AuthAccountDomainApi`
   - `AuthSessionDomainApi`
   - `ChannelMessageDomainApi`
   - `ChannelMessageAttachmentDomainApi`
   - `ChannelPinDomainApi`
4. 对已有单接口 service 也改成对应 `*DomainApi` 薄委托实现。
5. 保持 controller/realtime/support 依赖 `domain/api` 不变，由 Spring 注入新的实现类。
6. 更新必要测试替身，避免生产代码出现匿名 API 实现。
7. 运行编译与测试，并扫描正式代码是否仍存在不合规实现。

验收标准：

- `chat-domain/src/main/java` 中不存在 `class ... implements ...Api` 的 `*DomainService`。
- `chat-domain/src/main/java` 中每个 `domain/api` 接口都有一个明确命名的 `*DomainApi` 实现类。
- 正式代码不存在匿名 `new XxxApi() {}` 作为 API 实现。
- `mvn -pl chat-domain -am -DskipTests compile` 通过。
- `mvn -pl chat-domain -am test -DskipTests=false` 通过。

实际结果：

- 已为 `chat-domain` 中 16 个 `domain/api` 接口落地一一对应的 `*DomainApi` 实现类。
- 已从原内部业务服务中移除对外 API 实现职责：
  - `AuthDomainService`
  - `FileDomainService`
  - `ChannelAccessDomainService`
  - `ChannelApplicationFlowDomainService`
  - `ChannelGovernanceDomainService`
  - `ChannelLifecycleDomainService`
  - `ChannelQueryDomainService`
  - `ChannelMessageDomainApi` 保留为 `ChannelMessageApi` 的单一实现，不再同时实现附件和置顶 API
  - `MentionDomainService`
  - `MessagePluginCatalogDomainService`
  - `NotificationPreferenceDomainService`
  - `ServerDomainService`
  - `UserProfileDomainService`
- 新增或调整的 API 实现类包括：
  - `AuthAccountDomainApi`
  - `AuthSessionDomainApi`
  - `ChannelAccessDomainApi`
  - `ChannelApplicationFlowDomainApi`
  - `ChannelGovernanceDomainApi`
  - `ChannelLifecycleDomainApi`
  - `ChannelQueryDomainApi`
  - `FileTransferDomainApi`
  - `ChannelMessageDomainApi`
  - `ChannelMessageAttachmentDomainApi`
  - `ChannelPinDomainApi`
  - `MentionDomainApi`
  - `MessagePluginCatalogDomainApi`
  - `NotificationPreferenceDomainApi`
  - `ServerEntranceDomainApi`
  - `UserProfileDomainApi`
- 已把测试支撑中的匿名 `ChannelMessageApi` 替换为显式命名测试替身 `SendingOnlyChannelMessageApi`。
- 已修正 server 相关 controller 测试 fixture，使测试同样通过 `*DomainApi` 包装内部 service，而不是把内部 service 当作 API。

验证记录：

- `mvn -pl chat-domain -am -DskipTests compile`
  - 结果：通过。
- `mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：通过。
  - 最终统计：302 个测试，0 failures，0 errors，0 skipped。
- `rg -n "class .*DomainService .*implements .*Api|new [A-Za-z0-9]+Api\\(\\)|implements .*Api," chat-domain/src/main/java chat-domain/src/test/java`
  - 结果：无命中。
- `find chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features -path '*/domain/api/*.java' -print | sort | wc -l`
  - 结果：16。
- `rg -n "class .*DomainApi .*implements .*Api" chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features | wc -l`
  - 结果：16。

残留风险：

- 本次只做 API 实现边界重排，不改变业务规则；新增 `*DomainApi` 均为薄委托层，后续业务复杂度仍应继续沉在内部领域服务中。
- 当前仓库已有较多前序重构未提交文件，本任务未回滚或整理无关变更。

知识沉淀 / 是否回写 docs：

- 未回写正式 docs。
- 本次是对既有 `docs/architecture/包结构规范.md` 一接口一实现类规则的代码落地。

产物清理与保留说明：

- 本任务单归档为 `done` 后保留在 `ai-agent-workplace/`。

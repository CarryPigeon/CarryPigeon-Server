任务名称：

domain api 实现类承载业务实现

任务目标：

将 `chat-domain` 中当前薄委托式 `*DomainApi` 调整为真正承载业务实现的 API 实现类。API 实现类内部应直接编写业务实现代码，不再持有对应 `*DomainService` 并简单委托。

任务背景：

上一轮已按“一接口一实现类”新增 `*DomainApi`，但多数实现类只是对 `*DomainService` 的装饰/委托。用户明确要求 API 实现类内部写代码，而不是引用 service。

影响模块：

- `chat-domain`
- `ai-agent-workplace`

允许修改范围：

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/**/domain/service/**`
- 对应 `chat-domain/src/test/java/**` 测试与测试支撑
- 当前任务单

禁止修改范围：

- 不改变 HTTP/WS 协议、响应字段、错误码和业务行为。
- 不新增第三方依赖。
- 不修改 Maven 模块结构。
- 不移动 repository、port、api 包边界。
- 不回滚无关既有改动。

实施方案：

1. 将已有业务实现类的代码迁移到对应 `*DomainApi` 类。
2. 删除薄委托式 API 实现类。
3. 删除或停用不再承担职责的 `*DomainService` 业务实现类。
4. 对 message 中一个 API 组合多个内部服务的情况，将 `ChannelMessageDomainApi`、`ChannelMessageAttachmentDomainApi`、`ChannelPinDomainApi` 分别直接持有实际所需 repository/port/support 依赖或可复用协作对象，不再持有 `MessageDeliveryDomainService`、`MessageModerationDomainService`、`MessageQueryDomainService`。
5. 更新测试类引用，使单元测试直接验证新的 `*DomainApi` 实现类。
6. 运行编译、测试和结构扫描。

验收标准：

- 正式代码中的 `*DomainApi` 不再持有 `*DomainService` 字段做薄委托。
- 每个 `domain/api` 接口仍只有一个 `*DomainApi` 实现类。
- 正式代码不存在 `class ...DomainService implements ...Api`。
- `mvn -pl chat-domain -am -DskipTests compile` 通过。
- `mvn -pl chat-domain -am test -DskipTests=false` 通过。

实际结果：

- `AuthAccountDomainApi` 直接承载注册与邮箱验证码发送实现，删除薄委托的 `AuthDomainService`。
- `AuthSessionDomainApi` 直接承载登录、验证码会话、刷新和注销实现。
- `ChannelMessageDomainApi` 直接承载消息发送、HTTP 投递、system 消息、转发、编辑、撤回、删除、历史和搜索实现。
- `ChannelMessageAttachmentDomainApi` 直接承载消息附件上传实现。
- `ChannelPinDomainApi` 直接承载置顶、取消置顶和置顶列表实现。
- 删除旧的 `MessageDeliveryDomainService`、`MessageModerationDomainService`、`MessageQueryDomainService`。
- 其它 feature 的 `*DomainApi` 保持一接口一实现类，并统一测试与控制器变量命名，去除 `DomainService` 语义噪声。

验证记录：

- `mvn -pl chat-domain -am -DskipTests compile`：通过。
- `mvn -pl chat-domain -am test-compile`：通过。
- `mvn -pl chat-domain -am test -DskipTests=false`：通过，302 tests，0 failures，0 errors，0 skipped。
- 结构扫描：
  - `chat-domain` 主代码和 feature 测试中无 `DomainService` 残留。
  - `domain/service` 下共有 16 个 `*DomainApi` 实现类。
  - 未发现 `*DomainApi` 持有 `*DomainService` 字段的薄委托结构。

残留风险：

- 仍有共享 support 基类和协作对象，例如 `AbstractMessageDomainSupport`、`AuthTokenIssuer`、`MessageAfterCommitPublisher`。这些不是对外 API，也不是 `DomainService` 装饰对象，保留用于复用复杂领域流程中的公共支撑。
- 本次未改 HTTP/WS 协议行为；风险主要在实现迁移过程中遗漏语义，已通过现有 302 个测试覆盖回归。

知识沉淀 / 是否回写 docs：

- 本次落地的是已确认的实现约束，没有新增长期规则；未修改 `docs/`。

产物清理与保留说明：

- 当前任务单完成后归档为 `done`。

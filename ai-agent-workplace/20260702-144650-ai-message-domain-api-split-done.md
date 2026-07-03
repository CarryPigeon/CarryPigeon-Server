任务名称：

message 域 ChannelMessage API 按子域拆分

任务目标：

将过大的 `ChannelMessageApi` / `ChannelMessageDomainApi` 按消息子域拆分为发布、生命周期、时间线三组能力，降低单文件大小与职责集中度，同时保持 API 实现类直接承载业务实现，不退回 service 装饰模式。

任务背景：

当前 `ChannelMessageDomainApi` 约 543 行，聚合了消息发送、HTTP 投递、system 消息、转发、编辑、撤回、删除、历史和搜索能力。用户要求继续通过合理域拆分降低文件大小，并避免一个超级 API 暴露过多职责。

影响模块：

- `chat-domain`
- `ai-agent-workplace`

允许修改范围：

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/message/domain/api/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/message/domain/service/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/message/controller/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/**` 中引用消息 API 的 realtime 入口
- 对应 `chat-domain/src/test/java/**` 测试与测试支撑
- 当前任务单

禁止修改范围：

- 不改变 HTTP/WS 协议、响应字段、错误码和业务行为。
- 不新增第三方依赖。
- 不修改 Maven 模块结构。
- 不引入 `*DomainService` 装饰层。
- 不将 API 实现改成引用其它 API 实现类的装饰组合。
- 不回滚无关既有改动。

实施方案：

1. 新增 `ChannelMessagePublishingApi`、`ChannelMessageLifecycleApi`、`ChannelMessageTimelineApi`。
2. 将 `ChannelMessageDomainApi` 拆为：
   - `ChannelMessagePublishingDomainApi`
   - `ChannelMessageLifecycleDomainApi`
   - `ChannelMessageTimelineDomainApi`
3. 将原 `ChannelMessageApi` 与 `ChannelMessageDomainApi` 移除，避免超级 API 继续存在。
4. HTTP 控制器按实际使用注入对应子域 API。
5. realtime 入口只依赖 `ChannelMessagePublishingApi`。
6. 测试支撑与测试类按子域 API 更新。
7. 运行编译、测试与结构扫描。

验收标准：

- 不再存在 `ChannelMessageApi` / `ChannelMessageDomainApi` 超级 API。
- 发布、生命周期、时间线 API 分别一接口一实现类。
- API 实现类内部直接承载业务实现，不引用 `*DomainService` 或其它 API 实现类做装饰。
- `mvn -pl chat-domain -am test -DskipTests=false` 通过。
- 结构扫描无 `DomainService` 残留。

实际结果：

- 已删除原 `ChannelMessageApi` / `ChannelMessageDomainApi` 超级 API。
- 新增并落地三组子域 API：
  - `ChannelMessagePublishingApi` / `ChannelMessagePublishingDomainApi`
  - `ChannelMessageLifecycleApi` / `ChannelMessageLifecycleDomainApi`
  - `ChannelMessageTimelineApi` / `ChannelMessageTimelineDomainApi`
- HTTP 控制器按实际职责注入子域 API：
  - `ChannelMessageController` 注入发布 API、时间线 API、附件 API。
  - `MessageController` 注入发布 API、生命周期 API。
- realtime 入站链路只依赖 `ChannelMessagePublishingApi`。
- 测试支撑与领域测试按发布、生命周期、时间线三组拆分。
- `AbstractMessageDomainSupport` 注释从“领域服务共享支撑”调整为“领域 API 共享支撑”。

验证记录：

- `mvn -pl chat-domain -am test-compile` 通过。
- `mvn -pl chat-domain -am test -DskipTests=false` 通过，302 tests，0 failures，0 errors。
- 结构扫描：
  - `rg -n "ChannelMessageApi|ChannelMessageDomainApi|DomainService" chat-domain/src/main/java chat-domain/src/test/java` 无匹配。
  - message API 目录保留 7 个按域拆分的对外接口。
  - message API 实现目录保留 7 个一一对应 `*DomainApi` 实现类。

残留风险：

- `ChannelMessagePublishingDomainApi` 仍是 306 行，原因是发布域包含普通发送、HTTP 投递、system 消息、转发和 HTTP 附件消息构造；当前职责仍属于同一发布子域，未继续切成单功能接口。
- 三个子域实现复用 `AbstractMessageDomainSupport`，这是内部共享支撑，不是 API 装饰或 service facade。

知识沉淀 / 是否回写 docs：

- 本次未引入新的长期规则，仅落实已确认的 API 拆分约束，不回写 `docs/`。

产物清理与保留说明：

- 当前任务单将归档为 `done`，作为本次结构调整的可追踪记录。

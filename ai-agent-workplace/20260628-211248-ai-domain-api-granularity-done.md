任务名称：

domain api 粒度拆分与超级接口收敛

任务目标：

避免通过单个“超级 API”接口暴露一个 feature 的所有领域能力，将 `domain/api` 按业务子域和稳定用例簇进行适当切分。接口粒度以“一个业务域的一组相关能力”为准，不按单个功能动作拆成细碎接口。

任务背景：

上一轮已完成 `domain/api` 与 `domain/port` 的跨 feature 落地，但部分 API 仍以 feature 维度暴露过多能力：

- `AuthApi` 同时暴露注册、验证码、登录、刷新、注销。
- `FileApi` 同时暴露上传授权、上传执行、下载、固定头查询。
- `MessageDeliveryApi` 同时暴露消息发送与附件上传。
- `MessageModerationApi` 同时暴露消息编辑/撤回/删除/转发与置顶治理。
- `ServerApi` 同时暴露服务发现与 required plugin gate 检查。
- `UserProfileApi` 同时暴露资料查询、搜索、邮箱读取和资料/邮箱更新。

本任务按用户要求进行 API 语义切分，避免外部适配层被迫依赖超出自身职责的能力集合。

追加用户约束：

- 按域拆分，而不是按单个功能动作拆分。
- 一组强相关功能应放到同一个业务域 API 中，不创建一个功能一个接口的碎片化设计。

影响模块：

- `chat-domain`
- `ai-agent-workplace`

允许修改范围：

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/auth/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/file/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/message/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/server/**`
- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/features/user/**`
- 对应 `chat-domain/src/test/java/**` 测试与测试替身
- 当前任务单本身

禁止修改范围：

- 不改变 HTTP/WS 协议、响应字段、错误码和业务行为。
- 不拆分或重写现有 `*DomainService` 实现类，除非接口实现声明需要调整。
- 不新增第三方依赖。
- 不修改 Maven 模块结构。
- 不迁移 repository 抽象。
- 不修改正式 docs，除非发现长期规则需要补充并另行说明。

拟定切分方案：

1. auth API 按认证子域拆分：
   - `AuthAccountApi`: 账号注册与邮箱验证码相关能力。
   - `AuthSessionApi`: 登录、验证码会话、刷新、注销等会话生命周期能力。
   - 移除或停止使用原 `AuthApi` 聚合入口。
2. file API 按文件传输域收敛命名：
   - `FileTransferApi`: 上传授权、上传执行、下载、固定上传头和特殊文件识别。
   - 文件当前只有一个清晰业务域，不拆成上传/下载两个动作级接口。
   - 移除或停止使用原 `FileApi` 泛化入口。
3. message API 按消息子域拆分：
   - `ChannelMessageApi`: 频道消息发送、HTTP 入站消息、system 消息、编辑、撤回、删除、转发、历史和搜索能力。
   - `ChannelMessageAttachmentApi`: 消息附件上传能力。
   - `ChannelPinApi`: 频道消息置顶、取消置顶和置顶列表能力。
   - `MentionApi`: mention 查询和已读能力，保持。
   - `MessagePluginCatalogApi`: 消息插件目录能力，保持。
   - 移除或停止使用原 `MessageDeliveryApi`, `MessageModerationApi`, `MessageQueryApi` 的功能分层入口。
4. server API 按服务入口域收敛命名：
   - `ServerEntranceApi`: 服务发现文档与 required plugin gate 检查。
   - server 当前能力同属客户端进入服务前的入口域，不拆成单方法接口。
   - 移除或停止使用原 `ServerApi` 泛化入口。
5. user API 按用户资料域保留：
   - `UserProfileApi` 当前聚合的是用户资料域的一组查询、搜索、邮箱读取和更新能力，符合“按域拆分”约束，暂不拆成 query/mutation 动作接口。
6. channel API 当前已按访问、申请流、治理、生命周期、查询等业务用例簇拆分，本任务不继续拆。

实施步骤：

1. 新增细粒度 API 接口，调整对应 `*DomainService implements ...`。
2. 删除或停止使用旧聚合 API 接口，避免外部继续依赖超级入口。
3. 更新正式 controller、server support/realtime、mapper/config 的注入类型。
4. 更新测试和测试替身的 import、mock、fixture 构造。
5. 扫描是否仍存在旧聚合 API 引用。
6. 运行 `mvn -pl chat-domain -am -DskipTests compile`。
7. 运行 `mvn -pl chat-domain -am test -DskipTests=false`。

验收标准：

- 不再通过 `AuthApi`, `FileApi`, `MessageDeliveryApi`, `MessageModerationApi`, `MessageQueryApi`, `ServerApi` 这类粗粒度或功能分层不合理的接口作为正式外部依赖入口。
- controller/server support/mapper 依赖的 API 接口覆盖稳定业务域的一组相关能力，而不是单个动作碎片接口。
- `*DomainService` 可以实现多个细粒度 API，但不对外暴露为聚合超级接口。
- `chat-domain` 编译通过，全量测试通过。
- 任务单记录实际结果、验证命令、残留风险，并归档为 `done`。

执行前确认点：

本任务涉及 `domain/api` 结构调整和正式注入类型变更，属于架构敏感改动。需用户确认后再修改正式代码。

实际结果：

- 已移除旧粗粒度或功能分层 API 入口的正式依赖：
  - `AuthApi`
  - `FileApi`
  - `MessageDeliveryApi`
  - `MessageModerationApi`
  - `MessageQueryApi`
  - `ServerApi`
- 按业务域/稳定能力簇落地新的 `domain/api` 暴露面：
  - `AuthAccountApi`
  - `AuthSessionApi`
  - `FileTransferApi`
  - `ChannelMessageApi`
  - `ChannelMessageAttachmentApi`
  - `ChannelPinApi`
  - `ServerEntranceApi`
- 保留已有且语义仍合理的域 API：
  - channel 下的访问、申请流、治理、生命周期、查询 API。
  - `MentionApi`, `MessagePluginCatalogApi`, `NotificationPreferenceApi`, `UserProfileApi`。
- 新增 `ChannelMessageDomainApi` 作为 message 域对外 API 组合实现，对外实现 `ChannelMessageApi`, `ChannelMessageAttachmentApi`, `ChannelPinApi`，内部继续委托发送、治理、查询领域服务，避免把内部服务直接暴露给 controller/realtime 支撑层。
- 更新 controller、server realtime 支撑、mapper/config 和对应测试替身的依赖类型，使外部适配层依赖 `domain/api`，而不是依赖内部 `domain/service`。
- 修正 realtime 测试支撑：测试仍以 `ChannelMessageApi` 为依赖边界，只把发送能力委托给 `MessageDeliveryDomainService`，未覆盖的治理/查询方法显式标记为当前测试不支持。

验证记录：

- `mvn -pl chat-domain -am -DskipTests compile`
  - 结果：通过。
- `rg -n "\b(AuthApi|FileApi|MessageDeliveryApi|MessageModerationApi|MessageQueryApi|ServerApi)\b" chat-domain/src/main/java chat-domain/src/test/java`
  - 结果：无命中。
- `mvn -pl chat-domain -am test -DskipTests=false`
  - 结果：通过。
  - 最终统计：302 个测试，0 failures，0 errors，0 skipped。

残留风险：

- `ChannelMessageDomainApi` 是薄组合层，当前刻意不承载业务规则；后续如果 message 域继续扩张，应优先评估是否需要按更清晰的业务子域进一步拆分，而不是把所有新能力继续塞入 `ChannelMessageApi`。
- `UserProfileApi` 当前仍聚合资料查询、搜索、邮箱读取和更新；本次判断其仍属于用户资料域，不拆分。若后续用户账号/身份能力进入 user feature，应重新评估边界。
- 本任务未修改 HTTP/WS 协议、错误码和持久化抽象。

知识沉淀 / 是否回写 docs：

- 未回写正式 docs。
- 本次没有引入新的长期项目规则，只是落实已确认的 `domain/api` 粒度约束。

产物清理与保留说明：

- 当前任务单归档为 `done` 后保留在 `ai-agent-workplace/`，用于追踪本次架构敏感改动。

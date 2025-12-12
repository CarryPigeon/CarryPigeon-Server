# 模块说明：chat-domain

> `chat-domain` 模块承载了大部分聊天业务逻辑：  
> 控制器、LiteFlow 节点、上下文常量、通知与会话管理等。
>
> 本文配合 `chat-domain/AGENTS.md` 使用：  
> AGENTS 更偏“开发规范”，本文更偏“结构与能力说明”。

---

## 1. 目录结构概览

源码根路径：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain`

主要子包：

- `attribute`  
  - 定义 LiteFlow 上下文中使用的 key：
    - `CPNodeCommonKeys`：`SESSION`, `SessionId`, `response` 等；
    - `CPNodeChannelKeys` / `CPNodeChannelMemberKeys` / `CPNodeMessageKeys` 等；
    - `CPNodeChannelReadStateKeys`：读状态相关 key。
  - 统一管理 key 命名，避免魔法字符串。

- `cmp`（components）  
  LiteFlow 可调用组件，按功能划分：
  - `basic`：基础组件  
    - 如 `RenameArgNode`（重命名上下文 key）、`CleanerArgNode` 等；
  - `checker`：校验组件  
    - 用户登录校验（`UserLoginCheckerNode`）、频道权限检查、禁言检查等；
  - `biz`：业务组件  
    - 按领域再拆分：
      - `user`, `user.token`
      - `channel`, `channel.member`, `channel.application`, `channel.ban`
      - `message`（消息构建、列表、未读统计等）
      - `file` 等；
  - `notifier`：通知组件  
    - 核心节点 `CPNotifierNode`：根据 `Notifier_Uids` + `Notifier_Data` 发送通知；
    - `notifier/channel`、`notifier/user`、`notifier/message` 下是不同场景的收集器 / 构建器；
  - `service`：封装对 `external-service` 的调用等；
  - `info`：通用结构封装（如分页信息 `PageInfo`），不直接参与 LiteFlow。

- `controller`  
  - `controller/netty`：基于 Netty 的长连接业务控制器：
    - 使用 `@CPControllerTag(path = "...", voClazz = ..., resultClazz = ...)` 标注；
    - Path 对应 LiteFlow 链名。
  - `controller/web`：预留给 Web 场景的控制器（当前主要以 Netty 为主）。

- `service`  
  - 业务服务组件：
    - `notification`：`CPNotificationService` 封装通知发送逻辑；
    - `session`：`CPSessionCenterService` 管理用户与 `CPSession` 的映射。

- `config`  
  - Spring 配置类（如测试环境配置），通常不需要频繁修改。

---

## 2. 控制器与 LiteFlow 链路

### 2.1 Controller 规范

每条业务路由由一个 Netty 控制器类描述：

```java
@CPControllerTag(
    path = "/core/channel/message/create",
    voClazz = CPMessageCreateVO.class,
    resultClazz = CPMessageCreateResult.class
)
public class CPMessageCreateController {
}
```

- `path`：业务路由，也是 LiteFlow 链路名称；
- `voClazz`：请求 VO，负责将 `CPPacket.data` 写入 LiteFlow 上下文；
- `resultClazz`：结果处理类，从上下文读取数据，构造 `CPResponse.data`。

控制器本身不做业务逻辑，只提供路由与参数/结果的类型映射。

### 2.2 VO 和 Result 规范（简述）

- VO：实现 `CPControllerVO`，必须实现：

```java
boolean insertData(DefaultContext context);
```

将 JSON 请求体转换为 LiteFlow 上下文中的 key-value，例如：

```java
context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
context.setData(CPNodeMessageKeys.MESSAGE_LIST_START_TIME, startTime);
```

- Result：实现 `CPControllerResult`，必须实现：

```java
void process(CPSession session, DefaultContext context, ObjectMapper objectMapper);
```

从上下文读取业务结果，写入 `CPNodeCommonKeys.RESPONSE`：

```java
context.setData(CPNodeCommonKeys.RESPONSE,
    CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result)));
```

详细规范可参考：`chat-domain/AGENTS.md`。

### 2.3 LiteFlow 配置

配置位置：

- 测试：`chat-domain/src/test/resources/config/*.xml`
- 正式运行：`application-starter/src/main/resources/config/*.xml`

规则：

- 每个 `path` 对应一条 `chain`；
- `chain name` 必须与 `@CPControllerTag.path` 一致；
- 链内使用 `THEN(...)` 组合多个节点。

以频道消息读状态更新为例：

```xml
<chain name="/core/channel/message/read/state/update">
    THEN(
    UserLoginChecker,
    RenameArg.bind("key","ChannelMemberInfo_Uid:SessionId;ChannelMemberInfo_Cid:ChannelReadStateInfo_Cid"),
    CPChannelMemberSelector.bind("key","CidWithUid"),
    CPChannelReadStateUpserter,
    CPUserSelfCollector,
    CPChannelReadStateNotifyBuilder,
    CPNotifier.bind("route","/core/channel/message/read/state")
    )
</chain>
```

---

## 3. 组件（节点）规范与常见类型

### 3.1 基类：`CPNodeComponent`

包：`chat-domain/src/main/java/team/carrypigeon/backend/api/chat/domain/node/CPNodeComponent.java`

功能：

- 封装 LiteFlow 节点公共逻辑：
  - 从上下文获取 `CPSession`；
  - `requireContext` / `requireBind`：必填参数校验；
  - `businessError` / `argsError`：统一错误处理。
- 所有普通节点（非 Switch）建议继承该类。

### 3.2 常见业务节点类型

按照 `AGENTS.md` 约定，常见 node 类型包括：

1. **SelectorNode**  
   - 负责从数据库中查询实体并写入上下文；
   - 例如：
     - `CPChannelMemberSelectorNode`
     - `CPChannelReadStateSelectorNode`
2. **BuilderNode / CreatorNode**  
   - 根据上下文原始数据构建领域对象；
3. **SaverNode / UpdaterNode / DeleterNode**  
   - 调用 DAO 保存、更新或删除实体；
4. **CheckerNode**  
   - 检查用户是否登录、是否有权限、参数是否合法；
5. **Notifier 相关节点**  
   - 收集需要通知的 uid 列表（Collector）；
   - 构建通知 payload（Builder）；
   - 使用统一的 `CPNotifierNode` 发送通知。

### 3.3 上下文 key 约定

所有节点从 `attribute` 包中引用上下文 key，如：

- `CPNodeCommonKeys.SESSION` / `SESSION_ID` / `RESPONSE`
- `CPNodeChannelKeys.CHANNEL_INFO` / `CHANNEL_INFO_ID`
- `CPNodeMessageKeys.MESSAGE_INFO` / `MESSAGE_LIST` / `MESSAGE_UNREAD_COUNT`
- `CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO` / `..._LAST_READ_TIME`

约定：

- key 命名采用 `EntityName_FieldName` 的形式，例如：
  - `UserInfo_Id`, `ChannelInfo_Id`, `MessageInfo_Id`；
- 所有 key 应集中定义在 `attribute` 包中，禁止在节点中直接写字符串常量。

---

## 4. 通知与会话管理

### 4.1 通知服务：`CPNotificationService`

路径：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/service/notification/CPNotificationService.java`

功能：

- 封装服务端向客户端推送通知的逻辑；
- 根据 uid 列表，从 `CPSessionCenterService` 获取所有 `CPSession`；
- 构造 `CPResponse(id=-1, code=0, data=notificationJson)` 并写入会话。

输入：

- `uids`: `Collection<Long>`
- `notification`: `CPNotification { route, data }`

典型用法：

- 新消息通知；
- 消息删除通知；
- 读状态更新通知（`/core/channel/message/read/state`）。

### 4.2 会话中心：`CPSessionCenterService`

路径：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/service/session/CPSessionCenterService.java`

功能：

- 管理 uid → `List<CPSession>` 映射；
- 提供：
  - `addSession(uid, session)`
  - `removeSession(uid, session)`
  - `getSessions(uid)`
- 定期清理空列表条目（`@Scheduled`）。

与登录流程的关系：

- 登录成功后，通过 `CPSessionRegisterNode` 注册会话；
- `UserLoginCheckerNode` 从 `CPSession` 属性中读取当前用户 id，并写入 LiteFlow 上下文。

---

## 5. 与其他模块的交互

### 5.1 与 DAO 模块

- 所有数据库访问均通过 `api` 模块定义的 DAO 接口；
- `chat-domain` 中的节点只依赖接口，不依赖具体实现；
- `dao` 模块提供默认实现，侵入性插件可以替换这些实现而不影响节点代码。

### 5.2 与 connection 模块

- `connection` 模块负责：
  - 建立加密的 TCP 连接；
  - 将解密后的 JSON 文本交给 `CPControllerDispatcherImpl`；
- `chat-domain` 则从 `CPPacket` 开始接管整个业务处理流程。

### 5.3 与 external-service 模块

- `chat-domain` 通过内部 `service` 包中的组件调用 `external-service` 实现；
- 例如邮件相关服务通过统一的 Service 接口调用，便于替换实现。

---

## 6. 扩展与开发建议

1. **新增路由**
   - 在 `chat-domain.controller.netty` 下新增 Controller（`@CPControllerTag`）；
   - 编写 VO / Result；
   - 在 `config/*.xml` 中为该路由定义 LiteFlow 链；
   - 使用既有 `cmp` 节点组合，必要时新增节点。

2. **扩展现有链路**
   - 新增 LiteFlow 节点（继承 `CPNodeComponent`），放入合适的 `cmp` 子包；
   - 在 `config/*.xml` 中插入节点到对应链路中；
   - 确保节点使用的上下文 key 都已在 `attribute` 包中定义。

3. **注意事项**
   - 日志统一使用英文，并包含关键字段（uid/cid/route/id 等）；
   - 参数必填检查应在节点一开始完成，避免空指针和隐式失败；
   - 对于会影响多个用户的操作（如消息发送、读状态变更），应通过通知链路保证多端一致性。

结合本文件与 `AGENTS.md`，你可以较为完整地理解并扩展 `chat-domain` 模块中的业务逻辑。 


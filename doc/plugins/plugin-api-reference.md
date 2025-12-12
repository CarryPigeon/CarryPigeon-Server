# 插件 API 参考（服务端）

> 本文列出服务端对插件开放的主要 API 与扩展点，帮助你在开发插件时快速找到“能用什么”。
>
> 重点只包含 **稳定的、推荐依赖** 的接口；内部实现类不在此列表中。

---

## 1. 领域模型（Domain Models）

来自 `api` 模块，所有插件都可以安全依赖：

- 用户与认证
  - `team.carrypigeon.backend.api.bo.domain.user.CPUser`
  - `team.carrypigeon.backend.api.bo.domain.user.token.CPUserToken`
- 频道与成员
  - `team.carrypigeon.backend.api.bo.domain.channel.CPChannel`
  - `team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember`
  - `team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan`
  - `team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication`
- 消息与文件
  - `team.carrypigeon.backend.api.bo.domain.message.CPMessage`
  - `team.carrypigeon.backend.api.bo.domain.file.CPFileInfo`
- 读状态
  - `team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState`
- 连接 & 通知
  - `team.carrypigeon.backend.api.bo.connection.CPSession`
  - `team.carrypigeon.backend.api.connection.notification.CPNotification`
  - `team.carrypigeon.backend.api.connection.notification.CPMessageNotificationData`
  - `team.carrypigeon.backend.api.connection.notification.CPChannelReadStateNotificationData`
- 协议
  - `team.carrypigeon.backend.api.connection.protocol.CPPacket`
  - `team.carrypigeon.backend.api.connection.protocol.CPResponse`

这些类型代表插件能看到的“世界”，建议所有插件都从这里取数 / 传参。

---

## 2. DAO 接口（数据访问）

DAO 接口定义在 `api` 模块，实现由宿主或侵入性插件提供。  
插件可以：

- 拓展性插件：**只读使用**这些 DAO（慎重写入）；  
- 侵入性插件：**替换默认实现**（实现同名接口）。

### 2.1 用户与认证

- `team.carrypigeon.backend.api.dao.database.user.UserDao`
  - `CPUser getById(long id)`
  - `CPUser getByEmail(String email)`
  - `boolean save(CPUser user)`
- `team.carrypigeon.backend.api.dao.database.user.token.UserTokenDao`
  - `CPUserToken getById(long id)`
  - `CPUserToken[] getByUserId(long uid)`
  - `CPUserToken getByToken(String token)`
  - `boolean save(CPUserToken token)`
  - `boolean delete(CPUserToken token)`

### 2.2 频道与成员

- `team.carrypigeon.backend.api.dao.database.channel.ChannelDao`
  - `CPChannel getById(long id)`
  - `CPChannel[] getAllFixed()`
  - `boolean save(CPChannel channel)`
  - `boolean delete(CPChannel channel)`

- `team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao`
  - `CPChannelMember getById(long id)`
  - `CPChannelMember[] getAllMember(long cid)`
  - `CPChannelMember getMember(long uid, long cid)`
  - `CPChannelMember[] getAllMemberByUserId(long uid)`
  - `boolean save(CPChannelMember member)`
  - `boolean delete(CPChannelMember member)`

- `team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO`
  - `CPChannelBan getById(long id)`
  - `CPChannelBan[] getByChannelId(long cid)`
  - `CPChannelBan getByChannelIdAndUserId(long uid, long cid)`
  - `boolean save(CPChannelBan ban)`
  - `boolean delete(CPChannelBan ban)`

- `team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO`
  - `CPChannelApplication getById(long id)`
  - `CPChannelApplication getByUidAndCid(long uid, long cid)`
  - `CPChannelApplication[] getByCid(long cid, int page, int pageSize)`
  - `boolean save(CPChannelApplication application)`

### 2.3 消息与读状态

- `team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao`
  - `CPMessage getById(long id)`
  - `CPMessage[] getBefore(long cid, LocalDateTime time, int count)`
  - `int getAfterCount(long cid, long uid, LocalDateTime time)`
  - `boolean save(CPMessage message)`
  - `boolean delete(CPMessage message)`

- `team.carrypigeon.backend.api.dao.database.channel.read.ChannelReadStateDao`
  - `CPChannelReadState getById(long id)`
  - `CPChannelReadState getByUidAndCid(long uid, long cid)`
  - `boolean save(CPChannelReadState state)`
  - `boolean delete(CPChannelReadState state)`

> 建议：拓展性插件对这些 DAO 以“读为主、慎写”为原则；  
> 侵入性插件若要替换实现，请参考 `plugin-dev-guide.md` 中的 DAO 替换示例。

---

## 3. LiteFlow 节点基类与上下文 key

拓展性插件最常用的一块是业务节点扩展，主要依赖 `chat-domain` 中暴露的基类和 key。

### 3.1 节点基类

包：`team.carrypigeon.backend.api.chat.domain.node`

- `CPNodeComponent`
  - LiteFlow 普通节点基类，封装了：
    - 从上下文获取 `CPSession` 的逻辑；
    - `requireContext` / `requireBind` 的必填参数校验；
    - `businessError` / `argsError` 的统一错误处理（写入 `CPResponse` + 抛 `CPReturnException`）。
  - 拓展性节点通常继承此类。

- `CPNodeSwitchComponent`
  - Switch 节点基类，用于根据条件分支执行不同链路。

- `AbstractSelectorNode<T>`
  - 通用“查询实体”节点基类：
    - 通过 `bind("key", ...)` 读取查询模式；
    - 调用 `doSelect` 执行查询；
    - 将结果写入 `getResultKey()` 对应的上下文 key。

- `AbstractSaveNode<T>` / `AbstractDeleteNode<T>`
  - 保存 / 删除类节点的基类（若插件需要提供类似节点，可参考实现模式）。

### 3.2 上下文 key（部分）

包：`team.carrypigeon.backend.chat.domain.attribute`

常用 key（示例）：

- 通用：
  - `CPNodeCommonKeys.SESSION` → 当前 `CPSession`
  - `CPNodeCommonKeys.SESSION_ID` → Long，当前登录用户 id
  - `CPNodeCommonKeys.RESPONSE` → `CPResponse`，最终响应
- 消息：
  - `CPNodeMessageKeys.MESSAGE_INFO` / `MESSAGE_INFO_ID` / `MESSAGE_INFO_DATA` 等
  - `CPNodeMessageKeys.MESSAGE_UNREAD_START_TIME` / `MESSAGE_UNREAD_COUNT`
- 频道：
  - `CPNodeChannelKeys.CHANNEL_INFO` / `CHANNEL_INFO_ID` 等
  - `CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO` / `CHANNEL_MEMBER_INFO_UID` / `CHANNEL_MEMBER_INFO_CID` 等
- 读状态：
  - `CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO`
  - `CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID`
  - `CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME`
- 通知：
  - `CPNodeNotifierKeys.NOTIFIER_UIDS` → `Set<Long>`，需要通知的用户 id 集合
  - `CPNodeNotifierKeys.NOTIFIER_DATA` → `JsonNode`，通知 payload

> 插件编写节点时，应尽量复用这些 key，而不是发明新的字符串，保证可读性和可维护性。

---

## 4. 通知与会话管理 API

### 4.1 通知服务

包：`team.carrypigeon.backend.chat.domain.service.notification`

- `CPNotificationService`
  - `boolean sendNotification(Collection<Long> uids, CPNotification notification)`
  - 功能：
    - 根据 uid 列表，从会话中心拿到所有 `CPSession`；
    - 构造 `CPResponse(id=-1, code=0, data=notificationJson)`；
    - 向每个会话写出 JSON 字符串。
  - 用途：
    - 拓展性插件可以通过它给特定用户推送通知（例如读状态变更、审计结果）。

### 4.2 会话中心

包：`team.carrypigeon.backend.chat.domain.service.session`

- `CPSessionCenterService`
  - `void addSession(long uid, CPSession session)`
  - `void removeSession(long uid, CPSession session)`
  - `List<CPSession> getSessions(long uid)`
  - 说明：
    - 管理 uid → 多个 `CPSession` 的映射；
    - 插件通常不需要直接操作它，而是通过 `CPNotificationService` 间接使用；
    - 如确有需求（例如自定义广播逻辑），可以只读使用 `getSessions`。

---

## 5. LiteFlow 与 Controller 辅助 API

### 5.1 Controller 相关

包：`team.carrypigeon.backend.api.chat.domain.controller`

- `@CPControllerTag`
  - 标注 Netty 控制器，声明：
    - `path`：业务路由，例如 `/core/channel/message/read/state/update`
    - `voClazz`：请求 VO
    - `resultClazz`：结果处理类
  - 插件可以新增自己的 Controller（扩展新路由），只要：
    - 使用 `@CPControllerTag`；
    - 在 LiteFlow 配置中为该 `path` 提供一条链路。

- `CPControllerVO`
  - VO 接口，要求实现：
    - `boolean insertData(DefaultContext context)`
  - 插件可增加自己的 VO 类型，用于新路由。

- `CPControllerResult`
  - 结果处理接口，要求实现：
    - `void process(CPSession session, DefaultContext context, ObjectMapper objectMapper)`
  - 插件可以自定义结果封装逻辑。

### 5.2 LiteFlow 执行入口（宿主）

包：`team.carrypigeon.backend.chat.domain.controller.netty`

- `CPControllerDispatcherImpl`
  - 宿主 Controller 分发器，会：
    - 解析 `CPPacket`；
    - 找到对应 `@CPControllerTag` 控制器；
    - 调用 LiteFlow，以 `path` 为链名执行。
  - 插件一般不直接使用它，但需要理解：
    - 自己新增的 Controller + LiteFlow 链会自动被这个 Dispatcher 调用。

---

## 6. 时间与工具类（常用）

包：`team.carrypigeon.backend.common.time`

- `TimeUtil`
  - `long LocalDateTimeToMillis(LocalDateTime time)`
  - `LocalDateTime MillisToLocalDateTime(long millis)`
  - `long getCurrentTime()`

插件处理时间戳（尤其是读状态、未读消息等）时，建议统一通过该工具类，避免时区/精度不一致。

---

## 7. 兼容性与最佳实践

1. 只依赖公共 API（`api` 模块）和明确暴露的扩展点；避免引用 `impl` 包下的类。
2. 写节点时优先继承 `CPNodeComponent`，使用 `requireContext` / `argsError` 等工具方法。
3. 通知和会话尽量通过 `CPNotificationService` 间接访问，避免直接操作底层连接。
4. DAO 使用以“读为主、慎重写入”为原则；侵入性替换请参考 `plugin-dev-guide.md`。
5. 日志使用英文 + 关键字段（uid/cid/route/time），方便线上排查。

如果你需要更完整的「能做什么」说明，请配合阅读：

- `plugin-architecture.md`：从架构视角看扩展点；
- `plugin-dev-guide.md`：从实战视角一步步写插件；
- `plugin-security-and-sandbox.md`：上线前检查安全边界和风险。 


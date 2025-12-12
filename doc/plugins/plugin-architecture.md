# 插件架构说明（服务端）

> 本文从「架构视角」说明 CarryPigeon 的服务端插件体系：  
> 插件在项目中的位置、可扩展点、以及两类插件（侵入性 / 拓展性）的接入方式。

---

## 1. 总体视图

从插件开发者的角度，可以把服务端拆成三层：

1. **API 层（稳定边界）**
   - 模块：`api`
   - 职责：
     - 定义领域模型：`CPUser`, `CPChannel`, `CPChannelMember`, `CPMessage`, `CPChannelReadState` 等
     - 定义接口：DAO 抽象（`ChannelMessageDao`, `ChannelReadStateDao` 等）、通知模型（`CPNotification`）、LiteFlow 节点基类、Controller 协议等
   - 插件必须只依赖这一层，避免直接依赖宿主实现细节。

2. **宿主实现层**
   - 模块：`chat-domain`, `dao`, `connection`, `external-service`, `common`
   - 职责：
     - 提供 API 层接口的默认实现
     - 组合 Netty / LiteFlow / Spring / MyBatis-Plus 等框架
     - 提供默认的业务链路、缓存策略、外部服务接入方式

3. **应用启动层**
   - 模块：`application-starter`
   - 职责：
     - 聚合所有模块，加载配置（`application.yaml` / `config/*.xml`）
     - 启动 Spring Boot 与 Netty 服务
     - 决定最终加载哪些实现模块 / 插件 jar

**插件层** 就是附着在这个结构上的 **额外模块 / jar**，通过以下方式接入：

- Maven 依赖 / 打包脚本决定 **哪些实现 jar 被打包进应用**（侵入性插件）
- Spring + LiteFlow 扫描决定 **哪些 Bean / 节点参与运行时**（拓展性插件）

---

## 2. 插件类型与接入方式

插件分为两大类，接入方式不同。

### 2.1 侵入性插件：替换实现

**目标**：在不改上层业务代码的前提下，替换底层实现模块，例如：

- 把 `dao` 模块替换为支持分库分表 / 多数据源的 DAO 实现
- 把 `external-service` 模块替换为接入不同厂商 API 的实现

**接入方式：**

1. 接口放在 `api` 中，例如：
   - `api/src/main/java/team/carrypigeon/backend/api/dao/database/message/ChannelMessageDao.java`
   - `api/src/main/java/team/carrypigeon/backend/api/dao/database/channel/read/ChannelReadStateDao.java`
2. 宿主默认实现放在 `dao`：
   - `dao/.../impl/message/MessageDaoImpl.java`
   - `dao/.../impl/channel/read/ChannelReadStateDaoImpl.java`
3. 侵入性插件作为 **新的实现模块**（可以是新的 Maven 模块 / 外部 jar）：
   - 实现相同接口，包名和类名可以不同，但需要被 Spring 扫描到
   - 使用 `@Service` / `@Repository` 标注，成为 Spring Bean
4. 通过 **依赖顺序 / 配置** 决定哪个 Bean 被注入：
   - 使用 Spring 的 `@Primary` 标记插件 Bean，使其优先于默认实现
   - 或者通过 Spring Profile / 条件装配 (`@ConditionalOnProperty` 等) 控制启用

**典型例子：分布式 DAO 插件**

- 在插件模块中实现：

```java
@Service
@Primary
public class DistChannelReadStateDaoImpl implements ChannelReadStateDao {
    // 使用分布式存储 / 分库分表实现实际逻辑
}
```

- 在 `application-starter` 的配置中，通过 Profile 或配置项控制：
  - `cp.dao.mode=distributed` → 启用插件实现  
  - `cp.dao.mode=default` → 使用原始 `dao` 实现

**注意事项：**

- 侵入性插件对系统影响大，通常需要完整的压测和灰度发布
- 尽量保持和原实现相同的：
  - 行为（返回值语义、一致性保证）
  - 异常约定（例如参数错误 / 未找到 / 数据库异常）
  - 日志格式（方便统一排查）

### 2.2 拓展性插件：LiteFlow + Spring 扩展

**目标**：在不替换现有模块的前提下，对现有业务链路进行增强和扩展，例如：

- 在消息发送链 `/core/channel/message/create` 中插入审计节点  
- 在读状态变更链 `/core/channel/message/read/state/update` 中追加统计 / 监控节点  
- 新增独立业务链路处理新路由，而不影响原有链路

**关键机制：**

1. **LiteFlow 节点扩展**
   - 自定义节点继承 `CPNodeComponent` 或相关基类：

```java
@LiteflowComponent("MyAuditNode")
public class MyAuditNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        // 读取上下文数据进行审计或统计
    }
}
```

   - LiteFlow 会自动扫描所有 `@LiteflowComponent`，只要插件 jar 在 classpath 中，就能作为节点使用。

2. **Flow 配置扩展**
   - 在 `application-starter/src/main/resources/config/*.xml` 中修改或新增链路：

```xml
<chain name="/core/channel/message/create">
    THEN(
    UserLoginChecker,
    MyAuditNode,
    CPMessageParse,
    CPMessageBuilder,
    CPMessageSaver,
    ...
    )
</chain>
```

   - 拓展性插件可以：
     - 提供新节点 + 建议的链路片段
     - 由运维 / 配置管理来决定是否将其插入实际生产链路

3. **Service Bean 扩展**
   - 插件可以注册自己的 Spring Bean（带有独立配置空间），例如：
     - 统计上报客户端
     - 外部审计服务客户端
   - 业务节点通过注入这些 Bean 实现扩展逻辑：

```java
@AllArgsConstructor
@LiteflowComponent("MessageReadAudit")
public class MessageReadAuditNode extends CPNodeComponent {

    private final MyAuditService myAuditService;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        myAuditService.recordReadStateChange(...);
    }
}
```

**注意事项：**

- 拓展性插件不替换现有 `dao` / `connection`，只是“挂在旁路”的扩展  
- 如遇性能敏感链路，需要考虑：
  - 节点是否可以异步执行  
  - 是否需要限流 / 降级  
  - 出错时是否允许失败（例如只打日志，不影响主业务）

---

## 3. 扩展点一览

本项目主要提供以下可扩展点给插件使用：

1. **业务流程扩展点（LiteFlow）**
   - 所有 `@CPControllerTag` 标注的路由，其 `path` 对应的 LiteFlow 链都可扩展  
   - 例如：`/core/channel/message/create`, `/core/channel/message/read/state/update` 等
   - 扩展方式：
     - 新增节点并插入现有链路
     - 为新路由定义全新链路

2. **通知扩展点**
   - 通过 `CPNotificationService` + `CPSessionCenterService` 进行推送  
   - 插件可以：
     - 基于现有通知模型新增消息类型  
     - 或在拓展性节点中调用自己的外部通知服务（邮件、钉钉、监控系统）

3. **DAO 接口扩展点**
   - 所有 DAO 接口都定义在 `api` 模块，可以通过：
     - 侵入性插件：替换实现（例如 distributed DAO）  
     - 拓展性插件：新增只读 DAO 或缓存层，供业务扩展节点调用

4. **服务类扩展点**
   - 插件可以注册自己的 `@Service`，被节点 / 控制器注入使用：
     - 比如：`MyReadStateStatisticsService`，专门记录读状态变更

---

## 4. 插件与宿主的版本与部署

### 4.1 版本兼容

建议遵循以下原则：

- API 模块版本（`CarryPigeon:api`）是插件兼容性的 **主参考**  
  - 插件应声明兼容的 API 版本范围，例如：`[1.0.0, 1.1.0)`
- 宿主升级时：
  - 若 API 接口无破坏性变更，插件通常可直接沿用  
  - 若 API 接口有变更，应先更新插件再升级宿主

### 4.2 部署与启用

侵入性插件：

- 通过 Maven / 构建脚本控制最终打入 `application-starter` 的实现 jar  
- 或通过 Spring 配置（Profile/条件装配）控制启用哪个实现 Bean

拓展性插件：

- 将插件 jar 放入应用 classpath  
- 确保：
  - 插件中的 LiteFlow 节点使用 `@LiteflowComponent` 标注  
  - Flow 配置文件被 `liteflow.rule-source` 所包含
- 通过配置开关控制是否在实际链路中插入这些节点

---

## 5. 下一步阅读

如果你想继续深入插件开发：

- 看 `plugin-lifecycle.md`：了解插件从加载到卸载的完整生命周期  
- 看 `plugin-api-reference.md`：掌握可使用的服务端 API  
- 看 `plugin-dev-guide.md`：跟着一步一步写出第一个真实插件（可以从简单的审计 / 统计插件开始）  
- 看 `plugin-security-and-sandbox.md`：确保插件不会破坏宿主的安全性和稳定性。


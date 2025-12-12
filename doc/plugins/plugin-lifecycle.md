# 插件生命周期说明（服务端）

> 本文从「时间轴」的角度说明：  
> 一个插件从被引入 classpath 到参与业务链路，再到下线的典型生命周期。

---

## 1. 启动流程中的插件加载

以 `application-starter` 为例，服务端的启动流程大致如下：

1. **类加载阶段**
   - JVM 将所有在 classpath 中的 jar 加载进来，包括：
     - 宿主模块：`chat-domain`, `dao`, `connection`, `external-service` 等；
     - 插件 jar：你编写的拓展性 / 侵入性插件。

2. **Spring Boot 初始化阶段**
   - 扫描 `@Configuration`, `@Component`, `@Service`, `@Repository` 等注解；
   - 为所有可见模块（包括插件）创建 Spring Bean；
   - 若插件中存在：
     - `@LiteflowComponent`：会在 LiteFlow 组件扫描阶段被注册；
     - `@Primary` / 条件装配注解：会影响 Bean 注入优先级；
     - `@EnableCaching` / 自定义配置类：会参与全局配置。

3. **LiteFlow 初始化阶段**
   - 根据 `application.yaml` 中的配置：

```yaml
liteflow:
  rule-source: config/*.xml
```

   - 从 `application-starter/src/main/resources/config/*.xml` 读取所有链路定义；
   - 注册所有 `@LiteflowComponent` 声明的节点，包括插件提供的节点；
   - 构建 `chainName -> 节点序列` 的路由表。

4. **Netty / 业务组件启动**
   - `connection` 模块启动 Netty 服务，监听 TCP 端口；
   - `chat-domain` 提供的控制器分发器（`CPControllerDispatcherImpl`）准备就绪；
   - 此时插件提供的 Controller / 节点 / Service Bean 已经可以被正常使用。

**结论：**

- 只要插件 jar 在应用的 classpath 中，并符合 Spring + LiteFlow 的扫描规则，就会在启动时自动加载；
- 侵入性插件通过 Bean 覆盖或条件装配介入，拓展性插件通过节点 + 链路定义介入。

---

## 2. 侵入性插件的生命周期

侵入性插件主要通过 **替换实现** 的方式接入，例如 DAO 替换。  
典型生命周期如下：

1. **准备阶段**
   - 在插件模块中实现与 `api` 中接口一致的实现类（例如 `ChannelReadStateDao` 的分布式实现）；
   - 使用 `@Service` + `@Primary` 或条件装配注解；
   - 编写必要的配置项，例如：

```yaml
cp:
  read-state:
    dao:
      mode: distributed
```

2. **构建与部署**
   - 将插件模块打包为 jar；
   - 通过 Maven 依赖或部署脚本将插件 jar 引入到 `application-starter` 的运行环境；
   - 在目标环境的配置中开启对应模式（例如设置 `mode=distributed`）。

3. **运行期**
   - Spring 根据条件装配决定使用插件实现还是默认实现；
   - 上层业务（LiteFlow 节点、Controller）只感知到 `ChannelReadStateDao` 接口，不需要修改代码；
   - 侵入性插件承担具体的数据访问逻辑。

4. **升级与回滚**
   - 升级插件：
     - 构建新版本插件 jar；
     - 替换部署中的 jar；
     - 重启或滚动重启服务。
   - 回滚插件：
     - 恢复旧版本插件 jar，或在配置中切回默认模式；
     - 重启或滚动重启服务。

**注意：**

- 侵入性插件的生命周期与宿主服务几乎绑定在一起：**启服务即加载，停服务即卸载**；
- 上线前务必有测试环境验证，遵循灰度与回滚流程。

---

## 3. 拓展性插件的生命周期

拓展性插件主要通过 LiteFlow 节点 + 配置链路参与业务，不替换原有模块。  
典型生命周期如下：

1. **开发阶段**
   - 在插件模块中编写：
     - `@LiteflowComponent` 节点；
     - 可选的 `@Service` 辅助类；
   - 确保节点继承 `CPNodeComponent` 或其他合适基类，遵守上下文 key 约定。

2. **接入阶段**
   - 将插件 jar 加入到 classpath；
   - 在 `config/*.xml` 中：
     - 为已有链路插入新节点，或
     - 定义新的链路，对应新路由；
   - 无需修改 Java 代码，只改 XML 即可调整插件参与程度。

3. **运行期**
   - LiteFlow 按链路顺序依次调用节点，包括插件节点；
   - 若插件节点失败：
     - 可以选择将错误视为“软失败”（仅打日志、不抛异常）；
     - 或在确有需要时调用 `businessError` / `argsError` 中止链路。

4. **禁用或下线插件**
   - 最简单的方式是 **从链路中移除节点**：

```xml
<!-- 从 THEN(...) 中删除插件节点 -->
<chain name="/core/channel/message/read/state/update">
    THEN(
    UserLoginChecker,
    CPChannelReadStateUpserter,
    CPUserSelfCollector,
    CPChannelReadStateNotifyBuilder,
    CPNotifier.bind("route","/core/channel/message/read/state")
    )
</chain>
```

   - 更彻底的方式：
     - 从 classpath 中移除插件 jar；
     - 或通过条件装配（`@ConditionalOnProperty`）控制插件节点 Bean 不再创建。

**特点：**

- 拓展性插件可以通过配置来实现「热关」效果（修改 XML + 重载 LiteFlow 配置），对业务影响更可控；
- 节点设计得当时，即使插件出问题，也可以降级为“只打日志，不中断主链路”。

---

## 4. 配置与开关管理

为了在不同环境和阶段灵活控制插件启用状态，推荐：

1. **使用配置控制侵入性实现**
   - 通过 `@ConditionalOnProperty` 等注解，使插件实现 Bean 只有在特定配置下才创建；
   - 在配置中心或环境变量中切换模式，而不是改代码。

2. **使用 XML 链路控制拓展节点**
   - 优先通过修改 `config/*.xml` 决定是否在链路中加入插件节点；
   - 对于无需参与生产的实验性节点，可以只在测试环境链路中出现。

3. **约定统一的开关命名**

例如：

```yaml
cp:
  plugins:
    message-read-audit:
      enabled: true
  read-state:
    dao:
      mode: default   # or distributed
```

配合：

```java
@ConditionalOnProperty(prefix = "cp.plugins.message-read-audit", name = "enabled", havingValue = "true")
@LiteflowComponent("MessageReadAudit")
public class MessageReadAuditNode extends CPNodeComponent { ... }
```

这样能够：

- 让插件的启用状态在配置中一目了然；
- 支持按环境、按实例灵活切换。

---

## 5. 生命周期与监控

建议为插件增加基础监控：

1. **启动日志**
   - 插件初始化时打印清晰日志，例如：
     - 插件名、版本；
     - 运行模式（default / distributed 等）。

2. **运行指标**
   - 根据需要打埋点或接入 APM，尤其是：
     - 插件节点的平均耗时、错误率；
     - 替换 DAO 的调用耗时 / 错误率。

3. **下线与回滚审计**
   - 当修改配置或 XML 下线插件时，记录一次变更日志：
     - 谁改的、改了什么、时间点；
   - 方便出了问题时快速定位到最近的插件变更。

---

## 6. 小结

- 侵入性插件的生命周期基本等同于服务生命周期：跟着部署和重启走，适合底层设施改造；
- 拓展性插件可以通过 LiteFlow XML 和配置开关灵活启用/禁用，更适合作为日常的业务扩展手段；
- 在设计插件时，要同时考虑：
  - 技术接入点（DAO / LiteFlow / Service）；
  - 生命周期管理（如何启用、如何下线、如何回滚）；
  - 监控与日志（如何在出问题时快速感知并定位）。

在此基础上，再配合 `plugin-dev-guide.md` 和 `plugin-security-and-sandbox.md`，就可以相对安全地在生产环境中使用插件体系了。 


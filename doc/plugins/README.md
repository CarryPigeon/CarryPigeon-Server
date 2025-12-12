# CarryPigeon 插件开发指南（服务端）

> 本文是插件开发文档的入口，适合：  
> 想在 **不改核心代码** 的前提下，给 CarryPigeon 增加功能、埋点、审计、统计的同学。

---

## 0. 插件能做什么？

你可以把插件理解成挂在主服务上的「扩展模块」，它可以：

- 在 **LiteFlow 链路** 中插入自定义节点  
  例如：额外的风控校验、审计日志、灰度规则。
- 扩展 **业务能力**  
  例如：新消息类型的解析与通知、接第三方风控/监控服务。
- 扩展 **后台服务**  
  例如：自定义通知通道（钉钉/企业微信）、统计上报、自定义缓存。

设计目标：

- 插件可以 **独立开发、独立仓库**，只依赖稳定的 API 模块。
- 主工程升级时，插件尽量 **无需大改**，只需要对照插件 API 版本。
- 插件可以方便地在本地、测试、生产环境中 **启用 / 禁用**。

---

## 1. 插件类型：侵入性 vs 拓展性

从接入方式和对宿主的影响来看，这个项目的插件大致分两类：

### 1.1 侵入性插件（替换实现）

典型形式：**直接替换相关模块的 jar 包**，例如：

- 将原有的 `dao` 实现替换成 “分布式 DAO 层” 的实现  
  （例如本地单机 MyBatis-Plus → 分布式存储 / 分库分表实现）
- 将 `external-service` 模块替换为自研或第三方厂商的服务实现

特点：

- 通过 Maven 依赖或打包脚本，替换整个模块的实现 jar  
  （保持 API 接口一致：包名 + 类名 + 方法签名不变）
- 对宿主工程来说是「侵入性」变更：需要重新构建和部署服务
- 适合 **基础设施层** 的深度改造：存储、缓存、中间件、外部服务

推荐做法：

- 把稳定接口放在 `api` 模块中，侵入性插件只替换实现模块（`dao / external-service` 等）
- 严格对齐原接口的行为和异常约定，避免破坏上层业务
- 和团队约定好「可替换模块清单」和版本兼容策略

### 1.2 拓展性插件（LiteFlow + Spring 扩展）

典型形式：**基于 LiteFlow 流和 Spring Bean 的非侵入式扩展**，例如：

- 新增 LiteFlow 节点参与现有链路：审计、风控、统计、异步通知
- 注册新的 Service Bean：自定义埋点、监控上报、局部缓存策略
- 附加新的业务链：在不改原链的前提下为新路由提供处理逻辑

特点：

- 不替换已有模块，只是 **追加节点 / Bean / flow 配置**  
- 宿主服务依然使用原始 `dao` / `connection` 等模块  
- 插件可以通过配置启用 / 禁用，风险更可控

推荐做法：

- 插件中定义 `@LiteflowComponent` 节点，配合 `config/*.xml` 链路进行插入或追加
- 只通过公开的 Service / DAO 接口访问数据，避免直接操作底层实现类
- 针对性能敏感链路，谨慎控制拓展节点的耗时和外部依赖

---

## 2. 插件体系一览

从插件视角，服务器可以抽象为三层：

1. **公共 API 层（api 模块）**
   - 定义 BO：`CPUser`, `CPChannel`, `CPMessage`, `CPChannelReadState` 等
   - 定义接口：DAO 抽象、通知接口、控制器协议、LiteFlow 节点基类等
2. **宿主服务层（chat-domain / connection / dao / external-service / application-starter）**
   - 提供默认实现和业务链路
   - 通过 Spring + LiteFlow 暴露扩展点
3. **插件层（你写的模块 / jar）**
   - 依赖 `api`，以及约定的扩展接口
   - 通过 Spring Boot 自动装配 + LiteFlow 组件扫描接入到宿主

你可以通过三种方式参与到运行时：

- 实现自定义 **LiteFlow 节点**（继承 `CPNodeComponent` 等）  
- 提供自定义 **Service Bean**（统计、外部服务、缓存策略等）  
- 注册自定义 **配置 / 元数据**（例如追加新的 `config/*.xml` 流程文件）

详细设计参见：`plugin-architecture.md`。

---

## 3. 写一个「Hello Plugin」

下面是一个最小可运行的插件思路（伪代码级别）：

1. 新建插件工程（可以是独立 Maven 模块）

```text
my-plugin/
  pom.xml
  src/main/java/...
```

`pom.xml` 依赖：

```xml
<dependency>
  <groupId>CarryPigeon</groupId>
  <artifactId>api</artifactId>
  <version>1.0.0</version>
</dependency>
```

2. 写一个自定义 LiteFlow 节点

```java
@LiteflowComponent("MyAuditNode")
public class MyAuditNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        // 读取上下文数据，打审计日志或计数
        log.info("MyAuditNode - route={}, uid={}",
                context.getData("Route"),
                context.getData("SessionId"));
    }
}
```

3. 在你的插件 jar 被加载时，LiteFlow 会扫描到 `@LiteflowComponent("MyAuditNode")`，
   然后你只需要在对应的 `config/*.xml` 链路中插入：

```xml
<chain name="/core/channel/message/create">
    THEN(
    UserLoginChecker,
    MyAuditNode,
    ...
    )
</chain>
```

这就是最简单的「服务端插件」：一个纯附加的审计节点，不影响原有逻辑，随时可以下线。

更多实践细节会在 `plugin-dev-guide.md` 中展开。

---

## 4. 插件开发内容导航

在 `doc/plugins/` 目录下，你可以找到：

- `README.md`（本文件）  
  插件总体介绍，给你一个「能做什么」的视图。
- `plugin-architecture.md`  
  插件在服务端的技术架构：  
  - 扩展点清单（LiteFlow、Service Bean、配置）  
  - 插件如何与宿主模块协作
- `plugin-lifecycle.md`  
  插件从加载到卸载的生命周期：  
  - 如何初始化资源  
  - 如何保证关闭时不留下 “脏东西”（线程、连接、缓存）
- `plugin-api-reference.md`  
  宿主暴露给插件使用的 API 列表：  
  - 通知接口、会话中心、DAO 抽象、日志工具  
  - 如何正确使用这些接口，避免破坏主业务
- `plugin-dev-guide.md`  
  从零开始开发一个插件的完整教程：  
  - 工程结构、依赖  
  - 如何在 `application-starter` 中启用插件  
  - 本地调试与日志查看
- `plugin-security-and-sandbox.md`  
  安全与隔离相关的说明：  
  - 插件应遵守的安全边界  
  - 如何避免插件引入新的安全风险或性能热点

建议阅读顺序：

1. 先看完当前 `README.md`，确认插件的整体定位是否符合你的需求  
2. 再看 `plugin-architecture.md` 把架构和扩展点吃透  
3. 最后跟着 `plugin-dev-guide.md` 写一个真正可跑的插件

---

## 5. 下一步怎么做？

- 如果你已经有明确的扩展需求（比如「对读状态变更做额外统计」），
  可以直接跳到 `plugin-dev-guide.md`，按步骤实现一个专用插件。
- 如果你是在规划一套统一的插件生态，建议先从
  `plugin-architecture.md` 和 `plugin-security-and-sandbox.md` 开始，
  把「能做什么」和「不能做什么」定义好，再让其他同学参考这套约束开发插件。

插件是一个长期演进的能力，文档会随着真实插件的增长持续补充完善。欢迎在实际开发中，把踩过的坑和最佳实践回写到本目录下的文档中。

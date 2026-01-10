# 插件开发者文档（如何扩展后端）

> 目标读者：希望在不（或尽量少）修改核心代码的前提下扩展 CarryPigeon Backend 的同学。  
> 覆盖：插件类型、扩展点、最小实践路径、上线注意事项。

## 1. 快速导航

- 插件文档入口：`doc/plugins/README.md`
- 插件架构（侵入性 vs 拓展性）：`doc/plugins/plugin-architecture.md`
- 插件开发教程：`doc/plugins/plugin-dev-guide.md`
- 插件 API 列表：`doc/plugins/plugin-api-reference.md`
- 生命周期与安全：`doc/plugins/plugin-lifecycle.md`、`doc/plugins/plugin-security-and-sandbox.md`

## 2. 插件的两种模式

### 2.1 拓展性插件（推荐起步）

目标：**不替换既有模块**，通过 Spring + LiteFlow 增强链路：

- 新增 `@LiteflowComponent` 节点（继承 `CPNodeComponent`）
- 在 LiteFlow XML（`application-starter/src/main/resources/config/*.xml`）中插入/追加节点
- 通过 `CPNotifier` 推送通知（如审计、统计、刷新提示）

适合：

- 审计/风控/统计埋点
- 性能监控与慢请求观察
- 业务链路的小幅增强（不改变底层存储语义）

### 2.2 侵入性插件（替换实现）

目标：**替换宿主默认实现**（例如 DAO、外部服务），保持 `api` 接口不变：

- 在插件中实现 `api` 的 DAO/Service 接口
- 用 `@Primary` 或条件装配控制启用（建议 `@ConditionalOnProperty`）

适合：

- 分库分表/分布式存储的 DAO 实现替换
- 更换邮件/对象存储/鉴权等外部服务实现

## 3. 最小可用实践路径（拓展性插件）

1. 只依赖稳定边界 `api`（避免依赖 `dao/chat-domain/connection` 的内部实现类）
2. 写一个 LiteFlow 节点：
   - 继承 `CPNodeComponent`
   - 从 `CPFlowContext` 读取必要的上下文 key（优先使用 `chat-domain/attribute` 中的常量）
3. 将节点插入现有链路（XML），例如在通知之前插入审计节点
4. 打包并把插件 jar 放入运行时 classpath（或作为依赖进入最终分发包）
5. 通过配置控制启用/禁用，保证可灰度

一个最小节点示例：

```java
@LiteflowComponent("MyAuditNode")
public class MyAuditNode extends CPNodeComponent {
    @Override
    public void process(CPSession session, CPFlowContext context) throws Exception {
        Long uid = context.getData(CPNodeCommonKeys.SESSION_ID);
        if (uid == null) {
            argsError(context);
            return;
        }
        log.info("audit - uid={}", uid);
    }
}
```

将节点插入链路示例（以 `application-starter/src/main/resources/config/channel_message.xml` 为例）：

```xml
<chain name="/core/channel/message/read/state/update">
    THEN(
    UserLoginChecker,
    ...,
    MyAuditNode,
    ...,
    CPNotifier.bind("route","/core/channel/message/read/state")
    )
</chain>
```

完整示例与细节见：`doc/plugins/plugin-dev-guide.md`

## 4. 插件与接口契约（最容易踩坑的点）

- **错误码与响应结构**：必须遵守 `CPResponse` 约定（见 `doc/audience/client-developer-guide.md` 的状态码与推送说明）
- **JSON 命名**：对外 JSON 一律 `snake_case`
- **链路稳定性**：插件节点出现异常要可控（记录日志、必要时 `argsError/businessError` 终止链路）
- **性能**：避免在高频链路中做慢 IO；必要时异步化并可降级

## 5. 上线前检查清单

- 安全边界与依赖审计：`doc/plugins/plugin-security-and-sandbox.md`
- 生命周期资源释放（线程/连接/缓存）：`doc/plugins/plugin-lifecycle.md`
- 压测与灰度开关（条件装配/配置开关）

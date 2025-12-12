# 插件开发实战指南（服务端）

> 本文是「能跑起来」导向的开发指南。  
> 目标：带你从零实现两个示例插件：
>
> 1. 拓展性插件：基于 LiteFlow 的审计节点（非侵入式）  
> 2. 侵入性插件：替换 DAO 实现的分布式读状态 DAO（侵入式）

建议先通读 `README.md` 和 `plugin-architecture.md`，对整体有个概念，再动手跟本指南写代码。

---

## 1. 开发前准备

### 1.1 环境前提

- JDK 21
- Maven 3.8+（与仓库中使用的版本保持一致）
- 能正常构建当前项目：

```bash
mvn clean install -DskipTests=true
```

### 1.2 主要依赖模块

插件开发会用到的核心模块：

- `api`：领域模型、DAO 接口、协议对象
- `chat-domain`：LiteFlow 节点基类、Controller 帮助类
- `application-starter`：应用启动和 LiteFlow 规则加载

侵入性插件需要对齐 `api` 中的接口定义；拓展性插件更多关注 `chat-domain` 提供的基类和上下文约定。

---

## 2. 拓展性插件示例：读状态审计节点

场景：对「频道消息已读状态更新」做审计，把每次状态更新记录到日志或外部统计系统。  
目标：不替换任何现有模块，只通过 LiteFlow 节点挂在 `/core/channel/message/read/state/update` 链路上。

### 2.1 新建插件模块（或单独仓库）

以新 Maven 模块为例（也可以是独立仓库中的 jar）：

```text
plugins/
  message-read-audit/
    pom.xml
    src/main/java/...
```

`pom.xml` 中至少需要引入：

```xml
<dependencies>
    <!-- 领域模型 & 协议 -->
    <dependency>
        <groupId>CarryPigeon</groupId>
        <artifactId>api</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- LiteFlow 节点基类、上下文 key、会话对象等 -->
    <dependency>
        <groupId>CarryPigeon</groupId>
        <artifactId>chat-domain</artifactId>
        <version>1.0.0</version>
    </dependency>

    <!-- LiteFlow + Spring Boot （若需要独立启动调试） -->
    <dependency>
        <groupId>com.yomahub</groupId>
        <artifactId>liteflow-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

> 说明：拓展性插件依赖 `chat-domain` 是为了使用现成的基类 `CPNodeComponent` 和上下文 key，减少重复代码。

### 2.2 编写审计 Service（可选）

如果你希望把审计打到独立存储或第三方服务，可以先封装一个 Service：

```java
@Service
public class MessageReadAuditService {

    private static final Logger log = LoggerFactory.getLogger(MessageReadAuditService.class);

    public void record(long uid, long cid, long lastReadTimeMillis) {
        // 示例：简单打日志，实际可以写数据库或发 MQ
        log.info("MessageReadAudit - uid={}, cid={}, lastReadTime={}", uid, cid, lastReadTimeMillis);
    }
}
```

### 2.3 编写 LiteFlow 审计节点

在插件模块中新增一个节点，挂在读状态更新链之后：

```java
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.node.CPNodeComponent;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

@Slf4j
@AllArgsConstructor
@LiteflowComponent("MessageReadAudit")
public class MessageReadAuditNode extends CPNodeComponent {

    private final MessageReadAuditService auditService;

    @Override
    public void process(CPSession session, DefaultContext context) throws Exception {
        Long uid = context.getData(CPNodeCommonKeys.SESSION_ID);
        CPChannelReadState state = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        if (uid == null || state == null) {
            log.error("MessageReadAudit args error, uid={}, state={}", uid, state);
            argsError(context);
            return;
        }
        auditService.record(uid, state.getCid(), state.getLastReadTime());
        log.debug("MessageReadAudit success, uid={}, cid={}, lastReadTime={}",
                uid, state.getCid(), state.getLastReadTime());
    }
}
```

要点：

- 从 `CPNodeCommonKeys.SESSION_ID` 和 `CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO` 读取信息；
- 保持日志为英文、包含关键字段；
- 在参数不完整时调用 `argsError(context)` 提前终止链路。

### 2.4 将节点插入现有链路

在宿主项目的 `application-starter/src/main/resources/config/channel_message.xml` 中，将节点追加到 `/core/channel/message/read/state/update` 链路中合适的位置，例如在通知之前：

```xml
<!-- 更新频道消息已读状态 -->
<chain name="/core/channel/message/read/state/update">
    THEN(
    /** 判断用户是否登录 **/
    UserLoginChecker,
    /** 将 SessionId、频道 id 映射为成员选择所需的参数 **/
    RenameArg.bind("key","ChannelMemberInfo_Uid:SessionId;ChannelMemberInfo_Cid:ChannelReadStateInfo_Cid"),
    /** 校验用户是否在频道中 **/
    CPChannelMemberSelector.bind("key","CidWithUid"),
    /** 更新已读状态 **/
    CPChannelReadStateUpserter,
    /** 自定义审计节点（插件提供） **/
    MessageReadAudit,
    /** 收集当前用户自身会话对应的 uid **/
    CPUserSelfCollector,
    /** 构建已读状态通知数据 **/
    CPChannelReadStateNotifyBuilder,
    /** 通知该用户的所有在线会话 **/
    CPNotifier.bind("route","/core/channel/message/read/state")
    )
</chain>
```

> 注意：只要插件 jar 在运行时 classpath 中，LiteFlow 就能扫描到 `MessageReadAudit` 节点；  
> 不需要修改 Java 代码即可控制是否插入这个节点（通过 XML 配置）。

### 2.5 打包与验证

1. 确保插件模块构建成功：

```bash
mvn -pl plugins/message-read-audit -am package
```

2. 将生成的 jar 放入宿主服务运行时 classpath（例如添加到 `application-starter` 的依赖，或部署脚本引用）。
3. 启动 `application-starter`，对 `/core/channel/message/read/state/update` 发起请求，查看日志是否出现审计记录。

至此，一个拓展性插件就完成了。

---

## 3. 侵入性插件示例：分布式读状态 DAO

场景：将读状态存储从单数据库提升到分布式实现（例如多实例、分库分表，或独立 KV 存储），并希望上层逻辑无感知。

目标：替换 `ChannelReadStateDao` 的默认实现，同时保留接口不变。

### 3.1 定位要替换的接口与默认实现

- 接口定义（稳定边界）：
  - `api/src/main/java/team/carrypigeon/backend/api/dao/database/channel/read/ChannelReadStateDao.java`
- 默认实现：
  - `dao/src/main/java/team/carrypigeon/backend/dao/database/impl/channel/read/ChannelReadStateDaoImpl.java`

侵入性插件要做的，是提供一个新的 `ChannelReadStateDao` 实现，并让 Spring 优先注入它。

### 3.2 编写分布式 DAO 实现

在插件模块中新增实现类，例如：

```java
@Slf4j
@Service
@Primary  // 关键点：让这个 Bean 优先于默认实现
public class DistChannelReadStateDaoImpl implements ChannelReadStateDao {

    // 这里可以是访问分布式存储的客户端，例如自定义 SDK、Redis、ES 等
    private final MyDistributedStoreClient client;

    public DistChannelReadStateDaoImpl(MyDistributedStoreClient client) {
        this.client = client;
    }

    @Override
    public CPChannelReadState getById(long id) {
        log.debug("DistChannelReadStateDaoImpl#getById - id={}", id);
        return client.getById(id);
    }

    @Override
    public CPChannelReadState getByUidAndCid(long uid, long cid) {
        log.debug("DistChannelReadStateDaoImpl#getByUidAndCid - uid={}, cid={}", uid, cid);
        return client.getByUidAndCid(uid, cid);
    }

    @Override
    public boolean save(CPChannelReadState state) {
        if (state == null) {
            log.error("DistChannelReadStateDaoImpl#save called with null state");
            return false;
        }
        boolean success = client.save(state);
        if (success) {
            log.debug("DistChannelReadStateDaoImpl#save success, id={}, uid={}, cid={}",
                    state.getId(), state.getUid(), state.getCid());
        } else {
            log.warn("DistChannelReadStateDaoImpl#save failed, id={}, uid={}, cid={}",
                    state.getId(), state.getUid(), state.getCid());
        }
        return success;
    }

    @Override
    public boolean delete(CPChannelReadState state) {
        if (state == null) {
            log.error("DistChannelReadStateDaoImpl#delete called with null state");
            return false;
        }
        boolean success = client.delete(state.getId());
        if (success) {
            log.debug("DistChannelReadStateDaoImpl#delete success, id={}", state.getId());
        } else {
            log.warn("DistChannelReadStateDaoImpl#delete failed, id={}", state.getId());
        }
        return success;
    }
}
```

关键点：

- 实现 `ChannelReadStateDao` 接口，不修改方法签名和语义；
- 使用 `@Service` 和 `@Primary`，让 Spring 注入这个实现而不是默认的 `ChannelReadStateDaoImpl`；
- 保持日志风格与默认实现一致（方便排查）。

### 3.3 条件启用（可选但推荐）

为了更安全地上线侵入性插件，建议使用条件装配控制启用：

```java
@Slf4j
@Service
@Primary
@ConditionalOnProperty(prefix = "cp.read-state.dao", name = "mode", havingValue = "distributed")
public class DistChannelReadStateDaoImpl implements ChannelReadStateDao {
    ...
}
```

在 `application.yaml` 中配置：

```yaml
cp:
  read-state:
    dao:
      mode: distributed  # 或 default
```

这样可以：

- 本地 / 测试环境：使用 `default` 模式，依然走原始 DAO；
- 线上灰度：指定某些环境或实例使用 `distributed`，逐步放量。

### 3.4 打包与验证

1. 构建插件模块：

```bash
mvn -pl plugins/dist-read-state-dao -am package
```

2. 将插件 jar 加入 `application-starter` 的依赖或部署路径；
3. 配置 `cp.read-state.dao.mode=distributed`，启动服务；
4. 通过读状态相关接口（`/core/channel/message/read/state/update` / `/get`）进行回归验证，并观察 DAO 日志。

---

## 4. 开发插件时的注意事项

1. **保持接口边界清晰**
   - 插件只能依赖公共接口（`api`）以及明确暴露的扩展点；
   - 避免直接依赖宿主内部实现类，这会严重影响升级兼容性。

2. **日志与错误处理**
   - 日志统一使用英文，包含关键字段（uid / cid / route / time 等）；
   - 对于拓展性插件，尽量将错误处理为「不影响主业务」的模式：  
     例如失败时仅打日志或通过监控上报，而不抛出异常中断整条链路。

3. **性能与资源管理**
   - 避免在拓展性节点中进行阻塞式长耗时操作（例如外部 HTTP 调用）；  
     如有必要，考虑使用异步队列 / 异步上报。
   - 注意连接池、线程池、缓存等资源的生命周期，避免泄漏。

4. **与宿主团队的协作**
   - 在引入侵入性插件前，和宿主维护者对齐：
     - 接口语义、错误约定、索引要求；
     - 日志格式、监控埋点；
     - 灰度和回滚方案。

---

## 5. 下一步

如果你已经跟着指南写完第一个插件，可以继续：

- 查看 `plugin-api-reference.md`：了解更多可用的服务端 API 与扩展点；
- 查看 `plugin-security-and-sandbox.md`：在生产环境前审视安全性和隔离问题；
- 将你的最佳实践和常见问题整理回 `doc/plugins/`，形成团队内部的插件开发规范。 


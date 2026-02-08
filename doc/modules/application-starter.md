# 模块说明：application-starter

> `application-starter` 模块是整个服务端的启动入口与配置聚合点。  
> 它本身不包含业务逻辑，只负责：
> - 启动 Spring Boot；
> - 加载配置文件；
> - 启用定时任务与缓存；
> - 加载 LiteFlow 规则。

---

## 1. 目录结构概览

源码路径：`application-starter/src/main/java/team/carrypigeon/backend/starter`

- `ApplicationStarter.java`
  - 主启动类，包含 `main` 方法；
  - 使用注解：
    - `@SpringBootApplication`：声明 Spring Boot 应用；
    - `@EnableScheduling`：启用定时任务（供其他模块的 `@Scheduled` 使用，例如清理缓存、session）；
    - `@ComponentScan(basePackages = {"team.carrypigeon.backend"})`：
      - 扫描所有业务模块 Bean；
      - 插件约定：`team.carrypigeon.backend.plugin.{{PluginName}}` 也在扫描范围内；
    - `@Slf4j`：记录启动日志。

资源路径：`application-starter/src/main/resources`

- `application.yaml`：全局配置文件；
- `log4j2_config.xml`：日志配置；
- `config/*.xml`：LiteFlow 规则文件（渠道/消息/用户等）。

---

## 2. 启动过程

`ApplicationStarter.main`：

```java
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"team.carrypigeon.backend"})
@Slf4j
public class ApplicationStarter {
    public static void main(String[] args) {
        log.info("Application is starting...");
        new SpringApplication(ApplicationStarter.class).run(args);
        log.info("spring context ready.");
    }
}
```

步骤：

1. 打印启动日志；
2. 启动 Spring 容器，加载所有被扫描到的 Bean（包括 DAO、业务节点、控制器、连接层等）；
3. 初始化 Redis、数据库连接池、LiteFlow、Netty 连接服务等；
4. 打印 “spring context ready.”，表示主上下文可用。

---

## 3. 配置项说明（application.yaml）

路径：`application-starter/src/main/resources/application.yaml`

### 3.1 Spring 基础配置

```yaml
spring:
  application:
    name: CarryPigeonBackend
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
    enable: false
```

- `spring.application.name`：应用名称；
- `spring.mail.*`：邮件发送配置：
  - `enable: false` 表示默认关闭，可在生产配置中打开。

### 3.2 日志配置

```yaml
logging:
  config: classpath:log4j2_config.xml
```

- 指定使用 `log4j2_config.xml` 作为日志配置文件；
- 日志输出路径、级别、格式等在该 XML 中定义。

### 3.3 服务器信息（业务展示用）

```yaml
cp:
  server:
    server_name: CarryPigeonBackend
    avatar: default-server-avatar
    brief: CarryPigeon chat backend server
    time: 1733616000000
```

- 用于前端展示服务器信息（名称、头像、简介、创建时间）；
- 不影响内部逻辑。

### 3.4 MinIO 文件服务配置

```yaml
minio:
  endpoint: http://localhost:9005
  bucketName: carrygieon
  accessKey: minioadmin
  secretKey: minioadmin
```

- 文件上传/下载服务使用 MinIO；
- 这些配置由 `external-service` 和 `chat-domain` 中的文件服务读取。

### 3.5 连接服务端口

```yaml
connection:
  port: 7609
```

- Netty 连接服务监听端口；
- `connection` 模块的 `NettyConnectionStarter` 使用此配置启动。

### 3.6 LiteFlow 规则来源

```yaml
liteflow:
  rule-source: config/*.xml
```

- 告诉 LiteFlow 从 `classpath:config/*.xml` 加载所有链路规则；
- 这些 XML 文件定义了：
  - 用户/频道/成员/消息/文件等业务路由对应的链路；
  - 插件可通过追加节点和修改 XML 参与链路。

---

## 4. LiteFlow 规则文件

路径：`application-starter/src/main/resources/config`

当前包含：

- `user.xml`：用户相关链路；
- `channel.xml`：频道基础链路；
- `channel_member.xml`：频道成员；
- `channel_application.xml`：频道申请；
- `channel_ban.xml`：禁言；
- `channel_admin.xml`：管理员相关；
- `api_messages.xml`：消息发送、删除、列表、未读数、读状态更新/查询；
- `file.xml`：文件上传/下载链路。

说明：

- 每个 XML 中的 `chain name` 必须与 `@CPControllerTag.path` 一一对应；
- 修改或新增链路时，应保持 `chat-domain/AGENTS.md` 中的规范：
  - 确保上下文参数在进入节点前已准备好；
  - 不在 XML 中做过多业务逻辑，只做节点编排。

---

## 5. 与其他模块的协作

- 与 `connection`：
  - `connection.port` 由 `application.yaml` 提供；
  - Netty 启动（`NettyConnectionStarter`）在 Spring 容器就绪后运行。

- 与 `chat-domain`：
  - 掃描 `chat-domain` 中的 Controller、LiteFlow 节点、服务组件；
  - `config/*.xml` 中的链路名称与 `chat-domain` 控制器和节点绑定。

- 与 `dao` / `external-service`：
  - 通过 Spring Boot 自动配置加载数据源和 Redis；
  - 外部服务（邮件、文件）根据 `application.yaml` 中的配置进行初始化。

---

## 6. 部署层面的注意事项（简要）

更详细的部署与运维说明见：`doc/ops/config-and-deploy.md`。  
此处仅强调与 `application-starter` 直接相关的几点：

- 配置分环境管理：
  - 将 `application.yaml` 中的敏感信息（数据库密码、accessKey/secretKey 等）迁移到环境专用配置或密钥管理系统；
  - 使用 Spring Profile 加载不同环境配置。

- 日志目录与权限：
  - `log4j2_config.xml` 中默认将日志写到 `./service-logs`；
  - 部署时确保该路径存在且进程有写权限。

- LiteFlow XML 变更：
  - 修改 `config/*.xml` 会影响业务链路；
  - 建议在测试环境充分验证后，再同步到生产环境。

`application-starter` 是其他所有模块的“外壳”，其配置与启动逻辑对整个系统的行为有决定性影响，因此在修改此模块配置时应特别注意环境隔离与回滚方案。 


# 配置与部署说明

> 本文整理了 CarryPigeon Backend 在不同环境下的配置项与基本部署方式，  
> 帮助你快速完成本地开发、测试、生产环境的部署。

---

## 1. 配置文件与加载顺序

主配置文件：

- `application-starter/src/main/resources/application.yaml`

建议按环境拆分：

- `application-dev.yaml` — 开发环境；
- `application-test.yaml` — 测试环境；
- `application-prod.yaml` — 生产环境。

通过 Spring Profile 控制加载：

```bash
java -jar application-starter-1.0.0.jar --spring.profiles.active=prod
```

或在环境变量 / 启动脚本中设置 `SPRING_PROFILES_ACTIVE`。

---

## 2. 核心配置项说明

### 2.1 应用与邮件

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
- 邮件配置：
  - `enable: false` 时不会实际发送邮件；
  - 生产环境根据实际 SMTP 服务调整 host/port/认证方式；
  - 密码等敏感信息建议从环境变量或密钥管理系统注入。

### 2.2 日志

```yaml
logging:
  config: classpath:log4j2_config.xml
```

- 指定使用项目内的 `log4j2_config.xml` 作为日志配置；
- 日志路径、切分策略在该 XML 中配置（默认输出到 `./service-logs`）。

### 2.3 服务器元信息

```yaml
cp:
  server:
    server_name: CarryPigeonBackend
    avatar: default-server-avatar
    brief: CarryPigeon chat backend server
    time: 1733616000000
```

- 主要供前端展示使用，不会影响业务流程。

### 2.4 MinIO 文件服务

```yaml
minio:
  endpoint: http://localhost:9005
  bucketName: carrygieon
  accessKey: minioadmin
  secretKey: minioadmin
```

- `endpoint`：MinIO 服务地址；
- `bucketName`：文件容器名称；
- `accessKey` / `secretKey`：访问凭据（生产环境务必使用安全配置）。

### 2.5 连接端口

```yaml
connection:
  port: 7609
```

- Netty 长连接监听端口；
- 确保在防火墙 / 负载均衡器上已放通此端口。

### 2.6 LiteFlow 规则路径

```yaml
liteflow:
  rule-source: config/*.xml
```

- 告知 LiteFlow 从 `classpath:config/*.xml` 加载业务链路定义；
- 不同环境可根据需求加载不同的规则集（例如增加 `config/test-only/*.xml`）。

---

## 3. 依赖服务与资源准备

在部署前，需要准备以下外部依赖：

1. **数据库**
   - 类型（MySQL / PostgreSQL 等）；
   - 初始化 schema（依据 `doc/domain/database-schema.md` 和迁移脚本）；
   - 为业务服务配置连接串、用户名、密码。

2. **Redis**
   - 用于 Spring Cache 和可能的业务缓存；
   - 配置 Redis 地址、密码等信息；
   - 在 `RedisConfig` 中已经定义了 CacheManager 与常用模板。

3. **MinIO（或兼容 S3 的对象存储）**
   - 创建对应 bucket（如 `carrygieon`）；
   - 确保访问凭据与 `application.yaml` 中配置一致。

4. **邮件服务（可选）**
   - 若启用邮箱登录/验证码功能，需要可用的 SMTP 服务；
   - 在测试环境可使用专用测试邮箱。

---

## 4. 本地开发环境

建议配置：

- 使用 Docker 或本机安装 MySQL/Redis/MinIO；
- 启动 `application-starter`：

```bash
mvn -pl application-starter -am spring-boot:run -Dspring.profiles.active=dev
```

注意事项：

- 本地可使用较高的日志级别（例如 `debug`）；
- 可关闭部分重资源功能（如邮件、某些外部服务）；
- 使用 InMemory DAO 的单元/集成测试（chat-domain 模块）不需要真实 DB 与 Redis。

---

## 5. 测试与预发布环境

目标：

- 尽可能模拟生产环境配置；
- 在上线前发现配置与性能问题。

建议：

- 使用与生产相同类型的数据库和 Redis；
- 尽量使用与生产相同的 MinIO 或存储服务；
- 开启必要的监控与日志收集：
  - 如接入 ELK / Loki / Prometheus 等；
- 在此环境中验证：
  - 读状态同步链路；
  - 消息收发、未读统计；
  - 频道与成员管理；
  - 插件（如有）启用/禁用行为。

---

## 6. 生产环境部署

### 6.1 构建与部署包

- 通过 Maven 打包：

```bash
mvn clean package -DskipTests=true
```

- 部署产物：
  - `application-starter/target/application-starter-1.0.0.jar`（可执行 jar）；
  - 或根据 `distribution` 模块构建 Docker 镜像。

### 6.2 运行参数与环境变量

- 常用参数：

```bash
java -Xms512m -Xmx1024m \
  -Dspring.profiles.active=prod \
  -Dlogging.config=classpath:log4j2_config.xml \
  -jar application-starter-1.0.0.jar
```

- 建议将敏感配置（DB/Redis/MinIO 密码等）从环境变量或配置中心注入，而不是写死在 `application.yaml` 中。

### 6.3 扩容与高可用

由于使用了：

- 无状态业务节点（长连接状态通过 CPSessionCenterService 和 ChannelAttribute 管理，但不持久）；
- 外部存储（数据库/Redis/MinIO）；

可以通过多实例部署 + 负载均衡来实现高可用：

- 负载均衡器（如 Nginx / LVS / 云厂商 LB）将 TCP 连接分散到多个实例；
- 需考虑：
  - 长连接分布均衡；
  - 灰度发布策略（先上线部分实例，再逐步切流）。

---

## 7. 配置变更与回滚

原则：

- 配置改动应先在测试环境验证；
- 生产环境变更需有回滚方案。

建议流程：

1. 修改配置（例如调大连接端口、调整 LiteFlow 链路、更新 Redis/DB 地址）；
2. 在测试环境验证；
3. 将配置同步到生产（通过配置中心或版本管理）；
4. 滚动重启生产实例；
5. 如出现问题：
   - 立即回退配置到上一个版本；
   - 重启受影响实例。

更详细的安全与日志相关注意事项见：

- `doc/ops/logging-and-observability.md`
- `doc/ops/security-guidelines.md` 


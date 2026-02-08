# 配置与部署规范

> 本文定义 CarryPigeon Backend 的标准部署方式与最小配置集，适用于开发、测试、生产环境。

## 1. 配置原则

- 配置唯一入口：`application-starter/src/main/resources/application.yaml`
- 环境隔离：`application-{dev,test,prod}.yaml` + `spring.profiles.active`
- 敏感信息（密码、密钥、令牌）必须通过环境变量或密钥管理系统注入
- LiteFlow 规则文件统一放在 `config/`（外置部署）或 `classpath:config/`（内置运行）

## 2. 必需配置项（最小集合）

- 服务基础：`server.port`、`spring.application.name`
- 数据源：`spring.datasource.*`
- 缓存：`spring.data.redis.*`
- 文件服务：`minio.*`
- 认证：`cp.auth.*`
- 服务器信息：`cp.server.*`（用于 `GET /api/server`）
- required gate：`cp.api.required_plugins`
- 插件包扫描：`cp.api.plugin_package_scan.*`
- LiteFlow：`liteflow.rule-source=config/*.xml`

## 3. 推荐部署结构

使用 `distribution` 分发包时，目录建议如下：

- `application-starter-<version>.jar`
- `module/`
- `libs/`
- `config/`（`application.yaml`、`log4j2_config.xml`、`api_*.xml`）

启动示例（Linux/macOS）：

```bash
java -cp "config:.:application-starter-*.jar:module/*:libs/*" team.carrypigeon.backend.starter.ApplicationStarter
```

## 4. 环境差异建议

- 开发环境：降低外部依赖门槛，可关闭邮件、放宽日志级别
- 测试环境：启用完整鉴权与插件扫描，便于接口验收
- 生产环境：必须启用 TLS、受控日志等级、备份与告警

## 5. 升级与回滚

升级步骤：

1. 备份数据库与 `config/`
2. 发布新包并保留旧版本
3. 执行 SQL migration（如有）
4. 健康检查通过后切流

回滚步骤：

1. 恢复上一版本二进制与配置
2. 必要时回滚数据库迁移
3. 校验 `GET /api/server`、登录、消息收发、WS 事件

## 6. 发布检查清单

- [ ] `doc/api/11-HTTP端点清单.md` 中 P0 端点可用
- [ ] `wss://{host}/api/ws` 连接、`auth`、事件推送正常
- [ ] required gate 行为符合预期（未满足时返回 `412 required_plugin_missing`）
- [ ] 日志目录可写且滚动策略正常
- [ ] 关键依赖（DB/Redis/MinIO/SMTP）连通

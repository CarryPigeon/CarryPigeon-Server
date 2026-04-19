# application-starter

`application-starter` 是当前项目的启动与运行时装配模块。

## 职责

- 启动 Spring Boot 应用
- 承接最小运行配置
- 装配 `chat-domain` 与固定基础设施能力
- 后续接入外部服务实现时，负责装配对应 `infrastructure-service/*-impl`

## 边界

- 不承载核心业务规则
- 不直接实现数据库、缓存、对象存储等外部服务能力
- 不放置未来未启用的占位配置

## 启动命令

```bash
mvn -pl application-starter -am spring-boot:run
```

更多规则请参考根目录 `AGENTS.md` 与 `docs/架构文档.md`。

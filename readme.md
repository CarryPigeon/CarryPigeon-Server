# CarryPigeon Backend

CarryPigeon 后端项目，当前处于重写式重构阶段。

## 当前模块

- `application-starter`：启动与运行时装配模块
- `chat-domain`：核心业务域模块
- `infrastructure-basic`：固定全局基础设施模块
- `infrastructure-service`：可拔插外部服务基础设施父模块
- `distribution`：当前 thin jar + libs 打包与分发模块

## 当前原则

- 遵循 `docs/` 下的项目规范
- Spring Boot 与 Lombok 视为项目内部能力
- Docker 当前只提供 MySQL、Redis、MinIO 等外部服务
- AI 中间产物统一放入 `ai-agent-workplace/`

## 常用命令

```bash
mvn test -DskipTests=false
```

```bash
mvn -pl application-starter -am spring-boot:run
```

```bash
mvn -pl distribution -am package
```

```bash
cp .env.example .env
docker compose up -d
```

## 推荐命令入口

```bash
bash bin/dist-package.sh
```

```bash
bash bin/dist-start.sh \
  --cp.chat.auth.jwt.secret=YOUR_SECRET \
  --cp.chat.server.id=YOUR_SERVER_ID
```

```bash
bash bin/dist-start-background.sh \
  --cp.chat.auth.jwt.secret=YOUR_SECRET \
  --cp.chat.server.id=YOUR_SERVER_ID
```

```bash
bash bin/dist-stop.sh
```

```bash
bash bin/docker-up.sh
```

```bash
bash bin/docker-down.sh
```

```bash
bash bin/docker-reset.sh
```

```bash
bash bin/docker-logs.sh
```

详细规则请先阅读：

- `AGENTS.md`
- `docs/架构文档.md`
- `docs/部署手册.md`
- `docs/AI协作开发规范.md`

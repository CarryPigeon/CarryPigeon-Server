# 项目 Docker 配置

## 1. 文档目的

本文用于固化当前项目的 Docker 外部服务配置。

当前阶段只编排外部基础服务，不提前容器化应用本身。

若需要查看完整的分发、部署、前后台启动与停止流程，请同时阅读：

- `docs/operations/部署手册.md`
- `docs/operations/数据库部署手册.md`
- `docs/sql/README.md`

## 2. 当前容器化范围

当前已配置的外部服务包括：

- MySQL
- Redis
- MinIO
- Nginx（可选 edge profile，本地 HTTPS/WSS 反代入口）

原因：

- 这三类能力已被明确为当前项目的外部基础服务
- 具体接入代码现已进入 `infrastructure-service` 的对应 `*-api` 与 `*-impl`
- 当前仍保持“外部服务容器化、应用本身不容器化”的运行边界
- Nginx 仅作为入口反向代理，不承载 Java 应用容器

## 3. 文件落点

当前 Docker 相关文件如下：

- `docker-compose.yaml`
- `deploy/nginx/templates/carrypigeon.conf.template`
- `deploy/nginx/certs/README.md`
- `bin/linux/nginx-up.sh`
- `bin/windows/nginx-up.ps1`

说明：

- `docker-compose.yaml` 用于编排本地外部服务
- Docker Compose 配置固定来自 `docker-compose.yaml`
- `deploy/nginx/templates/carrypigeon.conf.template` 用于生成 Nginx 反代配置
- `deploy/nginx/certs/` 只放本地或生产证书文件，证书和私钥不进入 Git

## 4. 使用方式

### 4.1 配置来源

Docker / Nginx 脚本显式禁用 `.env` 自动加载，只使用 `docker-compose.yaml` 中的默认值和显式内容。

如需修改端口、账号、密码或 Nginx 域名，应直接修改 `docker-compose.yaml` 或 Nginx 模板相关配置。

### 4.2 启动外部服务

```bash
docker compose up -d
```

推荐使用项目脚本：

```bash
bash bin/linux/docker-up.sh
```

### 4.3 停止外部服务

```bash
docker compose down
```

推荐使用项目脚本：

```bash
bash bin/linux/docker-down.sh
```

### 4.4 连同数据卷一起清理

```bash
docker compose down -v
```

推荐使用项目脚本：

```bash
bash bin/linux/docker-reset.sh
```

### 4.5 查看外部服务日志

```bash
docker compose logs -f
```

推荐使用项目脚本：

```bash
bash bin/linux/docker-logs.sh
```

### 4.6 启动本地 Nginx 反代

服务端应用仍在宿主机以 jar 方式运行，Nginx 只负责提供客户端可访问的 HTTPS/WSS 入口。

首次启动：

```bash
bash bin/linux/nginx-up.sh
```

Windows：

```powershell
bin\windows\nginx-up.bat
```

脚本行为：

- 脚本使用 `docker-compose.yaml` 的 `edge` profile 启动 `nginx` 服务，并禁用 `.env` 自动加载。
- 若 `deploy/nginx/certs/fullchain.pem` 和 `deploy/nginx/certs/privkey.pem` 不存在，会生成本地自签开发证书。
- 使用 compose 的 `edge` profile 仅启动 `nginx` 服务。

客户端本地联调时填写：

```text
https://localhost
```

约束：

- 桌面客户端要接受 `https://localhost`，需要将生成的本地证书加入操作系统信任，或替换为可信 CA 签发证书。
- Nginx 默认把 `/api/` 转发到宿主机 `8080`，把 `/api/ws` 转发到宿主机 `18080`。
- Nginx 会在 `/api/server` 响应中把内部 `ws_url` 默认从 `ws://127.0.0.1:18080/api/ws` 替换为 `wss://localhost/api/ws`，避免客户端绕过 Nginx 直连明文 WS。
- Java 应用必须仍由 `bash bin/linux/app-start.sh`、分发包脚本或 `java -jar` 在宿主机启动。
- 邮箱验证码登录还需要配置 SMTP；Nginx 只解决 HTTPS/WSS 入口，不提供邮件能力。

## 5. 当前服务说明

### 5.1 MySQL

镜像：

- `mysql:8.4`

职责：

- 作为当前 `database-impl` 的默认本地数据库

数据库初始化和测试数据导入方式请参考：

- `docs/operations/数据库部署手册.md`
- `docs/sql/README.md`

当前约定：

- 默认数据库名：`carrypigeon`
- 默认字符集：`utf8mb4`
- 默认排序规则：`utf8mb4_unicode_ci`
- 默认时区：`+08:00`

默认端口：

- `3306`

### 5.2 Redis

镜像：

- `redis:7.4-alpine`

职责：

- 作为当前 `cache-impl` 的默认本地缓存服务

当前约定：

- 开启 AOF 持久化
- 启用密码保护

默认端口：

- `6379`

### 5.3 MinIO

镜像：

- `minio/minio:RELEASE.2025-04-08T15-41-24Z`

职责：

- 作为当前对象存储服务 `storage-impl` 的默认本地实现

当前约定：

- Compose 会在 MinIO 健康检查通过后自动执行 bucket 初始化
- 默认 bucket：`carrypigeon`

默认端口：

- API：`9000`
- Console：`9001`

### 5.4 Nginx

镜像：

- `nginx:1.27-alpine`

职责：

- 为桌面客户端提供 HTTPS/WSS 统一入口。
- 将 `/api/` 反代到宿主机 Spring MVC HTTP 端口。
- 将 `/api/ws` 反代到宿主机 Netty WebSocket 端口。

默认端口：

- HTTP：`80`
- HTTPS：`443`

默认后端：

- HTTP 后端：`host.docker.internal:8080`
- WebSocket 后端：`host.docker.internal:18080`

启动方式：

```bash
docker compose --profile edge up -d nginx
```

推荐使用：

```bash
bash bin/linux/nginx-up.sh
```

## 6. Compose 默认变量

Docker Compose 与 Nginx 模板中的变量都带有默认值，例如 `MYSQL_PORT`、`REDIS_PORT`、`MINIO_API_PORT`、`NGINX_SERVER_NAME`、`NGINX_HTTPS_PORT` 等。

补充说明：

- 仓库脚本设置 `COMPOSE_DISABLE_ENV_FILE=1`，避免本地 `.env` 影响 Docker Compose 渲染。
- 这些变量只表达 Docker Compose / Nginx 容器编排默认值。
- Java 应用运行配置已迁移到 `config/application.yaml`。
- 如修改 Nginx HTTPS 端口或后端地址，需要直接调整 `docker-compose.yaml` 中对应默认值。

## 7. 设计边界

- 当前 Docker 配置只负责本地外部服务运行
- 不在当前阶段引入应用镜像构建与应用容器编排
- Nginx edge profile 只提供入口层反代，不代表应用进入 Docker 部署
- 当前 `application.yaml` 中保留的数据库、缓存、对象存储配置均已对应到现有实现，不再属于“提前堆积未来配置”
- 具体客户端依赖与配置类当前已由 `infrastructure-service/*-impl` 持有，仍应继续保持这一边界

## 8. 后续接入方向

当前外部服务接入主链路已经完成，后续应重点推进：

1. 补充更真实的外部依赖协作验证（MySQL / Redis / MinIO）
2. 继续完善启动检查、健康检查与部署验证记录
3. 在正式文档中持续同步运行边界与配置现状

这样可以保持当前 Docker 配置与既定架构边界一致，同时避免文档落后于实现状态。

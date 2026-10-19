# Docker 与外部服务

**状态：草稿（待评审）**
**读者：** 本地开发者、负责部署的人
**边界：** 本文只说明 `docker-compose.yaml` 提供的外部服务与配套脚本。应用自身不容器化（见 `operations/部署手册.md`）。

---

## 1. 定位

`docker-compose.yaml` **只提供外部依赖服务**，不包含应用容器：

```text
MySQL      业务数据库         必要
Redis      缓存               必要
MinIO      对象存储           必要
minio-init 建 bucket 的一次性任务（不在 compose up 时常驻）
Nginx      边缘入口（本地 WSS/反代），profile=edge，默认不启动
```

应用以 jar 形式跑在宿主机上，通过 `127.0.0.1` 与这些服务通信。容器网络名为 `carrypigeon-network`（bridge）。

---

## 2. 服务清单

| 服务 | 镜像 | 容器名 | 宿主端口（默认） | 数据卷 | 健康检查 |
|---|---|---|---|---|---|
| `mysql` | `mysql:8.4` | `carrypigeon-mysql` | `3306` | `carrypigeon-mysql-data:/var/lib/mysql` | `mysqladmin ping` |
| `redis` | `redis:7.4-alpine` | `carrypigeon-redis` | `6379` | `carrypigeon-redis-data:/data` | `redis-cli ping` |
| `minio` | `minio/minio:RELEASE.2025-04-08T15-41-24Z` | `carrypigeon-minio` | `9000`（API）/ `9001`（控制台） | `carrypigeon-minio-data:/data` | `GET /minio/health/live` |
| `minio-init` | `minio/mc:RELEASE.2025-04-08T15-39-49Z` | `carrypigeon-minio-init` | — | — | 以退出码 0 表示 bucket 已就绪 |
| `nginx` | `nginx:1.27-alpine` | `carrypigeon-nginx` | `80` / `443` | 挂载 `deploy/nginx/{templates,certs}` | `GET /healthz` |

固定运行参数：

- MySQL：`--character-set-server=utf8mb4`、`--collation-server=utf8mb4_unicode_ci`、`--default-time-zone=+08:00`
- Redis：`--appendonly yes` 且启用 `--requirepass`
- MinIO：`server /data --console-address ":9001"`
- 三个常驻服务均为 `restart: unless-stopped`

---

## 3. 可覆盖的环境变量

`docker-compose.yaml` 里所有值都有默认，可用环境变量覆盖：

| 变量 | 默认值 | 用途 |
|---|---|---|
| `MYSQL_DATABASE` | `carrypigeon` | 初始化时创建的库名（只建库，不建表） |
| `MYSQL_USERNAME` | `carrypigeon` | 应用使用的数据库账号 |
| `MYSQL_PASSWORD` | `carrypigeon123` | 该账号密码 |
| `MYSQL_ROOT_PASSWORD` | `root123456` | root 密码（healthcheck 也用它） |
| `MYSQL_PORT` | `3306` | 宿主机映射端口 |
| `REDIS_PASSWORD` | `carrypigeon123` | Redis 访问密码 |
| `REDIS_PORT` | `6379` | 宿主机映射端口 |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | `carrypigeon` / `carrypigeon` | MinIO 管理账号 |
| `MINIO_BUCKET` | `carrypigeon` | `minio-init` 创建的 bucket |
| `MINIO_API_PORT` / `MINIO_CONSOLE_PORT` | `9000` / `9001` | 宿主机映射端口 |
| `NGINX_SERVER_NAME` | `localhost` | 反代 server_name 与证书域名 |
| `NGINX_BACKEND_HTTP_HOST` / `_PORT` | `host.docker.internal` / `8080` | 反代到宿主机应用的 HTTP 地址 |
| `NGINX_BACKEND_WS_HOST` / `_PORT` | `host.docker.internal` / `18080` | 反代到宿主机应用的 WS 地址 |
| `NGINX_CLIENT_MAX_BODY_SIZE` | `100m` | 反代请求体上限 |
| `NGINX_BACKEND_WS_DISCOVERY_URL` / `NGINX_PUBLIC_WS_URL` | `ws://127.0.0.1:18080/api/ws` / `wss://localhost/api/ws` | 服务发现里的 ws_url 替换规则 |
| `NGINX_HTTP_PORT` / `NGINX_HTTPS_PORT` | `80` / `443` | 反代对外端口 |

> **注意**：`bin/` 下的 compose 脚本都带 `COMPOSE_DISABLE_ENV_FILE=1`，即**仓库根目录的 `.env` 不会被读取**。覆盖只能通过当前 shell 的环境变量（见 §5）。

---

## 4. 与 `application.yaml` 的对应关系

本地默认值是成对设计的，改一侧要同步另一侧：

| compose 变量 | `application.yaml` 键 | 本地默认 |
|---|---|---|
| `MYSQL_*` | `spring.datasource.{url,username,password}` | `127.0.0.1:3306/carrypigeon`、`carrypigeon/carrypigeon123` |
| `REDIS_*` | `spring.data.redis.{host,port,password}` | `127.0.0.1:6379`、`carrypigeon123` |
| `MINIO_*` | `cp.infrastructure.service.storage.{endpoint,access-key,secret-key,bucket}` | `http://127.0.0.1:9000`、`carrypigeon/carrypigeon123`、`carrypigeon` |
| —（默认关闭） | `cp.infrastructure.service.mail.*` | 空主机、`enabled: false` |
| `NGINX_*` | 不直接对应；影响 `cp.chat.server.realtime.*` 的对外地址 | 见 `operations/部署手册.md` |

数据库表结构不在 compose 里初始化，见 `operations/数据库部署.md`。

---

## 5. 常用操作

Linux / macOS：

```bash
bin/linux/docker-up.sh       # 启动并等待全部健康
bin/linux/docker-down.sh     # 停止（保留数据卷）
bin/linux/docker-reset.sh    # 停止并删除数据卷（数据全失）
bin/linux/docker-logs.sh     # 跟随日志
```

Windows（`bin/windows/` 下有同名 `.bat` 与 `.ps1`）：

```powershell
bin\windows\docker-up.bat
bin\windows\docker-down.bat
bin\windows\docker-reset.bat
bin\windows\docker-logs.bat
```

带自定义变量时（示例：换端口与密码）：

```bash
MYSQL_PORT=13306 MYSQL_PASSWORD=mysecret bin/linux/docker-up.sh
```

```powershell
$env:MYSQL_PORT='13306'; $env:MYSQL_PASSWORD='mysecret'; bin\windows\docker-up.bat
```

`docker-up` 的等待策略（脚本内固定）：mysql 120s、redis 60s、minio 120s 变为 `healthy`，`minio-init` 120s 内以退出码 0 结束；任一超时脚本非零退出。

启动 Nginx 边缘入口（默认不启动，需要 `edge` profile）：

```bash
docker compose -f docker-compose.yaml --profile edge up -d
```

---

## 6. Nginx 边缘入口

- 用途：本地模拟生产形态——HTTP 跳 HTTPS、TLS 终止、WebSocket 升级反代到宿主机 Netty 端口。
- 模板：`deploy/nginx/templates/carrypigeon.conf.template`，由官方 entrypoint 做变量替换；`NGINX_ENVSUBST_FILTER="^NGINX_"` 限制只替换 `NGINX_` 前缀变量。
- 证书：`deploy/nginx/certs/{fullchain.pem,privkey.pem}`（自签证书生成见 `deploy/nginx/certs/README.md`）。
- 健康检查：`GET /healthz` 返回 `204`，用于 compose healthcheck 与外部探测。
- 内置 Netty 实时通道**仍是明文 WS**；客户端连接 WSS 时由 Nginx 终止 TLS，再把请求转发到 `NGINX_BACKEND_WS_HOST:NGINX_BACKEND_WS_PORT`。
- 反向代理到宿主机依赖 `extra_hosts: host.docker.internal:host-gateway`（Linux 也适用）。

---

## 7. 数据卷与重置风险

| 卷 | 内容 | 删除后果 |
|---|---|---|
| `carrypigeon-mysql-data` | 全部业务数据与表结构 | 需要重新执行 `docs/sql` 脚本 |
| `carrypigeon-redis-data` | 缓存与 AOF | 缓存丢失，应用可自愈 |
| `carrypigeon-minio-data` | 对象存储文件 | 已上传文件不可恢复 |

`docker-reset.sh` / `docker-reset.bat` 执行的是 `compose down -v`，**三条卷全部删除**。只重建结构、保留数据的场景请用 `docker-down` + 手工 SQL，不要用 `reset`。

---

## 8. 排障

| 现象 | 常见原因 | 处理 |
|---|---|---|
| `docker-up` 报 `Timed out waiting for carrypigeon-mysql to become healthy` | 首次初始化慢、3306 被占用、root 密码变量被改但 healthcheck 未同步 | `docker compose logs mysql`；确认 `MYSQL_PORT`/`MYSQL_ROOT_PASSWORD` |
| 应用报 `Communications link failure` | 端口映射与 `spring.datasource.url` 不一致 | 对齐 §4 表 |
| `minio-init` 退出码非 0 | MinIO 未 healthy 或被改过账号 | `docker compose logs minio-init`，重跑 |
| 客户端 WSS 握手失败 | 未启 `edge` profile、证书缺失、`NGINX_SERVER_NAME` 与证书域名不一致 | 检查 `deploy/nginx/certs`，`docker compose --profile edge logs nginx` |
| 中文写入乱码 | 库/表字符集不是 `utf8mb4` | 见 `operations/数据库部署.md` §2 |
| 时间差 8 小时 | 容器时区或 `serverTimezone` 不一致 | 容器已是 `+08:00`，检查 `spring.datasource.url` 的 `serverTimezone` |

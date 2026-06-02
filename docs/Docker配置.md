# 项目 Docker 配置

## 1. 文档目的

本文用于固化当前项目的 Docker 外部服务配置。

当前阶段只编排外部基础服务，不提前容器化应用本身。

若需要查看完整的分发、部署、前后台启动与停止流程，请同时阅读：

- `docs/部署手册.md`
- `docs/数据库部署手册.md`
- `docs/sql/README.md`

## 2. 当前容器化范围

当前已配置的外部服务包括：

- MySQL
- Redis
- MinIO

原因：

- 这三类能力已被明确为当前项目的外部基础服务
- 具体接入代码现已进入 `infrastructure-service` 的对应 `*-api` 与 `*-impl`
- 当前仍保持“外部服务容器化、应用本身不容器化”的运行边界

## 3. 文件落点

当前 Docker 相关文件如下：

- `docker-compose.yaml`
- `.env.example`

说明：

- `docker-compose.yaml` 用于编排本地外部服务
- `.env.example` 用于提供环境变量模板

## 4. 使用方式

### 4.1 准备环境变量

先复制模板：

```bash
cp .env.example .env
```

如需修改端口、账号或密码，直接编辑 `.env`。

当前 MinIO 默认 bucket 也通过 `.env` 中的 `MINIO_BUCKET` 提供。

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

## 5. 当前服务说明

### 5.1 MySQL

镜像：

- `mysql:8.4`

职责：

- 作为当前 `database-impl` 的默认本地数据库

数据库初始化和测试数据导入方式请参考：

- `docs/数据库部署手册.md`
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

## 6. 当前环境变量

当前 `.env.example` 中已定义：

```dotenv
MYSQL_DATABASE=carrypigeon
MYSQL_USERNAME=carrypigeon
MYSQL_PASSWORD=carrypigeon123
MYSQL_ROOT_PASSWORD=root123456
MYSQL_PORT=3306

REDIS_PORT=6379
REDIS_PASSWORD=carrypigeon123

MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ROOT_USER=carrypigeon
MINIO_ROOT_PASSWORD=carrypigeon123
MINIO_BUCKET=carrypigeon
```

## 7. 设计边界

- 当前 Docker 配置只负责本地外部服务运行
- 不在当前阶段引入应用镜像构建与应用容器编排
- 当前 `application.yaml` 中保留的数据库、缓存、对象存储配置均已对应到现有实现，不再属于“提前堆积未来配置”
- 具体客户端依赖与配置类当前已由 `infrastructure-service/*-impl` 持有，仍应继续保持这一边界

## 8. 后续接入方向

当前外部服务接入主链路已经完成，后续应重点推进：

1. 补充更真实的外部依赖协作验证（MySQL / Redis / MinIO）
2. 继续完善启动检查、健康检查与部署验证记录
3. 在正式文档中持续同步运行边界与配置现状

这样可以保持当前 Docker 配置与既定架构边界一致，同时避免文档落后于实现状态。

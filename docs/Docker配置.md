# 项目 Docker 配置

## 1. 文档目的

本文用于固化当前项目的 Docker 外部服务配置。

当前阶段只编排外部基础服务，不提前容器化应用本身。

## 2. 当前容器化范围

当前已配置的外部服务包括：

- MySQL
- Redis
- MinIO

原因：

- 这三类能力已被明确为当前项目的外部基础服务
- 具体接入代码后续应进入 `infrastructure-service` 的对应 `*-api` 与 `*-impl`
- 当前先提供稳定的本地运行环境，避免在服务实现尚未落地前提前绑定应用容器结构

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

### 4.2 启动外部服务

```bash
docker compose up -d
```

### 4.3 停止外部服务

```bash
docker compose down
```

### 4.4 连同数据卷一起清理

```bash
docker compose down -v
```

## 5. 当前服务说明

### 5.1 MySQL

镜像：

- `mysql:8.4`

职责：

- 作为后续数据库 `database-impl` 的默认本地数据库

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

- 作为后续缓存 `cache-impl` 的默认本地缓存服务

当前约定：

- 开启 AOF 持久化
- 启用密码保护

默认端口：

- `6379`

### 5.3 MinIO

镜像：

- `minio/minio:RELEASE.2025-04-08T15-41-24Z`

职责：

- 作为后续对象存储服务的默认本地实现

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
```

## 7. 设计边界

- 当前 Docker 配置只负责本地外部服务运行
- 不在当前阶段引入应用镜像构建与应用容器编排
- 不提前在 `application.yaml` 中堆积尚未接入实现的数据库、缓存、对象存储配置
- 后续接入实现时，应由对应的 `infrastructure-service/*-impl` 持有具体客户端依赖与配置类

## 8. 后续接入方向

后续如开始实现外部服务模块，建议按以下顺序推进：

1. 新增数据库、缓存、对象存储的 `*-api`
2. 新增对应 `*-impl`
3. 在 `application-starter` 中完成装配
4. 再补充应用侧配置项与接入文档

这样可以保持当前 Docker 配置与既定架构边界一致，不提前把运行结构写死。

任务名称：Docker 健康检查环境变量修复

任务目标：修复本地 Docker Redis 健康检查因容器内缺少 `REDIS_PASSWORD` 导致一直 unhealthy 的问题，并排查 MinIO bucket 健康检查失败原因。

任务背景：用户执行 Windows `docker-up.ps1` 时 Redis 等待 healthy 超时；应用启动阶段 database/cache 已通过，storage bucket 健康检查失败。

影响模块：Docker Compose 本地外部服务配置。

允许修改范围：仅允许修改 `docker-compose.yaml` 中外部服务运行所需环境变量透传配置。

禁止修改范围：不修改 Java 业务逻辑，不调整模块依赖，不新增第三方依赖，不改变服务端口和默认账号语义。

依赖限制：不新增依赖。

配置限制：仅补充当前已被容器健康检查实际读取的环境变量。

文档依据：`AGENTS.md`、`docs/operations/Docker配置.md`、`docs/standards/变更审核清单.md`。

任务分解 / 执行计划：
1. 检查 Redis 容器健康日志和容器内环境变量。
2. 修复 compose 中 Redis 健康检查所需环境变量透传。
3. 核对 MinIO init 与应用 storage 健康检查配置。
4. 执行 compose config 静态验证并给出重建命令。

关键假设与依赖：Redis 服务密码本身正确，失败原因是 healthcheck 使用的容器环境变量缺失。

实现要求：保持已有默认密码、端口、volume 和网络语义不变。

测试要求：执行 compose config 静态验证；可通过 Docker inspect/exec 证明现有故障原因。

质量门禁：compose 渲染后 Redis service 包含 `REDIS_PASSWORD` 环境变量，健康检查命令仍引用 `$$REDIS_PASSWORD`。

复审要求：确认修改不影响 MySQL、MinIO 默认行为。

文档要求：本次不引入长期规则，暂不修改 `docs/`。

验收标准：重新创建 Redis 容器后 `carrypigeon-redis` 能进入 healthy。

完成定义：配置修复完成、验证完成、任务单改名为 `done`。

实际结果：待填写。

验证记录：待填写。

残留风险：待填写。

知识沉淀 / 是否回写 docs：待填写。

产物清理与保留说明：保留任务单作为协作追踪材料。

补充说明：无。

## 执行结果补充

实际结果：已在 `docker-compose.yaml` 的 Redis 服务中补充 `REDIS_PASSWORD` 环境变量，保证容器内 healthcheck 使用的 `$$REDIS_PASSWORD` 与 Redis `--requirepass` 配置一致。

验证记录：
- `docker inspect carrypigeon-redis --format '{{json .State.Health}}'`：确认旧容器 healthcheck 使用空密码，报 `AUTH failed: WRONGPASS`。
- `docker exec carrypigeon-redis ... redis-cli -a carrypigeon123 ping`：确认 Redis 服务本身可用，返回 `PONG`。
- `docker compose --env-file .env -f docker-compose.yaml config`：确认渲染后 Redis service 包含 `REDIS_PASSWORD: carrypigeon123`，healthcheck 仍引用 `$$REDIS_PASSWORD`。
- MinIO 排查确认当前运行容器使用旧账号 `carrygieon/carrygieon`，而当前 `.env` 渲染为 `carrypigeon/carrypigeon123`，storage 初始化失败需要重建 MinIO 容器/volume。

残留风险：当前环境中运行中的旧 Redis/MinIO 容器不会因为 compose 文件修改自动变更，需要用户执行重建命令。

知识沉淀 / 是否回写 docs：不新增长期规则，不回写 `docs/`。

产物清理与保留说明：任务单保留在 `ai-agent-workplace/`，并关闭为 `done`。

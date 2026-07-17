任务名称：本地 .env 配置整理

任务目标：整理用户本地 `.env`，去除重复项并补齐本地脚本、Docker Compose 与应用启动实际读取的配置。

任务背景：本地启动过程中发现 `.env` 中 Redis 与 MinIO 配置重复，且缺少部分 `.env.example` 中定义的运行参数，容易导致容器重建和应用启动配置不一致。

影响模块：本地环境配置文件。

允许修改范围：仅允许修改仓库根目录 `.env`。

禁止修改范围：不修改 `.env.example`、业务代码、模块依赖、Docker 服务语义或正式文档。

依赖限制：不新增依赖。

配置限制：只保留当前已被脚本、Compose 或应用真实读取的配置，不增加未来占位配置。

文档依据：`AGENTS.md`、`docs/standards/配置规范.md`、`docs/operations/Docker配置.md`、`docs/standards/变更审核清单.md`。

任务分解 / 执行计划：
1. 对比 `.env` 与 `.env.example`。
2. 按 `.env.example` 的结构整理 `.env`。
3. 保留当前本地 JWT secret 和已验证可用的外部服务账号密码。
4. 检查重复键与 compose 渲染结果。

关键假设与依赖：当前本地默认外部服务使用 `carrypigeon/carrypigeon123`，JWT secret 使用本地开发值。

实现要求：保持配置清晰、单键单值、与启动脚本读取项一致。

测试要求：执行重复键检查和 `docker compose config` 渲染检查。

质量门禁：`.env` 不存在重复键；compose 渲染出的 MySQL、Redis、MinIO 关键配置与 `.env` 一致。

复审要求：确认没有误删当前启动需要的配置。

文档要求：不修改长期文档。

验收标准：`.env` 可直接被 Windows 启动脚本和 Docker Compose 使用。

完成定义：`.env` 整理完成、验证完成、任务单改名为 `done`。

实际结果：待填写。

验证记录：待填写。

残留风险：待填写。

知识沉淀 / 是否回写 docs：待填写。

产物清理与保留说明：保留任务单作为协作追踪材料。

补充说明：无。

## 执行结果补充

实际结果：已将 `.env` 按 `.env.example` 的结构整理为单份本地运行配置，保留当前本地开发 JWT secret 和已验证可用的 MySQL、Redis、MinIO 默认账号密码，补齐 host、端口、服务开关、雪花 ID 和邮件空值配置。

验证记录：
- `awk -F= ... .env`：无重复键输出。
- `docker compose --env-file .env -f docker-compose.yaml config`：MySQL、Redis、MinIO 渲染配置与 `.env` 一致；Redis 包含 `REDIS_PASSWORD: carrypigeon123`，MinIO 为 `carrypigeon/carrypigeon123`。

残留风险：`.env` 属于本地环境文件，未显示在 git diff 中；后续如手动修改需避免重复键。

知识沉淀 / 是否回写 docs：不引入长期规则，不回写 `docs/`。

产物清理与保留说明：任务单保留在 `ai-agent-workplace/`，并关闭为 `done`。

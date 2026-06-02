任务名称：
按领域合并数据库 SQL

任务目标：
在不破坏现有 Flyway 历史迁移链的前提下，将数据库 SQL 额外整理为按领域划分的聚合脚本，而不是仅保留 `V1`、`V2` 这类迁移文件视角。

影响模块：
- `docs`
- `README.md`
- `ai-agent-workplace`

允许修改范围：
- 允许新增面向部署和阅读的领域聚合 SQL 文件
- 允许新增领域 SQL 目录说明文档
- 允许更新数据库部署文档与 README 入口
- 允许更新当前任务单

禁止修改范围：
- 不修改现有 Flyway 迁移文件内容
- 不重排或替换 `db/migration/` 的版本链
- 不修改业务代码

文档依据：
- `AGENTS.md`
- `docs/数据库部署手册.md`
- `docs/部署手册.md`
- `docs/任务单模板.md`

执行计划：
1. 从现有 Flyway 迁移推导最终数据库结构。
2. 按领域输出聚合 SQL，而不是按迁移版本输出。
3. 在文档中明确这些脚本的用途和边界。
4. 自检并收口任务单。

验收标准：
- 仓库中新增按领域划分的 SQL 聚合脚本。
- 使用者可以不逐个阅读 `V1...V18` 就理解或初始化数据库。
- 文档明确这些脚本不替代 Flyway 升级链。

实际结果：
- 已新增 `docs/sql/` 目录，提供按领域划分的数据库聚合 SQL，而不是按 `V1`、`V2` 迁移版本划分。
- 已新增：
  - `docs/sql/00-all-in-one.sql`
  - `docs/sql/README.md`
  - `docs/sql/01-auth.sql`
  - `docs/sql/02-user.sql`
  - `docs/sql/03-channel.sql`
  - `docs/sql/04-message.sql`
  - `docs/sql/05-notification.sql`
- 已更新 `docs/数据库部署手册.md`，明确区分：
  - Flyway 升级链
  - 面向空库初始化和阅读的领域聚合 SQL
- 已更新 `README.md`、`docs/部署手册.md`、`docs/Docker配置.md` 的入口链接。

验证记录：
- 已人工复核：
  - `docs/sql/README.md`
  - `docs/sql/00-all-in-one.sql`
  - `docs/sql/*.sql`
  - `docs/数据库部署手册.md`
  - `README.md`
  - `docs/部署手册.md`
  - `docs/Docker配置.md`
- 已确认文档中明确声明：
  - 领域聚合 SQL 仅用于空库初始化和结构阅读
  - 现有数据库升级仍继续依赖 Flyway
- 本次为文档与 SQL 聚合产物整理，未运行自动化测试。

残留风险：
- 这些聚合 SQL 是根据当前 `V1` - `V18` 迁移推导出的最终结构快照；后续只要迁移链继续演进，就需要同步维护 `docs/sql/`。
- 聚合 SQL 适用于空库初始化，不适用于已有数据库的增量升级。

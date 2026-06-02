任务名称：
移除 `db/migration` 并保留领域 SQL

任务目标：
移除 `application-starter/src/main/resources/db/migration/` 下的版本迁移 SQL，仅保留按领域划分的结构 SQL，并补一份可重复执行的测试数据 SQL。

影响模块：
- `application-starter`
- `infrastructure-service/database-impl`
- `docs`
- `ai-agent-workplace`

允许修改范围：
- 允许删除 `db/migration/` 下的迁移 SQL
- 允许调整运行配置与依赖，移除 Flyway 默认初始化入口
- 允许补充领域 SQL 文档与测试数据 SQL
- 允许更新当前任务单

禁止修改范围：
- 不修改业务 Java 代码行为
- 不新增新的数据库中间件
- 不引入新的初始化机制，仍以仓库内 SQL 文档为准

文档依据：
- `AGENTS.md`
- `docs/数据库部署手册.md`
- `docs/部署手册.md`
- `docs/任务单模板.md`

执行结果：
1. 删除了 `application-starter/src/main/resources/db/migration/` 下全部历史迁移 SQL。
2. 从 `application.yaml` 中移除了 Flyway 默认扫描配置。
3. 从 `infrastructure-service/database-impl/pom.xml` 中移除了 Flyway 依赖。
4. 重写了 `docs/数据库部署手册.md`，改为以 `docs/sql/` 下的整库 SQL、领域 SQL、测试数据 SQL 为主。
5. 更新了 `docs/sql/README.md`、`docs/部署手册.md`、`docs/Docker配置.md` 的数据库初始化说明。
6. 新增 `docs/sql/10-test-data.sql`，覆盖账号、频道、消息、mention、已读、置顶、通知等联调样本数据。

验收结果：
- 仓库中不再保留 `db/migration/` 目录下的迁移 SQL。
- 数据库初始化入口只剩 `docs/sql/` 下的结构 SQL 与测试数据 SQL。
- 提供了一份可直接导入、可重复执行的测试数据 SQL。
- 文档不再把 Flyway 作为默认初始化方式。

验证记录：
- 通过全文检索确认 `application.yaml`、`database-impl/pom.xml`、`docs/` 不再残留 Flyway 作为初始化主路径的说明。
- 使用 Spring Security Argon2PasswordEncoder 生成了测试账号真实密码摘要，避免测试数据只能插入不能登录。
- 运行 `git diff --check`，当前改动未引入空白符错误。
- 尚未在当前环境执行完整 Maven 测试；本次改动集中在资源、依赖与文档层。

残留风险：
- 若外部使用者已有依赖旧版 Flyway 迁移链的私有部署脚本，需要改为执行 `docs/sql/00-all-in-one.sql` 或 `01` 到 `05` 的领域 SQL。
- 当前挂载环境对空目录删除返回只读错误；Git 不会保留空目录，因此仓库内容层面已完成移除，但工作区里可能仍暂留一个空的 `application-starter/src/main/resources/db/migration/` 目录。

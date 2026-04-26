任务名称：
thin-jar 分发模式改造

任务目标：
将当前基于 Spring Boot `exec` fat jar 的分发模式，设计并后续实施为 **thin jar + libs 目录** 交付模式，使内部模块与第三方依赖以独立 jar 形式分发，提升模块替换与交付结构透明度。

任务背景：
当前 `distribution` 模块采用 `application-starter-*-exec.jar + 配置文件` 的交付方式。该模式适合统一交付，但不利于替换内部模块（如 `chat-domain`、`database-impl`、`cache-impl`、`storage-impl`），因为这些模块会被打进单一可执行 fat jar 中。用户明确希望设计一份任务单，将分发模式调整为 thin jar + libs 目录模式。

影响模块：
- `distribution`
- `application-starter`
- 根 `pom.xml`
- 可能涉及 `docs/架构文档.md`
- 可能涉及 `AGENTS.md`
- 可能涉及 `readme.md`
- `ai-agent-workplace`

允许修改范围：
- `distribution/pom.xml`
- `distribution/src/assembly/dist.xml`
- 根 `pom.xml`
- `application-starter/pom.xml`
- 必要的启动脚本（如后续决定新增 `bin/start.sh` / `bin/start.bat`）
- 与分发模式相关的正式文档
- `ai-agent-workplace/` 内任务单与分析材料

禁止修改范围：
- 不改变业务模块职责
- 不改变 `chat-domain` / `infrastructure-basic` / `infrastructure-service` 的依赖方向
- 不借机重构无关业务代码
- 不把 thin-jar 改造扩大成容器化改造或部署体系重做

依赖限制：
- 优先使用 Maven 现有能力（assembly / dependency copy / jar packaging）
- 除非必要，不新增新的打包框架依赖
- 若确需新增插件或辅助工具，必须说明其必要性和边界

配置限制：
- 保持当前 `application.yaml` 作为主运行配置入口
- 不新增未来占位配置
- 新增启动脚本时只处理当前实际运行所需参数，不引入复杂环境编排

文档依据：
- `AGENTS.md`
- `docs/架构文档.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `ai-agent-workplace/20260425-231330-ai-remediation-implementation-done.md`

任务分解 / 执行计划：
1. 盘点当前分发链路：根 reactor、`distribution`、`application-starter`、assembly 规则、当前产物结构。
2. 设计 thin jar 模式的目标产物结构，例如：
   - `app/application-starter-<version>.jar`
   - `libs/*.jar`
   - `config/application.yaml`
   - `config/log4j2-spring.xml`
   - `bin/start.sh`（如需要）
3. 确认 `application-starter` 不再作为 fat jar 分发主入口，而是保留 thin jar 供 classpath 启动。
4. 设计 `distribution` 的 assembly / dependency copy 规则：
   - 如何放置内部模块 jar
   - 如何放置第三方依赖 jar
   - 如何避免遗漏 `database-impl` / `cache-impl` / `storage-impl`
5. 设计启动方式：
   - 使用 `java -cp "app/*.jar:libs/*" ...` 还是 manifest/classpath 脚本方式
   - Unix / Windows 的最小支持范围
6. 设计验证方案：
   - `mvn -pl distribution -am package`
   - 校验产物目录结构
   - 实际 thin-jar 启动验证（至少本地命令级）
7. 若规则已稳定，回写正式文档并更新模块说明。

关键假设与依赖：
- 当前交付模式允许从“fat jar 主体”切换为“thin jar + libs”而不改变业务模块边界。
- `application-starter` 仍然是启动入口，只是分发方式改变，不是运行职责改变。
- 内部模块 jar 的显式暴露是用户期望，不视为架构越界，而是交付形式变化。

实现要求：
- 新分发模式必须能明确看出：哪些是业务模块 jar，哪些是第三方 libs。
- 应支持单独替换内部模块 jar，而不需要重打整个 fat jar。
- 分发目录结构要稳定、可文档化、可脚本化。
- 启动方式要足够简单，不引入新的运维复杂度。

测试要求：
- 至少验证 `distribution` 打包成功。
- 至少验证 thin jar 启动命令可实际拉起应用。
- 若引入启动脚本，需验证脚本能正确组装 classpath。
- 若调整 `application-starter` 打包方式，需确保不破坏现有 Maven 测试链路。

质量门禁：
- `mvn test -DskipTests=false` 不因分发模式改造而退化。
- `mvn clean install -DskipTests=false` 仍能通过。
- `distribution` 产物结构与任务单设计一致。
- 至少有一次 thin-jar 实际启动成功证据。

复审要求：
- 需要重点复审以下方面：
  - 是否真的达成“模块可替换”目标
  - 是否引入了新的类路径顺序风险
  - 是否破坏了当前 CI 与本地运行命令
  - 是否与现有文档描述一致

文档要求：
- 若最终实施，需更新：
  - `docs/架构文档.md`
  - `AGENTS.md`
  - `readme.md`
  - 如有需要，新增分发/启动说明文档

验收标准：
- 分发产物改为 thin jar + libs 目录模式。
- `application-starter` 可通过 classpath 方式正常启动。
- 内部模块 jar 可在分发目录内独立替换。
- 文档与实际交付结构一致。

完成定义：
- 任务单设计完整，可直接进入实施阶段。
- 已明确影响范围、关键风险、验证方式和完成标准。

实际结果：
- 已将 `distribution` 分发模式从单一 `exec` fat jar 交付主导，调整为 **thin jar + libs + config + bin** 的主分发结构。
- `distribution/src/assembly/dist.xml` 已改为：
  - `app/` 放置 `application-starter-*.jar` thin jar
  - `libs/` 放置内部模块与第三方依赖 jar
  - `config/` 放置 `application.yaml` 与 `log4j2-spring.xml`
  - `bin/` 放置前台启动、后台启动、停止脚本
- 已新增：
  - `distribution/src/bin/start.sh`
  - `distribution/src/bin/start.bat`
  - `distribution/src/bin/start-background.sh`
  - `distribution/src/bin/stop.sh`
  - `distribution/README.md`
- 已新增项目级命令入口：
  - `bin/dist-package.sh`
  - `bin/dist-start.sh`
  - `bin/dist-start-background.sh`
  - `bin/dist-stop.sh`
  - `bin/docker-up.sh`
  - `bin/docker-down.sh`
  - `bin/docker-reset.sh`
  - `bin/docker-logs.sh`
- 已同步更新 `AGENTS.md`、`readme.md`、`docs/架构文档.md`、`docs/Docker配置.md` 以反映新的分发和命令使用方式。
- 为了让数据库禁用场景下的 thin-jar 最小启动真正成立，已将 `application-starter` 的 MyBatis mapper 扫描改成条件化配置，并移除了 `database-impl` mapper 接口上的重复 `@Mapper` 标注来源。

验证记录：
- `mvn -pl distribution -am package -Dmaven.test.skip=true`：成功。
- `mvn test -DskipTests=false`：成功。
- `mvn clean install -DskipTests=false`：成功。
- `sh -n bin/*.sh distribution/src/bin/*.sh`：成功。
- thin-jar 实际启动成功证据：
  - 使用 `distribution/target/full-distribution/full-distribution/app/application-starter-1.0.0.jar`
  - 通过 `distribution/.../bin/start.sh` 以 classpath 方式启动成功
  - 启动命令中显式排除了 JDBC / Flyway / MyBatis 自动配置，并禁用了数据库/缓存/存储服务，用于证明 thin 分发链路本身可运行
  - 启动日志出现 `Started ApplicationStarter ...` 且进入 `Application is running ...`

残留风险：
- thin jar 模式可能带来 classpath 顺序、脚本兼容性、运行入口复杂度上升等问题。
- 当前分发目录根部仍保留一个 `application-starter-*-exec.jar` 兼容产物，主模式已切到 thin-jar，但双产物并存仍需长期是否保留的产品/运维决策。
- thin-jar 启动成功证据目前基于“禁用外部服务 + 排除数据库相关自动配置”的最小启动场景；若要证明完整生产依赖链路的 thin 启动，还需在具备 MySQL/Redis/MinIO 的环境里进一步验证。

知识沉淀 / 是否回写 docs：
- 已回写 `AGENTS.md`、`readme.md`、`docs/架构文档.md`、`docs/Docker配置.md` 的相关说明。

产物清理与保留说明：
- 任务单已改为 `done`。
- 暂保留在 `ai-agent-workplace/` 根目录，便于当前阶段追溯；后续可按规则归档。

补充说明：
- 推荐优先确认的设计决策：
  1. 是否保留 `application-starter` 的 fat jar 作为兼容产物
  2. thin jar 的目录结构是否固定为 `app/ + libs/ + config/ + bin/`
  3. 启动方式是 shell/bat 脚本主导，还是仅提供命令模板
  4. `distribution` 是否只承担“组装目录”职责，而不再声明多余模块依赖

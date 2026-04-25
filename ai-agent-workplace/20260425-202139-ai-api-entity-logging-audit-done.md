任务名称：
API 汇总、实体 Lombok 化与日志审查

任务目标：
基于实际代码与文档，补充对外接口文档 `docs/API.md`，审查日志系统完整性与一致性，并在获得必要边界确认后将 `database-impl` 下手写 getter/setter 的实体改为 Lombok 生成实现。

任务背景：
当前仓库缺少统一的对外接口文档；`infrastructure-service/database-impl` 下多个 MyBatis/MyBatis-Plus 实体仍保留大量手写 getter/setter 样板代码；日志系统虽已集中在 Log4j2，但从代码与配置证据看仍存在一致性和完备性待核对项。

影响模块：
- `docs`
- `chat-domain`
- `infrastructure-basic`
- `infrastructure-service/database-impl`
- `ai-agent-workplace`

允许修改范围：
- 新增或更新 `docs/API.md`
- 在 `ai-agent-workplace/` 下新增与维护本任务任务单
- 在不突破既有架构边界的前提下，修正日志系统实现或配置缺口
- 在获得必要确认后，仅修改 `infrastructure-service/database-impl` 下目标实体与其模块 POM

禁止修改范围：
- 不修改模块职责与依赖方向
- 不把 `*-impl` 暴露给 `chat-domain`
- 不引入新的架构模式
- 不新增未来占位配置
- 不无依据改动未覆盖的业务逻辑

依赖限制：
- 仅使用项目既有基线能力：Spring Boot、Lombok、Log4j2、MyBatis-Plus
- 若 `database-impl` 需要新增 Lombok 依赖，视为模块依赖变更，需记录并取得用户确认后继续该子任务

配置限制：
- 保持配置最小化
- 配置前缀继续使用既有 `cp`
- 日志相关配置仅在现有 Log4j2 体系内修正，不引入新日志框架

文档依据：
- `AGENTS.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/任务单模板.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/配置规范.md`
- `docs/异常与错误码规范.md`
- `docs/测试规范.md`
- `docs/基建文档.md`

任务分解 / 执行计划：
1. 基于控制器、DTO、统一响应模型和测试梳理对外 HTTP / WebSocket 接口事实。
2. 创建 `docs/API.md`，先写全局约定，再按接口分组汇总路径、方法、鉴权、请求、响应与错误语义。
3. 审查日志系统：核对 `log4j2-spring.xml`、日志调用方式、MDC 上下文、打包引用与相关测试。
4. 形成日志审查结论；若发现缺口，在既有边界内最小修复并补充必要验证。
5. 确认实体 Lombok 化边界：当前已确认 `database-impl` 无 Lombok 依赖，需先等待用户确认是否允许修改该模块依赖。
6. 若获得确认，则对 `database-impl` 下目标实体进行 Lombok 化替换，并保持 MyBatis-Plus 映射与行为不变。
7. 运行与改动匹配的诊断、测试、构建，补充复审与结果记录。

关键假设与依赖：
- 已确认 `docs/API.md` 当前不存在，需要新建。
- 已确认公开 HTTP API 主要位于 `chat-domain` 的 6 个控制器中，另有 `/ws` 自定义 WebSocket 外部接口。
- 已确认 `database-impl` 下 9 个实体仅包含平凡 getter/setter，无自定义访问器逻辑。
- 已确认 `database-impl/pom.xml` 当前未声明 Lombok 依赖；若继续实体 Lombok 化，则属于需确认的范围扩大点。
- 已确认日志主配置位于 `infrastructure-basic/src/main/resources/log4j2-spring.xml`。

实现要求：
- API 文档必须来源于实际控制器、DTO、统一响应模型和测试，不得凭空扩写协议
- 日志审查必须区分“代码/配置证据已确认”与“未做运行时验证”
- 实体 Lombok 化只替换样板访问器，不顺带重构其它结构
- 对实体优先使用 `@Getter` / `@Setter` 这类保守注解；若采用 `@Data`，必须显式评估 `equals/hashCode/toString` 风险

测试要求：
- 至少执行与文档、日志、持久化改动匹配的诊断与测试
- 若日志实现发生改动，优先补充或运行 `infrastructure-basic` 相关测试
- 若实体改动落地，至少运行 `database-impl` 相关测试与受影响上游模块测试

质量门禁：
- 改动文件 LSP 诊断无新增错误
- 相关 Maven 模块测试通过
- 至少完成一次受影响范围的 Maven 验证
- 对外接口文档内容与实际代码一致
- 任务结果通过 `docs/变更审核清单.md` 的边界、依赖、配置、异常、测试、AI 工作目录检查

复审要求：
- 本任务涉及对外接口文档、日志系统和数据持久化实体，属于跨模块且协议/持久化敏感改动，需要深度自检
- 若发生日志实现修复或实体 Lombok 化，完成后进行独立复审（至少再次阅读受改文件并复验）

文档要求：
- 必须新增或更新 `docs/API.md`
- 若发现需要沉淀的长期规则，才回写 `docs/`；否则仅在任务单中记录任务级结论

验收标准：
- `docs/API.md` 能覆盖当前实际对外 HTTP 接口与 `/ws` WebSocket 接口的主要约定
- 日志系统审查结果明确说明完整项、缺口项、是否修复及证据
- 若获得依赖变更确认，则 9 个 `database-impl` 实体完成 Lombok 化且通过验证

完成定义：
- 任务单已补齐实际结果、验证记录、残留风险、清理说明
- 所有已确认范围内的改动完成并通过质量门禁
- 若实体 Lombok 化因确认门槛暂停，需在结果中显式标记为外部阻塞，而非默认完成

实际结果：
- 待执行

验证记录：
- 待执行

残留风险：
- `database-impl` 的 Lombok 化目前被“模块依赖变更需确认”规则阻塞
- 日志系统运行时行为仍需以实际测试/启动验证补充证据

知识沉淀 / 是否回写 docs：
- 目前预计仅新增 `docs/API.md`
- 若本次审查形成新的长期日志或接口规范，再决定是否回写 `docs/`

产物清理与保留说明：
- 保留当前任务单于 `ai-agent-workplace/`
- 任务完成后改名为 `done`；如需归档，再按规范移入 `ai-agent-workplace/archive/`

补充说明：
- 当前阶段优先推进不需要额外确认的 API 文档与日志审查工作。

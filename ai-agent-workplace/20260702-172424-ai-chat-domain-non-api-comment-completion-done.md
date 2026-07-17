任务名称：

chat-domain 非 API 主源码注释依次补全

任务目标：

在上一轮 `domain/api` 接口注释补全之后，继续对 `chat-domain` 主源码中其它关键类、嵌套类型、controller 入口、领域 API 实现、领域端口、支撑类与适配类补充规范 JavaDoc，使非 API 代码的注释覆盖达到当前注释规范要求。

任务背景：

只读扫描显示，除 `domain/api` 外，`chat-domain/src/main/java` 中仍存在类级 JavaDoc 和 public/protected 方法 JavaDoc 缺口。缺口主要集中在 controller endpoint、`domain/service/*DomainApi` 实现、抽象支撑类、领域端口嵌套模型、projection/DTO 嵌套 record 和 realtime 支撑类。

影响模块：

- `chat-domain`
- `ai-agent-workplace`

允许修改范围：

- `chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/**`
- 当前任务单

禁止修改范围：

- 不修改业务行为、方法签名、字段、返回类型、异常语义、响应字段或错误码。
- 不新增、删除或移动类。
- 不新增第三方依赖。
- 不修改 Maven 模块结构。
- 不修改运行时配置。
- 不做无关格式化或全仓整理。
- 不回滚工作区内既有无关改动。

依赖限制：

- 不引入任何新依赖。
- 只使用 JavaDoc 和必要的普通注释。

配置限制：

- 不新增、不修改配置。

文档依据：

- `AGENTS.md`
- `docs/standards/注释规范.md`
- `docs/architecture/包结构规范.md`
- `docs/standards/AI协作开发规范.md`
- `docs/standards/变更审核清单.md`
- `docs/standards/任务单模板.md`

任务分解 / 执行计划：

1. 保存并复核非 API 注释缺口扫描结果。
2. 第一批补齐类级缺口：嵌套 DTO/record、领域端口嵌套模型、projection 嵌套模型、realtime 存储事件模型。
3. 第二批补齐 controller 公开入口方法注释。
4. 第三批补齐 `domain/service/*DomainApi` 实现类公开方法注释。
5. 第四批补齐重要抽象支撑类、领域端口实现、support 适配类公开或受保护方法注释。
6. 对简单 getter/setter、record 访问器、常量字段等按 `docs/standards/注释规范.md` 允许豁免，不机械补无信息注释；但关键字段或状态语义需补充字段级注释。
7. 运行注释扫描，确认必须补齐的缺口已收敛。
8. 运行 `mvn -pl chat-domain -am test-compile`。
9. 更新任务单实际结果、验证记录、残留风险并归档。

关键假设与依赖：

- 本次只补注释，不改行为。
- 严格脚本扫描会把 getter/setter、record 访问器、构造器和框架 override 方法计入缺口；这些并不全部等同于规范缺陷。
- 本任务按“有业务边界或副作用的方法必须补齐”执行，避免为了数字归零制造低质量注释。

实现要求：

- 注释必须说明职责、边界、输入、输出、副作用、约束或失败语义中的有效信息。
- 禁止只写“构造方法”“返回结果”“处理请求”这类无信息注释。
- controller endpoint 注释应说明协议入口职责、认证主体来源和返回语义。
- `*DomainApi` 实现公开方法注释应和接口契约一致，但可以补充实现侧副作用。
- 抽象支撑类 protected 方法注释应说明复用规则、失败语义和调用约束。
- DTO/projection 嵌套 record 注释应说明该嵌套结构在协议或投影中的语义。

测试要求：

- 必须运行 `mvn -pl chat-domain -am test-compile`。
- 如意外触及非注释行为，必须运行 `mvn -pl chat-domain -am test -DskipTests=false`。

质量门禁：

- 类级 JavaDoc 缺口中的嵌套业务类型全部补齐。
- controller endpoint、`domain/service/*DomainApi` 公开方法和关键 support/port 方法具备方法级 JavaDoc。
- `mvn -pl chat-domain -am test-compile` 通过。
- 任务单记录严格扫描结果与按规范豁免项。

验收标准：

- 非 API 关键类与关键方法注释完整。
- 不新增业务改动。
- 编译验证通过。
- 任务单归档为 `done`。

实际结果：

- 已补齐非 API 主源码中的关键类级 JavaDoc 缺口，包含内部嵌套 record、私有辅助类型、消息发布上下文、文件/语音 payload、验证码条目、错误描述器等。
- 已补齐 controller 入口方法、`domain/service/*DomainApi` 实现公开方法、`AbstractChannelDomainSupport` 和 `AbstractMessageDomainSupport` 受保护方法、配置绑定 setter 等关键方法注释。
- 注释内容按业务语义补充职责、边界、输入、输出、副作用、约束或失败语义，没有修改业务逻辑、方法签名、字段、依赖、配置或模块结构。
- 准确类级扫描结果：`accurate_class_missing 0`。
- 非 API 方法级扫描结果：`method_missing_candidates 0`。

验证记录：

- 已运行：`mvn -pl chat-domain -am test-compile`。
- 结果：通过，Reactor 中 `Backend`、`infrastructure-basic`、`infrastructure-service`、`database-api`、`storage-api`、`cache-api`、`mail-api`、`chat-domain` 均为 `SUCCESS`。
- 说明：编译期间出现 javac 关于注解处理器自动启用的提示，为当前编译环境提示，不是本次注释变更导致的失败。

残留风险：

- 本次未运行完整单元测试，只运行 `test-compile`；原因是本次为注释-only 变更，未触及行为、协议或测试断言。
- 仓库工作区存在大量既有重构改动和未跟踪文件，本任务未回滚、未整理这些无关状态。
- 简单脚本扫描会对注解、多行 record、getter/setter 和继承方法产生误判；最终采用更准确扫描和人工语义判断结合。

知识沉淀 / 是否回写 docs：

- 本次没有新增长期规范，仅按既有 `docs/standards/注释规范.md` 执行，不需要回写 `docs/`。

产物清理与保留说明：

- 保留本任务单用于追踪本次注释补全闭环。
- 完成后将任务单由 `current` 归档为 `done`。

任务名称：
增强 swagger 错误响应示例

任务目标：
为当前 Swagger / OpenAPI 文档补充统一的客户端可读错误响应示例，使前端可以直接理解 `200/300/404/500` 业务码对应的常见失败场景。

任务背景：
当前 Swagger 已具备分组、摘要、鉴权展示、统一响应说明和关键 DTO 字段级文档，但对失败场景仍主要停留在响应码语义说明层，缺少可直接参考的示例 JSON。

影响模块：
- `application-starter`
- 视需要最小化触碰 `chat-domain` 注解层

允许修改范围：
- 允许修改 `application-starter/src/main/java/team/carrypigeon/backend/starter/config/OpenApiConfiguration.java`
- 允许最小化调整现有控制器 OpenAPI 注解以挂接共享错误响应示例
- 允许更新任务单和必要文档说明

禁止修改范围：
- 不允许改变现有业务逻辑与异常映射语义
- 不允许引入新依赖
- 不允许为每个操作机械复制大量重复 `@ApiResponse`

依赖限制：
- 仅使用当前已引入的 springdoc / swagger annotation 能力

配置限制：
- 优先不新增运行时配置

文档依据：
- `AGENTS.md`
- `docs/API.md`
- `docs/异常与错误码规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

任务分解 / 执行计划：
1. 读取统一响应和异常映射，确认共享错误示例内容。
2. 在 OpenAPI 装配层注册共享错误响应示例。
3. 将共享错误响应尽量以低重复方式挂到高价值操作或路径组。
4. 运行完整回归验证。
5. Oracle 复审并收尾。

关键假设与依赖：
- `CPResponse` 仍是所有 HTTP 接口统一响应包装
- `GlobalExceptionHandler` 仍是稳定错误码映射真相来源

实现要求：
- 优先共享化、低重复
- 示例必须符合当前真实响应语义
- 不为了文档漂亮而伪造不存在的错误结构

测试要求：
- 受影响 reactor 测试通过

质量门禁：
- Maven 完整 reactor 测试通过
- 不破坏现有 OpenAPI 鉴权与 DTO 文档增强

复审要求：
- 需做一轮只读复审，确认错误示例不误导客户端

文档要求：
- 如必要，可在 `docs/API.md` 中补一条“Swagger 已提供常见错误示例”的说明

验收标准：
- Swagger 中可看到常见错误业务码示例
- 错误示例与当前运行时响应保持一致

完成定义：
- 代码增强完成
- 回归验证完成
- 任务单改为 `done`

实际结果：
- 在 `application-starter` 的 `OpenApiConfiguration` 中增加了共享业务响应示例
- Swagger 现在会在单个 HTTP `200` 响应下展示成功、参数错误、认证/权限失败、资源不存在、内部错误等示例负载
- 错误示例与当前 `CPResponse` / `GlobalExceptionHandler` 真实语义保持一致，不再误导客户端认为会返回 HTTP 300/404/500

验证记录：
- `mvn -pl "application-starter" -am -DskipTests=false test`：通过
- Oracle 只读复审：指出初版错误示例错误地占用了 OpenAPI 的 `300/404/500` HTTP 响应槽位；随后已修正为在 HTTP `200` 下展示不同业务码示例，并复验通过
- Java LSP 初始化超时，未返回稳定结果；以 Maven 完整编译与完整 reactor 测试结果作为最终验证依据

残留风险：
- 当前共享错误示例仍以常见场景为主，没有为每个业务接口单独定制所有失败消息
- 部分 `300` 场景示例仍以 `authentication is required` 为代表，未穷举所有权限失败消息

知识沉淀 / 是否回写 docs：
- 当前未额外回写 `docs/API.md`，Swagger 本身已承载常见错误示例

产物清理与保留说明：
- 当前任务单保留在 `ai-agent-workplace/`，状态已关闭为 `done`

补充说明：
- 本任务目标是提升客户端调试体验，不改接口本身。

任务名称：
细化 controller 文档接入

任务目标：
为当前仓库所有 HTTP controller 构建一份可执行的高质量精细文档接入任务清单，使后续 Swagger/OpenAPI 文档能够从“已接入可用”提升为“接口级语义完整、示例充分、适合长期协作”的状态。

任务背景：
当前仓库已经完成了 Swagger / OpenAPI 基础接入、分组、摘要、DTO schema、成功/失败示例和 README/API 文档入口增强，但 controller 层仍存在进一步精细化空间，例如参数级说明、操作级 `@ApiResponse`、更细颗粒度的失败语义和更完整的成功示例覆盖。

影响模块：
- `chat-domain`
- `application-starter`
- `docs`

允许修改范围：
- `chat-domain/src/main/java/**/controller/http/*.java`
- `chat-domain/src/main/java/**/controller/dto/*.java`
- `chat-domain/src/main/java/**/application/dto/*.java`（仅在 controller 直接对外暴露时）
- `application-starter/src/main/java/team/carrypigeon/backend/starter/config/OpenApiConfiguration.java`
- `docs/API.md`

禁止修改范围：
- 不允许改业务逻辑
- 不允许改 HTTP 路径和参数语义
- 不允许改变模块依赖方向
- 不允许引入新依赖
- 不允许把文档任务扩展成接口重构任务

依赖限制：
- 仅使用当前已引入的 springdoc / swagger annotations 能力

配置限制：
- 不新增运行时配置，除非文档增强确有必要且已有明确使用场景

文档依据：
- `AGENTS.md`
- `README.md`
- `docs/API.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/依赖引入规范.md`
- `docs/注释规范.md`
- `docs/测试规范.md`
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`

当前 controller 面：
- `chat-domain/.../features/auth/controller/http/AuthController.java`
- `chat-domain/.../features/channel/controller/http/ChannelController.java`
- `chat-domain/.../features/message/controller/http/ChannelMessageController.java`
- `chat-domain/.../features/server/controller/http/ServerController.java`
- `chat-domain/.../features/server/controller/http/ServerWellKnownController.java`
- `chat-domain/.../features/user/controller/http/UserProfileController.java`

任务分解 / 执行计划：
1. 对全部 HTTP controller 做文档现状审计，标记“已覆盖 / 部分覆盖 / 缺失”。
2. 统一每个 controller 的 `@Tag` 风格，确保分组命名、读者视角和说明语气一致。
3. 为全部 endpoint 补齐或细化 `@Operation`，明确：动作、作用对象、认证要求、当前阶段边界。
4. 为所有非平凡 path/query 参数补 `@Parameter` 级说明，至少覆盖：含义、约束、取值范围、当前实现限制。
5. 为所有带 body 的接口补 `@RequestBody` / schema 说明，特别是：鉴权请求、资料更新、频道治理请求、附件上传 multipart。
6. 为关键接口补操作级 `@ApiResponses`，而不是只依赖全局共享示例；至少覆盖成功、参数错误、认证/权限失败、资源不存在、内部错误。
7. 对匿名接口与受保护接口分别补明确语义，避免仅依赖路径推断；尤其明确 `logout`、`echo`、`well-known`、`presence/me` 的边界。
8. 对所有 controller 直接暴露的 DTO / application DTO 深化字段 schema，重点是枚举值、nullable 字段、列表/分页包装、JSON 字符串字段。
9. 对 multipart / 上传接口补专门说明，明确 `messageType`、`file`、`objectKey` 后续如何使用。
10. 对 success 示例做 endpoint 分层：认证、资料、频道、消息、服务发现各补真实成功示例，不再只依赖共享 `success` 示例。
11. 对 failure 示例做 endpoint 分层：对不同类接口使用更贴切的 `404` / `300` 示例，而不是统一的 `channel does not exist` 或 `authentication is required`。
12. 同步 `docs/API.md`，确保文字版 API 文档与 Swagger 最终表现一致。
13. 跑完整验证并做独立复审，确认文档没有误导客户端。

按 feature 的细化任务：

### `auth`
- `register`：明确用户名/密码约束、成功示例、参数错误示例。
- `login`：明确登录凭证语义、token 返回字段、错误示例（认证失败 vs 参数错误）。
- `refresh`：明确 refresh token 语义、过期/无效场景。
- `logout`：明确“无需 Bearer、只依赖 body 中 refresh token”的特殊语义。
- `me`：明确 Bearer 鉴权要求与当前最小账户信息返回。

### `user`
- `me` / `/{accountId}`：明确当前账户可见性边界。
- `list`：明确它返回的是列表而不是分页对象。
- `page` / `search`：明确 `cursor` 和 `nextCursor` 的排他游标语义。
- `updateMe`：明确可更新字段、空串语义、长度限制、失败路径。

### `channel`
- `default` / `system`：明确用途差异。
- `private`：明确私有频道创建语义。
- `invites` / `accept`：明确邀请状态、角色边界和失败语义。
- `members`：明确返回角色字段含义与当前可见性。
- `admin` / `mute` / `kick` / `bans` / `ownership-transfer`：对每类治理动作单独补成功与失败说明，避免只给一个泛化描述。

### `message`
- `messages`：明确历史分页与 `nextCursor` 语义。
- `messages/search`：明确 keyword 行为和 limit 限制。
- `messages/attachments`：明确 multipart 字段、允许的 `messageType`、返回 `objectKey` 的用途。
- `messages/{messageId}/recall`：明确撤回权限和结果形态。

### `server`
- `echo`：明确这是联通性验证接口。
- `presence/me`：明确是受保护接口，返回当前节点在线状态。
- `/.well-known/carrypigeon-server`：明确匿名可读、服务发现用途、字段边界。

实现要求：
- 文档增强优先 controller 入口和 DTO 暴露面，不扩展到无直接消费价值的内部对象
- 成功/失败示例必须与真实序列化输出一致（snake_case / camelCase 必须按实际运行约定）
- 只在高价值接口上补精细 `@ApiResponses`，避免对所有操作机械复制注解
- 对“共享示例”和“操作级示例”分层处理，优先消除误导，再补精细度

测试要求：
- 受影响模块与 `application-starter` 所在 reactor 测试通过
- 如新增 OpenAPI 相关逻辑，补充对应 starter 级测试

质量门禁：
- Java LSP/编译诊断无新增错误
- `mvn -pl "application-starter" -am -DskipTests=false test` 通过
- Swagger 表现与 `docs/API.md` 一致
- 对匿名/鉴权语义的文档不再与运行时行为冲突

复审要求：
- 需做一轮只读复审，重点审查：
  - 是否仍有 success/error 示例与真实 JSON 不一致
  - 是否仍有共享示例误挂到不匹配的操作上
  - 是否仍存在 `300/404/500` 语义过于泛化的问题

文档要求：
- `docs/API.md` 应同步到与最终 Swagger 一致，但不要求重复 Swagger 中所有字段级细节

验收标准：
- 所有 HTTP controller 均具备高质量精细文档接入
- 前端/测试可直接依赖 Swagger 理解参数、成功/失败语义与鉴权方式
- 关键接口拥有贴合真实业务的 success/failure 示例

完成定义：
- 代码、注解、必要文档更新完成
- 验证通过
- 复审通过
- 任务单改为 `done`

实际结果：
- 本任务单最终作为细化规划单保留，没有单独启动专门的文档精修批次。
- 后续实际执行的 docs/t 协议整改过程中，controller 级 OpenAPI 注解、DTO schema 和 `docs/API.md` 已被持续补强，覆盖了这份计划单中的大量高优先项。

验证记录：
- `application-starter/src/test/java/team/carrypigeon/backend/starter/config/OpenApiConfigurationTests.java` 已存在并通过，用于验证 OpenAPI 基础装配与安全声明。
- 当前活跃 controller 已具备大范围 `@Operation` / `@ApiResponses` / `@Schema` 注解覆盖，可支撑联调使用。

残留风险：
- 若未来需要追求“接口级文案极致精修”，仍可再开独立文档任务；但这已不属于当前 docs/t 协议对齐的阻塞范围。

知识沉淀 / 是否回写 docs：
- 待后续实施阶段决定

产物清理与保留说明：
- 当前任务单保留在 `ai-agent-workplace/`

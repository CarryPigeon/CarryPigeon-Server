任务名称：
基于 docs-t 的 Files 接口补齐批次三

任务目标：
实现 `docs/t/11-http-endpoints-v1.md` 中优先级最高且最贴近现有基础设施的 Files 能力：
- `POST /api/files/uploads`
- `GET /api/files/download/{share_key}`

任务背景：
上一批已补齐消息查询与频道资料链路。剩余 docs/t 能力中，Files 相关接口最贴近当前已存在的对象存储与附件访问能力，具备最小爆炸半径和较高产品价值，因此作为 batch3 优先实现。

影响模块：
- `chat-domain`
- `infrastructure-service/storage-api`
- `infrastructure-service/storage-impl`
- `application-starter`（若测试运行时装配需要）

允许修改范围：
- `chat-domain/src/main/java/**/message/**`
- `chat-domain/src/main/java/**/server/**`
- `chat-domain/src/main/java/**/shared/**`
- 与文件下载/上传相关的 controller、application、dto、support 类
- 与上述改动直接相关的测试类

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不把本批次扩大为 pins / mentions / read_state / unreads 的并行实现
- 不擅自改写 docs/t 目标协议

依赖限制：
- 优先复用现有 `ObjectStorageService`、`PresignedUrl`、附件 object key 规则
- 不绕过既有 storage-api / storage-impl 分层

配置限制：
- 不新增未来占位配置
- 不引入额外外部服务

文档依据：
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/SERVER_API.md`
- `docs/架构文档.md`
- `docs/包结构规范.md`
- `docs/测试规范.md`

任务分解 / 执行计划：
1. 梳理现有对象存储与附件 object key / 访问 URL 生成链路。
2. 为 `POST /api/files/uploads` 设计最小应用层与 DTO，生成 upload 指针。
3. 为 `GET /api/files/download/{share_key}` 设计最小下载链路，区分 `server_avatar` 与普通对象。
4. 补充 controller / application / regression tests。
5. 执行诊断与定向 Maven 验证。

关键假设与依赖：
- 现有对象存储已能生成上传/下载 URL 或等价访问路径。
- `share_key` 可先采用与 object key 可映射的最小实现，不要求本批引入完整独立文件表。
- 若完全私有访问控制需要新增持久化模型，则本批优先实现最小可运行且与 docs/t 不冲突的版本。

实现要求：
- 接口字段必须遵守 docs/t 的 `snake_case` 形状。
- 上传响应必须返回 `file_id`、`share_key` 与 `upload` 结构。
- 下载入口至少支持 `server_avatar` 及已知 object key 路径的下载或跳转。

测试要求：
- 覆盖上传申请成功路径
- 覆盖下载入口成功路径与关键失败路径
- 覆盖 starter 级回归用例（若涉及装配）

质量门禁：
- 改动文件无新增诊断错误
- 相关定向 Maven 测试通过
- 受影响模块编译通过

复审要求：
- 需要复审
- 重点检查 docs/t 一致性、share_key 语义、访问控制与存储层边界

文档要求：
- 默认不修改 docs/t，只改实现对齐

验收标准：
- `POST /api/files/uploads` 可返回 docs/t 规定的上传结构
- `GET /api/files/download/{share_key}` 可返回可用下载能力

完成定义：
- 验收标准满足
- 质量门禁执行并记录

实际结果：
- 已实现 `POST /api/files/uploads`，返回 `file_id`、`share_key` 与同源 `upload` 结构。
- 已实现同源上传入口 `PUT /api/files/uploads/{share_key}`，供上传授权结果返回的 URL 实际可用。
- 已实现 `GET /api/files/download/{share_key}`，当前通过同源入口跳转到对象下载地址。
- 已支持保留 share_key `server_avatar` 的匿名下载入口。
- 已补充 `chat-domain` 层的 Files 应用服务与控制器测试。
- 已补充 `application-starter` 级回归测试，覆盖上传申请 → 上传 → 下载入口链路。

验证记录：
- `mvn -pl chat-domain,application-starter -am -Dtest=AuthWebMvcConfigurationTests,FileApplicationServiceTests,FileControllerTests,MessageAttachmentRegressionTests -Dsurefire.failIfNoSpecifiedTests=false test` 通过。

残留风险：
- 当前 `share_key -> object_key` 采用无独立文件表的稳定映射规则，后续若需要更强的持久化索引或分享策略，可继续增强。
- 下载当前采用同源入口跳转到对象地址，未在本批引入服务端二进制流式代理。

知识沉淀 / 是否回写 docs：
- 默认不回写 docs

产物清理与保留说明：
- 完成后改名为 `done`

补充说明：
- 本任务单选择 Files 作为 batch3 推荐实现面

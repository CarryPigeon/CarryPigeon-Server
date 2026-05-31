任务名称：
基于 docs-t 的 Message Forward 实现批次九

任务目标：
实现 `docs/t/SERVER_API.md` 中的：
- `POST /api/messages/{mid}/forward`

任务背景：
当前仓库已具备消息读取、消息发送和频道成员校验链路。Forward 可沿现有消息创建主链路扩展，而不必引入新的持久化表，因此适合作为下一批最小增强能力。

影响模块：
- `chat-domain`

允许修改范围：
- `chat-domain/src/main/java/**/message/**`
- 与上述改动直接相关的测试

禁止修改范围：
- 不修改模块依赖方向
- 不新增第三方依赖
- 不并行扩大到 mentions

依赖限制：
- 复用现有 `MessageApplicationService` 发送主链路
- 不新增新表

配置限制：
- 不新增未来占位配置

文档依据：
- `docs/t/SERVER_API.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：
1. 新增 forward command / DTO / controller 请求响应。
2. 在 `MessageApplicationService` 中实现最小 forward 用例。
3. 复用目标频道的消息创建链路生成新消息。
4. 补充 controller / application 定向测试。
5. 执行定向 Maven 验证。

关键假设与依赖：
- 本批次先用文本草稿表达 forward 结果，不引入新的持久化字段。
- `forwarded_from` 作为增强项可后续再补。

实现要求：
- 必须校验源消息存在、目标频道存在、目标频道成员资格。
- 不允许静默跨频道转发失败。

测试要求：
- 覆盖 forward 成功路径。
- 覆盖源消息不存在或目标频道不合法路径。

质量门禁：
- 定向 Maven 测试通过。

验收标准：
- `POST /api/messages/{mid}/forward` 可创建目标频道的新消息。

完成定义：
- 验收标准满足并完成验证。

实际结果：
- 已实现 `POST /api/messages/{mid}/forward`。
- 已在 `MessageController` 暴露 forward 资源路径，并在 `MessageApplicationService` 中完成源消息存在、目标频道存在、目标频道成员资格校验。
- 后续在 message edit / model 批次中进一步补齐了 `forwarded_from` 的结构化持久化与响应输出。

验证记录：
- `MessageApplicationServiceForwardTests`、`MessageForwardControllerTests` 已通过。
- forward 能力已并入后续 message batch 的定向 Maven 验证并通过。

残留风险：
- 已无该批次阻断项；forwarded_from 已在后续批次补齐。

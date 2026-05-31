任务名称：
Message Legacy Recall HTTP Cutover

任务目标：
删除 `messages` family 中不再符合 `docs/t` 的旧 `recall` HTTP 端点，继续减少残留旧协议入口。

任务背景：
用户已明确确认：
- 成功响应彻底移除 `CPResponse`
- 对外 API 以 `docs/t` 为唯一基准，不保留旧端点
- WebSocket 继续保留当前独立 Netty 模型

任务类型：
实现类任务

影响模块：
- `chat-domain`
- `ai-agent-workplace/`

允许修改范围：
- `chat-domain` 中 `messages` family 的旧 recall HTTP 入口与直接相关测试
- `ai-agent-workplace/` 任务材料

禁止修改范围：
- 不改当前附件上传链路
- 不改 WS recall 语义和应用服务 recall 用例
- 不调整模块依赖方向
- 不新增第三方依赖
- 不修改长期 `docs/`

文档依据：
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`

任务分解 / 执行计划：
1. 删除旧 `POST /api/channels/{cid}/messages/{mid}/recall`。
2. 清理对应控制器映射与测试残留。
3. 运行消息控制器定向测试。

验收标准：
- 旧 recall HTTP 端点不再存在
- 相关控制器测试通过

实际结果：
- `ChannelMessageController` 已删除旧 `POST /api/channels/{cid}/messages/{mid}/recall`。
- 与该旧入口绑定的控制器映射和契约测试残留已清理。
- 应用服务 recall 能力与相关业务测试保持不变，未影响内部撤回语义。

验证记录：
- 残留引用检索：
  - 已检索旧 recall HTTP 路由与 `ChannelMessageController` 中的 `recallChannelMessage(...)`。
  - 当前 recall 仅保留在应用服务内部用例与业务测试中，不再暴露为旧 HTTP 端点。
- 已执行定向测试：
  - `mvn -q -pl chat-domain -am -Dtest=ChannelMessageQueryControllerTests,ChannelMessageAttachmentControllerTests,MessageApplicationServiceSendTests -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`
- 关键结果：
  - `ChannelMessageQueryControllerTests`: 8 passed
  - `ChannelMessageAttachmentControllerTests`: 6 passed
  - `MessageApplicationServiceSendTests`: 14 passed

残留风险：
- 附件上传旧入口仍待后续结合 file/voice 新链路统一收口。

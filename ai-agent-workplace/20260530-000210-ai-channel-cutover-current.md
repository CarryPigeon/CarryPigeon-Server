任务名称：
Channel Family 协议切换实现

任务目标：
将 `channels` family 的对外 HTTP 协议切换到 `docs/t` 基准，删除残留 `CPResponse` 成功包装与不再保留的旧端点，保留并修正 v1 资源路径。

任务背景：
用户已明确确认：
- 成功响应彻底移除 `CPResponse`
- 对外 API 以 `docs/t` 为唯一基准，不保留旧端点
- WebSocket 继续保留当前独立 Netty 模型

任务类型：
实现类任务

影响模块：
- `chat-domain`
- `application-starter`
- `ai-agent-workplace/`

允许修改范围：
- `chat-domain` 中与 `channels` family 直接相关的控制器、DTO、应用服务与测试
- 如有必要，更新 `application-starter` 中与该 family 文档装配直接相关的最小内容
- `ai-agent-workplace/` 任务材料

禁止修改范围：
- 不改 `ws` family 正式协议实现
- 不新增第三方依赖
- 不修改长期 `docs/`

文档依据：
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/SERVER_API.md`

任务分解 / 执行计划：
1. 删除不在 `docs/t` 的旧 channel 端点与旧成功包装。
2. 将保留的 v1 资源路径端点统一为资源对象或 `204`。
3. 清理受影响 DTO / 测试 / OpenAPI 暴露残留。
4. 跑 channels family 定向测试并修复。

关键假设与依赖：
- 保留：`GET/POST /api/channels`、`GET/PATCH/DELETE /api/channels/{cid}`、`GET /api/channels/discover`、`GET /api/channels/{cid}/members`、`DELETE /api/channels/{cid}/members/{uid}`、`PUT/DELETE /api/channels/{cid}/admins/{uid}`、`POST/GET/POST-decisions /api/channels/{cid}/applications...`、`PUT/DELETE/GET /api/channels/{cid}/bans...`、`PUT /api/channels/{cid}/notification_preference`
- 删除：`/api/channels/default`、`/api/channels/system`、`/api/channels/private`、`/api/channels/{cid}/invites`、`/api/channels/{cid}/invites/accept`、`/api/channels/{cid}/members/{uid}/admin`、`/api/channels/{cid}/ownership-transfer`、`/api/channels/{cid}/members/{uid}/mute`、`/api/channels/{cid}/bans`（POST）

完成定义：
- 代码改动完成
- 目标测试通过
- 任务单记录结果、验证和残留风险

实际结果：
- 进行中

验证记录：
- 待补充

残留风险：
- channels 牵涉的旧 DTO 与测试面较大，删除旧入口后会带出较多编译/契约残留，需要跟随修复。

任务名称：
基于 docs-t 的 API 重写执行蓝图

任务目标：
在已确认的协议边界上，形成后续 API 重写实现的统一执行蓝图，明确目标状态、删除边界、批次顺序、控制器 family 落点与测试收口策略。

任务背景：
用户已明确确认：
- 成功响应彻底移除 `CPResponse`
- 对外 API 以 `docs/t` 为唯一基准，不保留旧端点
- WebSocket 继续保留当前独立 Netty 模型

任务类型：
只读方案 / 执行准备任务

影响模块：
- `chat-domain`
- `application-starter`
- `ai-agent-workplace/`

允许修改范围：
- 仅允许在 `ai-agent-workplace/` 新增或更新方案与任务材料

禁止修改范围：
- 不修改正式源码
- 不修改正式测试
- 不修改正式配置
- 不修改 `docs/`

依赖限制：
- 不新增依赖

配置限制：
- 不扩大全局配置体系

文档依据：
- `docs/AI协作开发规范.md`
- `docs/变更审核清单.md`
- `docs/架构文档.md`
- `docs/API.md`
- `docs/t/SERVER_API.md`
- `docs/t/10-http-ws-protocol-v1.md`
- `docs/t/11-http-endpoints-v1.md`
- `docs/t/12-ws-events-v1.md`
- `docs/t/13-error-model-and-reasons-v1.md`
- `docs/t/14-pagination-and-cursor-v1.md`

任务分解 / 执行计划：
1. 固化目标协议和删除边界。
2. 给出 HTTP 与 WS 的重写批次顺序。
3. 明确每个 family 的目标控制器状态。
4. 明确测试重写策略与验收顺序。

关键假设与依赖：
- `docs/t` 不再只是参考，而是唯一对外协议标准。
- 所有历史 `CPResponse` 成功包装与历史旧端点都将被删除或重写，不再保留兼容出口。
- WS 继续使用 Netty 独立监听端口与 `/api/ws` 路径，不切换到 Spring WebSocket。

实现要求：
- 实现时必须优先收敛对外协议，不允许继续新增双轨响应。
- 每一批次都应尽量做到“一个 family 改完即删旧入口并改测试”，避免新旧并存时间过长。

测试要求：
- 控制器契约测试优先。
- 应用服务测试按协议变化补齐。
- WS 事件与命令测试必须覆盖 `auth` / `reauth` / `resume` / `ping` / 最小事件集。

质量门禁：
- 蓝图足够指导后续多轮实现。
- family 级目标状态和删除边界清晰。
- 明确说明本次无正式代码改动。

复审要求：
- 进入正式实现后，每个批次完成前都要对照本蓝图做一次协议自检。

文档要求：
- 本次不修改 `docs/`；待实现收口后再评估是否同步更新正式 API 文档。

验收标准：
- 后续可直接按蓝图开工，而无需重新确认成功模型、旧端点保留策略和 WS 模型。

完成定义：
- 形成稳定执行蓝图。
- 向用户输出高信号摘要与推荐起手批次。

实际结果：
- 已固化协议边界：
  - 成功响应：统一返回资源对象、列表对象或 `204 No Content`
  - 失败响应：统一 `{ error: { status, reason, message, request_id?, details? } }`
  - ID：雪花 ID 一律以十进制字符串对外暴露
  - 时间：一律输出 epoch 毫秒
  - 分页：统一 `{ items, next_cursor, has_more }`
  - 字段命名：统一 `snake_case`
- 已固化删除边界：
  - 删除所有仅为旧协议服务的 `CPResponse` 成功出口
  - 删除所有历史兼容旧路径，不保留双轨控制器
  - 删除与 `docs/t` 不一致的成功响应外壳和历史路径语义
- 已固化 WS 保留边界：
  - 保留 `chat-domain` 内的 Netty realtime 运行时
  - 保留独立监听端口配置
  - 但命令集、事件 envelope、鉴权首帧和 resume 语义必须完全对齐 `docs/t`

目标状态：

- HTTP 基础层
  - `CPResponse` 不再作为任何对外成功模型使用
  - `GlobalExceptionHandler` 继续作为唯一错误出口
  - 所有 DTO 只表达 `docs/t` 资源模型，不再混入旧字段名

- Server / Auth family
  - 保留：
    - `GET /api/server`
    - `POST /api/gates/required/check`
    - `GET /api/plugins/catalog`
    - `GET /api/domains/catalog`
    - `POST /api/auth/email_codes`
    - `POST /api/auth/tokens`
    - `POST /api/auth/refresh`
    - `POST /api/auth/revoke`
  - 删除：
    - 任何仅为旧登录模型服务的对外协议语义
    - 任何成功响应仍为 `CPResponse` 的 server/auth 入口
  - 特别说明：
    - `/.well-known/carrypigeon-server` 若保留，只能作为补充发现入口，且输出也必须与新策略一致；不能继续输出旧 `CPResponse`

- User family
  - 目标仅保留 `docs/t` 对应的用户公开资料与当前用户资料接口
  - 删除旧分页包装与旧更新接口语义
  - `GET /api/users/me`、`PATCH /api/users/me`、背景图上传等都要完全按 `docs/t` 模型统一

- Channel family
  - 以 `docs/t` 中的频道资源、发现、成员、申请、封禁、读状态、通知偏好模型为准
  - 删除 `/default`、`/system`、`/private` 这类历史语义入口
  - 统一为资源式 `/api/channels...`

- Message family
  - 保留并收敛：
    - `GET /api/channels/{cid}/messages`
    - `GET /api/channels/{cid}/messages/search`
    - `POST /api/channels/{cid}/messages`
    - `PATCH /api/messages/{mid}`
    - `DELETE /api/messages/{mid}`
    - `POST /api/messages/{mid}/forward`
    - `POST/DELETE/GET /api/channels/{cid}/pins...`
    - `GET/PUT /api/mentions...`
  - 删除：
    - 旧 recall 路径和旧 envelope 返回
    - 任何消息 family 中仍保留的旧消息模型字段集

- WS family
  - 保留：
    - 独立 Netty runtime
    - `/api/ws`
  - 必须重写：
    - 首帧 `auth`
    - `reauth`
    - `ping/pong`
    - `resume.failed`
    - 统一 `event` envelope
    - `event_id` / `event_type` / `server_time` / `payload`
  - 必须删除：
    - 仅为旧实时消息发送协议存在的命令语义
    - 旧 `send_channel_message` 作为最终对外命令名

推荐实施顺序：

1. 批次 A：协议收口底座
- 删除成功响应使用场景中的 `CPResponse`
- 清理仍会对外暴露旧 envelope 的控制器方法
- 保持错误模型、时间、ID、分页规则不回退

2. 批次 B：Server/Auth 一次收口
- 完整对齐 `GET /api/server`、gate、auth token 流程
- 决定 `/.well-known/carrypigeon-server` 的最终补充职责
- 清理旧登录叙事与旧成功模型测试

3. 批次 C：Message family 一次收口
- 因为该 family 已最接近 `docs/t`，优先把编辑、转发、发送、查询、pins、mentions 全部彻底统一
- 删除 recall 和残留旧模型

4. 批次 D：Channel/User family 一次收口
- 这是旧风格残留最重的两组
- 重点删除 `/default`、`/system`、`/private` 等历史入口
- 把成员、申请、封禁、资料分页/更新统一到 `docs/t`

5. 批次 E：WS 一次收口
- 保留 Netty runtime，仅重写协议语义
- 删掉旧命令名和旧兼容壳
- 对齐 `docs/t/12-ws-events-v1.md`

6. 批次 F：测试与文档收口
- 重写契约测试
- 清理过期测试名与注释
- 更新 `docs/API.md` 或收口到新的正式文档表达

测试重写策略：
- 先改控制器契约测试，再改控制器实现。
- 每个 family 完成后，删除旧路径对应测试，不保留“双断言”。
- WS 测试从“兼容旧命令”改为“只验证新命令与新事件”。
- `application-starter` smoke tests 作为跨模块收口验证，放在每批次最后复跑。

主要风险：
- `channel` / `user` family 的旧控制器方法较多，批次内若只改一半，容易形成再次混合态。
- `docs/t` 并未逐条描述所有历史补充接口，删除旧端点时要严格区分“客户端基准需要”与“历史服务端自有能力”。
- WS 若保留 Netty runtime，但只改了一半命令集，会比 HTTP 更难联调，因此 WS 必须单批次完成。

建议起手批次：
- 从 Server/Auth 开始，而不是先动 Channel/User。
- 原因：这是客户端连接链路的入口，也是 `docs/t` 标准里最强约束的一组；先把连接、gate、token、发现文档收干净，后续业务 family 才有稳定基线。

验证记录：
- 只读材料复核：`ai-agent-workplace/20260519-202344-ai-api-redesign-roadmap-done.md`
- 只读材料复核：`ai-agent-workplace/20260519-202344-ai-api-redesign-round1-solution-done.md`
- 只读材料复核：`ai-agent-workplace/20260519-202344-ai-api-redesign-round2-protocol-foundation-done.md`
- 本任务为只读方案 / 执行准备任务，无正式代码改动，因此未运行测试 / 构建。

残留风险：
- 若后续实现时发现某些 `docs/t` family 与现有领域模型存在结构冲突，需要在对应实现批次补一轮领域到协议映射澄清。

知识沉淀 / 是否回写 docs：
- 暂不回写 `docs/`；待正式实现收口后统一处理。

产物清理与保留说明：
- 保留本蓝图作为后续正式编码任务的边界依据。

补充说明：
- 下一轮若进入正式编码，建议新建实现类任务单：`server-auth-cutover`，直接处理 Server/Auth family 的彻底切换。

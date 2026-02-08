# 客户端 / 机器人端文档（入口概览）

## 1. 主入口

- 客户端对接主文档：`doc/audience/client-developer-guide.md`
- 协议索引：`doc/api/文档索引.md`
- 机器人实践：`doc/clients/bot-client-guide.md`

## 2. 最短对接路径

1. `GET /api/server`
2. `POST /api/auth/tokens`
3. `wss://{host}/api/ws` + `auth`
4. 处理 `message.created` / `message.deleted` / `read_state.updated`
5. 断线后用 `resume` 或 HTTP 补拉恢复一致性

## 3. 规范约束

- 接口基线：HTTP `/api` + WebSocket `/api/ws`
- 字段命名：`snake_case`
- 错误分支：`error.reason`

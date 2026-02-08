# 日志与可观测性规范

> 目标：在不泄露敏感信息的前提下，支持接口追踪、故障定位与容量评估。

## 1. 日志分层

- 访问日志：HTTP 请求、WS 命令/事件统计
- 业务日志：LiteFlow 关键节点、DAO 关键操作、插件扫描与校验
- 错误日志：统一异常、外部依赖失败、权限与限流拒绝

## 2. 日志字段规范

建议固定字段（MDC 或结构化日志）：

- `request_id`
- `route`（HTTP path 或 WS type）
- `uid`、`cid`、`mid`（如适用）
- `reason`（失败时）
- `duration_ms`

禁止输出：

- access_token / refresh_token
- 密钥、密码、完整邮箱、敏感业务 payload 全量

## 3. 关键观测点

- HTTP：QPS、P95/P99 延迟、4xx/5xx 比例
- WS：在线连接数、断线重连率、`resume.failed` 比例
- 业务：消息发送成功率、限流命中率、required gate 拒绝率
- 依赖：DB/Redis/MinIO 调用耗时与错误率

## 4. 告警建议

- 5xx 比例突增
- WS 连接数异常下降
- `required_plugin_missing` 或 `token_expired` 突增
- 插件扫描失败或合约校验失败持续发生

## 5. 排障顺序（推荐）

1. 先看错误日志中的 `request_id` 与 `reason`
2. 按 `request_id` 回溯访问日志与业务链路
3. 结合依赖监控确认 DB/Redis/MinIO 是否异常
4. 对 WS 问题补查 `auth` / `resume` 相关日志

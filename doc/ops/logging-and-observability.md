# 日志与可观测性说明

> 本文介绍 CarryPigeon Backend 的日志体系与基本可观测性设计，  
> 帮助你在开发与运维中快速定位问题。

---

## 1. 日志框架与配置

### 1.1 日志框架

- 使用 **Log4j2** 作为底层日志框架；
- 通过 Spring Boot 的 `logging.config` 指定日志配置文件。

### 1.2 配置文件

路径：`application-starter/src/main/resources/log4j2_config.xml`

关键点：

- 日志输出到：
  - 控制台（带颜色）；
  - 多个 RollingFile（all/debug/info/warn/error/error-json）；
- 日志格式包含：
  - 时间（ISO8601）；
  - level；
  - 线程名；
  - MDC 中的 `packetId` / `route` / `uid`；
  - 类名；
  - 消息与堆栈。

根日志级别：

- `<Root level="debug">` — 默认输出 debug 及以上日志；
- 对 Spring、MyBatis、Netty 等框架设置较高的日志级别（WARN），避免噪音。

---

## 2. 日志文件与用途

主要 RollingFile Appender：

- `all.log`
  - 包含所有级别日志；
  - 主要用于全局排查和归档。

- `debug.log`
  - 过滤掉 info 及以上级别（只保留 debug）；
  - 用于开发或精细问题定位。

- `info.log`
  - 过滤掉 warn 及以上级别（只保留 info）；
  - 适合观察系统正常行为与关键业务日志。

- `warn.log`
  - 过滤掉 error 及以上级别；
  - 聚焦潜在问题。

- `error.log`
  - error 及以上级别；
  - 聚焦严重错误。

- `error-json.log`
  - JSON 格式的 error 日志；
  - 适合集成 ELK / Loki 等日志系统进行结构化分析。

日志文件路径：

- 默认路径：`./service-logs`（由 `LOG_HOME` 属性控制）；
- 部署时请确保该目录存在且具有写权限。

---

## 3. 日志规范与实践

### 3.1 基本规范

- 日志统一使用 **英文**；
- 日志消息中尽量包含关键字段：
  - `uid`（用户 id）；
  - `cid`（频道 id）；
  - `mid`（消息 id）；
  - `route`（业务路由）；
  - 其他业务关键字段（如 email、token 片段等，注意脱敏）。

### 3.2 关键模块日志

1. **LiteFlow 节点 (`CPNodeComponent` 及子类)**
   - 在参数缺失时调用 `argsError(context)`：
     - 记录节点 id + 缺失 key；
     - 写入 `CPResponse.code=100` 并中断链路；
   - 在业务错误时调用 `businessError(context, msg)`：
     - 记录节点 id + 业务错误信息；
     - 写入 `CPResponse.code=100` 并中断链路。

2. **DAO 层**
   - 在查询/保存/删除方法中打印 debug 日志：
     - 调用入参（id/cid/uid 等）；
     - 结果数量或是否成功；
   - 在参数为 null 或数据库操作失败时打印 error/warn 日志。

3. **连接层（Netty + 加密）**
   - 握手失败、解密失败、AAD 校验失败时：
     - 记录 session_id / remoteAddress / 错误原因；
     - 关闭连接。
   - 心跳超时或 IdleState 事件：
     - 记录远端地址与空闲类型。

4. **通知与会话中心**
   - `CPNotificationService` 在发送通知时：
     - 记录 route / uid / session 数量；
   - `CPSessionCenterService` 在添加/移除会话时：
     - 记录 uid / 当前 session 数。

### 3.3 敏感信息处理

- 避免在日志中输出：
  - 完整 token；
  - 密码、敏感配置；
  - 明文隐私数据（如身份证号等）。
- 如需输出，建议只打印片段（如 token 前几位）或完全脱敏。

---

## 4. 可观测性建议

目前项目内置的可观测能力主要是日志。建议在实践中逐步补充：

1. **指标（Metrics）**
   - 引入 Micrometer + Prometheus 或类似方案；
   - 对以下指标进行监控：
     - 每个路由的 QPS / 延迟；
     - DAO 调用耗时与错误率；
     - LiteFlow 链路耗时（可通过节点前后记录时间实现）。

2. **追踪（Tracing）**
   - 为每个请求生成一个 traceId（可复用 packetId）；
   - 在日志 MDC 中统一记录 traceId / route / uid；
   - 如接入分布式追踪系统（Zipkin/Jaeger），可将 traceId 传入上下游。

3. **告警与仪表盘**
   - 针对 error 日志量、特定错误码（如 500）设置告警；
   - 在仪表盘上展示：
     - 连接数变化；
     - QPS、延迟；
     - 错误率、超时次数。

---

## 5. 日常排查问题的路径

遇到问题时可以按以下步骤排查：

1. **确定时间范围和用户信息**
   - 从客户端报错中获取时间、用户 id、频道 id、路由、错误码。

2. **在日志中搜索**
   - 首选 `error.log` / `error-json.log`；
   - 根据 `uid` / `route` / `packetId` 搜索；
   - 查看最近的 LiteFlow 节点/DAO/连接层错误。

3. **定位到具体模块**
   - 若是参数错误：从 LiteFlow 节点日志看是哪个 key 缺失；
   - 若是权限错误：查看相关 checker 节点的日志；
   - 若是 DAO 或外部服务错误：查看对应 DAO/Service 的 warn/error。

4. **验证修复效果**
   - 在测试环境复现并验证修复；
   - 再查看生产环境日志是否仍有类似错误。

通过统一的日志风格和字段命名，可以大幅降低排查问题时的信息收集成本。 

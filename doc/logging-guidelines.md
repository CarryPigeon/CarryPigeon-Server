# Java 日志规范（强制）

> 目标：统一项目内日志的 **级别、字段、格式、敏感信息处理与关键链路埋点**，让排障可定位、可检索、可聚合。
>
> 适用范围：本仓库所有 Java 代码（含插件），除非目录下有更细粒度的规范文件另行覆盖。

---

## 0. 总原则（必须遵守）

1. **日志写“事件 + 关键上下文”，不要写大段描述**（便于检索与聚合）。
2. **高频路径避免 INFO**：每请求/每包日志默认使用 DEBUG，INFO 只记录生命周期与关键状态变更。
3. **必须参数化输出**：统一使用 `{}` 占位符，禁止字符串拼接构造日志。
4. **敏感信息禁止明文落盘**：token、验证码、密钥、邮箱等必须脱敏（见第 4 节）。
5. **一处抛错一处记录**：异常只在“边界层/责任层”记录一次堆栈，避免重复刷屏。

---

## 1. 统一上下文字段（MDC）——强制

日志输出格式已包含 MDC：`packetId`、`route`、`uid`（见 `application-starter/src/main/resources/log4j2_config.xml:1`）。

### 1.1 强制写入的 MDC key

- `packetId`：Netty 业务包 `CPPacket.id`（没有则不写）
- `route`：业务路由（没有则不写）
- `uid`：当前登录用户 id（没有则不写）

### 1.2 写入与清理规则

- **进入业务处理边界就写入**（例如：分发器/Controller 入口）。
- **必须在 `finally` 里清理 `MDC.clear()`**，避免线程复用导致串号。

参考实现：`chat-domain/src/main/java/team/carrypigeon/backend/chat/domain/controller/netty/CPControllerDispatcherImpl.java:1`。

---

## 2. 日志级别规范（强制）

### 2.1 ERROR（必须满足其一）

- 服务器 bug / 不可恢复错误（NPE、序列化失败、加解密失败、事务失败等）
- 安全相关严重事件（协议校验失败导致断连、解密失败、AAD 校验失败等）
- 外部依赖不可用且影响核心业务（DB/Redis/MinIO 等）

要求：
- 必须携带异常堆栈：`log.error("...", e)`
- 必须包含关键上下文（uid/route/ids/sessionId/remoteAddress 等，按场景）

### 2.2 WARN（必须满足其一）

- 可恢复异常或异常输入（参数缺失、业务软失败、权限不足、资源不存在等）
- 性能风险：慢请求/慢 IO/重试
- 不一致/兼容兜底路径（例如读取旧字段、降级策略触发）

### 2.3 INFO（严格控制）

仅用于：
- 生命周期：启动、端口绑定成功、组件初始化完成、配置加载（避免打印敏感配置）
- 关键状态变更：握手成功、会话注册/注销、链路执行耗时异常（低频）

禁止：
- 在高频业务链路中每请求都打 INFO（用 DEBUG 替代）

### 2.4 DEBUG

用于：
- 请求级诊断信息（入参摘要、查询命中/数量、分支选择、链路耗时）
- 预期内但需要排障的细节（例如 “no active session, uid=…”）

---

## 3. 日志内容与格式（强制）

### 3.1 消息格式

统一采用“事件 + 结果 + 关键字段”：

- ✅ `log.info("session registered, uid={}, sessionId={}", uid, sessionId);`
- ✅ `log.warn("slow request, route={}, packetId={}, costMs={}", route, packetId, cost);`
- ❌ `log.info("session registered uid=" + uid);`（禁止拼接）
- ❌ `log.error("error");`（缺少上下文）

### 3.2 字段命名（统一 key）

优先使用以下命名（保持全文一致，便于检索）：

- `uid` 用户 id
- `cid` 频道 id
- `mid` 消息 id
- `fileId` 文件 id
- `token`（仅允许脱敏/摘要）
- `route` 业务路由
- `packetId` 请求 id（CPPacket.id）
- `sessionId` 连接会话 id（握手后 AAD 内的 session_id）
- `remoteAddress` / `remoteIp` / `remotePort`
- `costMs` 耗时（毫秒）

---

## 4. 敏感信息与隐私（强制）

### 4.1 禁止直接输出（绝对禁止）

- AES 密钥、ECC 私钥、公钥原文
- 验证码、邮件服务密码、数据库密码、MinIO secretKey
- 完整 token（登录 token、下载/上传 token）
- 用户隐私原文（除非业务明确要求且经过脱敏审批）

### 4.2 允许输出的形式（必须脱敏）

- token：只输出前后片段，例如 `abcd...wxyz`，或输出 hash（推荐）
- email：仅输出域名或部分字符，例如 `a***@example.com`
- 请求体/响应体：只输出摘要（字段存在性/数量/长度），禁止整段 JSON

---

## 5. 关键链路强制埋点

### 5.1 connection（Netty/协议/加密）

- **握手成功**：INFO（带 `sessionId`、`remoteAddress`）
- **断连**：只记录一次（避免重复），并带 **reason code**（见 5.3）
- **加解密失败 / AAD 校验失败**：ERROR（带 `sessionId`/`remoteAddress`/reason）

### 5.2 chat-domain（分发/LiteFlow）

- **分发入口**：写入 MDC（packetId/route/uid），必要时 DEBUG 记录解析/耗时
- **慢请求**：WARN（建议阈值 `costMs >= 500`，当前实现参考 `CPControllerDispatcherImpl`）
- **参数缺失/业务失败**：
  - Node 内统一通过 `argsError/businessError` 写入 `CPResponse` 并中断；
  - 记录的日志必须包含 nodeId 与缺失 key 或业务失败原因。

### 5.3 断连 reason code（强制使用统一枚举语义）

连接层在关闭连接前必须写入 reason（建议通过 `DisconnectSupport.markDisconnect`）。

reason 命名规则：
- 小写 `snake_case`
- 表达“原因类别”，不要夹带动态文本

现有 reason 示例（可复用）：
- `frame_length_illegal`
- `aad_length_error`
- `aad_decode_error`
- `package_sequence_error`
- `session_id_mismatch`
- `package_timestamp_error`
- `decrypt_failed`
- `encrypt_failed`
- `serialize_failed`
- `idle_timeout_reader`
- `idle_timeout_all`
- `connection_reset`
- `exception`

新增 reason 时必须同步更新本列表，并在代码里保持一致。

---

## 6. DAO / 外部服务（强制）

- DAO 层默认只打 DEBUG（入参 id、命中数量、是否成功、耗时），避免高频 INFO。
- 外部服务失败（邮件/MinIO）：
  - 影响业务成功返回：ERROR
  - 可降级/可重试：WARN
- 禁止输出 SQL 原文与大字段内容（例如消息 data、文件二进制摘要除外）。

---

## 7. 禁止事项（强制）

- 禁止 `System.out.println` / `printStackTrace`
- 禁止在循环里对“每个成员/每个 session”打 INFO/WARN（应聚合为一次，或降级为 DEBUG）
- 禁止记录“永远不会用来检索”的日志（例如不带任何上下文的固定句子）
- 禁止把异常吞掉不记录（除非明确是可忽略并有 DEBUG 说明）

---

## 8. 与现有配置的关系

- 日志输出与分流策略见：`doc/ops/logging-and-observability.md`
- Log4j2 输出格式与 MDC 字段见：`application-starter/src/main/resources/log4j2_config.xml`

---

## 9. 强制校验（已启用）

- Maven `validate` 阶段通过 Checkstyle 禁止 `System.out.print(ln)` 与 `printStackTrace()`。

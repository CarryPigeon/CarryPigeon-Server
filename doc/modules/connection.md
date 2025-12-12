# 模块说明：connection

> `connection` 模块负责网络连接与传输协议：  
> 使用 Netty 提供 TCP 服务、完成握手与加解密，将业务 JSON 转交给 `chat-domain` 处理。

---

## 1. 目录结构概览

源码根路径：`connection/src/main/java/team/carrypigeon/backend/connection`

主要子包：

- `attribute`
  - `ConnectionAttributes`：为 Netty `Channel` 存储连接相关属性（如会话 id、AES key 等）的 key 封装。

- `handler`
  - `ConnectionHandler`：Netty 业务处理核心，负责：
    - 握手阶段处理 ECC 公钥 / AES 密钥包；
    - 业务阶段解包、解密、验证 AAD、调用 `CPControllerDispatcher`；
    - 将业务响应加密后写回客户端。

- `heart`
  - `CPNettyHeartBeatHandler`：基于 Netty IdleState 事件实现心跳检测和空闲连接清理。

- `protocol`
  - `protocol/codec`：
    - `NettyDecoder`：实现长度前缀帧解析；
    - `NettyEncoder`：实现业务包编码（长度前缀 + payload）。
  - `protocol/aad`：
    - `AeadAad`：封装 AES-GCM 的 AAD 结构（包序号、sessionId、时间戳）。

- `security`
  - `CPAESKeyPack`：服务端发给客户端的 AES 密钥包（握手阶段）；
  - `CPECCKeyPack`：客户端发给服务端的 ECC 公钥包。

- `protocol/encryption`
  - `encryption/aes`：
    - `AESUtil` / `AESData`：AES-GCM 加解密工具类；
  - `encryption/ecc`：
    - `ECCUtil`：ECC 加解密工具类（用于握手阶段对称密钥交换）。

- `session`
  - `NettySession`：实现 `CPSession` 接口，对 Netty `Channel` 的封装；
  - 负责业务层写入响应时的统一加密与帧编码。

- 根类：
  - `NettyConnectionStarter`：Netty 服务器启动入口，配置 Boss/Worker/EventLoop、Pipeline 等。

---

## 2. 帧格式与编解码

### 2.1 业务帧格式

在加密前，Netty 采用简单的长度前缀帧格式：

```text
+--------------------+---------------------+
| 2 bytes length (n) | n bytes payload ... |
+--------------------+---------------------+
```

- `length`：无符号短整型，表示后续 payload 长度；
- `payload`：在握手阶段为 JSON 文本，在业务阶段为 `nonce | AAD | cipherText`。

### 2.2 NettyDecoder

类：`connection/.../protocol/codec/NettyDecoder.java`

职责：

- 使用 `ByteBuf` 的 `markReaderIndex` / `resetReaderIndex` 处理半包；
- 读取 2 字节长度前缀；
- 检查帧长度合法性（最大长度 64KB），非法时写日志并关闭连接；
- 将完整 payload 作为 `byte[]` 传给后续 handler（`ConnectionHandler`）。

### 2.3 NettyEncoder

类：`connection/.../protocol/codec/NettyEncoder.java`

职责：

- 接收业务层传入的 `byte[]` payload；
- 在前面写入 2 字节长度；
- 输出给 Netty 进行发送。

---

## 3. 安全握手与加密

### 3.1 握手流程

握手阶段不使用 AES 加密，直接发送/接收 JSON：

1. **客户端 → 服务端：发送 ECC 公钥**

```json
{
  "id": 1,
  "key": "<Base64-encoded-ECC-public-key>"
}
```

映射为 `CPECCKeyPack`，服务端保存 `id`，生成 ECC 密钥对。

2. **服务端：生成 AES 会话密钥**
   - 使用 `AESUtil.generateKey()` 生成随机 AES key；
   - 使用 `ECCUtil.encrypt` 使用客户端公钥加密 AES key。

3. **服务端 → 客户端：发送 AES 密钥包**

```json
{
  "id": 1,
  "sessionId": 123456789,
  "key": "<Base64-encoded-encrypted-AES-key>"
}
```

映射为 `CPAESKeyPack`，其中：

- `id`：对应客户端发来的 `CPECCKeyPack.id`；
- `sessionId`：服务端生成的会话 id（用于 AAD）；
- `key`：用客户端 ECC 公钥加密后的 AES 密钥。

4. **客户端：解密 AES 密钥并保存**
   - 后续所有业务请求使用此对称密钥进行 AES-GCM 加密。

### 3.2 业务加密帧结构

握手完成后，每个业务包 payload 结构为：

```text
+---------+---------+--------------+
| nonce   | AAD     | cipherText   |
+---------+---------+--------------+
  12 B      20 B       (n-32) B
```

- `nonce`：12 字节随机数（AES-GCM nonce），全 0 时表示心跳包；
- `AAD`：20 字节，由 `AeadAad` 封装：
  - 4 字节：包序号（int, big-endian）；
  - 8 字节：sessionId（long, big-endian）；
  - 8 字节：时间戳（long, big-endian, 毫秒）。
- `cipherText`：AES-GCM 加密后的业务 JSON 文本。

### 3.3 AAD 校验

`ConnectionHandler` 中对 AAD 做严格校验：

- 校验长度 == `AeadAad.LENGTH`；
- 解析出：
  - `packageId`（包序号）；
  - `sessionId`；
  - `timestampMillis`。
- 检查：
  - **包序号**：必须单调递增，相对于 session 中保存的最近包序号；
  - **sessionId 一致**：与握手时分配的 `PACKAGE_SESSION_ID` 匹配；
  - **时间窗口**：`timestampMillis` 必须在一定时间窗口内（例如 ±3 分钟）。

验证失败时：

- 打印包含 sessionId、远端地址等信息的错误日志；
- 关闭连接，避免继续处理异常流量。

---

## 4. 业务处理与会话封装

### 4.1 ConnectionHandler

路径：`connection/src/main/java/team/carrypigeon/backend/connection/handler/ConnectionHandler.java`

职责：

1. 握手阶段：
   - 根据当前连接是否已有 AES 密钥区分握手/业务阶段；
   - 接收 `CPECCKeyPack`，生成 AES 密钥并发送 `CPAESKeyPack`。

2. 业务阶段：
   - 从 payload 中拆分 `nonce` / `aad` / `cipherText`；
   - 处理心跳包（nonce 全 0，不进入业务逻辑）；
   - 校验 AAD（包序号、sessionId、时间戳）；
   - 使用 `AESUtil.decryptWithAAD` 解密业务 JSON；
   - 调用 `CPControllerDispatcher` 处理业务：
     - 将解密后的 JSON 交给 `chat-domain`；
     - 获取 `CPResponse` 并通过 `NettySession` 写回。

错误处理：

- 对解密失败、AAD 校验失败、JSON 解析失败等情况写详细日志；
- 根据情况决定是否关闭连接。

### 4.2 NettySession

路径：`connection/src/main/java/team/carrypigeon/backend/connection/session/NettySession.java`

职责：

- 实现 `CPSession` 接口，对业务层提供统一的 `write` 方法；
- 在 `write` 时：
  - 构建 AAD（递增的包序号 + sessionId + 当前时间）；
  - 使用 AES-GCM 加密业务 JSON；
  - 组装 `nonce | aad | cipherText`，交给 `NettyEncoder` 写出；
- 维护每个会话的本地状态（如本地包序号等）。

---

## 5. 心跳与连接管理

### 5.1 心跳处理

类：`CPNettyHeartBeatHandler`  
路径：`connection/src/main/java/team/carrypigeon/backend/connection/heart/CPNettyHeartBeatHandler.java`

功能：

- 基于 Netty 的 `IdleStateEvent`：
  - 读空闲：长时间未收到数据；
  - 写空闲：长时间未发送数据；
  - 读写空闲：完全空闲；
- 策略：
  - 根据空闲类型写日志（包含远端地址）；
  - 读空闲和读写空闲时可关闭连接；
  - 写空闲时可以主动发送心跳包（如有需要）。

### 5.2 心跳帧

约定：

- `nonce` 全 0 的帧视为心跳包；
- AAD 和 cipherText 可以为空或固定占位；
- 业务逻辑忽略心跳帧，只在连接层更新活跃状态。

---

## 6. NettyConnectionStarter

路径：`connection/src/main/java/team/carrypigeon/backend/connection/NettyConnectionStarter.java`

职责：

- 从配置中读取端口（`connection.port`）；
- 创建 BossGroup / WorkerGroup / 业务线程组；
- 配置 `ServerBootstrap`：
  - Channel 类型；
  - ChildHandler（插入 `NettyDecoder`、`ConnectionHandler`、`NettyEncoder` 等）；
- 绑定端口并阻塞等待关闭；
- 在 `finally` 中优雅关闭事件循环组。

日志：

- 启动时输出端口、绑定结果；
- 关闭时输出各事件循环组的退出信息。

---

## 7. 与其他模块的关系

- 与 `api`：
  - 使用 `CPPacket` / `CPResponse` / `CPSession` 等协议与会话抽象；
  - 使用 `CPAESKeyPack` / `CPECCKeyPack` 等握手数据结构。

- 与 `chat-domain`：
  - 通过 `CPControllerDispatcherImpl` 将业务 JSON 转交给 chat-domain；
  - `chat-domain` 返回的 `CPResponse` 由本模块负责加密与发送。

- 与 `application-starter`：
  - `application-starter` 负责启动 Spring Boot 应用；
  - `NettyConnectionStarter` 可由 Spring Boot 配置类或入口类触发启动。

通过将连接与协议逻辑集中在 `connection` 模块，业务模块可以专注于处理 JSON 层的业务，而无需关心底层加密与网络细节。 


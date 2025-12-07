# CarryPigeon Backend API (Server-Side)

This document describes how a client should talk to the CarryPigeon chat backend.

It covers:

1. How to establish a connection to the server
2. The binary framing format on the TCP layer
3. The JSON protocol (request / response) and the main business routes

All examples below assume UTF-8 text encoding.

---

## 1. Establishing a Connection

### 1.1 Transport and port

- Protocol: TCP
- Port: configured by Spring Boot property `connection.port`
  - See: `api/.../ConnectionConfig` (`connection.port`)
  - Used by: `connection/NettyConnectionStarter` to start the Netty server

### 1.2 Netty frame format

Before encryption, Netty uses a very simple frame format:

    +--------------------+---------------------+
    | 2 bytes length (n) | n bytes payload ... |
    +--------------------+---------------------+

- `length`: unsigned short, length of the following payload
- `payload`: raw bytes, interpreted by the encryption / protocol layer

Implemented by:
- Encoder: `connection.protocol.codec.NettyEncoder`
- Decoder: `connection.protocol.codec.NettyDecoder`

### 1.3 Security handshake (ECC + AES)

The backend uses an ECC+AES scheme to protect business packets.

1. Client -> Server: send ECC public key
   - Plain JSON, mapped to `connection.security.CPECCKeyPack`:
     - `id`: long, client-generated request id
     - `key`: string, Base64-encoded ECC public key

2. Server: generate AES session key
   - `AESUtil.generateKey()` generates a random AES key
   - Encrypted with the client public key via `ECCUtil.encrypt`
   - Wrapped into `connection.security.CPAESKeyPack`:
     - `id`: copied from the ECC key pack
     - `sessionId`: long, server-generated session id
     - `key`: string, Base64-encoded encrypted AES key

3. Server -> Client: send AES key pack
   - Still plain JSON over the same 2-byte-length framing
   - Client decrypts the AES key using its ECC private key and stores it as the session key

4. Subsequent business packets: AES-GCM frames

For each business packet, the Netty payload is:

    +---------+---------+--------------+
    | nonce   | AAD     | cipherText   |
    +---------+---------+--------------+
      12 B      20 B       (n-32) B

- `nonce`: 12-byte random nonce for AES-GCM
- `AAD` (20 bytes):
  - 4 bytes: packet sequence id (int, big-endian)
  - 8 bytes: session id (long, big-endian)
  - 8 bytes: packet timestamp (long, big-endian, milliseconds)
- `cipherText`: AES-GCM encrypted JSON string (see CPProtocol below)

5. Server validation of AAD (`ConnectionHandler.checkAAD`):

- Packet sequence must be monotonically increasing per session
- Session id must match the stored `PACKAGE_SESSION_ID`
- Packet timestamp must be within a few minutes of the current time

6. Heartbeat packets

- If all 12 bytes of `nonce` are zero, the packet is treated as a heartbeat and ignored by business logic.

### 1.4 From TCP bytes to business logic

After AES-GCM decryption, the server obtains a UTF-8 JSON string and passes it to:

```java
CPResponse response = cpControllerDispatcher.process(json, session);
```

- Dispatcher implementation: `chat-domain.controller.netty.CPControllerDispatcherImpl`
  - Deserialises JSON to `CPPacket`
  - Uses `route` to find a controller (`@CPControllerTag`)
  - Executes LiteFlow chain and writes a `CPResponse` into the context

The client therefore needs to:

1. Complete the ECC+AES handshake
2. Encrypt JSON `CPPacket` with AES-GCM and send with the 2-byte length frame
3. Decrypt JSON `CPResponse` from server and handle it

---

## 2. Business JSON protocol

### 2.1 Request packet: `CPPacket`

Defined in `api.connection.protocol.CPPacket`:

```java
public class CPPacket {
    private long id;      // request id
    private String route; // e.g. "/core/user/login/email"
    private JsonNode data; // request body JSON
}
```

- `id`
  - Client -> Server: non-negative long used to match responses
  - Server -> Client (push): `-1` to indicate no response is expected
- `route`
  - Path of the business route, must match `@CPControllerTag(path = ...)`
- `data`
  - Arbitrary JSON object, schema defined by the VO class

Example (email login):

```json
{
  "id": 12345,
  "route": "/core/user/login/email",
  "data": {
    "email": "test@example.com",
    "code": 123456
  }
}
```

### 2.2 Response packet: `CPResponse`

Defined in `api.connection.protocol.CPResponse`:

```java
public class CPResponse {
    private long id;     // same as request.id
    private int code;    // status code
    private JsonNode data; // response body

    public static CPResponse ERROR_RESPONSE   = new CPResponse(-1, 100, null);
    public static CPResponse SUCCESS_RESPONSE = new CPResponse(-1, 200, null);
    public static CPResponse AUTHORITY_ERROR_RESPONSE = new CPResponse(-1, 300, null);
    public static CPResponse PATH_NOT_FOUND_RESPONSE  = new CPResponse(-1, 404, null);
    public static CPResponse SERVER_ERROR    = new CPResponse(-1, 500, null);
}
```

Status code conventions:

- 200: success
- 100: argument / business error
- 300: permission / authority error
- 404: route not found
- 500: internal server error

Success example:

```json
{
  "id": 12345,
  "code": 200,
  "data": {
    "token": "xxx.yyy.zzz"
  }
}
```

Error example:

```json
{
  "id": 12345,
  "code": 100,
  "data": {
    "msg": "user not found"
  }
}
```

---

## 3. Main business routes (overview)

Each route is defined by a Netty controller in `chat-domain`, annotated with
`@CPControllerTag(path = "...", voClazz = ..., resultClazz = ...)`.

- `voClazz` defines the shape of `CPPacket.data`
- `resultClazz` defines the shape of `CPResponse.data`

Below is a high-level overview of the key routes.

> Field names match VO fields exactly (camelCase).

### 3.1 User routes

#### 3.1.1 Register

- Route: `/core/user/register`
- Request VO: `CPUserRegisterVO`

```json
{
  "email": "string",
  "code": 123456
}
```

- Success: `CPUserRegisterResult`:

```json
{
  "token": "string"
}
```

#### 3.1.2 Login by email

- Route: `/core/user/login/email`
- VO: `CPUserEmailLoginVO`

```json
{
  "email": "string",
  "code": 123456
}
```

- Success: `CPUserEmailLoginResult`:

```json
{
  "token": "string"
}
```

#### 3.1.3 Login by token

- Route: `/core/user/login/token`
- VO: `CPUserTokenLoginVO`

```json
{
  "token": "string"
}
```

- Success: `CPUserTokenLoginResult`:

```json
{
  "token": "string",
  "uid": 12345
}
```

#### 3.1.4 Logout (invalidate token)

- Route: `/core/user/login/token/logout`
- VO: `CPUserTokenLogoutVO`

```json
{
  "token": "string"
}
```

- Success: default result (code 200).

#### 3.1.5 Get user profile

- Route: `/core/user/profile/get`
- VO: `CPUserGetProfileVO`

```json
{
  "uid": 12345
}
```

- Success: `CPUserGetProfileResult`:

```json
{
  "username": "string",
  "avatar": 0,
  "email": "string",
  "sex": 0,
  "brief": "string",
  "birthday": 0
}
```

#### 3.1.6 Update profile / email

- `/core/user/profile/update` → `CPUserUpdateProfileVO`
- `/core/user/profile/update/email` → `CPUserUpdateEmailProfileVO`

Both return default success result.

---
### 3.2 Channel routes

#### 3.2.1 Create channel

- Route: `/core/channel/create`
- VO: `CPChannelCreateVO`

```json
{
  "name": "string",
  "brief": "string",
  "avatar": 0
}
```

- Success: `CPChannelCreateResult`:

```json
{
  "cid": 12345
}
```

#### 3.2.2 Delete channel

- Route: `/core/channel/delete`
- VO: `CPChannelDeleteVO`:

```json
{
  "cid": 12345
}
```

- Success: default result.

#### 3.2.3 Get channel profile

- Route: `/core/channel/profile/get`
- VO: `CPChannelGetProfileVO`:

```json
{
  "cid": 12345
}
```

- Success: `CPChannelGetProfileResult`:

```json
{
  "name": "string",
  "owner": 12345,
  "brief": "string",
  "avatar": 0,
  "createTime": 0
}
```

#### 3.2.4 Update channel profile

- Route: `/core/channel/profile/update`
- VO: `CPChannelUpdateProfileVO`:

```json
{
  "cid": 12345,
  "name": "string?",
  "owner": 12345?,
  "brief": "string?",
  "avatar": 0?
}
```

- Success: default result.

#### 3.2.5 Channel members

- List members: `/core/channel/member/list`
  - VO: `CPChannelListMemberVO` → `{ "cid": 12345 }`
  - Success: `CPChannelListMemberResult`:

```json
{
  "count": 2,
  "members": [
    { "uid": 1, "name": "A", "authority": 0, "joinTime": 0 },
    { "uid": 2, "name": "B", "authority": 0, "joinTime": 0 }
  ]
}
```

- Remove member: `/core/channel/member/delete`
  - VO: `CPChannelDeleteMemberVO` → `{ "cid": 123, "uid": 456 }`
  - Success: default result.

#### 3.2.6 Channel applications

- Create: `/core/channel/application/create`
- Process: `/core/channel/application/process`
- List: `/core/channel/application/list` → `CPChannelListApplicationResult`

Result shape:

```json
{
  "count": 1,
  "applications": [
    { "id": 1, "uid": 123, "state": 0, "msg": "...", "applyTime": 0 }
  ]
}
```

#### 3.2.7 Channel bans

- Create: `/core/channel/ban/create`
- Delete: `/core/channel/ban/delete`
- List: `/core/channel/ban/list` → `CPChannelListBanResult`

Result shape:

```json
{
  "count": 1,
  "bans": [
    {
      "uid": 123,
      "duration": 3600
      // other fields, see CPChannelListBanResultItem
    }
  ]
}
```

---

### 3.3 Message routes

#### 3.3.1 Send message

- Route: `/core/channel/message/create`
- VO: `CPMessageCreateVO`:

```json
{
  "cid": 12345,
  "domain": "Core:Text",
  "data": { "text": "hello" }
}
```

- Success: `CPMessageCreateResult`:

```json
{
  "mid": 1
}
```

#### 3.3.2 Delete message

- Route: `/core/channel/message/delete`
- VO: `CPMessageDeleteVO` → `{ "mid": 1 }`
- Success: default result.

#### 3.3.3 List messages

- Route: `/core/channel/message/list`
- VO: `CPMessageListVO`:

```json
{
  "cid": 12345,
  "startTime": 0,
  "count": 50
}
```

- Success: `CPMessageListResult` (see above).

#### 3.3.4 Get unread count

- Route: `/core/channel/message/unread/get`
- VO: `CPMessageGetUnreadVO`:

```json
{
  "cid": 12345,
  "startTime": 0
}
```

- Success: `CPMessageGetUnreadResult`:

```json
{
  "count": 10
}
```
### 3.4 File routes

#### 3.4.1 Apply upload token

- Route: `/core/file/upload/token/apply`
- VO: `CPFileUploadTokenApplyVO`:

```json
{
  "fileId": "sha256-or-other-id"
}
```

- Success: `CPFileUploadTokenApplyResult`:

```json
{
  "token": "string"
}
```

#### 3.4.2 Apply download token

- Route: `/core/file/download/token/apply`
- VO: `CPFileDownloadTokenApplyVO`:

```json
{
  "fileId": "sha256-or-other-id"
}
```

- Success: `CPFileDownloadTokenApplyResult`:

```json
{
  "token": "string"
}
```

### 3.5 Service routes

#### 3.5.1 Send email

- Route: `/core/service/email/send`
- VO: `CPServiceSendEmailVO`:

```json
{
  "email": "string"
}
```

- Success: default result.

---

## 4. Error handling on the client

On the client side, typical handling is:

1. If `code == 200`, parse `data` according to the route definition
2. If `code != 200`:
   - Show `data.msg` if present
   - Optionally branch on `code` (100/300/404/500 etc.)

---

## 5. Client implementation tips

- Wrap all calls into a helper like `send(route, data)` that:
  - Builds `CPPacket`
  - Serialises to JSON
  - Encrypts with AES-GCM
  - Sends over TCP with 2-byte length prefix
  - Decrypts and parses `CPResponse`

- During local debugging you can temporarily bypass encryption in the connection layer
  to test CPProtocol (CPPacket/CPResponse) directly over plain TCP.

- For complex result structures (applications, bans, etc.), refer to the corresponding
  `*Result` and `*ResultItem` classes in `chat-domain` for exact field definitions.


package team.carrypigeon.backend.api.connection.protocol;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 业务请求包（解密后的 JSON 协议体）。
 * <p>
 * 在 TCP 连接完成握手（ECC → AES 会话密钥）后，客户端发送的每个业务请求都会被解密成该结构：
 * <pre>
 * { "id": 123, "route": "/core/...", "data": { ... } }
 * </pre>
 * 其中：
 * <ul>
 *     <li>{@link #id}：请求 id，用于在响应中回显以匹配请求；</li>
 *     <li>{@link #route}：业务路由（对应服务端 {@code @CPControllerTag.path} / LiteFlow chain name）；</li>
 *     <li>{@link #data}：请求体对象，字段名对外统一为 {@code snake_case}。</li>
 * </ul>
 *
 * <p>注意：推送（通知）不使用该结构，服务端推送统一通过 {@code CPResponse(id=-1, code=0)} 包裹。
 */
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CPPacket {
    /**
     * 请求 id。
     * <p>
     * 客户端生成并保证在同一连接内足够唯一；服务端响应时会回显该 id。
     * <p>
     * 约定：服务端主动推送的 {@code CPResponse.id} 固定为 -1。
     */
    private long id;
    /**
     * 业务路由。
     * <p>
     * 必须与服务端注册的 {@code @CPControllerTag.path} 一致。
     */
    private String route;
    /**
     * 业务请求体。
     * <p>
     * 由路由对应的 VO 定义其字段结构；序列化对外字段名统一为 {@code snake_case}。
     */
    @JsonSetter(nulls = Nulls.SKIP)
    private JsonNode data;
}

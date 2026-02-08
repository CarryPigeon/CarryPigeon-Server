package team.carrypigeon.backend.chat.domain.service.ws;

/**
 * WebSocket 会话属性 Key（存储在 {@link org.springframework.web.socket.WebSocketSession#getAttributes()}）。
 * <p>
 * 约束：
 * <ul>
 *   <li>Key 必须稳定，避免跨版本升级导致旧连接状态丢失</li>
 *   <li>Value 的类型在写入处与读取处必须保持一致</li>
 * </ul>
 */
public final class ApiWsSessionAttributes {

    /**
     * 当前 WS 会话绑定的用户 ID。
     * <p>
     * 类型：{@link Long}
     */
    public static final String UID = "cp_ws_uid";

    /**
     * 频道订阅集合（可选优化）。
     * <p>
     * 若该属性不存在：表示服务端按“默认推送模型”向该用户推送所有相关事件。<br/>
     * 若存在：表示只推送 cid 命中的事件（例如 message.created/message.deleted/channel.changed/read_state.updated）。<br/>
     * <p>
     * 类型：{@code java.util.Set<Long>}
     */
    public static final String SUB_CIDS = "cp_ws_sub_cids";

    private ApiWsSessionAttributes() {
    }
}


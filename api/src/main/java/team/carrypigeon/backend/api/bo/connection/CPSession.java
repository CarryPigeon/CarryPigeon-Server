package team.carrypigeon.backend.api.bo.connection;

/**
 * 连接会话抽象（业务层与底层连接的解耦边界）。
 * <p>
 * 该接口屏蔽具体网络实现（Netty/WebSocket 等），对上层业务暴露：
 * <ul>
 *     <li>写回客户端的能力（可选择是否加密）；</li>
 *     <li>连接级属性读写（用于握手状态、用户绑定、远端地址等）。</li>
 * </ul>
 *
 * <p>约束：
 * <ul>
 *     <li>{@link #write(String)} 与 {@link #write(String, boolean)} 的 msg 必须是 JSON 明文；</li>
 *     <li>是否加密由连接层实现决定：握手后业务包通常为加密写出；</li>
 *     <li>属性 key 建议使用常量类统一管理（例如连接信息：{@link CPConnectionAttributes}）。</li>
 * </ul>
 */
public interface CPSession {
    /**
     * 向客户端写出 JSON（默认加密）。
     *
     * @param msg JSON 明文字符串
     */
    default void write(String msg){
        write(msg,true);
    }
    /**
     * 向客户端写出 JSON。
     *
     * @param msg JSON 明文字符串
     * @param encrypted 是否加密写出（业务阶段通常为 true；握手前/心跳可为 false）
     */
    void write(String msg,boolean encrypted);
    /**
     * 读取会话属性。
     *
     * @param key 属性 key（建议使用常量，避免魔法字符串）
     * @param type 期望类型
     * @return 若不存在或类型不匹配返回 null
     */
    <T> T getAttributeValue(String key, Class<T> type);
    /**
     * 写入会话属性。
     *
     * @param key 属性 key（建议使用常量）
     * @param value 属性值
     */
    void setAttributeValue(String key, Object value);

    /**
     * 关闭会话（断开连接）。
     */
    void close();
}

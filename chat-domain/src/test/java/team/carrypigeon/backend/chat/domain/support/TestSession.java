package team.carrypigeon.backend.chat.domain.support;

import team.carrypigeon.backend.api.bo.connection.CPSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple in-memory CPSession implementation for tests.
 * Stores written messages and attributes in memory only.
 */
public class TestSession implements CPSession {

    private final Map<String, Object> attributes = new HashMap<>();
    private final List<String> writtenMessages = new ArrayList<>();

    /**
     * 记录测试输出。
     *
     * @param msg 待记录的输出消息
     * @param encrypted 加密标记（测试中仅透传，不影响存储）
     */
    @Override
    public void write(String msg, boolean encrypted) {
        writtenMessages.add(msg);
    }

    /**
     * 读取测试会话属性。
     *
     * @param key 属性键名
     * @param type 属性目标类型
     * @return 命中的属性值；不存在时返回 { null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttributeValue(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new ClassCastException("Attribute " + key + " is not of type " + type.getName());
        }
        return (T) value;
    }

    /**
     * 写入测试会话属性。
     *
     * @param key 属性键名
     * @param value 待写入的属性值
     */
    @Override
    public void setAttributeValue(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 关闭测试会话。
     */
    @Override
    public void close() {
    }

    /**
     * 返回测试期间写出的消息列表。
     *
     * @return 按写入顺序保存的消息列表
     */
    public List<String> getWrittenMessages() {
        return writtenMessages;
    }

    /**
     * 返回测试会话中的属性映射。
     *
     * @return 可变属性 Map，用于断言会话状态
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}


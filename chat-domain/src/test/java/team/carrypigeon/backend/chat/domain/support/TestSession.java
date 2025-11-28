package team.carrypigeon.backend.chat.domain.support;

import team.carrypigeon.backend.api.bo.connection.CPSession;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试用的 CPSession 实现：
 *  - 使用内存 Map 维护属性；
 *  - 记录最近一次 write 的内容，方便断言通知类 Node 的行为；
 *  - 不进行任何真实的网络写入。
 */
public class TestSession implements CPSession {

    private final Map<String, Object> attrs = new HashMap<>();
    private String lastMessage;
    private boolean lastEncrypted;

    @Override
    public void write(String msg, boolean encrypted) {
        this.lastMessage = msg;
        this.lastEncrypted = encrypted;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttributeValue(String key, Class<T> type) {
        Object value = attrs.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    @Override
    public void setAttributeValue(String key, Object value) {
        attrs.put(key, value);
    }

    @Override
    public void close() {
        // no-op for tests
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean isLastEncrypted() {
        return lastEncrypted;
    }
}
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

    @Override
    public void write(String msg, boolean encrypted) {
        // For tests we just record the message, ignore encryption flag.
        writtenMessages.add(msg);
    }

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

    @Override
    public void setAttributeValue(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public void close() {
        // no-op for tests
    }

    public List<String> getWrittenMessages() {
        return writtenMessages;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}


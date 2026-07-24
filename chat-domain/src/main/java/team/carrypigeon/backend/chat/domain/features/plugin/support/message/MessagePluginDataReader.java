package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.file.domain.api.FileReferenceApi;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * 消息插件 data 严格读取器。
 * 职责：为内建插件提供类型安全的 JSON 字段读取和版本校验。
 * 边界：只解析不可信 data，不创建消息、不访问仓储或决定业务权限。
 */
final class MessagePluginDataReader {

    private MessagePluginDataReader() {
    }

    static Map<String, Object> copyData(Map<String, Object> data) {
        if (data == null) {
            throw ProblemException.validationFailed("data must not be null");
        }
        return new LinkedHashMap<>(data);
    }

    static void requireVersion(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw ProblemException.validationFailed("schema_invalid", "domain version is not supported");
        }
    }

    static String requiredString(Map<String, Object> data, String field, String message) {
        String value = optionalString(data, field);
        if (value == null) {
            throw ProblemException.validationFailed(message);
        }
        return value;
    }

    static String optionalString(Map<String, Object> data, String field) {
        Object value = data.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String stringValue)) {
            throw ProblemException.validationFailed(field + " must be string");
        }
        String normalized = stringValue.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static Long optionalLong(Map<String, Object> data, String field) {
        Object value = data.get(field);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof BigInteger integer) {
                return integer.longValueExact();
            }
            if (value instanceof BigDecimal decimal) {
                return decimal.longValueExact();
            }
            if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
                return ((Number) value).longValue();
            }
            if (value instanceof String stringValue) {
                return Long.parseLong(stringValue.trim());
            }
        } catch (ArithmeticException | NumberFormatException exception) {
            throw ProblemException.validationFailed(field + " must be integer");
        }
        throw ProblemException.validationFailed(field + " must be integer");
    }

    static Long requiredPositiveLong(Map<String, Object> data, String field, String message) {
        Long value = optionalLong(data, field);
        if (value == null || value <= 0L) {
            throw ProblemException.validationFailed(message);
        }
        return value;
    }

    static Boolean optionalBoolean(Map<String, Object> data, String field) {
        Object value = data.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Boolean booleanValue)) {
            throw ProblemException.validationFailed(field + " must be boolean");
        }
        return booleanValue;
    }

    static Map<String, Object> optionalObject(Map<String, Object> data, String field) {
        Object value = data.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map<?, ?> rawMap)) {
            throw ProblemException.validationFailed(field + " must be object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw ProblemException.validationFailed(field + " must contain string keys");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    static Map<String, Object> requiredObject(Map<String, Object> data, String field) {
        Map<String, Object> value = optionalObject(data, field);
        if (value == null) {
            throw ProblemException.validationFailed(field + " must be object");
        }
        return value;
    }

    static String optionalSnowflake(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String stringValue) || !stringValue.matches("[1-9][0-9]*")) {
            throw ProblemException.validationFailed(field + " must be decimal snowflake string");
        }
        try {
            new BigInteger(stringValue).longValueExact();
            return stringValue;
        } catch (ArithmeticException exception) {
            throw ProblemException.validationFailed(field + " must be decimal snowflake string");
        }
    }

    static String requiredSnowflake(Object value, String field) {
        String result = optionalSnowflake(value, field);
        if (result == null) {
            throw ProblemException.validationFailed(field + " must be decimal snowflake string");
        }
        return result;
    }

    static String resolveAttachmentObjectKey(Map<String, Object> data, FileReferenceApi fileReferenceApi) {
        String objectKey = optionalString(data, "object_key");
        if (objectKey != null) {
            return objectKey;
        }
        String shareKey = requiredString(data, "share_key", "share_key must not be blank");
        return fileReferenceApi.attachmentObjectKey(shareKey)
                .orElseThrow(() -> ProblemException.validationFailed("invalid_share_key", "share_key is invalid"));
    }
}

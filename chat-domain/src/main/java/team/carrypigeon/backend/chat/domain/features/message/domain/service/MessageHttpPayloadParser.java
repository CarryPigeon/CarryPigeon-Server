package team.carrypigeon.backend.chat.domain.features.message.domain.service;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.file.domain.service.FileShareKeyCodec;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;

/**
 * HTTP 消息 payload 解析协作对象。
 * 职责：读取 HTTP data 中的字符串、数字字段，并解析附件 object key。
 * 边界：不创建消息、不校验频道权限、不访问仓储。
 */
class MessageHttpPayloadParser {

    String requiredString(Map<String, Object> data, String fieldName, String message) {
        String value = optionalString(data, fieldName);
        if (value == null || value.isBlank()) {
            throw ProblemException.validationFailed("validation_failed", message);
        }
        return value;
    }

    String optionalString(Map<String, Object> data, String fieldName) {
        if (data == null) {
            return null;
        }
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    Long requiredLong(Map<String, Object> data, String fieldName, String message) {
        Long value = optionalLong(data, fieldName);
        if (value == null || value <= 0L) {
            throw ProblemException.validationFailed("validation_failed", message);
        }
        return value;
    }

    Long optionalLong(Map<String, Object> data, String fieldName) {
        if (data == null) {
            return null;
        }
        Object value = data.get(fieldName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw ProblemException.validationFailed("validation_failed", fieldName + " must be a number");
        }
    }

    String resolveAttachmentObjectKey(Map<String, Object> data) {
        String objectKey = optionalString(data, "object_key");
        if (objectKey != null) {
            return objectKey;
        }
        String shareKey = requiredString(data, "share_key", "share_key must not be blank");
        return FileShareKeyCodec.attachmentObjectKey(shareKey)
                .orElseThrow(() -> ProblemException.validationFailed("invalid_share_key", "share_key is invalid"));
    }
}

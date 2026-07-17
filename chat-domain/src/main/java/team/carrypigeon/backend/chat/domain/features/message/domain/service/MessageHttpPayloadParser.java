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

    /**
     * 从 HTTP 消息 data 中读取必填字符串字段。
     * 输入：协议 data、字段名和校验失败提示。
     * 输出：去除首尾空白后的字段值。
     * 失败语义：字段缺失或空白时抛出校验问题。
     *
     * @param data HTTP 消息 data
     * @param fieldName 需要读取的字段名
     * @param message 字段缺失或空白时返回给调用方的提示
     * @return 规范化后的字符串字段值
     */
    String requiredString(Map<String, Object> data, String fieldName, String message) {
        String value = optionalString(data, fieldName);
        if (value == null || value.isBlank()) {
            throw ProblemException.validationFailed("validation_failed", message);
        }
        return value;
    }

    /**
     * 从 HTTP 消息 data 中读取可选字符串字段。
     * 输出：字段不存在或规范化后为空时返回 null。
     *
     * @param data HTTP 消息 data
     * @param fieldName 需要读取的字段名
     * @return 规范化后的字符串字段值，缺失时为 null
     */
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

    /**
     * 从 HTTP 消息 data 中读取必填正整数 ID 字段。
     * 输入：协议 data、字段名和校验失败提示。
     * 输出：大于 0 的 long 值。
     * 失败语义：字段缺失、非数字或非正数时抛出校验问题。
     *
     * @param data HTTP 消息 data
     * @param fieldName 需要读取的字段名
     * @param message 字段缺失或非正数时返回给调用方的提示
     * @return 规范化后的正整数字段值
     */
    Long requiredLong(Map<String, Object> data, String fieldName, String message) {
        Long value = optionalLong(data, fieldName);
        if (value == null || value <= 0L) {
            throw ProblemException.validationFailed("validation_failed", message);
        }
        return value;
    }

    /**
     * 从 HTTP 消息 data 中读取可选数字字段。
     * 输出：字段不存在时返回 null，数字对象或数字字符串都会转换为 long。
     * 失败语义：字段存在但不能解析为数字时抛出校验问题。
     *
     * @param data HTTP 消息 data
     * @param fieldName 需要读取的字段名
     * @return 解析后的 long 值，缺失时为 null
     */
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

    /**
     * 从 HTTP 附件消息 data 中解析 canonical objectKey。
     * 输入：客户端提交的附件消息 data。
     * 输出：可写入消息 payload 的附件 objectKey。
     * 失败语义：缺少 share_key 或 share_key 不是附件引用时抛出校验问题。
     *
     * @param data HTTP 附件消息 data
     * @return 附件对象存储 key
     */
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

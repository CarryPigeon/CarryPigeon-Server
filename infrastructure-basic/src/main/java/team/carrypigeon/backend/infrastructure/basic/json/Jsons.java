package team.carrypigeon.backend.infrastructure.basic.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureErrorCode;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;

/**
 * JSON 基础工具。
 * 职责：收敛常见 JSON 序列化与反序列化操作，避免业务代码重复封装 ObjectMapper 调用。
 */
public final class Jsons {

    private Jsons() {
    }

    /**
     * 序列化任意对象为 JSON 字符串。
     * 失败：底层 Jackson 异常统一收口为基础设施异常。
     */
    public static String toJson(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.JSON_SERIALIZE_FAILED,
                    "Failed to serialize object to json",
                    ex
            );
        }
    }

    /**
     * 按具体类型反序列化 JSON 字符串。
     */
    public static <T> T fromJson(ObjectMapper objectMapper, String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.JSON_DESERIALIZE_FAILED,
                    "Failed to deserialize json",
                    ex
            );
        }
    }

    /**
     * 按泛型类型引用反序列化 JSON 字符串。
     */
    public static <T> T fromJson(ObjectMapper objectMapper, String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.JSON_DESERIALIZE_FAILED,
                    "Failed to deserialize json",
                    ex
            );
        }
    }

    /**
     * 解析 JSON 树模型。
     */
    public static JsonNode readTree(ObjectMapper objectMapper, String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.JSON_DESERIALIZE_FAILED,
                    "Failed to parse json tree",
                    ex
            );
        }
    }
}

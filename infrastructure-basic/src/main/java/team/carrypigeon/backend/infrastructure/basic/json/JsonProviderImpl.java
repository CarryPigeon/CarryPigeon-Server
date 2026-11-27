package team.carrypigeon.backend.infrastructure.basic.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.infrastructure.basic.exception.ProblemException;

/**
 * JSON 能力对外入口。
 * 职责：基于项目统一 ObjectMapper 提供序列化、反序列化和 JsonNode 解析能力。
 * 边界：不承载业务字段转换规则。
 */
public class JsonProviderImpl implements JsonProvider {

    private final ObjectMapper objectMapper;

    public JsonProviderImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 使用项目统一 ObjectMapper 序列化对象。
     */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw ProblemException.of(JsonProblem.JSON_SERIALIZE_FAILED, e);
        }
    }

    /**
     * 按目标类型反序列化 JSON。
     */
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw ProblemException.of(JsonProblem.JSON_DESERIALIZE_FAILED, e);
        }
    }

    /**
     * 按泛型类型引用反序列化 JSON。
     */
    public <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw ProblemException.of(JsonProblem.JSON_DESERIALIZE_FAILED, e);
        }
    }

    /**
     * 把 JSON 文本解析为树模型。
     */
    public JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw ProblemException.of(JsonProblem.JSON_DESERIALIZE_FAILED, e);
        }
    }
}

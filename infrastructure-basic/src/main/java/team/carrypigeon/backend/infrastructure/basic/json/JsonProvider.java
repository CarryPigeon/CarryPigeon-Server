package team.carrypigeon.backend.infrastructure.basic.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 能力对外入口。
 * 职责：基于项目统一 ObjectMapper 提供序列化、反序列化和 JsonNode 解析能力。
 * 边界：不承载业务字段转换规则。
 */
public class JsonProvider {

    private final ObjectMapper objectMapper;

    public JsonProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 使用项目统一 ObjectMapper 序列化对象。
     */
    public String toJson(Object value) {
        return Jsons.toJson(objectMapper, value);
    }

    /**
     * 按目标类型反序列化 JSON。
     */
    public <T> T fromJson(String json, Class<T> type) {
        return Jsons.fromJson(objectMapper, json, type);
    }

    /**
     * 按泛型类型引用反序列化 JSON。
     */
    public <T> T fromJson(String json, TypeReference<T> type) {
        return Jsons.fromJson(objectMapper, json, type);
    }

    /**
     * 把 JSON 文本解析为树模型。
     */
    public JsonNode readTree(String json) {
        return Jsons.readTree(objectMapper, json);
    }
}

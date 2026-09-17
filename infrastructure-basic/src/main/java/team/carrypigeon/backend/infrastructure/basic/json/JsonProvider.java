package team.carrypigeon.backend.infrastructure.basic.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

public interface JsonProvider {

    /**
     * 使用项目统一 ObjectMapper 序列化对象。
     */
    String toJson(Object value);

    /**
     * 按目标类型反序列化 JSON。
     */
    <T> T fromJson(String json, Class<T> type);

    /**
     * 按泛型类型引用反序列化 JSON。
     */
    <T> T fromJson(String json, TypeReference<T> type);

    /**
     * 把 JSON 文本解析为树模型。
     */
    JsonNode readTree(String json);
}

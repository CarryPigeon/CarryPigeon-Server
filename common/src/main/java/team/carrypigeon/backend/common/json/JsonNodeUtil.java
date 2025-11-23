package team.carrypigeon.backend.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JsonNode工具类，用于方便生成简单的JsonNode而不是通过ObjectMapper
 * */
public class JsonNodeUtil {

    /**
     * 快捷方法，用于生成JsonNode<br/>
     * 参数为键值对，键值对数量必须为偶数<br/>
     * @param keyValuePairs 键值对
     * */
    public static JsonNode createJsonNode(String... keyValuePairs){
        if (keyValuePairs.length % 2 != 0) return null;
        ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            jsonNode.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return jsonNode; // Removed erroneous semicolon
    }

    /**
     * JsonNode值获取<br/>
     * 只处理文本、long、int、boolean、数组<br/>
     * 其中数组统一返回JsonNode本身
     * @param node JsonNode
     * */
    public static Object getValue(JsonNode node){
        // 判断文本
        if (node.isTextual()){
            return node.asText();
        }
        // 判断long，主要用于id、时间戳等数据
        if (node.isLong()){
            return node.asLong();
        }
        // 其他数值类型统一用int处理
        if (node.isNumber()){
            return node.asInt();
        }
        // 判断boolean
        if (node.isBoolean()){
            return node.asBoolean();
        }
        // 为数组则放回node数据
        if (node.isArray()){
            return node;
        }
        // 默认返回null
        return null;
    }
}

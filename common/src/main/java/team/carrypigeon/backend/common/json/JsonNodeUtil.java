package team.carrypigeon.backend.common.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * `JsonNode` 工具类。
 */
public class JsonNodeUtil {

    /**
     * 根据键值对创建 `ObjectNode`。
     *
     * @param keyValuePairs 键值对数组（长度需为偶数）。
     * @return 创建后的 `JsonNode`；参数非法时返回 {@code null}。
     */
    public static JsonNode createJsonNode(String... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            return null;
        }
        ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            jsonNode.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return jsonNode;
    }

    /**
     * 读取 `JsonNode` 基础值。
     *
     * @param node 节点对象。
     * @return 文本、数字、布尔或数组节点；无法识别时返回 {@code null}。
     */
    public static Object getValue(JsonNode node) {
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isLong()) {
            return node.asLong();
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isArray()) {
            return node;
        }
        return null;
    }
}

package team.carrypigeon.backend.infrastructure.basic.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureErrorCode;
import team.carrypigeon.backend.infrastructure.basic.exception.InfrastructureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 JSON 基础工具的最小契约。
 * 职责：确保常用 JSON 字符串、泛型对象和 JsonNode 转换行为稳定。
 * 边界：不验证 Jackson 内部机制，只验证项目封装契约。
 */
@Tag("unit")
class JsonsTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 测试对象序列化。
     * 输入：简单 Map 对象。
     * 期望：输出稳定 JSON 字符串。
     */
    @Test
    void toJson_map_returnsJsonString() {
        String json = Jsons.toJson(objectMapper, Map.of("name", "carry"));

        assertEquals("{\"name\":\"carry\"}", json);
    }

    /**
     * 测试泛型反序列化。
     * 输入：JSON 数组字符串和 TypeReference。
     * 期望：返回对应泛型 List。
     */
    @Test
    void fromJson_typeReference_returnsGenericValue() {
        List<String> values = Jsons.fromJson(objectMapper, "[\"a\",\"b\"]", new TypeReference<>() {
        });

        assertEquals(List.of("a", "b"), values);
    }

    /**
     * 测试 JsonNode 读取。
     * 输入：JSON 对象字符串。
     * 期望：返回可读取字段的 JsonNode。
     */
    @Test
    void readTree_object_returnsJsonNode() {
        JsonNode node = Jsons.readTree(objectMapper, "{\"id\":1}");

        assertEquals(1, node.get("id").asInt());
    }

    /**
     * 测试非法 JSON。
     * 输入：非法 JSON 字符串。
     * 期望：抛出基础设施异常，并携带 JSON_DESERIALIZE_FAILED 错误码。
     */
    @Test
    void fromJson_invalidJson_throwsInfrastructureException() {
        InfrastructureException exception = assertThrows(
                InfrastructureException.class,
                () -> Jsons.fromJson(objectMapper, "{", Map.class)
        );

        assertEquals(InfrastructureErrorCode.JSON_DESERIALIZE_FAILED, exception.getErrorCode());
        assertEquals("Failed to deserialize json", exception.getMessage());
        assertNotNull(exception.getCause());
    }
}

package team.carrypigeon.backend.infrastructure.basic.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证 JSON 能力对外入口的最小契约。
 * 职责：确保 JsonProvider 能基于统一 ObjectMapper 对外提供简洁 JSON 能力。
 * 边界：不验证 Jackson 自动配置，只验证 Provider 委托行为。
 */
class JsonProviderTests {

    /**
     * 测试对象序列化入口。
     * 输入：JsonProvider 和简单 Map。
     * 期望：返回对应 JSON 字符串，调用方无需直接传入 ObjectMapper。
     */
    @Test
    void toJson_map_returnsJsonStringWithoutExternalObjectMapper() {
        JsonProvider jsonProvider = new JsonProvider(new ObjectMapper());

        assertEquals("{\"id\":1}", jsonProvider.toJson(Map.of("id", 1)));
    }
}

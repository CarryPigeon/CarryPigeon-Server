package team.carrypigeon.backend.chat.domain.features.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RealtimeServerProperties 契约测试。
 * 职责：验证 Netty 实时通道配置的默认值和关键边界校验。
 * 边界：不验证 Spring 绑定流程，只验证配置语义本身。
 */
class RealtimeServerPropertiesTests {

    /**
     * 验证默认构造能提供最小可运行的实时通道配置。
     */
    @Test
    @DisplayName("default constructor returns minimal runtime config")
    void defaultConstructor_called_returnsMinimalRuntimeConfig() {
        RealtimeServerProperties properties = new RealtimeServerProperties();

        assertEquals(false, properties.enabled());
        assertEquals("127.0.0.1", properties.host());
        assertEquals(18080, properties.port());
        assertEquals("/ws", properties.path());
    }

    /**
     * 验证非法路径会在配置对象创建阶段被拒绝。
     */
    @Test
    @DisplayName("constructor invalid path throws exception")
    void constructor_invalidPath_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RealtimeServerProperties(true, "0.0.0.0", 18080, "ws", 1, 0)
        );
    }
}

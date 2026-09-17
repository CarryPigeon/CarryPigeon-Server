package team.carrypigeon.backend.chat.domain.features.server.config;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RealtimeServerProperties 契约测试。
 * 职责：验证 Netty 实时通道配置的默认值和关键边界校验。
 * 边界：不验证 Spring 绑定流程，只验证配置语义本身。
 */
@Tag("unit")
class RealtimeServerPropertiesTests {

    /**
     * 验证默认构造能提供最小可运行的实时通道配置。
     */
    @Test
    @DisplayName("default constructor returns minimal runtime config")
    void defaultConstructor_called_returnsMinimalRuntimeConfig() {
        RealtimeServerProperties properties = new RealtimeServerProperties(
                false, "127.0.0.1", 18080, "/api/ws", 1, 0, 10, 60
        );

        assertEquals(false, properties.enabled());
        assertEquals("127.0.0.1", properties.host());
        assertEquals(18080, properties.port());
        assertEquals("/api/ws", properties.path());
        assertEquals(10, properties.authenticationTimeoutSeconds());
        assertEquals(60, properties.readIdleTimeoutSeconds());
        assertEquals(Duration.ofHours(1), properties.eventRetention());
        assertEquals(10_000, properties.maxEventAccounts());
    }

    /**
     * 验证非法路径会在配置对象创建阶段被拒绝。
     */
    @Test
    @DisplayName("constructor invalid path throws exception")
    void constructor_invalidPath_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RealtimeServerProperties(true, "0.0.0.0", 18080, "api/ws", 1, 0, 10, 60)
        );
    }

    /**
     * 验证鉴权与读空闲超时必须为正数，防止关闭连接保护。
     */
    @Test
    @DisplayName("constructor non-positive timeouts throws exception")
    void constructor_nonPositiveTimeouts_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RealtimeServerProperties(true, "0.0.0.0", 18080, "/api/ws", 1, 0, 0, 60)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RealtimeServerProperties(true, "0.0.0.0", 18080, "/api/ws", 1, 0, 10, 0)
        );
    }

    /**
     * 验证事件保留时间和账号窗口总数必须为正数。
     */
    @Test
    @DisplayName("constructor invalid event limits throws exception")
    void constructor_invalidEventLimits_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RealtimeServerProperties(
                        true, "0.0.0.0", 18080, "/api/ws", 1, 0, 10, 60, Duration.ZERO, 10_000
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RealtimeServerProperties(
                        true, "0.0.0.0", 18080, "/api/ws", 1, 0, 10, 60, Duration.ofHours(1), 0
                )
        );
    }
}

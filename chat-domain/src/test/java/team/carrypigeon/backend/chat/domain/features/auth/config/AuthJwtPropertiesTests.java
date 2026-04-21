package team.carrypigeon.backend.chat.domain.features.auth.config;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AuthJwtProperties 契约测试。
 * 职责：验证鉴权 JWT 配置的默认值与关键敏感配置校验边界。
 * 边界：不验证 Spring 绑定流程，只验证配置语义本身。
 */
class AuthJwtPropertiesTests {

    /**
     * 验证 issuer 与 TTL 在缺省时仍会回退到最小稳定默认值。
     */
    @Test
    @DisplayName("constructor missing issuer and ttl uses stable defaults")
    void constructor_missingIssuerAndTtl_usesStableDefaults() {
        AuthJwtProperties properties = new AuthJwtProperties("", "test-secret", null, null);

        assertEquals("carrypigeon", properties.issuer());
        assertEquals(Duration.ofMinutes(30), properties.accessTokenTtl());
        assertEquals(Duration.ofDays(14), properties.refreshTokenTtl());
    }

    /**
     * 验证缺失 JWT secret 时会在配置对象创建阶段被拒绝。
     */
    @Test
    @DisplayName("constructor missing secret throws exception")
    void constructor_missingSecret_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AuthJwtProperties("carrypigeon", "", Duration.ofMinutes(30), Duration.ofDays(14))
        );
    }
}

package team.carrypigeon.backend.infrastructure.service.cache.impl.startup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CacheInitializationCheck 契约测试。
 * 职责：验证缓存健康检查到共享启动检查契约的稳定转换。
 * 边界：不验证具体 Redis 连接细节，只验证 passed/failed 映射与名称契约。
 */
@Tag("contract")
class CacheInitializationCheckTests {

    /**
     * 验证健康检查通过时会映射为 passed 的初始化检查结果。
     */
    @Test
    @DisplayName("check available health returns passed result")
    void check_availableHealth_returnsPassedResult() {
        CacheInitializationCheck check = new CacheInitializationCheck(() -> new CacheHealth(true, "redis ready"));

        InitializationCheckResult result = check.check();

        assertEquals("cache", check.name());
        assertTrue(check.required());
        assertTrue(result.passed());
        assertEquals("redis ready", result.message());
    }

    /**
     * 验证健康检查失败时会映射为 failed 的初始化检查结果。
     */
    @Test
    @DisplayName("check unavailable health returns failed result")
    void check_unavailableHealth_returnsFailedResult() {
        CacheInitializationCheck check = new CacheInitializationCheck(() -> new CacheHealth(false, "redis unavailable"));

        InitializationCheckResult result = check.check();

        assertFalse(result.passed());
        assertEquals("redis unavailable", result.message());
    }
}

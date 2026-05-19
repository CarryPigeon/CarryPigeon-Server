package team.carrypigeon.backend.infrastructure.service.storage.impl.startup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import team.carrypigeon.backend.infrastructure.basic.startup.InitializationCheckResult;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealth;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealthService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StorageInitializationCheck 契约测试。
 * 职责：验证对象存储健康检查到共享启动检查契约的稳定转换。
 * 边界：不验证具体 MinIO 细节，只验证 passed/failed 映射与名称契约。
 */
@Tag("contract")
class StorageInitializationCheckTests {

    /**
     * 验证健康检查通过时会映射为 passed 的初始化检查结果。
     */
    @Test
    @DisplayName("check available health returns passed result")
    void check_availableHealth_returnsPassedResult() {
        StorageInitializationCheck check = new StorageInitializationCheck(() -> new StorageHealth(true, "storage bucket ready"));

        InitializationCheckResult result = check.check();

        assertEquals("storage", check.name());
        assertTrue(check.required());
        assertTrue(result.passed());
        assertEquals("storage bucket ready", result.message());
    }

    /**
     * 验证健康检查失败时会映射为 failed 的初始化检查结果。
     */
    @Test
    @DisplayName("check unavailable health returns failed result")
    void check_unavailableHealth_returnsFailedResult() {
        StorageInitializationCheck check = new StorageInitializationCheck(() -> new StorageHealth(false, "storage bucket missing"));

        InitializationCheckResult result = check.check();

        assertFalse(result.passed());
        assertEquals("storage bucket missing", result.message());
    }
}

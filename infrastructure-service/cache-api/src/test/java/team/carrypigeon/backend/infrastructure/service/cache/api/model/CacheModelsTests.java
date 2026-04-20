package team.carrypigeon.backend.infrastructure.service.cache.api.model;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证缓存 API 模型的最小契约。
 * 职责：确保缓存键、缓存 TTL 和缓存条目遵守稳定输入约束。
 * 边界：不验证 Redis 行为，只验证 API 值对象自身语义。
 */
class CacheModelsTests {

    /**
     * 测试合法缓存条目。
     * 输入：非空缓存键、非空缓存值和正数 TTL。
     * 期望：缓存模型创建成功并保留原始值。
     */
    @Test
    void constructor_validEntry_keepsValues() {
        CacheEntry entry = new CacheEntry(new CacheKey("user:1"), "value", new CacheTtl(Duration.ofMinutes(1)));

        assertEquals("user:1", entry.key().value());
        assertEquals("value", entry.value());
        assertEquals(Duration.ofMinutes(1), entry.ttl().value());
    }

    /**
     * 测试非法缓存键和 TTL。
     * 输入：空白缓存键或非正 TTL。
     * 期望：值对象拒绝创建，避免非法缓存契约流入实现层。
     */
    @Test
    void constructor_invalidKeyOrTtl_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new CacheKey(" "));
        assertThrows(IllegalArgumentException.class, () -> new CacheTtl(Duration.ZERO));
    }
}

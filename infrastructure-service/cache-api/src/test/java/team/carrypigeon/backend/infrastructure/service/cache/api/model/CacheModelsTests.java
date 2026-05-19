package team.carrypigeon.backend.infrastructure.service.cache.api.model;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证缓存 API 模型的最小契约。
 * 职责：确保缓存键、缓存 TTL 和缓存条目遵守稳定输入约束。
 * 边界：不验证 Redis 行为，只验证 API 值对象自身语义。
 */
@Tag("unit")
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
        IllegalArgumentException keyException = assertThrows(IllegalArgumentException.class, () -> new CacheKey(" "));
        IllegalArgumentException ttlException = assertThrows(IllegalArgumentException.class, () -> new CacheTtl(Duration.ZERO));

        assertEquals("cache key must not be blank", keyException.getMessage());
        assertEquals("cache ttl must be positive", ttlException.getMessage());
    }

    /**
     * 测试缓存条目拒绝 null 字段。
     * 输入：缺失键、值或 TTL 的缓存条目。
     * 期望：条目构造失败，并给出稳定错误信息。
     */
    @Test
    void constructor_nullEntryFields_rejected() {
        IllegalArgumentException nullKeyException = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheEntry(null, "value", new CacheTtl(Duration.ofMinutes(1)))
        );
        IllegalArgumentException nullValueException = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheEntry(new CacheKey("user:1"), null, new CacheTtl(Duration.ofMinutes(1)))
        );
        IllegalArgumentException nullTtlException = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheEntry(new CacheKey("user:1"), "value", null)
        );

        assertEquals("cache key must not be null", nullKeyException.getMessage());
        assertEquals("cache value must not be null", nullValueException.getMessage());
        assertEquals("cache ttl must not be null", nullTtlException.getMessage());
    }

    /**
     * 测试缓存键和 TTL 拒绝 null 与负数值。
     * 输入：null 缓存键、null TTL、负数 TTL。
     * 期望：值对象构造失败并暴露稳定错误语义。
     */
    @Test
    void constructor_nullOrNegativeInputs_rejected() {
        IllegalArgumentException nullKeyException = assertThrows(IllegalArgumentException.class, () -> new CacheKey(null));
        IllegalArgumentException nullTtlException = assertThrows(IllegalArgumentException.class, () -> new CacheTtl(null));
        IllegalArgumentException negativeTtlException = assertThrows(
                IllegalArgumentException.class,
                () -> new CacheTtl(Duration.ofSeconds(-1))
        );

        assertEquals("cache key must not be blank", nullKeyException.getMessage());
        assertEquals("cache ttl must be positive", nullTtlException.getMessage());
        assertEquals("cache ttl must be positive", negativeTtlException.getMessage());
    }
}

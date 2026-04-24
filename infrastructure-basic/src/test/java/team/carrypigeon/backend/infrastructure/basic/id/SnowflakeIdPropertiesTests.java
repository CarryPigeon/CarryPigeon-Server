package team.carrypigeon.backend.infrastructure.basic.id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证雪花 ID 配置属性的最小契约。
 * 职责：确保 workerId 与 datacenterId 有稳定默认值并符合 Hutool Snowflake 范围约束。
 * 边界：不验证 Spring Boot 配置绑定，只验证属性对象自身约束。
 */
@Tag("unit")
class SnowflakeIdPropertiesTests {

    /**
     * 测试默认构造。
     * 输入：不传入任何节点配置。
     * 期望：使用安全默认值 workerId=1、datacenterId=1。
     */
    @Test
    void constructor_default_usesSafeNodeIds() {
        SnowflakeIdProperties properties = new SnowflakeIdProperties();

        assertEquals(1, properties.workerId());
        assertEquals(1, properties.datacenterId());
    }

    /**
     * 测试合法边界值。
     * 输入：workerId=0、datacenterId=31。
     * 期望：属性对象创建成功，说明闭区间 0..31 被接受。
     */
    @Test
    void constructor_validRange_accepted() {
        assertDoesNotThrow(() -> new SnowflakeIdProperties(0, 31));
    }

    /**
     * 测试非法边界值。
     * 输入：大于 31 或小于 0 的节点配置。
     * 期望：属性对象拒绝创建，避免运行时生成器使用非法 Snowflake 节点参数。
     */
    @Test
    void constructor_outOfRange_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdProperties(32, 1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdProperties(1, -1));
    }
}

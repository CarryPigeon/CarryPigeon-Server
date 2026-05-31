package team.carrypigeon.backend.infrastructure.basic.id;

import cn.hutool.core.lang.Snowflake;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证雪花 ID 生成器的最小契约。
 * 职责：确保项目统一 ID 入口能生成正数且连续调用不重复。
 * 边界：不验证 Hutool Snowflake 内部算法，只验证项目侧使用契约。
 */
@Tag("unit")
class SnowflakeIdGeneratorTests {

    /**
     * 测试连续生成两个 ID。
     * 输入：同一个 SnowflakeIdGenerator 实例。
     * 期望：两次生成结果均为正数，且彼此不同。
     */
    @Test
    void nextLongId_calledTwice_generatesDifferentPositiveIds() {
        IdGenerator idGenerator = new SnowflakeIdGenerator(new Snowflake(1, 1));

        long first = idGenerator.nextLongId();
        long second = idGenerator.nextLongId();

        assertTrue(first > 0);
        assertTrue(second > 0);
        assertNotEquals(first, second);
    }

    /**
     * 测试字符串形式 ID 输出遵循十进制字符串契约。
     * 输入：同一个 SnowflakeIdGenerator 实例。
     * 期望：`nextStringId()` 输出仅包含数字字符，且可被项目统一 ID 工具无损解析与回写。
     */
    @Test
    void nextStringId_called_returnsDecimalString() {
        IdGenerator idGenerator = new SnowflakeIdGenerator(new Snowflake(1, 1));

        String stringId = idGenerator.nextStringId();

        assertEquals(Ids.toString(Ids.parse(stringId)), stringId);
        assertTrue(stringId.chars().allMatch(Character::isDigit));
    }
}

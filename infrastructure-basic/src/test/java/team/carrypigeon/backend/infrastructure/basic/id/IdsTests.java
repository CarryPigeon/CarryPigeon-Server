package team.carrypigeon.backend.infrastructure.basic.id;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Ids 契约测试。
 * 职责：验证统一雪花 ID 文本编码工具的最小稳定行为。
 * 边界：只验证项目侧字符串编码与解析契约，不验证具体雪花算法实现。
 */
@Tag("unit")
class IdsTests {

    /**
     * 验证 long 类型雪花 ID 会被稳定编码为十进制字符串。
     */
    @Test
    void toString_positiveLongId_returnsDecimalString() {
        assertEquals("723155640365318144", Ids.toString(723155640365318144L));
    }

    /**
     * 验证十进制字符串雪花 ID 可以被解析回 long。
     */
    @Test
    void parse_decimalStringId_returnsLongValue() {
        assertEquals(723155640365318144L, Ids.parse("723155640365318144"));
    }

    /**
     * 验证空白字符串不会被接受为合法 ID。
     */
    @Test
    void parse_blankId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Ids.parse(" "));
    }
}

package team.carrypigeon.backend.common.id;

import cn.hutool.core.codec.Base64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdUtilTests {

    @Test
    void generateId_shouldBePositiveAndChangeAcrossCalls() {
        new IdUtil();
        long first = IdUtil.generateId();
        long second = IdUtil.generateId();
        assertTrue(first > 0);
        assertTrue(second > 0);
        assertNotEquals(first, second);
    }

    @Test
    void generateToken_shouldBeBase64OfPositiveLongString() {
        String token = IdUtil.generateToken();
        assertNotNull(token);
        assertFalse(token.isBlank());

        String decoded = Base64.decodeStr(token);
        assertNotNull(decoded);
        assertFalse(decoded.isBlank());

        long id = Long.parseLong(decoded);
        assertTrue(id > 0);
    }
}

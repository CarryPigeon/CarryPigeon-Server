package team.carrypigeon.backend.common.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilTests {

    @Test
    void getCurrentTime_shouldBeBetweenBeforeAndAfterSystemMillis() {
        long before = System.currentTimeMillis();
        long now = TimeUtil.currentTimeMillis();
        long after = System.currentTimeMillis();

        assertTrue(now >= before);
        assertTrue(now <= after);
    }

    @Test
    void localDateTimeToMillis_andMillisToLocalDateTime_roundTrip() {
        LocalDateTime input = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        long millis = TimeUtil.localDateTimeToMillis(input);
        LocalDateTime output = TimeUtil.millisToLocalDateTime(millis).truncatedTo(ChronoUnit.MILLIS);

        assertEquals(input, output);
    }

    @Test
    void getCurrentLocalTime_shouldBeNonNull() {
        assertNotNull(TimeUtil.currentLocalDateTime());
    }
}

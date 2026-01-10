package team.carrypigeon.backend.common.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilTests {

    @Test
    void getCurrentTime_shouldBeBetweenBeforeAndAfterSystemMillis() {
        new TimeUtil();
        long before = System.currentTimeMillis();
        long now = TimeUtil.getCurrentTime();
        long after = System.currentTimeMillis();

        assertTrue(now >= before);
        assertTrue(now <= after);
    }

    @Test
    void localDateTimeToMillis_andMillisToLocalDateTime_roundTrip() {
        LocalDateTime input = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        long millis = TimeUtil.LocalDateTimeToMillis(input);
        LocalDateTime output = TimeUtil.MillisToLocalDateTime(millis).truncatedTo(ChronoUnit.MILLIS);

        assertEquals(input, output);
    }

    @Test
    void getCurrentLocalTime_shouldBeNonNull() {
        assertNotNull(TimeUtil.getCurrentLocalTime());
    }
}

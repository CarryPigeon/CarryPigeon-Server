package team.carrypigeon.backend.infrastructure.basic.time;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 时间相关基础转换工具。
 * 职责：统一毫秒时间戳与 Java 时间对象之间的基础转换。
 */
public final class TimeFormats {

    private TimeFormats() {
    }

    public static Instant fromMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis);
    }

    public static long toMillis(Instant instant) {
        return instant.toEpochMilli();
    }

    public static LocalDateTime toLocalDateTime(long epochMillis, ZoneId zoneId) {
        return LocalDateTime.ofInstant(fromMillis(epochMillis), zoneId);
    }
}

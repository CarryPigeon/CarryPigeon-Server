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

    /**
     * 将毫秒时间戳转换为 {@link Instant}。
     * 输入：Unix epoch 毫秒值。
     * 输出：对应的 UTC 时间点对象。
     *
     * @param epochMillis Unix epoch 毫秒值
     * @return 对应的时间点对象
     */
    public static Instant fromMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis);
    }

    /**
     * 将 {@link Instant} 转换为毫秒时间戳。
     * 输入：业务或基础设施侧使用的时间点对象。
     * 输出：可用于存储或协议传输的 epoch 毫秒值。
     *
     * @param instant 时间点对象
     * @return epoch 毫秒值
     */
    public static long toMillis(Instant instant) {
        return instant.toEpochMilli();
    }

    /**
     * 按指定时区将毫秒时间戳转换为本地日期时间。
     * 输入：Unix epoch 毫秒值与目标时区。
     * 输出：目标时区下的本地日期时间表示。
     *
     * @param epochMillis Unix epoch 毫秒值
     * @param zoneId 目标时区
     * @return 目标时区下的本地日期时间
     */
    public static LocalDateTime toLocalDateTime(long epochMillis, ZoneId zoneId) {
        return LocalDateTime.ofInstant(fromMillis(epochMillis), zoneId);
    }
}

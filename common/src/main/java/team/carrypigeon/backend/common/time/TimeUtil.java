package team.carrypigeon.backend.common.time;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 时间相关工具类。
 * <p>
 * 本项目内“时间戳”默认指 {@code epoch millis}（毫秒），并以服务端默认时区做 {@link LocalDateTime} 的转换。
 * </p>
 */
public class TimeUtil {
    private TimeUtil() {
    }

    /**
     * 获取当前时间戳（毫秒）。
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前本地时间（{@link LocalDateTime}）。
     */
    public static LocalDateTime currentLocalDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 将 {@link LocalDateTime}（按系统默认时区解释）转换为时间戳（毫秒）。
     */
    public static long localDateTimeToMillis(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 将时间戳（毫秒）转换为 {@link LocalDateTime}。
     */
    public static LocalDateTime millisToLocalDateTime(long millis) {
        return LocalDateTimeUtil.of(millis);
    }

    /**
     * @deprecated 使用 {@link #currentTimeMillis()}。
     */
    @Deprecated
    public static long getCurrentTime() {
        return currentTimeMillis();
    }

    /**
     * @deprecated 使用 {@link #currentLocalDateTime()}。
     */
    @Deprecated
    public static LocalDateTime getCurrentLocalTime() {
        return currentLocalDateTime();
    }

    /**
     * @deprecated 使用 {@link #localDateTimeToMillis(LocalDateTime)}。
     */
    @Deprecated
    public static long LocalDateTimeToMillis(LocalDateTime localDateTime) {
        return localDateTimeToMillis(localDateTime);
    }

    /**
     * @deprecated 使用 {@link #millisToLocalDateTime(long)}。
     */
    @Deprecated
    public static LocalDateTime MillisToLocalDateTime(long millis) {
        return millisToLocalDateTime(millis);
    }
}

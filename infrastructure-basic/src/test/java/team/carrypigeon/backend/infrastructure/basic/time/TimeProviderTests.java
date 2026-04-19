package team.carrypigeon.backend.infrastructure.basic.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证统一时间访问入口的最小契约。
 * 职责：确保 TimeProvider 基于注入的 Clock 返回稳定时间。
 * 边界：不验证系统真实时间，只验证项目侧时间访问契约。
 */
class TimeProviderTests {

    /**
     * 测试固定 Clock 下的毫秒时间戳。
     * 输入：固定在 epoch+1000ms 的 Clock。
     * 期望：nowMillis 返回 1000。
     */
    @Test
    void nowMillis_fixedClock_returnsConfiguredMillis() {
        TimeProvider timeProvider = new TimeProvider(Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC));

        assertEquals(1000, timeProvider.nowMillis());
    }

    /**
     * 测试固定 Clock 下的 LocalDateTime。
     * 输入：固定在 epoch+1000ms 且时区为 UTC 的 Clock。
     * 期望：nowLocalDateTime 返回对应 UTC 本地时间。
     */
    @Test
    void nowLocalDateTime_fixedClock_returnsConfiguredLocalDateTime() {
        TimeProvider timeProvider = new TimeProvider(Clock.fixed(Instant.ofEpochMilli(1000), ZoneOffset.UTC));

        assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0, 1), timeProvider.nowLocalDateTime());
    }
}

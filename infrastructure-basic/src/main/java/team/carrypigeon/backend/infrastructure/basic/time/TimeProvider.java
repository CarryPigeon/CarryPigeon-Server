package team.carrypigeon.backend.infrastructure.basic.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * 统一时间访问入口。
 * 职责：收敛项目中的当前时间读取逻辑，降低直接调用系统时间的分散性。
 * 依赖：基于全局 Clock 提供时间读取能力。
 */
@Component
public class TimeProvider {

    private final Clock clock;

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    /**
     * @return 当前时刻的 Instant
     */
    public Instant nowInstant() {
        return Instant.now(clock);
    }

    /**
     * @return 当前时刻的毫秒时间戳
     */
    public long nowMillis() {
        return nowInstant().toEpochMilli();
    }

    /**
     * @return 当前默认时区下的 LocalDateTime
     */
    public LocalDateTime nowLocalDateTime() {
        return LocalDateTime.ofInstant(nowInstant(), clock.getZone());
    }

    /**
     * @return 当前时钟使用的时区
     */
    public ZoneId zoneId() {
        return clock.getZone();
    }
}

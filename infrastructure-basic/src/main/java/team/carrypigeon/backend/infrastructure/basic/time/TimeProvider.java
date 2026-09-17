package team.carrypigeon.backend.infrastructure.basic.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 统一时间访问接口
 * 职责：收敛项目中的当前时间读取逻辑，降低直接调用系统时间的分散性。
 * 依赖：基于全局 Clock 提供时间读取能力。
 */
public interface TimeProvider {


    /**
     * @return 当前时刻的 Instant
     */
    Instant nowInstant();

    /**
     * @return 当前时刻的毫秒时间戳
     */
    long nowMillis();

    /**
     * @return 当前默认时区下的 LocalDateTime
     */
    LocalDateTime nowLocalDateTime();

    /**
     * @return 当前时钟使用的时区
     */
    ZoneId zoneId();
}

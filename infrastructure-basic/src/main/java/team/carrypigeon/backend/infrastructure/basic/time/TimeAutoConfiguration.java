package team.carrypigeon.backend.infrastructure.basic.time;

import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 全局时间基础设施自动配置。
 * 职责：提供统一的时间来源，避免业务代码直接散落调用系统时间。
 * 边界：这里只提供基础时间能力，不承载业务时间策略。
 */
@AutoConfiguration
public class TimeAutoConfiguration {

    /**
     * 提供系统默认时区下的统一时钟。
     *
     * @return 项目默认 Clock Bean
     */
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}

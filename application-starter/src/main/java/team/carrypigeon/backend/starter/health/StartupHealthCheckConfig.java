package team.carrypigeon.backend.starter.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;

/**
 * 应用启动时的基础依赖自检。
 *
 * - 在 Spring Boot 完成上下文初始化后，主动探测 MySQL 与 Redis 是否可用；
 * - 任一关键依赖不可用时，抛出异常终止启动（进程以非 0 状态退出）。
 *
 * 开关：
 *   cp.startup.health-check.enabled=true|false
 * 默认开启，如需在开发或特殊环境关闭自检，可在配置中显式设为 false。
 */
@Slf4j
@Configuration
@ConditionalOnClass({DataSource.class, StringRedisTemplate.class})
@ConditionalOnProperty(prefix = "cp.startup.health-check",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class StartupHealthCheckConfig {

    /**
     * 启动时自检 runner：在 Spring Boot 初始化完成后探测 MySQL 与 Redis 的可用性。
     */
    @Bean
    public ApplicationRunner startupHealthChecker(DataSource dataSource,
                                                 StringRedisTemplate stringRedisTemplate) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                checkMySql(dataSource);
                checkRedis(stringRedisTemplate);
                log.info("Startup health-check passed: MySQL and Redis are reachable.");
            }
        };
    }

    private void checkMySql(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
            ps.execute();
            log.info("MySQL health-check success.");
        } catch (Exception e) {
            log.error("MySQL health-check failed, aborting startup.", e);
            throw new IllegalStateException("MySQL is not available, aborting startup.", e);
        }
    }

    private void checkRedis(StringRedisTemplate stringRedisTemplate) {
        String key = "cp:startup:health-check";
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, "ok", Duration.ofSeconds(5));
            stringRedisTemplate.delete(key);
            log.info("Redis health-check success.");
        } catch (Exception e) {
            log.error("Redis health-check failed, aborting startup.", e);
            throw new IllegalStateException("Redis is not available, aborting startup.", e);
        }
    }
}

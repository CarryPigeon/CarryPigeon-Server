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
 * 应用启动依赖健康检查配置。
 * <p>
 * 容器启动后主动探测 MySQL 与 Redis，可用性不足时立即中止启动。
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
     * 创建启动健康检查 Runner。
     *
     * @param dataSource 数据源。
     * @param stringRedisTemplate Redis 模板。
     * @return 启动健康检查 Runner。
     */
    @Bean
    public ApplicationRunner startupHealthChecker(DataSource dataSource,
                                                  StringRedisTemplate stringRedisTemplate) {
        return new ApplicationRunner() {
            /**
             * 执行启动健康检查。
             *
             * @param args 启动参数。
             */
            @Override
            public void run(ApplicationArguments args) {
                checkMySql(dataSource);
                checkRedis(stringRedisTemplate);
                log.info("Startup health-check passed: MySQL and Redis are reachable.");
            }
        };
    }

    /**
     * 检查 MySQL 可用性。
     *
     * @param dataSource 数据源。
     */
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

    /**
     * 检查 Redis 可用性。
     *
     * @param stringRedisTemplate Redis 模板。
     */
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

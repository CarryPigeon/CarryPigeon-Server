package team.carrypigeon.backend.starter.env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import team.carrypigeon.backend.infrastructure.service.cache.api.health.CacheHealthService;
import team.carrypigeon.backend.infrastructure.service.database.api.health.DatabaseHealthService;
import team.carrypigeon.backend.infrastructure.service.storage.api.health.StorageHealthService;
import team.carrypigeon.backend.starter.ApplicationStarter;

/**
 * Real application environment tests for application-starter.
 * Contract: the final Spring Boot assembly can start with real database, cache, and storage service implementations.
 * Boundary: this test runs only when explicitly enabled and does not replace service-specific env data round trips.
 */
@Tag("env")
@Tag("env-app")
class ApplicationStarterEnvTests {

    /**
     * Verifies the real application assembly can start and expose external-service health beans.
     */
    @Test
    @DisplayName("real application assembly starts with external service health beans")
    void applicationStarter_realEnvironment_startsWithExternalServiceHealthBeans() {
        EnvApplicationSettings settings = EnvApplicationSettings.fromEnvironment();
        assumeTrue(settings.enabled(), "set CP_ENV_APP_TEST_ENABLED=true or -Dcp.env.app.test.enabled=true to run env-app tests");

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ApplicationStarter.class)
                .web(WebApplicationType.NONE)
                .properties(settings.properties())
                .run()) {
            assertThat(context.getBeansOfType(DatabaseHealthService.class)).hasSize(1);
            assertThat(context.getBeansOfType(CacheHealthService.class)).hasSize(1);
            assertThat(context.getBeansOfType(StorageHealthService.class)).hasSize(1);
        }
    }

    private record EnvApplicationSettings(boolean enabled, String[] properties) {

        static EnvApplicationSettings fromEnvironment() {
            String mysqlHost = value("cp.env.app.mysql.host", "CP_ENV_APP_MYSQL_HOST", "127.0.0.1");
            String mysqlPort = value("cp.env.app.mysql.port", "CP_ENV_APP_MYSQL_PORT", "3306");
            String mysqlDatabase = value("cp.env.app.mysql.database", "CP_ENV_APP_MYSQL_DATABASE", "carrypigeon");
            String redisHost = value("cp.env.app.redis.host", "CP_ENV_APP_REDIS_HOST", "127.0.0.1");
            String redisPort = value("cp.env.app.redis.port", "CP_ENV_APP_REDIS_PORT", "6379");
            String minioEndpoint = value("cp.env.app.minio.endpoint", "CP_ENV_APP_MINIO_ENDPOINT", "http://127.0.0.1:9000");
            return new EnvApplicationSettings(
                    booleanValue("cp.env.app.test.enabled", "CP_ENV_APP_TEST_ENABLED"),
                    new String[] {
                            "spring.main.banner-mode=off",
                            "spring.datasource.url=jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase
                                    + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                            "spring.datasource.username=" + value("cp.env.app.mysql.username", "CP_ENV_APP_MYSQL_USERNAME", "carrypigeon"),
                            "spring.datasource.password=" + value("cp.env.app.mysql.password", "CP_ENV_APP_MYSQL_PASSWORD", "carrypigeon123"),
                            "spring.data.redis.host=" + redisHost,
                            "spring.data.redis.port=" + redisPort,
                            "spring.data.redis.password=" + value("cp.env.app.redis.password", "CP_ENV_APP_REDIS_PASSWORD", "carrypigeon123"),
                            "cp.infrastructure.service.storage.endpoint=" + minioEndpoint,
                            "cp.infrastructure.service.storage.access-key="
                                    + value("cp.env.app.minio.access-key", "CP_ENV_APP_MINIO_ACCESS_KEY", "carrypigeon"),
                            "cp.infrastructure.service.storage.secret-key="
                                    + value("cp.env.app.minio.secret-key", "CP_ENV_APP_MINIO_SECRET_KEY", "carrypigeon123"),
                            "cp.infrastructure.service.storage.bucket="
                                    + value("cp.env.app.minio.bucket", "CP_ENV_APP_MINIO_BUCKET", "carrypigeon"),
                            "cp.infrastructure.service.mail.enabled=false",
                            "cp.chat.server.realtime.enabled=false"
                    }
            );
        }

        private static boolean booleanValue(String propertyName, String envName) {
            return Boolean.parseBoolean(value(propertyName, envName, "false"));
        }

        private static String value(String propertyName, String envName, String defaultValue) {
            String propertyValue = System.getProperty(propertyName);
            if (propertyValue != null && !propertyValue.isBlank()) {
                return propertyValue;
            }
            String envValue = System.getenv(envName);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
            return defaultValue;
        }
    }
}

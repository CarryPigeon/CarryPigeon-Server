package team.carrypigeon.backend.infrastructure.service.cache.impl.env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import team.carrypigeon.backend.infrastructure.service.cache.impl.config.CacheServiceProperties;
import team.carrypigeon.backend.infrastructure.service.cache.impl.health.RedisCacheHealthService;
import team.carrypigeon.backend.infrastructure.service.cache.impl.redis.RedisCacheService;

/**
 * Real Redis environment tests for cache-impl.
 * Contract: RedisCacheService can write, read, check, and delete namespaced cache data against real Redis.
 * Boundary: this test runs only when explicitly enabled and never falls back to a mock template.
 */
@Tag("env")
@Tag("env-cache")
class RedisCacheServiceEnvTests {

    /**
     * Verifies Redis read/write/delete behavior against a real Redis service.
     */
    @Test
    @DisplayName("real redis cache round trip")
    void cacheService_realRedis_roundTripAndCleanup() {
        EnvRedisSettings settings = EnvRedisSettings.fromEnvironment();
        assumeTrue(settings.enabled(), "set CP_ENV_CACHE_TEST_ENABLED=true or -Dcp.env.cache.test.enabled=true to run env-cache tests");

        LettuceConnectionFactory connectionFactory = settings.connectionFactory();
        connectionFactory.afterPropertiesSet();
        try {
            runCacheRoundTrip(connectionFactory);
        } finally {
            connectionFactory.destroy();
        }
    }

    private static void runCacheRoundTrip(LettuceConnectionFactory connectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        RedisCacheHealthService healthService = new RedisCacheHealthService(redisTemplate);
        assumeTrue(healthService.check().available(), "real Redis service is not available");

        RedisCacheService cacheService = new RedisCacheService(redisTemplate, new CacheServiceProperties(true, Duration.ofMinutes(5)));
        String key = namespacedKey("cache-round-trip", "value");

        try {
            cacheService.set(key, "payload", Duration.ofMinutes(1));

            assertTrue(cacheService.exists(key));
            assertEquals("payload", cacheService.get(key).orElseThrow());

            cacheService.delete(key);

            assertFalse(cacheService.exists(key));
            assertTrue(cacheService.get(key).isEmpty());
        } finally {
            redisTemplate.delete(key);
        }
    }

    private static String namespacedKey(String caseName, String suffix) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        return "it_" + timestamp + "_" + sanitize(caseName) + "_" + sanitize(suffix);
    }

    private static String sanitize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
    }

    private record EnvRedisSettings(boolean enabled, String host, int port, String password, int database) {

        static EnvRedisSettings fromEnvironment() {
            return new EnvRedisSettings(
                    booleanValue("cp.env.cache.test.enabled", "CP_ENV_CACHE_TEST_ENABLED"),
                    value("cp.env.cache.host", "CP_ENV_CACHE_HOST", "127.0.0.1"),
                    Integer.parseInt(value("cp.env.cache.port", "CP_ENV_CACHE_PORT", "6379")),
                    value("cp.env.cache.password", "CP_ENV_CACHE_PASSWORD", "carrypigeon123"),
                    Integer.parseInt(value("cp.env.cache.database", "CP_ENV_CACHE_DATABASE", "0"))
            );
        }

        LettuceConnectionFactory connectionFactory() {
            RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host, port);
            configuration.setDatabase(database);
            if (password != null && !password.isBlank()) {
                configuration.setPassword(RedisPassword.of(password));
            }
            return new LettuceConnectionFactory(configuration);
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

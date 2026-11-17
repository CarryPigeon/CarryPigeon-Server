package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * 数据库服务配置。
 * 职责：声明 MySQL 连接参数与数据库查询缓存参数。
 * 边界：只描述数据库服务自身参数，不承载具体缓存键规则与业务查询选择。
 *
 * <p>缓存参数默认值面向“高频只读、短 TTL、写后精确失效”：
 * 实体负载默认 5 分钟，唯一键 lookup 指针默认 5 分钟，
 * 单次批量缓存读取最多 128 个主键（超过直接读库）。</p>
 *
 * @param enabled 是否启用数据库服务
 * @param url MySQL 连接地址
 * @param username MySQL 用户名
 * @param password MySQL 密码
 * @param driverClassName MySQL 驱动类名
 * @param connectionTimeout 获取连接超时
 * @param validationTimeout 连接有效性检查超时
 * @param cacheEntityTtl 主键实体负载缓存 TTL
 * @param cacheLookupTtl 唯一键 lookup 指针缓存 TTL
 * @param cacheMaxBatchSize 单次批量缓存读取允许的最大主键数量
 */
@ConfigurationProperties(prefix = "service.database.mysql")
public record DatabaseServiceProperties(
        boolean enabled,
        String url,
        String username,
        String password,
        String driverClassName,
        Duration connectionTimeout,
        Duration validationTimeout,
        Duration cacheEntityTtl,
        Duration cacheLookupTtl,
        int cacheMaxBatchSize
) {
    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofMillis(10000);
    private static final Duration DEFAULT_VALIDATION_TIMEOUT = Duration.ofMillis(5000);
    private static final Duration DEFAULT_CACHE_ENTITY_TTL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_CACHE_LOOKUP_TTL = Duration.ofMinutes(5);
    private static final int DEFAULT_CACHE_MAX_BATCH_SIZE = 128;

    @ConstructorBinding
    public DatabaseServiceProperties {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("service.database.mysql.url must not be blank when database is enabled");
        }
        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
        if (driverClassName == null || driverClassName.isBlank()) {
            driverClassName = "com.mysql.cj.jdbc.Driver";
        }
        connectionTimeout = positiveOrDefault(connectionTimeout, DEFAULT_CONNECTION_TIMEOUT, "connection-timeout");
        validationTimeout = positiveOrDefault(validationTimeout, DEFAULT_VALIDATION_TIMEOUT, "validation-timeout");
        cacheEntityTtl = positiveOrDefault(cacheEntityTtl, DEFAULT_CACHE_ENTITY_TTL, "cache-entity-ttl");
        cacheLookupTtl = positiveOrDefault(cacheLookupTtl, DEFAULT_CACHE_LOOKUP_TTL, "cache-lookup-ttl");
        // 普通展示数据只接受最多 5 分钟的缓存 TTL，授权数据仍必须直读。
        if (cacheEntityTtl.compareTo(Duration.ofMinutes(5)) > 0 || cacheLookupTtl.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("database cache TTLs must not exceed 5 minutes");
        }
        if (cacheMaxBatchSize == 0) {
            cacheMaxBatchSize = DEFAULT_CACHE_MAX_BATCH_SIZE;
        } else if (cacheMaxBatchSize < 0) {
            throw new IllegalArgumentException("service.database.mysql.cache-max-batch-size must be positive");
        }
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue, String propertyName) {
        Duration resolved = value == null ? defaultValue : value;
        if (resolved.isZero() || resolved.isNegative()) {
            throw new IllegalArgumentException("service.database.mysql." + propertyName + " must be positive");
        }
        return resolved;
    }
}

package team.carrypigeon.backend.infrastructure.basic.id;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 雪花 ID 节点配置。
 * 职责：为全局 ID 生成器提供 workerId 与 datacenterId。
 * 约束：Hutool Snowflake 要求两个值都处于 0 到 31。
 */
@ConfigurationProperties(prefix = "cp.infrastructure.id")
public record SnowflakeIdProperties(long workerId, long datacenterId) {

    public SnowflakeIdProperties {
        validate("workerId", workerId);
        validate("datacenterId", datacenterId);
    }

    public SnowflakeIdProperties() {
        this(1, 1);
    }

    private static void validate(String fieldName, long value) {
        if (value < 0 || value > 31) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 31");
        }
    }
}

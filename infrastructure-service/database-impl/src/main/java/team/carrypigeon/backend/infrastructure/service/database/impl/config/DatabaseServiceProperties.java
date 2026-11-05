package team.carrypigeon.backend.infrastructure.service.database.impl.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "service.database.mysql")
public record DatabaseServiceProperties(
        boolean enabled,
        String url,
        String username,
        String password,
        String driverClassName,
        Duration connectionTimeout,
        Duration validationTimeout
) {
    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofMillis(10000);
    private static final Duration DEFAULT_VALIDATION_TIMEOUT = Duration.ofMillis(5000);

    @ConstructorBinding
    public DatabaseServiceProperties {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("cp.service.database.mysql.url must not be blank when database is enabled");
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
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue, String propertyName) {
        Duration resolved = value == null ? defaultValue : value;
        if (resolved.isZero() || resolved.isNegative()) {
            throw new IllegalArgumentException("cp.service.database.mysql." + propertyName + " must be positive");
        }
        return resolved;
    }

}

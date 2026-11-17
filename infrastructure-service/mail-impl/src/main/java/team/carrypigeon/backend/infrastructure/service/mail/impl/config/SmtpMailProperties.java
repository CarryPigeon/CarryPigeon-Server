package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "service.mail.smtp")
public record SmtpMailProperties(
        boolean enabled,
        String host,
        int port,
        String username,
        String password,
        String fromAddress,
        boolean auth,
        int connectionTimeout,
        int timeout,
        int writeTimeout,
        boolean starttlsEnable
) {
    private static final int DEFAULT_PORT = 587;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final int DEFAULT_TIMEOUT = 10000;
    private static final int DEFAULT_WRITE_TIMEOUT = 10000;

    @ConstructorBinding
    public SmtpMailProperties {
        if (host == null) {
            host = "";
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (username == null) {
            username = "";
        }
        if (password == null) {
            password = "";
        }
        if (fromAddress == null) {
            fromAddress = "";
        }
        if (connectionTimeout <= 0) {
            connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        }
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        if (writeTimeout <= 0) {
            writeTimeout = DEFAULT_WRITE_TIMEOUT;
        }
        if (enabled && fromAddress.isBlank()) {
            throw new IllegalArgumentException("service.mail.smtp.from-address must not be blank when mail is enabled");
        }
    }

    public SmtpMailProperties() {
        this(false, "", DEFAULT_PORT, "", "", "", true, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_WRITE_TIMEOUT, true);
    }
}

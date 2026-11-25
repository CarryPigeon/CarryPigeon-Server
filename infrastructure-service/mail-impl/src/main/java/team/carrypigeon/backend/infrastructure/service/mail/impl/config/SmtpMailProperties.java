package team.carrypigeon.backend.infrastructure.service.mail.impl.config;

import static team.carrypigeon.backend.infrastructure.basic.environment.EnvironmentComponent.PROD;

import team.carrypigeon.backend.infrastructure.basic.environment.InEnvironment;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@InEnvironment(profiles = PROD)
@ConfigurationProperties(prefix = "service.mail.smtp")
public record SmtpMailProperties(
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
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("service.mail.smtp.host must not be blank");
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
        if (fromAddress.isBlank()) {
            throw new IllegalArgumentException("service.mail.smtp.from-address must not be blank");
        }
    }

}

package team.carrypigeon.backend.api.starter.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * General server information that is configured via application.yaml.
 * All fields here are mapped from the {@code cp.server.*} configuration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cp.server")
public class ServerInfoConfig {

    /**
     * Server display name, mapped from {@code cp.server.server_name}.
     */
    @JsonProperty("server_name")
    private String serverName;

    /**
     * Avatar image id or key, mapped from {@code cp.server.avatar}.
     */
    @JsonProperty("avatar")
    private String avatar;

    /**
     * Short introduction of this server, mapped from {@code cp.server.brief}.
     */
    @JsonProperty("brief")
    private String brief;

    /**
     * Created time of this server in milliseconds since epoch,
     * mapped from {@code cp.server.time}.
     */
    @JsonProperty("time")
    private Long time;
}

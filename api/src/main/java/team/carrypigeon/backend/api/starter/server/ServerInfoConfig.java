package team.carrypigeon.backend.api.starter.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 服务端基础信息配置。
 * <p>
 * 从 `cp.server.*` 配置前缀映射，用于 API Server 信息输出与客户端展示。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cp.server")
public class ServerInfoConfig {

    /**
     * 服务端稳定标识（`cp.server.server_id`）。
     */
    @JsonProperty("server_id")
    private String serverId;

    /**
     * 服务端展示名称（`cp.server.server_name`）。
     */
    @JsonProperty("server_name")
    private String serverName;

    /**
     * 服务端头像标识（`cp.server.avatar`）。
     */
    @JsonProperty("avatar")
    private String avatar;

    /**
     * 服务端简介（`cp.server.brief`）。
     */
    @JsonProperty("brief")
    private String brief;

    /**
     * 服务端创建时间（毫秒时间戳，`cp.server.time`）。
     */
    @JsonProperty("time")
    private Long time;
}

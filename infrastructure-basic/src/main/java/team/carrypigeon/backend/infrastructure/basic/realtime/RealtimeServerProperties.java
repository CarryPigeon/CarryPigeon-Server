package team.carrypigeon.backend.infrastructure.basic.realtime;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实时通道配置。
 * 职责：收敛实时通道的地址、端口、路径、线程模型、连接超时与事件窗口参数，并给出可公开的 websocket 地址。
 * 边界：监听地址仅用于本机绑定，公开地址由部署方显式提供；不推断代理域名或 TLS 状态。
 * 约束：通过 `@ConfigurationPropertiesScan` 注册为配置 Bean，采用无参构造 + setter 绑定；
 * 字段初始值即默认值。不要恢复 Lombok `@AllArgsConstructor`：ECJ/jdtls 会对“带初始值的字段”报
 * early construction context 误报（javac 构建不受影响）。
 */
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "cp.chat.server.realtime")
public class RealtimeServerProperties implements InitializingBean {

    /**
     * 固定 WebSocket 握手路径。
     * 约束：路径属于已发布协议的一部分，不再通过配置覆盖。
     */
    public static final String WEBSOCKET_PATH = "/api/ws";

    boolean enabled;
    String host;
    int port;
    /** 客户端实际访问的完整 ws/wss URI；实时通道启用时必填，路径固定为 /api/ws。 */
    String publicUrl;
    int bossThreads;
    int workerThreads;
    int authenticationTimeoutSeconds = 10;
    int readIdleTimeoutSeconds = 60;
    Duration eventRetention = Duration.ofHours(1);
    int maxPayloadByte = 262144;

    /**
     * 返回服务发现可公开的 websocket 地址。
     *
     * @return 启用时返回 ws 地址；禁用时返回 null
     */
    public String wsUrl() {
        if (!enabled) {
            return null;
        }
        return publicUrl;
    }

    /**
     * 绑定完成后校验公开地址，拒绝把不可用或不属于当前协议的 URI 放入发现文档。
     * 禁用实时通道时不要求公开入口，wsUrl 始终返回 null。
     */
    @Override
    public void afterPropertiesSet() {
        if (!enabled) {
            return;
        }
        if (publicUrl == null || publicUrl.isBlank()) {
            throw new IllegalArgumentException("cp.chat.server.realtime.public-url is required when realtime is enabled");
        }
        try {
            URI uri = new URI(publicUrl);
            if (!("ws".equals(uri.getScheme()) || "wss".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || !WEBSOCKET_PATH.equals(uri.getRawPath())
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getPort() == 0
                    || uri.getPort() > 65535
                    || uri.getRawAuthority().endsWith(":")) {
                throw new IllegalArgumentException("cp.chat.server.realtime.public-url must be a ws/wss URI with path /api/ws");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("cp.chat.server.realtime.public-url must be a valid URI", exception);
        }
    }
}

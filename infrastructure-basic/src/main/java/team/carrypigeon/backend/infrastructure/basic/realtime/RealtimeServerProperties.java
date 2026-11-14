package team.carrypigeon.backend.infrastructure.basic.realtime;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实时通道配置。
 * 职责：收敛实时通道的地址、端口、路径、线程模型、连接超时与事件窗口参数，并给出可公开的 websocket 地址。
 * 边界：只承载稳定运行参数与地址拼装，不承载启动逻辑、连接状态或业务语义。
 * 约束：通过 `@ConfigurationPropertiesScan` 注册为配置 Bean，采用无参构造 + setter 绑定；
 * 字段初始值即默认值。不要恢复 Lombok `@AllArgsConstructor`：ECJ/jdtls 会对“带初始值的字段”报
 * early construction context 误报（javac 构建不受影响）。
 */
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "cp.chat.server.realtime")
public class RealtimeServerProperties{

    /**
     * 固定 WebSocket 握手路径。
     * 约束：路径属于已发布协议的一部分，不再通过配置覆盖。
     */
    public static final String WEBSOCKET_PATH = "/api/ws";

    boolean enabled;
    String host;
    int port;
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
        return "wss://" + host + ":" + port + WEBSOCKET_PATH;
    }
}

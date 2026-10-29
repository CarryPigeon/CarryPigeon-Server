package team.carrypigeon.backend.chat.domain.config.ws;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 实时通道配置。
 * 职责：收敛实时通道的地址、端口、路径、线程模型、连接超时与事件窗口参数，并给出可公开的 websocket 地址。
 * 边界：只承载稳定运行参数与地址拼装，不承载启动逻辑、连接状态或业务语义。
 * 约束：不可变记录只能构造器绑定，因此由 `@ConfigurationPropertiesScan` 启用，而不是标 `@Component`。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "cp.chat.server.realtime")
public class RealtimeServerProperties{
    boolean enabled;
    String host;
    int port;
    int bossThreads;
    int workerThreads;
    int authenticationTimeoutSeconds = 10;
    int readIdleTimeoutSeconds = 60;
    Duration eventRetention = Duration.ofHours(1);
    int maxEventAccounts = 10000;
    int maxEventsPerAccount = 1000;
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
        return "wss://" + host + ":" + port+"/api/ws";
    }
}

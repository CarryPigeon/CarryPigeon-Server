package team.carrypigeon.backend.chat.domain.features.health.controller.http;

import io.netty.util.NetUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 启动就绪探针；不代替持续依赖健康监控，也不采信代理转发的来源地址。 */
@RestController
@EnableConfigurationProperties(ReadinessController.Properties.class)
public class ReadinessController implements ApplicationListener<ContextClosedEvent> {
    private final ApplicationAvailability availability;
    private final List<IpAddressMatcher> trustedNetworks;
    private volatile boolean closing;

    public ReadinessController(ApplicationAvailability availability, Properties properties) {
        this.availability = availability;
        this.trustedNetworks = properties.trustedNetworks().stream().map(IpAddressMatcher::new).toList();
    }

    /** 本机及配置的管理网段可读；启动未完成或已开始关闭返回 503，就绪返回 204。 */
    @GetMapping("/internal/readiness")
    public ResponseEntity<Void> readiness(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (NetUtil.createByteArrayFromIpAddressString(peer) == null
                || !(new IpAddressMatcher("127.0.0.0/8").matches(peer)
                || new IpAddressMatcher("::1").matches(peer)
                || trustedNetworks.stream().anyMatch(network -> network.matches(peer)))) {
            return ResponseEntity.status(403).build();
        }
        // Boot 在初始化检查、SmartLifecycle 和 runner 完成后才进入 ACCEPTING_TRAFFIC。
        return !closing && availability.getReadinessState() == ReadinessState.ACCEPTING_TRAFFIC
                ? ResponseEntity.noContent().build() : ResponseEntity.status(503).build();
    }

    @Override
    public void onApplicationEvent(@NonNull ContextClosedEvent event) {
        closing = true;
    }

    /** 管理网段只接受 IP/CIDR 字面量，避免配置域名触发 DNS 或扩大访问范围。 */
    @ConfigurationProperties(prefix = "cp.http.readiness")
    public record Properties(List<String> trustedNetworks) {
        public Properties {
            trustedNetworks = trustedNetworks == null ? List.of() : List.copyOf(trustedNetworks);
            for (String network : trustedNetworks) {
                String[] parts = network.split("/", -1);
                if (parts.length > 2 || NetUtil.createByteArrayFromIpAddressString(parts[0]) == null) {
                    throw new IllegalArgumentException("cp.http.readiness.trusted-networks must contain IP addresses or CIDRs");
                }
                if (parts.length == 2) {
                    int bits = Integer.parseInt(parts[1]);
                    if (bits < 0 || bits > (parts[0].contains(":") ? 128 : 32)) {
                        throw new IllegalArgumentException("readiness CIDR prefix is out of range");
                    }
                }
            }
        }
    }
}

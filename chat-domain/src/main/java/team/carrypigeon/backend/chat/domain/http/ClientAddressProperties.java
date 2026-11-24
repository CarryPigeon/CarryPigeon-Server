package team.carrypigeon.backend.chat.domain.http;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP 客户端地址的代理信任边界；默认不信任任何转发头。
 * trustedProxies 仅接受 IP 或 CIDR，填写直接上游及实际受控的代理网段，不能填写域名。
 */
@ConfigurationProperties(prefix = "cp.http.client-address")
public record ClientAddressProperties(List<String> trustedProxies) {
    public ClientAddressProperties {
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
    }
}

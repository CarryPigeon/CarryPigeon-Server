package team.carrypigeon.backend.chat.domain.http.address;

import io.netty.util.NetUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

/**
 * HTTP 客户端地址解析器，为限流提供规范化的 IP 身份。
 * 只对可信直接上游解释转发头，从右向左剥离可信代理；不执行 DNS 查询。
 * 请求进入此处前不能再由容器或 ForwardedHeaderFilter 无条件改写 remoteAddr。
 */
@Component
public class ClientAddressResolver {

    // 限制转发链解析的资源开销，超限按直连代理地址限流，不采信截断的链。
    private static final int MAX_HEADER_LENGTH = 4096;
    private static final int MAX_FORWARDED_ADDRESSES = 32;
    private final List<IpAddressMatcher> trustedProxies;

    public ClientAddressResolver(ClientAddressProperties properties) {
        List<IpAddressMatcher> matchers = new ArrayList<>();
        for (String configured : properties.trustedProxies()) {
            String[] parts = configured.split("/", -1);
            String address = normalize(parts[0]);
            if (parts.length > 2 || address == null) {
                throw new IllegalArgumentException("cp.http.client-address.trusted-proxies must contain IP addresses or CIDRs");
            }
            // 规范化后的地址再交给框架匹配，避免配置域名触发 DNS 或 IPv4 映射地址分歧。
            String cidr = address;
            if (parts.length == 2) {
                int bits;
                try {
                    bits = Integer.parseInt(parts[1]);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("trusted proxy CIDR prefix must be numeric", exception);
                }
                int maxBits = address.contains(":") ? 128 : 32;
                if (bits < 0 || bits > maxBits) {
                    throw new IllegalArgumentException("trusted proxy CIDR prefix is out of range");
                }
                cidr += "/" + bits;
            }
            matchers.add(new IpAddressMatcher(cidr));
        }
        trustedProxies = List.copyOf(matchers);
    }

    /**
     * 返回规范化 IP；非法、缺失或超限的代理头全部回退直连地址。
     * X-Forwarded-For 存在时不再采信 X-Real-IP，避免非法链绕过信任边界。
     */
    public String resolve(HttpServletRequest request) {
        String peer = normalize(request.getRemoteAddr());
        if (peer == null) {
            return "unknown";
        }
        if (!trusted(peer)) {
            return peer;
        }
        List<String> chain = new ArrayList<>();
        var headers = request.getHeaders("X-Forwarded-For");
        int length = 0;
        boolean forwarded = false;
        while (headers.hasMoreElements()) {
            forwarded = true;
            String header = headers.nextElement();
            length += header.length();
            if (length > MAX_HEADER_LENGTH) {
                return peer;
            }
            for (String part : header.split(",", -1)) {
                String address = normalize(part.trim());
                if (address == null || chain.size() >= MAX_FORWARDED_ADDRESSES) {
                    return peer;
                }
                chain.add(address);
            }
        }
        if (!forwarded) {
            var realIpHeaders = request.getHeaders("X-Real-IP");
            if (!realIpHeaders.hasMoreElements()) {
                return peer;
            }
            String realIp = realIpHeaders.nextElement();
            if (realIp.length() > MAX_HEADER_LENGTH || realIpHeaders.hasMoreElements()) {
                return peer;
            }
            String address = normalize(realIp.trim());
            return address == null ? peer : address;
        }
        String client = peer;
        // 第一个不可信节点就是信任边界，它左侧的任意值均可能由该节点伪造。
        for (int i = chain.size() - 1; i >= 0 && trusted(client); i--) {
            client = chain.get(i);
        }
        return client;
    }

    private boolean trusted(String address) {
        return trustedProxies.stream().anyMatch(proxy -> proxy.matches(address));
    }

    /** 仅解析 IP 字面量，统一 IPv6 压缩形式及 IPv4 映射地址；不接受 zone ID 或主机名。 */
    private static String normalize(String value) {
        if (value == null || value.contains("%") || value.startsWith("[")) {
            return null;
        }
        byte[] bytes = NetUtil.createByteArrayFromIpAddressString(value);
        if (bytes == null) {
            return null;
        }
        try {
            return NetUtil.toAddressString(InetAddress.getByAddress(bytes));
        } catch (UnknownHostException exception) {
            throw new IllegalStateException("IP parser returned an invalid address length", exception);
        }
    }
}

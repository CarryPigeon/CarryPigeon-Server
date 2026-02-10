package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 服务发现内部请求。
 *
 * @param scheme 协议（`http` / `https`）。
 * @param host 主机名。
 * @param port 端口号。
 */
public record ServerInfoRequest(String scheme, String host, int port) {

    /**
     * 从 Servlet 请求提取服务发现信息。
     *
     * @param request HTTP 请求对象。
     * @return 服务发现内部请求。
     */
    public static ServerInfoRequest from(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        return new ServerInfoRequest(scheme, host, port);
    }
}

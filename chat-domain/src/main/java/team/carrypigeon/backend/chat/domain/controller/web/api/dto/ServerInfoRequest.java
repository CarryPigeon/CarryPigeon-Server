package team.carrypigeon.backend.chat.domain.controller.web.api.dto;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Internal request object used to compute discovery URLs for {@code GET /api/server}.
 * <p>
 * Built from servlet request and stored under {@link team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowKeys#REQUEST}.
 */
public record ServerInfoRequest(String scheme, String host, int port) {

    public static ServerInfoRequest from(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        return new ServerInfoRequest(scheme, host, port);
    }
}

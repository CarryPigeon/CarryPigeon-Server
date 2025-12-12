package team.carrypigeon.backend.chat.domain.controller.web.core;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.starter.server.ServerInfoConfig;

/**
 * HTTP controller for exposing basic server information.
 *
 * Route:
 *   GET /core/server/data/get
 *
 * Response body:
 * {
 *   "server_name": "...",
 *   "avatar": "...",
 *   "brief": "...",
 *   "time": 1733616000000
 * }
 *
 * All values are loaded from {@link ServerInfoConfig}, which is bound to
 * configuration properties with prefix {@code cp.server}.
 */
@RestController
public class ServerInfoController {

    private final ServerInfoConfig serverInfoConfig;

    public ServerInfoController(ServerInfoConfig serverInfoConfig) {
        this.serverInfoConfig = serverInfoConfig;
    }

    /**
     * Get current server information configured in application.yaml.
     */
    @GetMapping("/core/server/data/get")
    public ServerInfoConfig getServerInfo() {
        return serverInfoConfig;
    }
}


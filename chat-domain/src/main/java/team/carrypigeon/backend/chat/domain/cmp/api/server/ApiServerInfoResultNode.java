package team.carrypigeon.backend.chat.domain.cmp.api.server;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.api.starter.server.ServerInfoConfig;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ServerInfoRequest;
import team.carrypigeon.backend.api.chat.domain.error.CPProblem;
import team.carrypigeon.backend.api.chat.domain.error.CPProblemException;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;

import java.util.List;

/**
 * Server discovery response for {@code GET /api/server}.
 * <p>
 * Input: {@link ApiFlowKeys#REQUEST} = {@link ServerInfoRequest} (used to build {@code ws_url})
 * Output: {@link ApiFlowKeys#RESPONSE} = {@link ServerInfoResponse}
 */
@Slf4j
@LiteflowComponent("ApiServerInfoResult")
@RequiredArgsConstructor
public class ApiServerInfoResultNode extends AbstractResultNode<ApiServerInfoResultNode.ServerInfoResponse> {

    private final ServerInfoConfig serverInfoConfig;
    private final CpApiProperties properties;

    @Override
    protected ServerInfoResponse build(CPFlowContext context) {
        Object reqObj = context.get(CPFlowKeys.REQUEST);
        if (!(reqObj instanceof ServerInfoRequest req)) {
            throw new CPProblemException(CPProblem.of(422, "validation_failed", "validation failed"));
        }
        String wsUrl = buildWsUrl(req);
        ServerInfoResponse response = new ServerInfoResponse(
                serverInfoConfig.getServerId(),
                serverInfoConfig.getServerName(),
                serverInfoConfig.getBrief(),
                serverInfoConfig.getAvatar(),
                "1.0",
                "1.0",
                wsUrl,
                properties.getApi().getRequiredPlugins(),
                new Capabilities(true, true, true),
                System.currentTimeMillis()
        );
        log.debug("ApiServerInfoResult success");
        return response;
    }

    private String buildWsUrl(ServerInfoRequest req) {
        String scheme = req.scheme() == null ? "http" : req.scheme();
        String host = req.host();
        int port = req.port();
        String wsScheme = "https".equalsIgnoreCase(scheme) ? "wss" : "ws";

        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443);
        if (defaultPort) {
            return wsScheme + "://" + host + "/api/ws";
        }
        return wsScheme + "://" + host + ":" + port + "/api/ws";
    }

    public record ServerInfoResponse(
            String serverId,
            String name,
            String brief,
            String avatar,
            String apiVersion,
            String minSupportedApiVersion,
            String wsUrl,
            List<String> requiredPlugins,
            Capabilities capabilities,
            long serverTime
    ) {
    }

    public record Capabilities(boolean messageDomains, boolean pluginCatalog, boolean eventResume) {
    }
}

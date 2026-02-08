package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ServerInfoRequest;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * Public `/api` endpoints that do not require authentication.
 * <p>
 * This controller is intentionally thin:
 * it only builds a {@link CPFlowContext}, writes {@link ApiFlowKeys#REQUEST} when the chain needs input,
 * and executes LiteFlow chains defined in {@code application-starter/src/main/resources/config/api.xml}.
 */
@RestController
public class ApiServerController {

    private static final String CHAIN_SERVER = "api_server";
    private static final String CHAIN_PLUGINS_CATALOG = "api_plugins_catalog";
    private static final String CHAIN_DOMAINS_CATALOG = "api_domains_catalog";

    private final ApiFlowRunner flowRunner;

    public ApiServerController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * Get basic server metadata and discovery URLs (including {@code ws_url}).
     * <p>
     * Route: {@code GET /api/server} (public)
     * <p>
     * Chain: {@code api_server}
     */
    @GetMapping("/api/server")
    public Object server(HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, ServerInfoRequest.from(request));
        flowRunner.executeOrThrow(CHAIN_SERVER, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * Get server-side plugin catalog.
     * <p>
     * Route: {@code GET /api/plugins/catalog} (public)
     * <p>
     * Chain: {@code api_plugins_catalog}
     */
    @GetMapping("/api/plugins/catalog")
    public Object pluginsCatalog() {
        CPFlowContext ctx = new CPFlowContext();
        flowRunner.executeOrThrow(CHAIN_PLUGINS_CATALOG, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * Get server-supported message domain catalog.
     * <p>
     * Route: {@code GET /api/domains/catalog} (public)
     * <p>
     * Chain: {@code api_domains_catalog}
     */
    @GetMapping("/api/domains/catalog")
    public Object domainsCatalog() {
        CPFlowContext ctx = new CPFlowContext();
        flowRunner.executeOrThrow(CHAIN_DOMAINS_CATALOG, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }
}

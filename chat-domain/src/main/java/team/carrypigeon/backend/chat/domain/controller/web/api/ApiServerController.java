package team.carrypigeon.backend.chat.domain.controller.web.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowKeys;
import team.carrypigeon.backend.chat.domain.controller.web.api.dto.ServerInfoRequest;
import team.carrypigeon.backend.chat.domain.controller.web.api.flow.ApiFlowRunner;

/**
 * 服务发现 API 控制器。
 * <p>
 * 提供服务信息、插件目录与消息域目录查询路由。
 */
@RestController
public class ApiServerController {

    private static final String CHAIN_SERVER = "api_server";
    private static final String CHAIN_PLUGINS_CATALOG = "api_plugins_catalog";
    private static final String CHAIN_DOMAINS_CATALOG = "api_domains_catalog";

    private final ApiFlowRunner flowRunner;

    /**
     * 构造服务发现控制器。
     *
     * @param flowRunner API 责任链执行器。
     */
    public ApiServerController(ApiFlowRunner flowRunner) {
        this.flowRunner = flowRunner;
    }

    /**
     * 查询基础服务信息与发现入口。
     *
     * @param request HTTP 请求对象。
     * @return 标准服务信息响应。
     */
    @GetMapping("/api/server")
    public Object server(HttpServletRequest request) {
        CPFlowContext ctx = new CPFlowContext();
        ctx.set(CPFlowKeys.REQUEST, ServerInfoRequest.from(request));
        flowRunner.executeOrThrow(CHAIN_SERVER, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 查询插件目录。
     *
     * @return 标准插件目录响应。
     */
    @GetMapping("/api/plugins/catalog")
    public Object pluginsCatalog() {
        CPFlowContext ctx = new CPFlowContext();
        flowRunner.executeOrThrow(CHAIN_PLUGINS_CATALOG, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }

    /**
     * 查询消息域目录。
     *
     * @return 标准消息域目录响应。
     */
    @GetMapping("/api/domains/catalog")
    public Object domainsCatalog() {
        CPFlowContext ctx = new CPFlowContext();
        flowRunner.executeOrThrow(CHAIN_DOMAINS_CATALOG, ctx);
        return ctx.get(CPFlowKeys.RESPONSE);
    }
}

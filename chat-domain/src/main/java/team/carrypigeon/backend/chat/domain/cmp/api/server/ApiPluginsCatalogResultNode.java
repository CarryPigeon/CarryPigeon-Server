package team.carrypigeon.backend.chat.domain.cmp.api.server;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.controller.web.api.config.CpApiProperties;
import team.carrypigeon.backend.chat.domain.service.catalog.ApiPluginCatalogIndex;

import java.util.List;

/**
 * 插件目录结果节点。
 * <p>
 * 生成 `GET /api/plugins/catalog` 响应，包含必需插件与当前目录视图。
 */
@Slf4j
@LiteflowComponent("ApiPluginsCatalogResult")
@RequiredArgsConstructor
public class ApiPluginsCatalogResultNode extends AbstractResultNode<ApiPluginsCatalogResultNode.PluginsCatalogResponse> {

    private final CpApiProperties properties;
    private final ApiPluginCatalogIndex catalogIndex;

    /**
     * 构建插件目录响应。
     */
    @Override
    protected PluginsCatalogResponse build(CPFlowContext context) {
        List<CpApiProperties.PluginItem> plugins = catalogIndex.snapshot().pluginsView();
        PluginsCatalogResponse response = new PluginsCatalogResponse(properties.getApi().getRequiredPlugins(),
                plugins == null ? List.of() : plugins);
        log.debug("ApiPluginsCatalogResult success");
        return response;
    }

    /**
     * 插件目录响应体。
     */
    public record PluginsCatalogResponse(List<String> requiredPlugins, List<CpApiProperties.PluginItem> plugins) {
    }
}

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
 * Plugin catalog response for {@code GET /api/plugins/catalog}.
 * <p>
 * Current P0 behavior:
 * <ul>
 *   <li>Return {@code required_plugins} from configuration.</li>
 *   <li>Return empty {@code plugins[]} placeholder (catalog integration can be added later).</li>
 * </ul>
 */
@Slf4j
@LiteflowComponent("ApiPluginsCatalogResult")
@RequiredArgsConstructor
public class ApiPluginsCatalogResultNode extends AbstractResultNode<ApiPluginsCatalogResultNode.PluginsCatalogResponse> {

    private final CpApiProperties properties;
    private final ApiPluginCatalogIndex catalogIndex;

    @Override
    protected PluginsCatalogResponse build(CPFlowContext context) {
        List<CpApiProperties.PluginItem> plugins = catalogIndex.snapshot().pluginsView();
        PluginsCatalogResponse response = new PluginsCatalogResponse(properties.getApi().getRequiredPlugins(),
                plugins == null ? List.of() : plugins);
        log.debug("ApiPluginsCatalogResult success");
        return response;
    }

    public record PluginsCatalogResponse(List<String> requiredPlugins, List<CpApiProperties.PluginItem> plugins) {
    }
}

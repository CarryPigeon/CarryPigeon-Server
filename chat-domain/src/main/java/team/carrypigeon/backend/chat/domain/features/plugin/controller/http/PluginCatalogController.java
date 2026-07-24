package team.carrypigeon.backend.chat.domain.features.plugin.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.api.PluginCatalogApi;
import team.carrypigeon.backend.chat.domain.features.plugin.controller.dto.PluginCatalogItemResponse;
import team.carrypigeon.backend.chat.domain.features.plugin.controller.dto.PluginCatalogResponse;
import team.carrypigeon.backend.chat.domain.features.plugin.controller.dto.PluginDomainResponse;
import team.carrypigeon.backend.chat.domain.features.plugin.controller.dto.PluginDownloadResponse;

/**
 * 插件目录 HTTP 入口。
 */
@RestController
@RequestMapping("/api/plugins")
@Tag(name = "插件目录", description = "服务端插件目录发现接口。")
public class PluginCatalogController {

    private final PluginCatalogApi pluginCatalogApi;

    /**
     * 创建插件目录 HTTP 入口。
     *
     * @param pluginCatalogApi 插件目录领域 API
     */
    public PluginCatalogController(PluginCatalogApi pluginCatalogApi) {
        this.pluginCatalogApi = pluginCatalogApi;
    }

    /**
     * 获取当前服务公开的插件目录。
     * 输出：包含 required plugin 列表和公开插件能力清单的响应。
     *
     * @return 当前服务公开的插件目录
     */
    @GetMapping("/catalog")
    @Operation(summary = "获取插件目录", description = "返回当前服务端可公开暴露的插件目录。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回插件目录")
    })
    public PluginCatalogResponse getPluginCatalog() {
        List<PluginCatalogItemResponse> plugins = pluginCatalogApi.listPublicPlugins().stream()
                .map(plugin -> new PluginCatalogItemResponse(
                        plugin.publicPluginKey(),
                        plugin.description(),
                        "1.0.0",
                        "0.1.0",
                        false,
                        plugin.declaredPermissions(),
                        List.of(new PluginDomainResponse(plugin.domain(), "1.0.0")),
                        new PluginDownloadResponse(null, null)
                ))
                .toList();
        return new PluginCatalogResponse(pluginCatalogApi.requiredPluginIds(), plugins);
    }

}

package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.message.domain.api.MessagePluginCatalogApi;
import team.carrypigeon.backend.chat.domain.features.server.domain.projection.ServerDiscoveryDocument;
import team.carrypigeon.backend.chat.domain.features.server.domain.api.ServerEntranceApi;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.PluginCatalogItemResponse;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.PluginCatalogResponse;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.PluginDomainResponse;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.PluginDownloadResponse;

/**
 * 插件目录 HTTP 入口。
 */
@RestController
@RequestMapping("/api/plugins")
@Tag(name = "插件目录", description = "服务端插件目录发现接口。")
public class ServerPluginCatalogController {

    private final MessagePluginCatalogApi messagePluginCatalogDomainApi;
    private final ServerEntranceApi serverEntranceDomainApi;

    /**
     * 创建插件目录 HTTP 入口。
     *
     * @param messagePluginCatalogDomainApi 消息插件目录领域 API
     * @param serverEntranceDomainApi 服务入口领域 API
     */
    public ServerPluginCatalogController(
            MessagePluginCatalogApi messagePluginCatalogDomainApi,
            ServerEntranceApi serverEntranceDomainApi
    ) {
        this.messagePluginCatalogDomainApi = messagePluginCatalogDomainApi;
        this.serverEntranceDomainApi = serverEntranceDomainApi;
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
        ServerDiscoveryDocument discoveryDocument = serverEntranceDomainApi.getServerDiscoveryDocument();
        List<PluginCatalogItemResponse> plugins = messagePluginCatalogDomainApi.listPublicPlugins().stream()
                .map(plugin -> new PluginCatalogItemResponse(
                        plugin.publicPluginKey(),
                        plugin.description(),
                        "1.0.0",
                        "0.1.0",
                        false,
                        plugin.declaredPermissions(),
                        List.of(new PluginDomainResponse(toDomain(plugin.messageType()), "1.0.0")),
                        new PluginDownloadResponse(null, null)
                ))
                .toList();
        return new PluginCatalogResponse(discoveryDocument.requiredPlugins(), plugins);
    }

    private String toDomain(String messageType) {
        return switch (messageType) {
            case "text" -> "Core:Text";
            case "file" -> "Core:File";
            case "voice" -> "Core:Voice";
            default -> messageType;
        };
    }
}

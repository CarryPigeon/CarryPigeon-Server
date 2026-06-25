package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessagePluginCatalogApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.server.application.dto.ServerDiscoveryDocument;
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

    private final MessagePluginCatalogApplicationService messagePluginCatalogApplicationService;
    private final team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService serverApplicationService;

    public ServerPluginCatalogController(
            MessagePluginCatalogApplicationService messagePluginCatalogApplicationService,
            team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService serverApplicationService
    ) {
        this.messagePluginCatalogApplicationService = messagePluginCatalogApplicationService;
        this.serverApplicationService = serverApplicationService;
    }

    public ServerPluginCatalogController(
            ChannelMessagePluginRegistry channelMessagePluginRegistry,
            team.carrypigeon.backend.chat.domain.features.server.application.service.ServerApplicationService serverApplicationService
    ) {
        this(new MessagePluginCatalogApplicationService(channelMessagePluginRegistry), serverApplicationService);
    }

    @GetMapping("/catalog")
    @Operation(summary = "获取插件目录", description = "返回当前服务端可公开暴露的插件目录。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回插件目录")
    })
    public PluginCatalogResponse getPluginCatalog() {
        ServerDiscoveryDocument discoveryDocument = serverApplicationService.getServerDiscoveryDocument();
        List<PluginCatalogItemResponse> plugins = messagePluginCatalogApplicationService.listPublicPlugins().stream()
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

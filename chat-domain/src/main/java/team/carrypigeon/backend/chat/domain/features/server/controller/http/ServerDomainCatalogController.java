package team.carrypigeon.backend.chat.domain.features.server.controller.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.carrypigeon.backend.chat.domain.features.message.application.dto.MessagePluginCatalogItemResult;
import team.carrypigeon.backend.chat.domain.features.message.application.service.MessagePluginCatalogApplicationService;
import team.carrypigeon.backend.chat.domain.features.message.support.plugin.ChannelMessagePluginRegistry;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.DomainCatalogItemResponse;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.DomainCatalogResponse;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.DomainConstraintsResponse;
import team.carrypigeon.backend.chat.domain.features.server.controller.dto.DomainProviderResponse;

/**
 * Domain 目录 HTTP 入口。
 */
@RestController
@RequestMapping("/api/domains")
@Tag(name = "Domain 目录", description = "服务端 domain 目录发现接口。")
public class ServerDomainCatalogController {

    private final MessagePluginCatalogApplicationService messagePluginCatalogApplicationService;

    public ServerDomainCatalogController(MessagePluginCatalogApplicationService messagePluginCatalogApplicationService) {
        this.messagePluginCatalogApplicationService = messagePluginCatalogApplicationService;
    }

    public ServerDomainCatalogController(ChannelMessagePluginRegistry channelMessagePluginRegistry) {
        this(new MessagePluginCatalogApplicationService(channelMessagePluginRegistry));
    }

    @GetMapping("/catalog")
    @Operation(summary = "获取 Domain 目录", description = "返回当前服务端公开的最小 domain 目录。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "返回 domain 目录")
    })
    public DomainCatalogResponse getDomainCatalog() {
        return new DomainCatalogResponse(messagePluginCatalogApplicationService.listPublicPlugins().stream()
                .map(plugin -> new DomainCatalogItemResponse(
                        toDomain(plugin.messageType()),
                        List.of("1.0.0"),
                        "1.0.0",
                        new DomainConstraintsResponse(4096, 10),
                        List.of(provider(plugin))
                ))
                .toList());
    }

    private DomainProviderResponse provider(MessagePluginCatalogItemResult plugin) {
        if ("text".equals(plugin.messageType())) {
            return new DomainProviderResponse("core", null, null);
        }
        return new DomainProviderResponse("plugin", plugin.publicPluginKey(), "1.0.0");
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

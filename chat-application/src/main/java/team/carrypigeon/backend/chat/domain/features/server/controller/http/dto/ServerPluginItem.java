package team.carrypigeon.backend.chat.domain.features.server.controller.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.chat.domain.features.server.config.PluginPropertyItem;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerPluginItem {
    @Schema(description = "插件 UUID")
    private String pluginUUID;
    @Schema(description = "插件名称")
    private String pluginName;
    @Schema(description = "插件作者")
    private String pluginAuthor;
    @Schema(description = "插件 URL")
    private String pluginUrl;
    @Schema(description = "插件版本")
    private String pluginVersion;
    @Schema(description = "插件描述")
    private String pluginDescription;

    public static PluginPropertyItem from(PluginPropertyItem pluginPropertyItem) {
        return new PluginPropertyItem(
                pluginPropertyItem.getPluginUuid(),
                pluginPropertyItem.getPluginName(),
                pluginPropertyItem.getPluginAuthor(),
                pluginPropertyItem.getPluginUrl(),
                pluginPropertyItem.getPluginVersion(),
                pluginPropertyItem.getPluginDescription()
        );
    }
}

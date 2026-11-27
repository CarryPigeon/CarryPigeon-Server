package team.carrypigeon.backend.chat.domain.features.server.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 插件属性项
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginPropertyItem {
    private String pluginUuid;
    private String pluginName;
    private String pluginAuthor;
    private String pluginUrl;
    private String pluginVersion;
    private String pluginDescription;
}

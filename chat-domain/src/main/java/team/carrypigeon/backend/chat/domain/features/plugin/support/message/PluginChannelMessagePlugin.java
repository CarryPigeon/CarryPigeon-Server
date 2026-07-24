package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;

/**
 * 扩展 domain 消息插件。
 * 职责：校验注册扩展的 plugin_key、结构化 payload 与可选 metadata。
 * 边界：每个实例只负责构造时声明的 domain，不执行插件业务或消息持久化。
 */
public class PluginChannelMessagePlugin implements ChannelMessagePlugin {

    private final String supportedType;

    public PluginChannelMessagePlugin(String supportedType) {
        this.supportedType = supportedType;
    }

    @Override
    public String supportedType() {
        return supportedType;
    }

    @Override
    public String supportedDomain() {
        return supportedType;
    }

    @Override
    public CanonicalData validateCanonicalData(
            ChannelMessageBuildContext context,
            String domainVersion,
            Map<String, Object> rawData
    ) {
        MessagePluginDataReader.requireVersion(domainVersion, "1.0.0");
        Map<String, Object> data = MessagePluginDataReader.copyData(rawData);
        String pluginKey = MessagePluginDataReader.requiredString(
                data,
                "plugin_key",
                "plugin_key must not be blank"
        );
        Map<String, Object> payload = MessagePluginDataReader.requiredObject(data, "payload");
        String text = MessagePluginDataReader.optionalString(data, "text");
        if (text == null) {
            text = pluginKey;
        }
        data.put("plugin_key", pluginKey);
        data.put("payload", payload);
        data.put("text", text);
        Map<String, Object> metadata = MessagePluginDataReader.optionalObject(data, "metadata");
        if (metadata != null) {
            data.put("metadata", metadata);
        }
        return new CanonicalData(data, "[插件消息] " + text);
    }
}

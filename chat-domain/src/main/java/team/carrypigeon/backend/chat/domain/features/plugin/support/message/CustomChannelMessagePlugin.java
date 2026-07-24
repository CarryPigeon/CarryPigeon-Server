package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;

/**
 * Core:Custom 消息插件。
 * 职责：校验自定义消息的通用文本与可选 metadata 对象，并派生 preview。
 * 边界：保留扩展 data 字段，但不允许已知字段绕过 JSON 类型校验。
 */
public class CustomChannelMessagePlugin implements ChannelMessagePlugin {

    @Override
    public String supportedType() {
        return "custom";
    }

    @Override
    public String supportedDomain() {
        return "Core:Custom";
    }

    @Override
    public CanonicalData validateCanonicalData(
            ChannelMessageBuildContext context,
            String domainVersion,
            Map<String, Object> rawData
    ) {
        MessagePluginDataReader.requireVersion(domainVersion, "1.0.0");
        Map<String, Object> data = MessagePluginDataReader.copyData(rawData);
        String text = MessagePluginDataReader.optionalString(data, "text");
        if (text == null) {
            text = "custom message";
        }
        data.put("text", text);
        Map<String, Object> metadata = MessagePluginDataReader.optionalObject(data, "metadata");
        if (metadata != null) {
            data.put("metadata", metadata);
        }
        return new CanonicalData(data, "[自定义消息] " + text);
    }
}

package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;

/**
 * Core:Text 消息插件。
 * 职责：校验文本与可选链接预览，并生成规范化 data 和 preview。
 * 边界：只拥有 Core:Text schema，不负责消息身份、权限或持久化。
 */
public class TextChannelMessagePlugin implements ChannelMessagePlugin {

    @Override
    public String supportedType() {
        return "text";
    }

    @Override
    public String supportedDomain() {
        return "Core:Text";
    }

    @Override
    public CanonicalData validateCanonicalData(
            ChannelMessageBuildContext context,
            String domainVersion,
            Map<String, Object> rawData
    ) {
        MessagePluginDataReader.requireVersion(domainVersion, "1.0.0");
        Map<String, Object> data = MessagePluginDataReader.copyData(rawData);
        String text = MessagePluginDataReader.requiredString(data, "text", "text must not be blank");
        data.put("text", text);
        Map<String, Object> linkPreview = MessagePluginDataReader.optionalObject(data, "link_preview");
        if (linkPreview != null) {
            data.put("link_preview", linkPreview);
        }
        return new CanonicalData(data, text);
    }
}

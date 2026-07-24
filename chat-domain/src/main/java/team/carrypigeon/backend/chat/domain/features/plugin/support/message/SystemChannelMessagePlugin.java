package team.carrypigeon.backend.chat.domain.features.plugin.support.message;

import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.plugin.domain.extension.ChannelMessagePlugin;

/**
 * Core:System 消息插件。
 * 职责：校验内部系统消息 data 并生成稳定 preview。
 * 边界：该 domain 仅允许内部专用入口创建，客户端通用发送入口必须拒绝。
 */
public class SystemChannelMessagePlugin implements ChannelMessagePlugin {

    @Override
    public String supportedType() {
        return "system";
    }

    @Override
    public String supportedDomain() {
        return "Core:System";
    }

    @Override
    public boolean clientSendable() {
        return false;
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
            text = "system message";
        }
        data.put("text", text);
        Map<String, Object> metadata = MessagePluginDataReader.optionalObject(data, "metadata");
        if (metadata != null) {
            data.put("metadata", metadata);
        }
        return new CanonicalData(data, "[系统消息] " + text);
    }
}

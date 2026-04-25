package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.PluginChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * 插件频道消息插件。
 * 职责：校验 plugin 消息草稿并生成稳定的结构化插件消息。
 * 边界：只负责当前 plugin 消息的校验与构造，不扩展动态插件运行或异步事件执行。
 */
public class PluginChannelMessagePlugin implements ChannelMessagePlugin {

    private final JsonProvider jsonProvider;

    public PluginChannelMessagePlugin(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    @Override
    public String supportedType() {
        return "plugin";
    }

    @Override
    public ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft) {
        if (!(draft instanceof PluginChannelMessageDraft pluginDraft)) {
            throw new IllegalArgumentException("plugin plugin only supports PluginChannelMessageDraft");
        }
        String pluginKey = requireNonBlank(pluginDraft.pluginKey(), "pluginKey must not be blank");
        JsonNode payloadNode = requireJsonObject(pluginDraft.payload(), "payload must not be blank");
        String normalizedBody = normalizeBody(pluginDraft.body(), pluginKey);
        String previewText = "[插件消息] " + normalizedBody;
        String searchableText = normalizedBody + " " + pluginKey;
        Map<String, Object> canonicalPayload = new LinkedHashMap<>();
        canonicalPayload.put("plugin_key", pluginKey);
        canonicalPayload.put("payload", payloadNode);

        return new ChannelMessage(
                context.messageId(),
                context.serverId(),
                context.conversationId(),
                context.channelId(),
                context.senderId(),
                supportedType(),
                normalizedBody,
                previewText,
                searchableText.trim(),
                jsonProvider.toJson(canonicalPayload),
                normalizeMetadata(pluginDraft.metadata()),
                "sent",
                context.createdAt()
        );
    }

    private String normalizeBody(String body, String pluginKey) {
        if (body != null && !body.isBlank()) {
            return body.trim();
        }
        return pluginKey;
    }

    private String normalizeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        return jsonProvider.toJson(requireJsonObject(metadata, "metadata is invalid"));
    }

    private JsonNode requireJsonObject(String json, String message) {
        if (json == null || json.isBlank()) {
            throw ProblemException.validationFailed(message);
        }
        JsonNode jsonNode = jsonProvider.readTree(json);
        if (!jsonNode.isObject()) {
            throw ProblemException.validationFailed(message);
        }
        return jsonNode;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ProblemException.validationFailed(message);
        }
        return value.trim();
    }
}

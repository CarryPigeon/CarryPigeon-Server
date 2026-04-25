package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.CustomChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * 自定义频道消息插件。
 * 职责：校验 custom 消息草稿并生成稳定的结构化自定义消息。
 * 边界：只负责当前 custom 消息的校验与构造，不扩展客户端渲染协议或动态插件运行。
 */
public class CustomChannelMessagePlugin implements ChannelMessagePlugin {

    private static final String DEFAULT_BODY = "custom message";

    private final JsonProvider jsonProvider;

    public CustomChannelMessagePlugin(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    @Override
    public String supportedType() {
        return "custom";
    }

    @Override
    public ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft) {
        if (!(draft instanceof CustomChannelMessageDraft customDraft)) {
            throw new IllegalArgumentException("custom plugin only supports CustomChannelMessageDraft");
        }
        JsonNode payloadNode = requireJsonObject(customDraft.payload(), "payload must not be blank");
        String normalizedBody = normalizeBody(customDraft.body());
        String previewText = "[自定义消息] " + normalizedBody;

        return new ChannelMessage(
                context.messageId(),
                context.serverId(),
                context.conversationId(),
                context.channelId(),
                context.senderId(),
                supportedType(),
                normalizedBody,
                previewText,
                normalizedBody,
                jsonProvider.toJson(payloadNode),
                normalizeMetadata(customDraft.metadata()),
                "sent",
                context.createdAt()
        );
    }

    private String normalizeBody(String body) {
        if (body == null || body.isBlank()) {
            return DEFAULT_BODY;
        }
        return body.trim();
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
}

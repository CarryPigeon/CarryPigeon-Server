package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.application.draft.SystemChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.service.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * system 频道消息插件。
 * 职责：校验 system 消息草稿并生成稳定的系统消息领域对象。
 * 边界：只负责当前 system 消息的构造，不扩展服务端发件人模型或频道治理逻辑。
 */
public class SystemChannelMessagePlugin implements ChannelMessagePlugin {

    private static final String DEFAULT_BODY = "system message";

    private final JsonProvider jsonProvider;

    public SystemChannelMessagePlugin(JsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    @Override
    public String supportedType() {
        return "system";
    }

    @Override
    public ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft) {
        if (!(draft instanceof SystemChannelMessageDraft systemDraft)) {
            throw new IllegalArgumentException("system plugin only supports SystemChannelMessageDraft");
        }
        String normalizedBody = normalizeBody(systemDraft.body());
        String previewText = "[系统消息] " + normalizedBody;

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
                normalizePayload(systemDraft.payload()),
                normalizeMetadata(systemDraft.metadata()),
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

    private String normalizePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        return jsonProvider.toJson(requireJsonObject(payload, "payload is invalid"));
    }

    private String normalizeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        return jsonProvider.toJson(requireJsonObject(metadata, "metadata is invalid"));
    }

    private JsonNode requireJsonObject(String json, String message) {
        JsonNode jsonNode = jsonProvider.readTree(json);
        if (!jsonNode.isObject()) {
            throw ProblemException.validationFailed(message);
        }
        return jsonNode;
    }
}

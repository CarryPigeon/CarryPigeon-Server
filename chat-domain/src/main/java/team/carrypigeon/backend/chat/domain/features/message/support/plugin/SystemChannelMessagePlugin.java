package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.SystemChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.ChannelMessagePlugin;
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

    /**
     * 返回当前插件负责的消息类型。
     *
     * @return `system` 消息类型标识
     */
    @Override
    public String supportedType() {
        return "system";
    }

    /**
     * 校验 system 草稿并构造系统消息。
     * 输入：消息构建上下文与 system 草稿。
     * 输出：可持久化的系统消息领域对象。
     *
     * @param context 消息构建上下文
     * @param draft 入站消息草稿
     * @return 系统消息领域对象
     */
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

    /**
     * 规范化 system 消息 payload。
     * 约束：payload 为空时不写入；存在时必须是 JSON object，并重新序列化为 canonical JSON。
     *
     * @param payload 客户端或服务端提交的 payload JSON
     * @return 可持久化的 payload JSON，缺失时为 null
     */
    private String normalizePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        return jsonProvider.toJson(requireJsonObject(payload, "payload is invalid"));
    }

    /**
     * 规范化 system 消息 metadata。
     * 约束：metadata 为空时不写入；存在时必须是 JSON object，并重新序列化为 canonical JSON。
     *
     * @param metadata 客户端或服务端提交的 metadata JSON
     * @return 可持久化的 metadata JSON，缺失时为 null
     */
    private String normalizeMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        return jsonProvider.toJson(requireJsonObject(metadata, "metadata is invalid"));
    }

    /**
     * 要求指定 JSON 文本是对象结构。
     * 失败语义：非法 JSON 或非对象 JSON 都映射为调用方传入的校验失败提示。
     *
     * @param json 待校验 JSON 文本
     * @param message 校验失败提示
     * @return JSON 对象节点
     */
    private JsonNode requireJsonObject(String json, String message) {
        JsonNode jsonNode = jsonProvider.readTree(json);
        if (!jsonNode.isObject()) {
            throw ProblemException.validationFailed(message);
        }
        return jsonNode;
    }
}

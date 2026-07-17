package team.carrypigeon.backend.chat.domain.features.message.support.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.ChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.draft.PluginChannelMessageDraft;
import team.carrypigeon.backend.chat.domain.features.message.domain.model.ChannelMessage;
import team.carrypigeon.backend.chat.domain.features.message.domain.port.ChannelMessagePlugin;
import team.carrypigeon.backend.chat.domain.shared.domain.problem.ProblemException;
import team.carrypigeon.backend.infrastructure.basic.json.JsonProvider;

/**
 * 扩展频道消息插件。
 * 职责：校验扩展消息草稿并生成稳定的结构化扩展消息。
 * 边界：只负责当前扩展消息的校验与构造，不扩展动态插件运行或异步事件执行。
 */
public class PluginChannelMessagePlugin implements ChannelMessagePlugin {

    private final String supportedType;
    private final JsonProvider jsonProvider;

    public PluginChannelMessagePlugin(String supportedType, JsonProvider jsonProvider) {
        this.supportedType = supportedType;
        this.jsonProvider = jsonProvider;
    }

    /**
     * 返回当前插件实例负责的扩展消息类型。
     *
     * @return 扩展消息类型标识
     */
    @Override
    public String supportedType() {
        return supportedType;
    }

    /**
     * 校验扩展插件草稿并构造领域消息。
     * 输入：消息构建上下文与插件草稿。
     * 输出：包含 `plugin_key`、`message_type` 与业务 payload 的领域消息对象。
     *
     * @param context 消息构建上下文
     * @param draft 入站消息草稿
     * @return 扩展插件领域消息
     */
    @Override
    public ChannelMessage createMessage(ChannelMessageBuildContext context, ChannelMessageDraft draft) {
        if (!(draft instanceof PluginChannelMessageDraft pluginDraft)) {
            throw new IllegalArgumentException("plugin plugin only supports PluginChannelMessageDraft");
        }
        String extensionMessageType = requireNonBlank(pluginDraft.type(), "messageType must not be blank");
        String pluginKey = requireNonBlank(pluginDraft.pluginKey(), "pluginKey must not be blank");
        JsonNode payloadNode = requireJsonObject(pluginDraft.payload(), "payload must not be blank");
        String normalizedBody = normalizeBody(pluginDraft.body(), pluginKey);
        String previewText = "[插件消息] " + normalizedBody;
        String searchableText = normalizedBody + " " + pluginKey;
        Map<String, Object> canonicalPayload = new LinkedHashMap<>();
        canonicalPayload.put("plugin_key", pluginKey);
        canonicalPayload.put("message_type", extensionMessageType);
        canonicalPayload.put("payload", payloadNode);

        return new ChannelMessage(
                context.messageId(),
                context.serverId(),
                context.conversationId(),
                context.channelId(),
                context.senderId(),
                extensionMessageType,
                normalizedBody,
                previewText,
                searchableText.trim(),
                jsonProvider.toJson(canonicalPayload),
                normalizeMetadata(pluginDraft.metadata()),
                null,
                null,
                "sent",
                context.createdAt(),
                null,
                1L
        );
    }

    private String normalizeBody(String body, String pluginKey) {
        if (body != null && !body.isBlank()) {
            return body.trim();
        }
        return pluginKey;
    }

    /**
     * 规范化插件消息 metadata。
     * 约束：metadata 为空时不写入；存在时必须是 JSON object，并重新序列化为 canonical JSON。
     *
     * @param metadata 客户端提交的 metadata JSON
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
     * 失败语义：空文本、非法 JSON 或非对象 JSON 都映射为调用方传入的校验失败提示。
     *
     * @param json 待校验 JSON 文本
     * @param message 校验失败提示
     * @return JSON 对象节点
     */
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

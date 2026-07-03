package team.carrypigeon.backend.chat.domain.features.message.domain.draft;

/**
 * 插件消息草稿。
 * 职责：表达非内建消息类型进入 plugin-style 扩展链路时的最小输入。
 * 边界：这里只承载扩展消息的结构化输入，不扩展动态插件运行时语义。
 *
 * @param messageType 扩展消息类型
 * @param body 消息正文摘要
 * @param pluginKey 插件稳定标识
 * @param payload 插件消息结构化载荷 JSON
 * @param metadata 插件消息元数据 JSON
 */
public record PluginChannelMessageDraft(
        String messageType,
        String body,
        String pluginKey,
        String payload,
        String metadata
) implements ChannelMessageDraft {

    @Override
    public String type() {
        return messageType;
    }
}

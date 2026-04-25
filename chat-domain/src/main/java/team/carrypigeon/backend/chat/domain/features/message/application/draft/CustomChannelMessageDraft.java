package team.carrypigeon.backend.chat.domain.features.message.application.draft;

/**
 * 自定义消息草稿。
 * 职责：表达当前 custom 消息发送链路的最小输入。
 * 边界：这里只承载结构化自定义消息输入，不扩展客户端渲染协议。
 *
 * @param body 消息正文摘要
 * @param payload 自定义结构化载荷 JSON
 * @param metadata 自定义消息元数据 JSON
 */
public record CustomChannelMessageDraft(
        String body,
        String payload,
        String metadata
) implements ChannelMessageDraft {

    @Override
    public String type() {
        return "custom";
    }
}

package team.carrypigeon.backend.chat.domain.features.message.application.draft;

/**
 * 系统频道消息草稿。
 * 职责：表达当前 system 消息发送链路的最小输入。
 * 边界：这里只承载 system 消息的稳定字段，不扩展系统频道管理或服务端身份模型。
 *
 * @param body 消息正文摘要
 * @param payload 系统消息结构化载荷 JSON
 * @param metadata 系统消息元数据 JSON
 */
public record SystemChannelMessageDraft(
        String body,
        String payload,
        String metadata
) implements ChannelMessageDraft {

    @Override
    public String type() {
        return "system";
    }
}

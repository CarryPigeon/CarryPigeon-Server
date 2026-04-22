package team.carrypigeon.backend.chat.domain.features.message.application.draft;

/**
 * 文本频道消息草稿。
 * 职责：表达当前 text 消息发送主链路的最小输入。
 * 边界：当前阶段只承载纯文本，不扩展文件、语音或富文本负载。
 *
 * @param body 文本正文
 */
public record TextChannelMessageDraft(String body) implements ChannelMessageDraft {

    @Override
    public String type() {
        return "text";
    }

    @Override
    public String payload() {
        return null;
    }

    @Override
    public String metadata() {
        return null;
    }
}

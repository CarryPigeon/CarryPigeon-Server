package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本消息的业务数据与持久化投影。
 * 边界：回复 ID 使用 Long，null 表示不回复；字符串表示仅由协议适配层生成。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPChannelTextMessageData {
    public static final String DOMAIN = "Core:Text:V1";

    private String content;
    private Long replyToMid;
    private CPChannelTextMessageReplyToUser replyToUser;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CPChannelTextMessageReplyToUser {
        private String name;
        private String preview;
        private Long createAt;
    }
}

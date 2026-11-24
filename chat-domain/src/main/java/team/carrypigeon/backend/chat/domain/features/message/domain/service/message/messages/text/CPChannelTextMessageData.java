package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本消息的业务数据与持久化投影。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPChannelTextMessageData {

    private String content;
    private String replyToMid;
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

package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实际存储的消息格式：<br/>
 * ```json
 * {
 *     "content":"",
 *     "reply_to_mid":"",
 *     "reply_to_user":{
 *       "sender_name": "",
 *       "preview": "",
 *       "created_at":,
 *     }
 * }
 * ```
 * */
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

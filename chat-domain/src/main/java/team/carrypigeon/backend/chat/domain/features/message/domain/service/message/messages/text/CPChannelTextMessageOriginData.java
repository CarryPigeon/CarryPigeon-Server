package team.carrypigeon.backend.chat.domain.features.message.domain.service.message.messages.text;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户请求发送的消息格式：<br/>
 * ```json
 * {
 *     "content":"",
 *     "reply_to_mid":""
 * }
 * ```
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPChannelTextMessageOriginData {
    private String content;
    private String replyToMid;
}

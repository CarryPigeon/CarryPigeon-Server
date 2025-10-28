package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.unread.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取未读消息列表的请求
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageGetUnreadVO {
    private long cid;
    private long startTime;
}

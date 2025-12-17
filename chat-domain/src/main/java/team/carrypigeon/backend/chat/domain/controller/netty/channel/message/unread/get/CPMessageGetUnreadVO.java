package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.unread.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

/**
 * 获取未读消息数量的请求参数
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageGetUnreadVO implements CPControllerVO {

    private long cid;
    private long startTime;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (cid <= 0) {
            return false;
        }
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.setData(CPNodeMessageKeys.MESSAGE_UNREAD_START_TIME, startTime);
        return true;
    }
}

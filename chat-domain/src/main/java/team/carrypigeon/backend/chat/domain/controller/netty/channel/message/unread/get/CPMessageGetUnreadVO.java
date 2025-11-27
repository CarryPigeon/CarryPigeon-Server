package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.unread.get;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

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
    public boolean insertData(DefaultContext context) {
        if (cid <= 0) {
            return false;
        }
        context.setData("ChannelInfo_Id", cid);
        context.setData("MessageUnread_StartTime", startTime);
        return true;
    }
}


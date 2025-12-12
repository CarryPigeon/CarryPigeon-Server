package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

/**
 * 拉取消息列表的请求参数
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageListVO implements CPControllerVO {

    private long cid;
    private long startTime;
    private int count;

    @Override
    public boolean insertData(DefaultContext context) {
        if (cid <= 0 || count <= 0 || count > 50) {
            return false;
        }
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.setData(CPNodeMessageKeys.MESSAGE_LIST_START_TIME, startTime);
        context.setData(CPNodeMessageKeys.MESSAGE_LIST_COUNT, count);
        return true;
    }
}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

/**
 * 获取频道信息请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelGetProfileVO implements CPControllerVO {
    private long cid;

    @Override
    public boolean insertData(CPFlowContext context) {
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        return true;
    }
}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.create;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

/**
 * 创建频道的申请参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelCreateApplicationVO implements CPControllerVO {
    private long cid;
    private String msg;

    @Override
    public boolean insertData(CPFlowContext context) {
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_MSG, msg);
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_CID, cid);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        return true;
    }
}

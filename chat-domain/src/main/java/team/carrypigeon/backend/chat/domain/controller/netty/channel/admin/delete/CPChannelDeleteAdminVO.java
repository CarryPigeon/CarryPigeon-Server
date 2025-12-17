package team.carrypigeon.backend.chat.domain.controller.netty.channel.admin.delete;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

/**
 * 创建频道管理员的请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelDeleteAdminVO implements CPControllerVO {
    private long cid;
    private long uid;

    @Override
    public boolean insertData(CPFlowContext context) {
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, uid);
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_CID, cid);
        return true;
    }
}

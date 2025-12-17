package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.delete;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;

/**
 * 删除群成员的请求参数
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelDeleteMemberVO implements CPControllerVO {

    private long cid;
    private long uid;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (cid <= 0 || uid <= 0) {
            return false;
        }
        // 设置频道信息
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        // 寰呭垹闄ゆ垚鍛樹俊鎭?
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_CID, cid);
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_UID, uid);
        return true;
    }
}

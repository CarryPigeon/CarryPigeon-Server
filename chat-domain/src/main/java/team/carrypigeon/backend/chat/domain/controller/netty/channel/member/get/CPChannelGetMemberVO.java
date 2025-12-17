package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.get;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

/**
 * 获取单个频道成员信息的请求参数。<br/>
 * 入参：cid(频道id)、uid(成员id)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelGetMemberVO implements CPControllerVO {

    private long cid;
    private long uid;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (cid <= 0 || uid <= 0) {
            return false;
        }
        context.setData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_CID, cid);
        // 目标成员 uid 单独存放，方便先用 SessionId 做成员校验，再切换为目标成员查询
        context.setData(CPNodeValueKeyExtraConstants.TARGET_MEMBER_UID, uid);
        return true;
    }
}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.delete;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

/**
 * 删除频道成员的请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelDeleteMemberVO implements CPControllerVO {

    private long cid;
    private long uid;

    @Override
    public boolean insertData(DefaultContext context) {
        if (cid <= 0 || uid <= 0) {
            return false;
        }
        // 频道信息
        context.setData("ChannelInfo_Id", cid);
        // 待删除成员信息
        context.setData("ChannelMemberInfo_Cid", cid);
        context.setData("ChannelMemberInfo_Uid", uid);
        return true;
    }
}

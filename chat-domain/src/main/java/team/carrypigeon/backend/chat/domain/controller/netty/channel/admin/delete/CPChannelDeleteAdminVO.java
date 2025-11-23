package team.carrypigeon.backend.chat.domain.controller.netty.channel.admin.delete;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

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
    public boolean insertData(DefaultContext context) {
        context.setData("ChannelInfo_Id",cid);
        context.setData("ChannelMemberInfo_Uid",uid);
        context.setData("ChannelMemberInfo_Cid", cid);
        return true;
    }
}

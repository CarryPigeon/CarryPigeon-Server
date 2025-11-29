package team.carrypigeon.backend.chat.domain.controller.netty.channel.admin.delete;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

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
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID, cid);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_UID, uid);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_CID, cid);
        return true;
    }
}

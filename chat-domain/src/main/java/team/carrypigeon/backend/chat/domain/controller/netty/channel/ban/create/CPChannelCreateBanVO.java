package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.create;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 创建频道封禁的请求参数
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelCreateBanVO implements CPControllerVO {

    private long cid;
    private long uid;
    private int duration;

    @Override
    public boolean insertData(DefaultContext context) {
        if (cid <= 0 || uid <= 0 || duration <= 0) {
            return false;
        }
        // 频道
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_INFO_ID, cid);
        // 目标成员（用于成员选择）
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_CID, cid);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_UID, uid);
        // 封禁信息
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_BAN_TARGET_UID, uid);
        context.setData(CPNodeValueKeyBasicConstants.CHANNEL_BAN_DURATION, duration);
        return true;
    }
}

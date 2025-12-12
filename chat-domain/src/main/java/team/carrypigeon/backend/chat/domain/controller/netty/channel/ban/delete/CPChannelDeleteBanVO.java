package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.delete;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

/**
 * 解除频道封禁的请求参数
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelDeleteBanVO implements CPControllerVO {

    private long cid;
    private long uid;

    @Override
    public boolean insertData(DefaultContext context) {
        if (cid <= 0 || uid <= 0) {
            return false;
        }
        // 频道
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        // 目标用户
        context.setData(CPNodeChannelBanKeys.CHANNEL_BAN_TARGET_UID, uid);
        return true;
    }
}

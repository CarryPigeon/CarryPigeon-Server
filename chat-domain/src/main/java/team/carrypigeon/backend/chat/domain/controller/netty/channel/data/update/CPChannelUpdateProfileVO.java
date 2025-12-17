package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.update;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;

/**
 * 更新通道数据的参数类<br/>
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelUpdateProfileVO implements CPControllerVO {
    private long cid;
    private String name;
    private long owner;
    private String brief;
    private long avatar;

    @Override
    public boolean insertData(CPFlowContext context) {
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_NAME, name);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_OWNER, owner);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_BRIEF, brief);
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_AVATAR, avatar);
        return true;
    }
}

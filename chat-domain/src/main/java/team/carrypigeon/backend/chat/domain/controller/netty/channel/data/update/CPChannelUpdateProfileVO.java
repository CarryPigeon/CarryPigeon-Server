package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.update;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

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
    public boolean insertData(DefaultContext context) {
        context.setData("ChannelInfo_Id", cid);
        context.setData("ChannelInfo_Name", name);
        context.setData("ChannelInfo_Owner", owner);
        context.setData("ChannelInfo_Brief", brief);
        context.setData("ChannelInfo_Avatar", avatar);
        return true;
    }
}

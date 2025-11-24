package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.create;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

/**
 * 创建频道的申请参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelCreateApplicationVO implements CPControllerVO {
    private long cid;
    private String msg;

    @Override
    public boolean insertData(DefaultContext context) {
        context.setData("ChannelMemberInfo_Msg",msg);
        context.setData("ChannelMemberInfo_Cid",cid);
        context.setData("ChannelInfo_Id",cid);
        return true;
    }
}

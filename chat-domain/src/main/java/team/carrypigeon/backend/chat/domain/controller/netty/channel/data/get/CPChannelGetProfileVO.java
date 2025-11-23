package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

/**
 * 获取频道信息请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelGetProfileVO implements CPControllerVO {
    private long cid;

    @Override
    public boolean insertData(DefaultContext context) {
        context.setData("ChannelInfo_Id",cid);
        return true;
    }
}

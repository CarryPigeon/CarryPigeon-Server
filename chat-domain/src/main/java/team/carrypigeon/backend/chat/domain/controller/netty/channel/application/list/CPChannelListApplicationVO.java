package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.list;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;
import team.carrypigeon.backend.chat.domain.cmp.info.PageInfo;

/**
 * 获取通道申请列表的参数
 * @author midreamsheep
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelListApplicationVO implements CPControllerVO {
    private long cid;
    private int page;
    private int pageSize;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (page<0||pageSize<0||pageSize>50){
            return false;
        }
        context.setData(CPNodeChannelKeys.CHANNEL_INFO_ID, cid);
        context.setData(CPNodeValueKeyExtraConstants.PAGE_INFO, new PageInfo(page, pageSize));
        return true;
    }
}

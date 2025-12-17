package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.process;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelApplicationKeys;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyExtraConstants;

/**
 * 通道处理申请的请求参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPChannelProcessApplicationVO implements CPControllerVO {
    private long aid;
    private int result;

    @Override
    public boolean insertData(CPFlowContext context) {
        context.setData(CPNodeValueKeyExtraConstants.CHANNEL_APPLICATION_INFO_ID, aid);
        context.setData(CPNodeChannelApplicationKeys.CHANNEL_APPLICATION_INFO_STATE, result);
        return true;
    }
}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.create;

import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

/**
 * 创建频道的返回参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
public class CPChannelCreateVO implements CPControllerVO {
    @Override
    public boolean insertData(CPFlowContext context) {
        return true;
    }
}

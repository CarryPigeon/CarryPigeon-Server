package team.carrypigeon.backend.chat.domain.controller.netty.channel.create;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

/**
 * 创建频道的返回参数
 * @author midreamsheep
 * */
@Data
@NoArgsConstructor
public class CPChannelCreateVO implements CPControllerVO {
    @Override
    public boolean insertData(DefaultContext context) {
        return true;
    }
}

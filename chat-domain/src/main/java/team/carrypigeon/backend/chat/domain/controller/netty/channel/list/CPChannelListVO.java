package team.carrypigeon.backend.chat.domain.controller.netty.channel.list;

import com.yomahub.liteflow.slot.DefaultContext;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;

@Data
@NoArgsConstructor
public class CPChannelListVO implements CPControllerVO {
    @Override
    public boolean insertData(DefaultContext context) {
        return true;
    }
}

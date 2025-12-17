package team.carrypigeon.backend.chat.domain.controller.netty.channel.list;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;

@Data
@NoArgsConstructor
public class CPChannelListVO implements CPControllerVO {
    @Override
    public boolean insertData(CPFlowContext context) {
        return true;
    }
}

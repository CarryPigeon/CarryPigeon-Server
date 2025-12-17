package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.read.state.get;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;

/**
 * Request VO for getting channel message read state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageReadStateGetVO implements CPControllerVO {

    /** Channel id */
    private long cid;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (cid <= 0) {
            return false;
        }
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, cid);
        return true;
    }
}

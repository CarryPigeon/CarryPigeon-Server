package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.read.state.update;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerVO;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;

/**
 * Request VO for updating channel message read state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPMessageReadStateUpdateVO implements CPControllerVO {

    /** Channel id */
    private long cid;

    /** Latest read time (epoch millis) */
    private long lastReadTime;

    @Override
    public boolean insertData(CPFlowContext context) {
        if (cid <= 0 || lastReadTime <= 0) {
            return false;
        }
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_CID, cid);
        context.setData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO_LAST_READ_TIME, lastReadTime);
        return true;
    }
}

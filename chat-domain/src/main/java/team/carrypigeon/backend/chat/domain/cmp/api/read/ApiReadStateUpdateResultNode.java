package team.carrypigeon.backend.chat.domain.cmp.api.read;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;

/**
 * Result mapper for {@code PUT /api/channels/{cid}/read_state}.
 * <p>
 * Input (from {@code CPChannelReadStateUpdater}):
 * {@link CPNodeChannelReadStateKeys#CHANNEL_READ_STATE_INFO} = {@link CPChannelReadState}
 * <p>
 * Output: {@link ApiFlowKeys#RESPONSE} = {@link ReadStateResponse}
 */
@Slf4j
@LiteflowComponent("ApiReadStateUpdateResult")
public class ApiReadStateUpdateResultNode extends AbstractResultNode<ApiReadStateUpdateResultNode.ReadStateResponse> {

    @Override
    protected ReadStateResponse build(CPFlowContext context) throws Exception {
        CPChannelReadState state = requireContext(context, CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        ReadStateResponse resp = new ReadStateResponse(
                Long.toString(state.getCid()),
                Long.toString(state.getUid()),
                Long.toString(state.getLastReadMid()), state.getLastReadTime());
        log.debug("ApiReadStateUpdateResult success, uid={}, cid={}", state.getUid(), state.getCid());
        return resp;
    }

    public record ReadStateResponse(String cid, String uid, String lastReadMid, long lastReadTime) {
    }
}

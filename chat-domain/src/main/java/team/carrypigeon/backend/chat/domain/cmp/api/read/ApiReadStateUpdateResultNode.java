package team.carrypigeon.backend.chat.domain.cmp.api.read;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.chat.domain.node.AbstractResultNode;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;

/**
 * 已读状态更新结果节点。
 * <p>
 * 输出更新后的已读状态响应。
 */
@Slf4j
@LiteflowComponent("ApiReadStateUpdateResult")
public class ApiReadStateUpdateResultNode extends AbstractResultNode<ApiReadStateUpdateResultNode.ReadStateResponse> {

    /**
     * 构建已读状态响应。
     */
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

    /**
     * 已读状态响应体。
     */
    public record ReadStateResponse(String cid, String uid, String lastReadMid, long lastReadTime) {
    }
}

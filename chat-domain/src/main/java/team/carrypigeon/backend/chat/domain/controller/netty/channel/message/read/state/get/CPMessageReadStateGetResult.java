package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.read.state.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.read.CPChannelReadState;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelReadStateKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

/**
 * Result for getting channel message read state.
 */
public class CPMessageReadStateGetResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        CPChannelReadState state = context.getData(CPNodeChannelReadStateKeys.CHANNEL_READ_STATE_INFO);
        if (state == null) {
            argsError(context);
            return;
        }
        Result result = new Result(state.getCid(), state.getUid(), state.getLastReadTime());
        context.setData(
                CPNodeCommonKeys.RESPONSE,
                CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result))
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result {
        private long cid;
        private long uid;
        private long lastReadTime;
    }
}


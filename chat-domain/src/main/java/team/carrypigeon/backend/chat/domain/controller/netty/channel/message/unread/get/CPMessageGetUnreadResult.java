package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.unread.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;

/**
 * 获取未读消息数量结果
 * @author midreamsheep
 */
public class CPMessageGetUnreadResult implements CPControllerResult {

    @Override
    public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
        Long count = context.getData(CPNodeMessageKeys.MESSAGE_UNREAD_COUNT);
        if (count == null) {
            argsError(context);
            return;
        }
        Result result = new Result(count);
        context.setData(CPNodeCommonKeys.RESPONSE,
                CPResponse.success().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result {
        private long count;
    }
}

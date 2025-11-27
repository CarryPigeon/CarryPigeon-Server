package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.unread.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

/**
 * 获取未读消息数量结果
 * @author midreamsheep
 */
public class CPMessageGetUnreadResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        Long count = context.getData("MessageUnread_Count");
        if (count == null) {
            argsError(context);
            return;
        }
        Result result = new Result(count);
        context.setData("response", CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result {
        private long count;
    }
}


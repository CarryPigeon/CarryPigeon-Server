package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.create;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;

/**
 * 创建消息的返回结果<br/>
 * 返回字段 mid 表示新消息的 id。
 */
public class CPMessageCreateResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        CPMessage message = context.getData(CPNodeValueKeyBasicConstants.MESSAGE_INFO);
        if (message == null) {
            argsError(context);
            return;
        }
        Result result = new Result(message.getId());
        context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result {
        private long mid;
    }
}

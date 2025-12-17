package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 获取单条消息的结果。<br/>
 * 入参：MessageInfo:{@link CPMessage}<br/>
 * 出参：response:{@link CPResponse}
 */
public class CPMessageGetResult implements CPControllerResult {

    @Override
    public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
        CPMessage message = context.getData(CPNodeMessageKeys.MESSAGE_INFO);
        if (message == null) {
            argsError(context);
            return;
        }
        Result result = new Result();
        result.setMid(message.getId());
        result.setUid(message.getUid());
        result.setCid(message.getCid());
        result.setDomain(message.getDomain());
        result.setData(message.getData());
        result.setSendTime(TimeUtil.LocalDateTimeToMillis(message.getSendTime()));
        context.setData(CPNodeCommonKeys.RESPONSE,
                CPResponse.success().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result {
        private long mid;
        private long uid;
        private long cid;
        private String domain;
        private Object data;
        private long sendTime;
    }
}

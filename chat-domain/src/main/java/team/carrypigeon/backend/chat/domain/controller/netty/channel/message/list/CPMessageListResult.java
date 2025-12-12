package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeMessageKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 获取消息列表结果
 */
public class CPMessageListResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        CPMessage[] messages = context.getData(CPNodeMessageKeys.MESSAGE_LIST);
        if (messages == null) {
            argsError(context);
            return;
        }
        CPMessageListResultItem[] items = new CPMessageListResultItem[messages.length];
        for (int i = 0; i < messages.length; i++) {
            CPMessage message = messages[i];
            CPMessageListResultItem item = new CPMessageListResultItem()
                    .setMid(message.getId())
                    .setUid(message.getUid())
                    .setCid(message.getCid())
                    .setDomain(message.getDomain())
                    .setData(message.getData())
                    .setSendTime(TimeUtil.LocalDateTimeToMillis(message.getSendTime()));
            items[i] = item;
        }
        Result result = new Result(messages.length, items);
        context.setData(CPNodeCommonKeys.RESPONSE,
                CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result {
        private int count;
        private CPMessageListResultItem[] messages;
    }
}

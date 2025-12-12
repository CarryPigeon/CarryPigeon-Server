package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelBanKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;

import java.util.List;

/**
 * 获取频道封禁列表的返回结果
 * @author midreamsheep
 */
public class CPChannelListBanResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        List<CPChannelListBanResultItem> items =
                context.getData(CPNodeChannelBanKeys.CHANNEL_BAN_ITEMS);
        if (items == null) {
            argsError(context);
            return;
        }
        Result result = new Result(items.size(), items.toArray(new CPChannelListBanResultItem[0]));
        context.setData(CPNodeCommonKeys.RESPONSE,
                CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result {
        private int count;
        private CPChannelListBanResultItem[] bans;
    }
}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.create;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

/**
 * 创建频道的返回参数Node<br/>
 * 入参：ChannelInfo:{@link CPChannel}<br/>
 * 出餐；response:{@link CPResponse}
 * @author midreamsheep
 * */
public class CPChannelCreateResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        CPChannel channelInfo = context.getData("ChannelInfo");
        if (channelInfo == null){
            argsError(context);
            return;
        }
        context.setData("response", CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new Result().setCid(channelInfo.getId()))));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    private static class Result{
        private long cid;
    }
}

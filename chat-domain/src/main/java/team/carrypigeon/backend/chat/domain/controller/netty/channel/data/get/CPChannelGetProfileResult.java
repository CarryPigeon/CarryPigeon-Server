package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 获取通道信息结果节点<br/>
 * 入参:ChannelInfo{@link CPChannel}<br/>
 * 出参:response:{@link CPResponse}
 * @author midreamsheep
 * */
public class CPChannelGetProfileResult implements CPControllerResult {

    @Override
    public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
        CPChannel channelInfo = context.getData(CPNodeChannelKeys.CHANNEL_INFO);
        if (channelInfo == null){
            argsError(context);
            return;
        }
        Result result = new Result();
        result.setName(channelInfo.getName())
                .setOwner(channelInfo.getOwner())
                .setBrief(channelInfo.getBrief())
                .setAvatar(channelInfo.getAvatar())
                .setCreateTime(TimeUtil.LocalDateTimeToMillis(channelInfo.getCreateTime()));
        context.setData(CPNodeCommonKeys.RESPONSE,
                CPResponse.success().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    private static class Result{
        private String name;
        private long owner;
        private String brief;
        private long avatar;
        private long createTime;
    }

}

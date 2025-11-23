package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.chat.domain.controller.CPNodeComponent;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 获取通道信息结果节点<br/>
 * 入参:ChannelInfo{@link CPChannel}<br/>
 * 出参:response:{@link CPResponse}
 * @author midreamsheep
 * */
@AllArgsConstructor
public class CPChannelGetProfileResultNode implements CPControllerResult {

    private final ObjectMapper objectMapper;

    @Override
    public void process(CPSession session, DefaultContext context) {
        CPChannel channelInfo = context.getData("ChannelInfo");
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
        context.setData("response",CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result)));
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

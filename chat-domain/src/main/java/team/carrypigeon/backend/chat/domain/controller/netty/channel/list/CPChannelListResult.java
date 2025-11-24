package team.carrypigeon.backend.chat.domain.controller.netty.channel.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 频道列表的返回参数Node<br/>
 * 入参：<br/>
 * channels:Set<CPChannel><br/>
 * 出参：<br/>
 * 1.response:{@link CPResponse}
 * @author midreamsheep
 * */
public class CPChannelListResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        Set<CPChannel> channels = context.getData("channels");
        if (channels == null){
            argsError( context);
            return;
        }
        // 结果链表
        List<CPChannelListResultItem> result = new ArrayList<>(channels.size());
        // 添加响应值
        for (CPChannel channel : channels) {
            CPChannelListResultItem cpChannelListResultItem = new CPChannelListResultItem();
            cpChannelListResultItem.setCid(channel.getId())
                    .setName(channel.getName())
                    .setOwner(channel.getOwner())
                    .setAvatar(channel.getAvatar())
                    .setBrief(channel.getBrief());
            result.add(cpChannelListResultItem);
        }
        // 组合响应
        Result responseData = new Result();
        responseData.setChannels(result.toArray(new CPChannelListResultItem[0]));
        responseData.setCount(result.size());
        context.setData("response",CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(responseData)));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Result {
        private int count;
        private CPChannelListResultItem[] channels;
    }
}

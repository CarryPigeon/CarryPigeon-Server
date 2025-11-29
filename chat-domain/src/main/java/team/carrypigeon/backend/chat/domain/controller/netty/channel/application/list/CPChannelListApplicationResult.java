package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 获取通道申请列表的返回结果<br/>
 * 参数:applications:Set<CPChannelApplication>
 * @author midreamsheep
 */
public class CPChannelListApplicationResult implements CPControllerResult {

    @Override
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        Set<CPChannelApplication> applications = context.getData(
                CPNodeValueKeyBasicConstants.CHANNEL_APPLICATION_INFO_LIST);
        if (applications == null){
            argsError( context);
            return;
        }
        // 结果链表
        List<CPChannelListApplicationResultItem> result = new ArrayList<>(applications.size());
        for (CPChannelApplication application : applications) {
            CPChannelListApplicationResultItem cpChannelListApplicationResultItem = new CPChannelListApplicationResultItem();
            cpChannelListApplicationResultItem.setId(application.getId())
                    .setUid(application.getUid())
                    .setState(application.getState().getValue())
                    .setMsg(application.getMsg())
                    .setApplyTime(TimeUtil.LocalDateTimeToMillis(application.getApplyTime()));
            result.add(cpChannelListApplicationResultItem);
        }
        CPChannelListApplicationResultItem[] items =
                result.toArray(new CPChannelListApplicationResultItem[0]);
        context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                CPResponse.SUCCESS_RESPONSE.copy()
                        .setData(objectMapper.valueToTree(new Result(applications.size(), items))));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result{
        private int count;
        private CPChannelListApplicationResultItem[] applications;
    }
}

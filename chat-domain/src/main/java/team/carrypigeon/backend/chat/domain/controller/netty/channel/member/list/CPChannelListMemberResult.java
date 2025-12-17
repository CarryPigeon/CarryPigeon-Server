package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.chat.domain.flow.CPFlowContext;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeChannelMemberKeys;
import team.carrypigeon.backend.chat.domain.attribute.CPNodeCommonKeys;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 频道成员列表结果节点<br/>
 * 入参:members:Set<CPChannelMember><br/>
 * 出参:response:{@link CPResponse}
 * @author midreamsheep
 * */
public class CPChannelListMemberResult implements CPControllerResult {

    @Override
    public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
        Set<CPChannelMember> members = context.getData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO_LIST);
        if (members == null){
            argsError(context);
            return;
        }
        List<CPChannelListMemberResultItem> memberList = new ArrayList<>(members.size());
        for (CPChannelMember member : members) {
            CPChannelListMemberResultItem cpChannelListMemberResultItem = new CPChannelListMemberResultItem();
            cpChannelListMemberResultItem.setUid(member.getUid())
                    .setName(member.getName())
                    .setAuthority(member.getAuthority().getAuthority())
                    .setJoinTime(TimeUtil.LocalDateTimeToMillis(member.getJoinTime()));
            memberList.add(cpChannelListMemberResultItem);
        }
        context.setData(CPNodeCommonKeys.RESPONSE,
                CPResponse.success().setData(
                        objectMapper.valueToTree(
                                new Result(memberList.size(), memberList.toArray(new CPChannelListMemberResultItem[0]))
                        )));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result{
        private int count;
        private CPChannelListMemberResultItem[] members;
    }
}

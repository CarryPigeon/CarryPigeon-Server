package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerResult;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.chat.domain.cmp.basic.CPNodeValueKeyBasicConstants;
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
    public void process(CPSession session, DefaultContext context, ObjectMapper objectMapper) {
        Set<CPChannelMember> members = context.getData(CPNodeValueKeyBasicConstants.CHANNEL_MEMBER_INFO_LIST);
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
        context.setData(CPNodeValueKeyBasicConstants.RESPONSE,
                CPResponse.SUCCESS_RESPONSE.copy().setData(
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

package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.get;

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

/**
 * 获取单个频道成员信息的结果。<br/>
 * 入参：ChannelMemberInfo:{@link CPChannelMember}<br/>
 * 出参：response:{@link CPResponse}
 */
public class CPChannelGetMemberResult implements CPControllerResult {

    @Override
    public void process(CPSession session, CPFlowContext context, ObjectMapper objectMapper) {
        CPChannelMember member = context.getData(CPNodeChannelMemberKeys.CHANNEL_MEMBER_INFO);
        if (member == null) {
            argsError(context);
            return;
        }
        Result result = new Result();
        result.setUid(member.getUid());
        result.setName(member.getName());
        result.setAuthority(member.getAuthority().getAuthority());
        result.setJoinTime(TimeUtil.LocalDateTimeToMillis(member.getJoinTime()));
        context.setData(CPNodeCommonKeys.RESPONSE,
                CPResponse.success().setData(objectMapper.valueToTree(result)));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Result {
        private long uid;
        private String name;
        private int authority;
        private long joinTime;
    }
}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 获取频道成员列表的接口<br/>
 * 访问url:/core/channel/member/list<br/>
 * 访问参数:{@link CPChannelListMemberVO}<br/>
 * 访问返回:{@link CPChannelListMemberResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag(
        path = "/core/channel/member/list",voClazz = CPChannelListMemberVO.class,resultClazz = CPChannelListMemberResult.class
)
public class CPChannelListMemberController extends CPControllerAbstract<CPChannelListMemberVO> {

    private final ChannelMemberDao channelMemberDao;

    public CPChannelListMemberController(ObjectMapper objectMapper, ChannelMemberDao channelMemberDao) {
        super(objectMapper, CPChannelListMemberVO.class);
        this.channelMemberDao = channelMemberDao;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelListMemberVO data, Map<String, Object> context) {
        // 判断是否是群成员
        CPChannelMember member = channelMemberDao.getMember(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class), data.getCid());
        if (member == null) {
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not a member of this channel");
        }
        context.put("member", member);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelListMemberVO data, Map<String, Object> context) {
        // 获取成员列表
        CPChannelListMemberResult result = new CPChannelListMemberResult();
        CPChannelMember[] allMember = channelMemberDao.getAllMember(data.getCid());
        List<CPChannelListMemberResultItem> items = new ArrayList<>(allMember.length);
        for (CPChannelMember member : allMember) {
            CPChannelListMemberResultItem item = new CPChannelListMemberResultItem();
            item.setUid(member.getUid())
                    .setName(member.getName())
                    .setAuthority(member.getAuthority().getAuthority())
                    .setJoinTime(TimeUtil.LocalDateTimeToMillis(member.getJoinTime()));
            items.add(item);
        }
        result.setCount(items.size());
        result.setMembers(items.toArray(new CPChannelListMemberResultItem[0]));
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result));
    }
}

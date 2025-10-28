package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.unread.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.Map;

/**
 * 获取未读消息列表<br/>
 * url: /core/channel/message/unread/get<br/>
 * <br/>
 * 请求参数:{@link CPMessageGetUnreadVO}<br/>
 * 返回参数:{@link CPMessageGetUnreadResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag("/core/channel/message/unread/get")
public class CPMessageGetUnreadController extends CPControllerAbstract<CPMessageGetUnreadVO> {

    private final ChannelMessageDao channelMessageDao;
    private final ChannelMemberDao channelMemberDao;

    public CPMessageGetUnreadController(ObjectMapper objectMapper, Class<CPMessageGetUnreadVO> clazz, ChannelMessageDao channelMessageDao, ChannelMemberDao channelMemberDao) {
        super(objectMapper, clazz);
        this.channelMessageDao = channelMessageDao;
        this.channelMemberDao = channelMemberDao;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPMessageGetUnreadVO data, Map<String, Object> context) {
        // 检查是否为群聊成员
        long uid = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID,Long.class);
        CPChannelMember member = channelMemberDao.getMember(uid, data.getCid());
        if (member == null){
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not a member of this channel");
        }
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPMessageGetUnreadVO data, Map<String, Object> context) {
        long uid = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID,Long.class);
        CPMessageGetUnreadResult cpMessageGetUnreadResult = new CPMessageGetUnreadResult();
        cpMessageGetUnreadResult.setCount(channelMessageDao.getAfterCount(data.getCid(),uid, TimeUtil.MillisToLocalDateTime(data.getStartTime())));
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(cpMessageGetUnreadResult));
    }
}

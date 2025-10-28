package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 拉取消息列表<br/>
 * url: /core/channel/message/list<br/>
 * 请求参数:{@link CPMessageListVO}<br/>
 * 返回参数:{@link CPMessageListResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag("/core/channel/message/list")
public class CPMessageListController extends CPControllerAbstract<CPMessageListVO> {

    private final ChannelMessageDao channelMessageDao;
    private final ChannelMemberDao channelMemberDao;

    public CPMessageListController(ObjectMapper objectMapper, Class<CPMessageListVO> clazz, ChannelMessageDao channelMessageDao, ChannelMemberDao channelMemberDao) {
        super(objectMapper, clazz);
        this.channelMessageDao = channelMessageDao;
        this.channelMemberDao = channelMemberDao;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPMessageListVO data, Map<String, Object> context) {
        // 检查是否为群聊成员
        long uid = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID,Long.class);
        CPChannelMember member = channelMemberDao.getMember(uid, data.getCid());
        if(member == null){
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not a member of this channel");
        }
        // 检查参数
        if(data.getCount() <= 0 || data.getCount() > 50){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("count must be between 1 and 100");
        }
        context.put("member",member);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPMessageListVO data, Map<String, Object> context) {
        CPMessage[] messages = channelMessageDao.getBefore(data.getCid(), TimeUtil.MillisToLocalDateTime(data.getStartTime()), data.getCount());
        CPMessageListResult result = new CPMessageListResult();
        List<CPMessageListResultItem> items = new ArrayList<>(messages.length);
        for (CPMessage message : messages) {
            CPMessageListResultItem cpMessageListResultItem = new CPMessageListResultItem()
                    .setMid(message.getId())
                    .setUid(message.getUid())
                    .setCid(message.getCid())
                    .setDomain(message.getDomain())
                    .setData(message.getData())
                    .setSendTime(TimeUtil.LocalDateTimeToMillis(message.getSendTime()));
            items.add(cpMessageListResultItem);
        }
        result.setCount(items.size());
        result.setMessages(items.toArray(new CPMessageListResultItem[0]));
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result));
    }
}
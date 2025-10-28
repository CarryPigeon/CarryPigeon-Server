package team.carrypigeon.backend.chat.domain.controller.netty.channel.list;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * 拉取通道的接口<br/>
 * 请求url：/core/channel/list<br/>
 * 请求参数：空<br/>
 * 请求响应：{@link CPChannelListResult}
 * @author midreamsheep
 * */
@CPControllerTag("/core/channel/list")
public class CPChannelListController implements CPController {

    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;
    private final ObjectMapper objectMapper;

    public CPChannelListController(ChannelDao channelDao, ChannelMemberDao channelMemberDao, ObjectMapper objectMapper) {
        this.channelDao = channelDao;
        this.channelMemberDao = channelMemberDao;
        this.objectMapper = objectMapper;
    }

    @Override
    @LoginPermission
    public CPResponse process(CPSession session, JsonNode data) {
        // 获取用户
        long userId = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID,Long.class);
        // 获取固有通道
        CPChannel[] allFixedChannel = channelDao.getAllFixed();
        // 获取成员通道
        CPChannelMember[] allUserChannel = channelMemberDao.getAllMemberByUserId(userId);
        // 结果链表
        List<CPChannelListResultItem> result = new ArrayList<>(allFixedChannel.length+allUserChannel.length);
        // 添加响应值
        for (CPChannel channel : allFixedChannel) {
            CPChannelListResultItem cpChannelListResultItem = new CPChannelListResultItem();
            cpChannelListResultItem.setCid(channel.getId())
                    .setName(channel.getName())
                    .setOwner(channel.getOwner())
                    .setAvatar(channel.getAvatar())
                    .setBrief(channel.getBrief());
            result.add(cpChannelListResultItem);
        }
        for (CPChannelMember channelMember : allUserChannel) {
            CPChannel channel = channelDao.getById(channelMember.getCid());
            CPChannelListResultItem cpChannelListResultItem = new CPChannelListResultItem();
            cpChannelListResultItem.setCid(channel.getId())
                    .setName(channel.getName())
                    .setOwner(channel.getOwner())
                    .setAvatar(channel.getAvatar())
                    .setBrief(channel.getBrief());
            result.add(cpChannelListResultItem);
        }
        // 组合响应
        CPChannelListResult cpChannelListResult = new CPChannelListResult();
        cpChannelListResult.setChannels(result.toArray(new CPChannelListResultItem[0]));
        cpChannelListResult.setCount(result.size());
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(cpChannelListResult));
    }
}

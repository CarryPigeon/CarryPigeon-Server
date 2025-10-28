package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.notification.CPNotificationService;

import java.util.HashSet;
import java.util.Set;

/**
 * 更新通道数据的接口<br/>
 * 请求url:/core/channel/profile/update<br/>
 * 请求参数:{@link CPChannelUpdateProfileVO}<br/>
 * 成功返回参数:{@link CPChannelUpdateProfileResult}
 * @author midreamsheep
 * */
@CPControllerTag("/core/channel/profile/update")
public class CPChannelUpdateProfileController implements CPController {

    private final ObjectMapper objectMapper;
    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;
    private final CPNotificationService cpNotificationService;

    public CPChannelUpdateProfileController(ObjectMapper objectMapper, ChannelDao channelDao, ChannelMemberDao channelMemberDao, CPNotificationService cpNotificationService) {
        this.objectMapper = objectMapper;
        this.channelDao = channelDao;
        this.channelMemberDao = channelMemberDao;
        this.cpNotificationService = cpNotificationService;
    }

    @Override
    @LoginPermission
    public CPResponse process(CPSession session, JsonNode data) {
        // 解析数据
        CPChannelUpdateProfileVO cpChannelUpdateProfileVO;
        try {
            cpChannelUpdateProfileVO = objectMapper.treeToValue(data, CPChannelUpdateProfileVO.class);
        } catch (Exception e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 获取通道
        CPChannel channel = channelDao.getById(cpChannelUpdateProfileVO.getCid());
        if (channel == null) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("channel not found");
        }

        // 权限校验
        if(channel.getOwner() != session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class)){
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not the owner of this channel");
        }

        // 更新数据
        channel.setName(cpChannelUpdateProfileVO.getName())
                .setBrief(cpChannelUpdateProfileVO.getBrief())
                .setAvatar(cpChannelUpdateProfileVO.getAvatar())
                .setOwner(cpChannelUpdateProfileVO.getOwner());
        if(!channelDao.save(channel)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving channel");
        }

        // 通知所有群成员
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember cpChannelMember : channelMemberDao.getAllMember(cpChannelUpdateProfileVO.getCid())) {
            if (cpChannelMember.getUid() != channel.getOwner()){
                uids.add(cpChannelMember.getUid());
            }
        }

        // 发送通知
        CPNotification notification = new CPNotification().setRoute("/core/channel/profile/get");
        cpNotificationService.sendNotification(uids, notification);

        // 返回成功
        return CPResponse.SUCCESS_RESPONSE.copy();
    }
}

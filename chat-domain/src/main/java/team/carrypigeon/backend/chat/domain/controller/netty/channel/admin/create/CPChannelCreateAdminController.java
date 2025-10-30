package team.carrypigeon.backend.chat.domain.controller.netty.channel.admin.create;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.notification.CPNotificationService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 创建管理员<br/>
 * 请求url:/core/channel/admin/create<br/>
 * 请求参数:{@link CPChannelCreateAdminVO}<br/>
 * 成功返回参数:{@link CPChannelCreateAdminResult}<br/>
 * @author midreamsheep
 * */
@CPControllerTag("/core/channel/admin/create")
public class CPChannelCreateAdminController extends CPControllerAbstract<CPChannelCreateAdminVO> {

    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;
    private final CPNotificationService cpNotificationService;

    public CPChannelCreateAdminController(ObjectMapper objectMapper, ChannelDao channelDao, ChannelMemberDao channelMemberDao, CPNotificationService cpNotificationService) {
        super(objectMapper,CPChannelCreateAdminVO.class);
        this.channelDao = channelDao;
        this.channelMemberDao = channelMemberDao;
        this.cpNotificationService = cpNotificationService;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelCreateAdminVO data, Map<String, Object> context) {
        // 获取频道
        CPChannel cpChannel = channelDao.getById(data.getCid());
        if (cpChannel == null) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("channel not found");
        }

        // 判断是否为频道的创建者
        if (!(cpChannel.getOwner() == (session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class)))) {
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not the owner of this channel");
        }

        // 获取用户
        CPChannelMember member = channelMemberDao.getMember(data.getUid(), data.getCid());
        if (member == null) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("user not found");
        }

        context.put("member", member);
        context.put("cpChannel", cpChannel);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelCreateAdminVO data, Map<String, Object> context) {
        CPChannelMember member = (CPChannelMember) context.get("member");
        // 创建管理员
        member.setAuthority(CPChannelMemberAuthorityEnum.ADMIN);
        if (!channelMemberDao.save(member)) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving member");
        }
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new CPChannelCreateAdminResult()));
    }

    @Override
    protected void notify(CPSession session, CPChannelCreateAdminVO data, Map<String, Object> context) {
        CPChannel cpChannel = (CPChannel) context.get("cpChannel");
        // 通知所有人
        CPNotification notification = new CPNotification().setRoute("/core/channel/member/list");
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember cpChannelMember : channelMemberDao.getAllMember(cpChannel.getId())) {
            if (cpChannelMember.getUid()!=cpChannel.getOwner()){
                uids.add(cpChannelMember.getUid());
            }
        }
        cpNotificationService.sendNotification(uids, notification);
    }
}

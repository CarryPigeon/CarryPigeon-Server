package team.carrypigeon.backend.chat.domain.controller.netty.channel.member.delete;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.notification.CPNotificationService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 删除频道成员接口<br/>
 * url: /core/channel/member/delete <br/>
 * 请求参数:{@link CPChannelDeleteMemberVO}<br/>
 * 返回参数:{@link CPChannelDeleteMemberResult}
 * @author midreamsheep
 * */
@CPControllerTag("/core/channel/member/delete")
public class CPChannelDeleteMemberController extends CPControllerAbstract<CPChannelDeleteMemberVO> {

    private final ChannelMemberDao channelMemberDao;

    private final CPNotificationService notificationService;

    public CPChannelDeleteMemberController(ObjectMapper objectMapper, Class<CPChannelDeleteMemberVO> clazz, ChannelMemberDao channelMemberDao, CPNotificationService notificationService) {
        super(objectMapper, clazz);
        this.channelMemberDao = channelMemberDao;
        this.notificationService = notificationService;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelDeleteMemberVO data, Map<String, Object> context) {
        // 判断是否是管理员
        long requesterId = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        // 获取用户的成员信息
        CPChannelMember member = channelMemberDao.getMember(requesterId, data.getCid());
        // 判断是否为成员且为管理员
        if (member == null || member.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN) {
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not a member of this channel or you are not an admin");
        }
        // 判断被删除成员
        CPChannelMember memberToDelete = channelMemberDao.getMember(data.getUid(), data.getCid());
        if (memberToDelete == null|| memberToDelete.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("member to delete not found");
        }
        // 上下文注册
        context.put("memberToDelete", memberToDelete);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelDeleteMemberVO data, Map<String, Object> context) {
        CPChannelMember memberToDelete = (CPChannelMember) context.get("memberToDelete");
        if (!channelMemberDao.delete(memberToDelete)) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("delete member failed");
        }
        return CPResponse.SUCCESS_RESPONSE.copy();
    }

    @Override
    protected void notify(CPSession session, CPChannelDeleteMemberVO vo, Map<String, Object> context) {
        // 获取通道成员列表
        CPChannelMember[] members = channelMemberDao.getAllMember(vo.getCid());
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember member : members) {
            uids.add(member.getUid());
        }
        uids.remove(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class));
        CPNotification notification = new CPNotification().setRoute("/core/channel/member/list");
        notificationService.sendNotification(uids, notification);
    }
}
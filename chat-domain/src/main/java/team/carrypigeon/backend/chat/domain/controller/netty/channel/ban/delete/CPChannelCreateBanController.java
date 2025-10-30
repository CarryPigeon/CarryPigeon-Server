package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.delete;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.notification.CPNotificationService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 创建频道的封禁<br/>
 * 请求url:/core/channel/ban/delete<br/>
 * 请求参数:{@link CPChannelDeleteBanVO}<br/>
 * 响应参数:{@link CPChannelDeleteBanResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag("/core/channel/ban/delete")
public class CPChannelCreateBanController extends CPControllerAbstract<CPChannelDeleteBanVO> {

    private final ChannelBanDAO channelBanDAO;
    private final ChannelMemberDao channelMemberDao;
    private final CPNotificationService cpNotificationService;

    public CPChannelCreateBanController(ObjectMapper objectMapper, ChannelBanDAO channelBanDAO, ChannelMemberDao channelMemberDao, CPNotificationService cpNotificationService) {
        super(objectMapper,CPChannelDeleteBanVO.class);
        this.channelBanDAO = channelBanDAO;
        this.channelMemberDao = channelMemberDao;
        this.cpNotificationService = cpNotificationService;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelDeleteBanVO data, Map<String, Object> context) {
        // 判断是否为成员且为管理员
        CPChannelMember member = channelMemberDao.getMember(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class), data.getCid());
        if (member == null || member.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN) {
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not a member of this channel or you are not an admin");
        }
        // 判断禁言表是否存在
        CPChannelBan ban = channelBanDAO.getByChannelIdAndUserId( data.getUid(),data.getCid());
        if (ban == null) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("this user is not banned");
        }
        context.put("ban", ban);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelDeleteBanVO data, Map<String, Object> context) {
        CPChannelBan byChannelIdAndUserId = (CPChannelBan) context.get("ban");
        // 删除禁言
        if (!channelBanDAO.delete(byChannelIdAndUserId)) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error deleting channel ban");
        }
        return CPResponse.SUCCESS_RESPONSE.copy();
    }

    @Override
    protected void notify(CPSession session, CPChannelDeleteBanVO data, Map<String, Object> context) {
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember cpChannelMember : channelMemberDao.getAllMember(data.getCid())) {
            uids.add(cpChannelMember.getUid());
        }
        uids.remove(data.getUid());
        // 构建通知
        CPNotification notification = new CPNotification().setRoute("/core/channel/ban/list");
        cpNotificationService.sendNotification(uids, notification);
    }
}

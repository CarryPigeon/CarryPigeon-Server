package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.create;

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
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 创建频道的封禁<br/>
 * 请求url:/core/channel/ban/create<br/>
 * 请求参数:{@link CPChannelCreateBanVO}<br/>
 * 响应参数:{@link CPChannelCreateBanResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag("/core/channel/ban/create")
public class CPChannelCreateBanController extends CPControllerAbstract<CPChannelCreateBanVO> {

    private final ChannelBanDAO channelBanDAO;
    private final ChannelMemberDao channelMemberDao;
    private final CPNotificationService cpNotificationService;

    public CPChannelCreateBanController(ObjectMapper objectMapper, ChannelBanDAO channelBanDAO, ChannelMemberDao channelMemberDao, CPNotificationService cpNotificationService) {
        super(objectMapper, CPChannelCreateBanVO.class);
        this.channelBanDAO = channelBanDAO;
        this.channelMemberDao = channelMemberDao;
        this.cpNotificationService = cpNotificationService;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelCreateBanVO data, Map<String, Object> context) {
        // 判断是否为成员且为管理员
        CPChannelMember member = channelMemberDao.getMember(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class), data.getCid());
        if (member == null || member.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN) {
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not a member of this channel or you are not an admin");
        }
        // 判断被禁言用户是否为成员且为非管理员
        CPChannelMember targetMember = channelMemberDao.getMember(data.getUid(), data.getCid());
        if (targetMember == null || targetMember.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("user is not a member of this channel or is an admin");
        }
        context.put("member", member);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelCreateBanVO data, Map<String, Object> context) {
        CPChannelMember member = (CPChannelMember) context.get("member");
        //判断禁言表中是否已存在
        CPChannelBan cpChannelBan = channelBanDAO.getByChannelIdAndUserId(data.getCid(), data.getUid());
        // 没有则创建禁言
        if (cpChannelBan == null) {
            cpChannelBan = new CPChannelBan();
            cpChannelBan.setId(IdUtil.generateId())
                    .setCid(data.getCid())
                    .setUid(data.getUid());
        }

        // 设置禁言信息
        cpChannelBan.setAid(member.getUid())
                .setCreateTime(TimeUtil.getCurrentLocalTime())
                .setDuration(data.getDuration());

        // 保存禁言
        if (!channelBanDAO.save(cpChannelBan)) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving channel ban");
        }
        return CPResponse.SUCCESS_RESPONSE.copy();
    }

    @Override
    protected void notify(CPSession session, CPChannelCreateBanVO data, Map<String, Object> context) {
        CPChannelMember member = (CPChannelMember) context.get("member");
        // 通知频道成员
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember cpChannelMember : channelMemberDao.getAllMember(member.getCid())) {
            if (cpChannelMember.getUid()!=member.getUid()){
                uids.add(cpChannelMember.getUid());
            }
        }

        // 构建通知
        CPNotification notification = new CPNotification().setRoute("/core/channel/ban/list");
        cpNotificationService.sendNotification(uids, notification);

    }
}

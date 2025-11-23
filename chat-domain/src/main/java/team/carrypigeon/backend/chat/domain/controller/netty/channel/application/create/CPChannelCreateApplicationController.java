package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.create;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
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
 * 创建频道申请<br/>
 * url: /core/channel/application/create <br/>
 * 参数: {@link CPChannelCreateApplicationVO} <br/>
 * 返回参数: {@link CPChannelCreateApplicationResult}
 * @author midreamsheep
 * */
@CPControllerTag(
        path = "/core/channel/application/create",
        voClazz = CPChannelCreateApplicationVO.class,
        resultClazz = CPChannelCreateApplicationResult.class
)
public class CPChannelCreateApplicationController extends CPControllerAbstract<CPChannelCreateApplicationVO> {

    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;
    private final ChannelApplicationDAO channelApplicationDAO;
    private final CPNotificationService cpNotificationService;

    public CPChannelCreateApplicationController(ObjectMapper objectMapper, ChannelDao channelDao, ChannelMemberDao channelMemberDao, ChannelApplicationDAO channelApplicationDAO, CPNotificationService cpNotificationService) {
        super(objectMapper,CPChannelCreateApplicationVO.class);
        this.channelDao = channelDao;
        this.channelMemberDao = channelMemberDao;
        this.channelApplicationDAO = channelApplicationDAO;
        this.cpNotificationService = cpNotificationService;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelCreateApplicationVO data, Map<String, Object> context) {
        // 判断通道是否存在且不为固有通道
        CPChannel cpChannel = channelDao.getById(data.getCid());
        if (cpChannel == null || cpChannel.getOwner()==-1){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("channel not found");
        }
        // 获取当前用户id
        long uid = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        // 判断用户是否已经在频道中
        CPChannelMember member = channelMemberDao.getMember(uid, cpChannel.getId());
        if (member != null){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("user already in channel");
        }
        // 放入上下文
        context.put("uid", uid);
        context.put("cpChannel", cpChannel);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelCreateApplicationVO data, Map<String, Object> context) {
        long uid = (Long)context.get("uid");
        CPChannel cpChannel = (CPChannel) context.get("cpChannel");
        // 判断用户是否已经创建过申请
        CPChannelApplication application = channelApplicationDAO.getByUidAndCid(uid, cpChannel.getId());
        if (application == null){
            application = new CPChannelApplication();
            application.setCid(cpChannel.getId())
                    .setUid(uid)
                    .setId(IdUtil.generateId());
        }
        // 设置数据
        application.setState(CPChannelApplicationStateEnum.PENDING)
                .setApplyTime(TimeUtil.getCurrentLocalTime())
                .setMsg(data.getMsg());
        // 保存申请
        if (!channelApplicationDAO.save(application)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("save application failed");
        }
        return CPResponse.SUCCESS_RESPONSE.copy().setTextData("create application success");
    }

    @Override
    protected void notify(CPSession session, CPChannelCreateApplicationVO data, Map<String, Object> context) {
        CPChannel cpChannel = (CPChannel) context.get("cpChannel");
        // 通知所有管理员
        Set<Long> admins = new HashSet<>();
        for (CPChannelMember channelMember : channelMemberDao.getAllMemberByUserId(cpChannel.getId())) {
            if (channelMember.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN){
                admins.add(channelMember.getUid());
            }
        }
        // 通知
        CPNotification notification = new CPNotification().setRoute("/core/channel/application/list");
        cpNotificationService.sendNotification(admins,notification);
    }
}

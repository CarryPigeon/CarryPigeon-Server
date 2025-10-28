package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplicationStateEnum;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
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
 * 处理申请加入的接口<br/>
 * url: /core/channel/application/process <br/>
 * 请求参数:{@link CPChannelProcessApplicationVO}<br/>
 * 返回参数:{@link CPChannelProcessApplicationResult}
 * @author midreamsheep
 * */
@CPControllerTag("/core/channel/application/process")
public class CPChannelProcessApplicationController extends CPControllerAbstract<CPChannelProcessApplicationVO> {

    private final ChannelMemberDao channelMemberDao;
    private final CPNotificationService cpNotificationService;
    private final ChannelApplicationDAO channelApplicationDAO;

    public CPChannelProcessApplicationController(ObjectMapper objectMapper, ChannelMemberDao channelMemberDao, CPNotificationService cpNotificationService, ChannelApplicationDAO channelApplicationDAO) {
        super(objectMapper,CPChannelProcessApplicationVO.class);
        this.channelMemberDao = channelMemberDao;
        this.cpNotificationService = cpNotificationService;
        this.channelApplicationDAO = channelApplicationDAO;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelProcessApplicationVO data, Map<String, Object> context) {
        // 判断result是否合法
        if (data.getResult()==1||data.getResult()==2){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("result is not valid");
        }
        // 获取申请表
        CPChannelApplication application = channelApplicationDAO.getById(data.getAid());
        if (application == null) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("application not found");
        }
        context.put("application", application);
        // 判断是否为管理员
        long requesterId = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID,Long.class);
        CPChannelMember requester = channelMemberDao.getMember(requesterId, application.getCid());
        if (requester == null || requester.getAuthority() != CPChannelMemberAuthorityEnum.ADMIN) {
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not an admin");
        }
        context.put("member",requester);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelProcessApplicationVO data, Map<String, Object> context) {
        // 获取申请表
        CPChannelApplication application = (CPChannelApplication) context.get("application");
        // 处理申请
        application.setState(CPChannelApplicationStateEnum.valueOf(data.getResult()));
        // 保存申请状态
        if (!channelApplicationDAO.save(application)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving application");
        }
        // 如果是拒绝则直接返回
        if (data.getResult()==2){
            return CPResponse.SUCCESS_RESPONSE.copy();
        }
        // 处理新增用户处理
        CPChannelMember newMember = new CPChannelMember();
        newMember.setId(IdUtil.generateId())
                .setCid(application.getCid())
                .setUid(application.getUid())
                .setName("")
                .setJoinTime(TimeUtil.getCurrentLocalTime())
                .setAuthority(CPChannelMemberAuthorityEnum.MEMBER);
        if (!channelMemberDao.save(newMember)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving channel member");
        }
        return CPResponse.SUCCESS_RESPONSE.copy();
    }

    @Override
    protected void notify(CPSession session, CPChannelProcessApplicationVO vo, Map<String, Object> context) {
        // 从上下文获取数据
        CPChannelApplication application = (CPChannelApplication) context.get("application");
        // 通知所有管理员
        Set<Long> admins = new HashSet<>();
        for (CPChannelMember channelMember : channelMemberDao.getAllMember(application.getCid())) {
            if (channelMember.getAuthority() == CPChannelMemberAuthorityEnum.ADMIN) {
                admins.add(channelMember.getUid());
            }
        }
        admins.remove(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID,Long.class));
        CPNotification notification = new CPNotification().setRoute("/core/channel/application/list");
        cpNotificationService.sendNotification(admins, notification);
        // 如果是拒绝则直接返回
        if(vo.getResult()==2){
            return;
        }
        notification = new CPNotification().setRoute("/core/channel/member/list");
        // 通知所有用户
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember channelMember : channelMemberDao.getAllMember(application.getCid())) {
            uids.add(channelMember.getUid());
        }
        cpNotificationService.sendNotification(uids, notification);
    }
}
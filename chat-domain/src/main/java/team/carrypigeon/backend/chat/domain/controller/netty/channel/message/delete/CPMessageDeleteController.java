package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.delete;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.notification.CPMessageNotificationData;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.notification.CPNotificationService;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 删除消息<br/>
 * url:/core/channel/message/delete<br/>
 * 请求参数：{@link CPMessageDeleteVO}<br/>
 * 返回参数：{@link CPMessageDeleteResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag("/core/channel/message/delete")
public class CPMessageDeleteController extends CPControllerAbstract<CPMessageDeleteVO> {

    private final ChannelMemberDao channelMemberDao;
    private final ChannelMessageDao channelMessageDao;
    private final CPNotificationService notificationService;

    public CPMessageDeleteController(ObjectMapper objectMapper, Class<CPMessageDeleteVO> clazz, ChannelMemberDao channelMemberDao, ChannelMessageDao channelMessageDao, CPNotificationService notificationService) {
        super(objectMapper, clazz);
        this.channelMemberDao = channelMemberDao;
        this.channelMessageDao = channelMessageDao;
        this.notificationService = notificationService;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPMessageDeleteVO data, Map<String, Object> context) {
        // 检查是否为消息的发送者
        long uid = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        // 获取消息
        CPMessage message = channelMessageDao.getById(data.getMid());
        if (message==null){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("message not found");
        }
        // 检查是否超过2分钟时间
        if (TimeUtil.getCurrentLocalTime().isAfter(message.getSendTime().plusMinutes(2))){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("message is too old");
        }
        // 获取用户信息
        CPChannelMember member = channelMemberDao.getMember(uid, message.getCid());
        if (member==null){
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("user member not found");
        }
        // 检查权限
        if (!(member.getAuthority()== CPChannelMemberAuthorityEnum.ADMIN) && member.getUid()!=message.getUid()){
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not the sender or an admin");
        }
        context.put("message",message);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPMessageDeleteVO data, Map<String, Object> context) {
        CPMessage message = (CPMessage)context.get("message");
        if (!channelMessageDao.delete(message)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("delete message error");
        }
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new CPMessageDeleteResult()));
    }

    @Override
    protected void notify(CPSession session, CPMessageDeleteVO vo, Map<String, Object> context) {
        CPMessage message = (CPMessage)context.get("message");
        CPNotification notification = new CPNotification();
        notification.setRoute("/core/message");
        CPMessageNotificationData cpMessageNotificationData = new CPMessageNotificationData();
        cpMessageNotificationData.setSContent("delete a message")
                .setUid(message.getUid())
                .setCid(message.getCid())
                .setSendTime(TimeUtil.getCurrentTime());
        notification.setData(objectMapper.valueToTree(cpMessageNotificationData));
        // 获取所有群成员
        CPChannelMember[] allMember = channelMemberDao.getAllMember(message.getCid());
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember member : allMember) {
            uids.add(member.getUid());
        }
        uids.remove(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class));
        // 发送通知
        notificationService.sendNotification(uids,notification);
    }
}

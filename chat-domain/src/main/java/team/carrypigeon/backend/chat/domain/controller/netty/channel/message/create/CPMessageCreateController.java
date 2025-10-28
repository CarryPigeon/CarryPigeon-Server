package team.carrypigeon.backend.chat.domain.controller.netty.channel.message.create;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.message.CPMessage;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.chat.domain.message.CPMessageData;
import team.carrypigeon.backend.api.connection.notification.CPMessageNotificationData;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.api.dao.database.message.ChannelMessageDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.message.CPMessageParserService;
import team.carrypigeon.backend.chat.domain.service.notification.CPNotificationService;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 发送消息<br/>
 * url:/core/channel/message/create<br/>
 * 请求参数：{@link CPMessageCreateVO}<br/>
 * 返回参数：{@link CPMessageCreateResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag("/core/channel/message/create")
public class CPMessageCreateController extends CPControllerAbstract<CPMessageCreateVO> {

    private final ObjectMapper objectMapper;
    private final ChannelMemberDao channelMemberDao;
    private final ChannelBanDAO channelBanDAO;
    private final ChannelMessageDao channelMessageDao;
    private final CPMessageParserService messageParserService;
    private final CPNotificationService notificationService;

    public CPMessageCreateController(ObjectMapper objectMapper, Class<CPMessageCreateVO> clazz, ObjectMapper objectMapper1, ChannelMemberDao channelMemberDao, ChannelBanDAO channelBanDAO, ChannelMessageDao messageDao, CPMessageParserService messageParserService, CPNotificationService notificationService) {
        super(objectMapper, clazz);
        this.objectMapper = objectMapper1;
        this.channelMemberDao = channelMemberDao;
        this.channelBanDAO = channelBanDAO;
        this.channelMessageDao = messageDao;
        this.messageParserService = messageParserService;
        this.notificationService = notificationService;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPMessageCreateVO data, Map<String, Object> context) {
        long uid = session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class);
        // 判断是否是群成员
        CPChannelMember member = channelMemberDao.getMember(uid, data.getCid());
        if (member == null) {
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy();
        }
        // 判断是否被禁言
        CPChannelBan ban = channelBanDAO.getByChannelIdAndUserId(uid, data.getCid());
        if (ban!=null&&ban.isValid()){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("you are banned");
        }
        // 判断消息是否合规
        CPMessageData messageData = messageParserService.parse(data.getType(), data.getData());
        if (messageData==null){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("message type error");
        }
        context.put("messageData",messageData);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPMessageCreateVO data, Map<String, Object> context) {
        // 构建数据
        CPMessage message = new CPMessage();
        message.setId(IdUtil.generateId())
                .setUid(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class))
                .setData(data.getData())
                .setCid(data.getCid())
                .setSendTime(TimeUtil.getCurrentLocalTime())
                .setDomain(data.getType());
        // 保存数据
        if (!channelMessageDao.save(message)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("save message error");
        }
        // 将消息放入上下文中
        context.put("message",message);
        // 返回成功响应
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new CPMessageCreateResult(message.getId())));
    }

    @Override
    protected void notify(CPSession session, CPMessageCreateVO vo, Map<String, Object> context) {
        // 获取已经解析的消息
        CPMessageData messageData = (CPMessageData) context.get("messageData");
        CPMessage message = (CPMessage) context.get("messageData");
        // 获取待通知的成员
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember member : channelMemberDao.getAllMember(vo.getCid())) {
            uids.add(member.getUid());
        }
        // 删除发送者
        uids.remove(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class));
        // 构造通知
        CPNotification notification = new CPNotification();
        notification.setRoute("/core/message");
        CPMessageNotificationData cpMessageNotificationData = new CPMessageNotificationData();
        cpMessageNotificationData.setSContent(messageData.getSContent())
                .setUid(message.getUid())
                .setCid(vo.getCid())
                .setSendTime(TimeUtil.LocalDateTimeToMillis(message.getSendTime()));
        notification.setData(objectMapper.valueToTree(cpMessageNotificationData));
        // 发送消息
        notificationService.sendNotification(uids, notification);
    }
}

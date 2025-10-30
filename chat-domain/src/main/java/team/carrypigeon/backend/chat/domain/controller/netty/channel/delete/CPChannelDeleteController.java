package team.carrypigeon.backend.chat.domain.controller.netty.channel.delete;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
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
 * 删除通道的接口<br/>
 * 请求url:/core/channel/delete<br/>
 * 请求参数:{@link CPChannelDeleteVO}<br/>
 * 成功返回参数:{@link CPChannelDeleteResult}<br/>
 * @author midreamsheep
 * */
@Slf4j
@CPControllerTag("/core/channel/delete")
public class CPChannelDeleteController extends CPControllerAbstract<CPChannelDeleteVO> {

    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;
    private final CPNotificationService cpNotificationService;

    public CPChannelDeleteController(ObjectMapper objectMapper, ChannelDao channelDao, ChannelMemberDao channelMemberDao, CPNotificationService cpNotificationService) {
        super(objectMapper,CPChannelDeleteVO.class);
        this.channelDao = channelDao;
        this.channelMemberDao = channelMemberDao;
        this.cpNotificationService = cpNotificationService;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelDeleteVO data, Map<String, Object> context) {
        // 判断是否为频道的创建者
        CPChannel cpChannel = channelDao.getById(data.getCid());
        if (!(cpChannel.getOwner() == (session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class)))) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("you are not the owner of this channel");
        }
        context.put("channel", cpChannel);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelDeleteVO data, Map<String, Object> context) {
        CPChannel channel = (CPChannel) context.get("channel");
        // 删除频道
        if (!channelDao.delete(channel)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error deleting channel");
        }
        // 删除频道成员表
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember cpChannelMember : channelMemberDao.getAllMember(channel.getId())) {
            if (cpChannelMember.getUid()!=channel.getOwner()){
                uids.add(cpChannelMember.getUid());
            }
            if (channelMemberDao.delete(cpChannelMember)) {
                log.error("error deleting channel member,user id:{},channel id:{},channel member id:{}", cpChannelMember.getUid(), cpChannelMember.getCid(), cpChannelMember.getId());
            }
        }
        context.put("uids", uids);
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new CPChannelDeleteResult()));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void notify(CPSession session, CPChannelDeleteVO data, Map<String, Object> context) {
        Set<Long> uids = (Set<Long>) context.get("uids");
        // 构建通知
        CPNotification notification = new CPNotification().setRoute("/core/channel/list");
        cpNotificationService.sendNotification(uids, notification);
    }
}

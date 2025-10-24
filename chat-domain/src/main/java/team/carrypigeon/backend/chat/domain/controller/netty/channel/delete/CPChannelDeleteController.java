package team.carrypigeon.backend.chat.domain.controller.netty.channel.delete;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.notification.CPNotification;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.chat.domain.service.notification.CPNotificationService;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.HashSet;
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
public class CPChannelDeleteController implements CPController {

    private final ObjectMapper objectMapper;

    private final ChannelDao channelDao;

    private final ChannelMemberDao channelMemberDao;

    private final CPNotificationService cpNotificationService;

    public CPChannelDeleteController(ObjectMapper objectMapper, ChannelDao channelDao, ChannelMemberDao channelMemberDao, CPNotificationService cpNotificationService) {
        this.objectMapper = objectMapper;
        this.channelDao = channelDao;
        this.channelMemberDao = channelMemberDao;
        this.cpNotificationService = cpNotificationService;
    }

    @Override
    @LoginPermission
    public CPResponse process(JsonNode data, CPSession session) {
        // 获取参数
        CPChannelDeleteVO vo;
        try {
            vo = objectMapper.treeToValue(data, CPChannelDeleteVO.class);
        } catch (JsonProcessingException e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 判断是否为频道的创建者
        CPChannel cpChannel = channelDao.getById(vo.getCid());
        if (!(cpChannel.getOwner() == (session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class)))) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("you are not the owner of this channel");
        }
        // 删除频道
        if (!channelDao.delete(cpChannel)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error deleting channel");
        }
        // 删除频道成员表
        Set<Long> uids = new HashSet<>();
        for (CPChannelMember cpChannelMember : channelMemberDao.getAllMember(cpChannel.getId())) {
            if (cpChannelMember.getUid()!=cpChannel.getOwner()){
                uids.add(cpChannelMember.getUid());
            }
            if (channelMemberDao.delete(cpChannelMember)) {
                log.error("error deleting channel member,user id:{},channel id:{},channel member id:{}", cpChannelMember.getUid(), cpChannelMember.getCid(), cpChannelMember.getId());
            }
        }

        // 构建通知
        CPNotification notification = new CPNotification().setRoute("/core/channel/list");
        cpNotificationService.sendNotification(uids, notification);

        // 返回成功值
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new CPChannelDeleteResult()));

    }
}

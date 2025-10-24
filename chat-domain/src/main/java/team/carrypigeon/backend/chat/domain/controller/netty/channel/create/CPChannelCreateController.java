package team.carrypigeon.backend.chat.domain.controller.netty.channel.create;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.id.IdUtil;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 创建通道的接口<br/>
 * 请求url:/core/channel/create<br/>
 * 请求参数:{@link CPChannelCreateVO}<br/>
 * 成功返回参数:{@link CPChannelCreateResult}<br/>
 * @author midreamsheep
 * */
@CPControllerTag("/core/channel/create")
public class CPChannelCreateController implements CPController {

    private final ObjectMapper objectMapper;

    private final ChannelDao channelDao;

    private final ChannelMemberDao channelMemberDao;

    public CPChannelCreateController(ObjectMapper objectMapper, ChannelDao channelDao, ChannelMemberDao channelMemberDao) {
        this.objectMapper = objectMapper;
        this.channelDao = channelDao;
        this.channelMemberDao = channelMemberDao;
    }

    @Override
    @LoginPermission
    public CPResponse process(JsonNode data, CPSession session) {
        //TODO 读取配置文件查看是否允许创建私有通道

        // 获取参数
        CPChannelCreateVO vo;
        try {
            vo = objectMapper.treeToValue(data, CPChannelCreateVO.class);
        } catch (JsonProcessingException e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }

        // 创建通道
        CPChannel cpChannel = new CPChannel();
        cpChannel.setId(IdUtil.generateId())
                .setName(IdUtil.generateId()+"")
                .setOwner(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class))
                .setBrief("")
                .setCreateTime(TimeUtil.getCurrentLocalTime())
                .setAvatar(-1);
        if (!channelDao.save(cpChannel)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving channel");
        }

        // 创建用户表
        CPChannelMember cpChannelMember = new CPChannelMember();
        cpChannelMember.setId(IdUtil.generateId())
                .setUid(cpChannel.getOwner())
                .setCid(cpChannel.getId())
                .setAuthority(CPChannelMemberAuthorityEnum.ADMIN)
                .setJoinTime(TimeUtil.getCurrentLocalTime())
                .setName("");
        if (!channelMemberDao.save(cpChannelMember)){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error saving user");
        }
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(new CPChannelCreateResult(cpChannel.getId())));

    }
}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.ban.list;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.ban.CPChannelBan;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ban.ChannelBanDAO;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 获取频道的封禁列表<br/>
 * 请求url:/core/channel/ban/list<br/>
 * 请求参数:{@link CPChannelListBanVO}<br/>
 * 响应参数:{@link CPChannelListBanResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag("/core/channel/ban/list")
public class CPChannelListBanController implements CPController {

    private final ObjectMapper objectMapper;
    private final ChannelMemberDao channelMemberDao;
    private final ChannelBanDAO channelBanDAO;

    public CPChannelListBanController(ObjectMapper objectMapper, ChannelMemberDao channelMemberDao, ChannelBanDAO channelBanDAO) {
        this.objectMapper = objectMapper;
        this.channelMemberDao = channelMemberDao;
        this.channelBanDAO = channelBanDAO;
    }

    @Override
    @LoginPermission
    public CPResponse process(CPSession session, JsonNode data) {
        // 解析数据
        CPChannelListBanVO cpChannelListBanVO;
        try {
            cpChannelListBanVO = objectMapper.treeToValue(data, CPChannelListBanVO.class);
        } catch (Exception e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 校验用户是否属于该群组
        CPChannelMember member = channelMemberDao.getMember(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class), cpChannelListBanVO.getCid());
        if (member == null) {
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not a member of this channel");
        }
        // 获取封禁列表
        List<CPChannelListBanResultItem> items = new LinkedList<>();
        for (CPChannelBan cpChannelBan : channelBanDAO.getByChannelId(cpChannelListBanVO.getCid())) {
            CPChannelListBanResultItem cpChannelListBanResultItem = new CPChannelListBanResultItem();
            cpChannelListBanResultItem.setUid(cpChannelBan.getUid());
            cpChannelListBanResultItem.setAid(cpChannelBan.getAid());
            cpChannelListBanResultItem.setBanTime(TimeUtil.LocalDateTimeToMillis(cpChannelBan.getCreateTime()));
            cpChannelListBanResultItem.setDuration(cpChannelBan.getDuration());
            // 判断ban是否有效
            if (TimeUtil.getCurrentTime() - cpChannelListBanResultItem.getBanTime() > cpChannelListBanResultItem.getDuration()) {
                items.add(cpChannelListBanResultItem);
                continue;
            }
            // 删除无效ban
            channelBanDAO.delete(cpChannelBan);
        }

        // 创建返回值
        CPChannelListBanResult cpChannelListBanResult = new CPChannelListBanResult(items.size(), items.toArray(new CPChannelListBanResultItem[0]));
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(cpChannelListBanResult));
    }
}

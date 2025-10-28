package team.carrypigeon.backend.chat.domain.controller.netty.channel.application.list;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.bo.domain.channel.application.CPChannelApplication;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMember;
import team.carrypigeon.backend.api.bo.domain.channel.member.CPChannelMemberAuthorityEnum;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.api.dao.database.channel.application.ChannelApplicationDAO;
import team.carrypigeon.backend.api.dao.database.channel.member.ChannelMemberDao;
import team.carrypigeon.backend.chat.domain.attribute.CPChatDomainAttributes;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 获取通道申请列表的接口<br/>
 * 访问url:/core/channel/application/list<br/>
 * 访问参数:{@link CPChannelListApplicationVO}<br/>
 * 访问返回:{@link CPChannelListApplicationResult}<br/>
 * @author midreamsheep
 */
@CPControllerTag("/core/channel/application/list")
public class CPChannelListApplicationController extends CPControllerAbstract<CPChannelListApplicationVO> {

    private final ChannelApplicationDAO channelApplicationDAO;
    private final ChannelDao channelDao;
    private final ChannelMemberDao channelMemberDao;

    public CPChannelListApplicationController(ObjectMapper objectMapper, ChannelApplicationDAO channelApplicationDAO, ChannelDao channelDao, ChannelMemberDao channelMemberDao) {
        super(objectMapper, CPChannelListApplicationVO.class);
        this.channelApplicationDAO = channelApplicationDAO;
        this.channelDao = channelDao;
        this.channelMemberDao = channelMemberDao;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelListApplicationVO data, Map<String, Object> context) {
        // 校验参数数量是否在合理范围内
        if (data.getPage() < 0 || data.getPageSize() < 0||data.getPageSize()>50){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error args");
        }
        // 校验通道是否存在
        CPChannel channel = channelDao.getById(data.getCid());
        if (channel==null){
            return CPResponse.ERROR_RESPONSE.copy().setTextData("channel not exist");
        }
        // 判断是否为频道管理员
        CPChannelMember member = channelMemberDao.getMember(session.getAttributeValue(CPChatDomainAttributes.CHAT_DOMAIN_USER_ID, Long.class), data.getCid());
        if (member==null||member.getAuthority()!= CPChannelMemberAuthorityEnum.ADMIN){
            return CPResponse.AUTHORITY_ERROR_RESPONSE.copy().setTextData("you are not a member of this channel or you are not an admin");
        }
        // 将数据保存到上下文
        context.put("member",member);
        context.put("channel",channel);
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelListApplicationVO data, Map<String, Object> context) {
        // 获取申请列表
        CPChannelApplication[] applications = channelApplicationDAO.getByCid(data.getCid(), data.getPage(), data.getPageSize());
        // 封装数据
        CPChannelListApplicationResult result = new CPChannelListApplicationResult();
        List<CPChannelListApplicationResultItem> items = new ArrayList<>(applications.length);
        for (CPChannelApplication application : applications) {
            CPChannelListApplicationResultItem item = new CPChannelListApplicationResultItem();
            item.setId(application.getId())
                    .setUid(application.getUid())
                    .setState(application.getState().getValue())
                    .setMsg(application.getMsg())
                    .setApplyTime(TimeUtil.LocalDateTimeToMillis(application.getApplyTime()));
            items.add(item);
        }
        result.setApplications(items.toArray(CPChannelListApplicationResultItem[]::new));
        result.setCount(applications.length);
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(result));
    }
}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerAbstract;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.time.TimeUtil;

import java.util.Map;

/**
 * 删除通道的接口<br/>
 * 请求url:/core/channel/profile/get<br/>
 * 请求参数:{@link CPChannelGetProfileVO}<br/>
 * 成功返回参数:{@link CPChannelGetProfileResult}<br/>
 * @author midreamsheep
 * */
@CPControllerTag("/core/channel/profile/get")
public class CPChannelGetProfileController extends CPControllerAbstract<CPChannelGetProfileVO> {

    private final ChannelDao channelDao;

    public CPChannelGetProfileController(ObjectMapper objectMapper, ChannelDao channelDao) {
        super(objectMapper,CPChannelGetProfileVO.class);
        this.channelDao = channelDao;
    }

    @Override
    @LoginPermission
    protected CPResponse check(CPSession session, CPChannelGetProfileVO data, Map<String, Object> context) {
        return null;
    }

    @Override
    protected CPResponse process0(CPSession session, CPChannelGetProfileVO data, Map<String, Object> context) {
        // 获取数据
        CPChannel channel = channelDao.getById(data.getCid());
        // 封装数据
        CPChannelGetProfileResult cpChannelGetProfileResult = new CPChannelGetProfileResult();
        cpChannelGetProfileResult.setName(channel.getName())
                .setOwner(channel.getOwner())
                .setAvatar(channel.getAvatar())
                .setBrief(channel.getBrief())
                .setCreateTime(TimeUtil.LocalDateTimeToMillis(channel.getCreateTime()));
        return CPResponse.SUCCESS_RESPONSE.copy().setData(objectMapper.valueToTree(cpChannelGetProfileResult));

    }
}

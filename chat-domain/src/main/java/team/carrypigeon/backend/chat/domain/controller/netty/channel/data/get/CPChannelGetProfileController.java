package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.carrypigeon.backend.api.bo.connection.CPSession;
import team.carrypigeon.backend.api.bo.domain.channel.CPChannel;
import team.carrypigeon.backend.api.chat.domain.controller.CPController;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;
import team.carrypigeon.backend.api.connection.protocol.CPResponse;
import team.carrypigeon.backend.api.dao.database.channel.ChannelDao;
import team.carrypigeon.backend.chat.domain.permission.login.LoginPermission;
import team.carrypigeon.backend.common.time.TimeUtil;

/**
 * 删除通道的接口<br/>
 * 请求url:/core/channel/profile/get<br/>
 * 请求参数:{@link CPChannelGetProfileVO}<br/>
 * 成功返回参数:{@link CPChannelGetProfileResult}<br/>
 * @author midreamsheep
 * */
@CPControllerTag("/core/channel/profile/get")
public class CPChannelGetProfileController implements CPController {

    private final ObjectMapper objectMapper;

    private final ChannelDao channelDao;

    public CPChannelGetProfileController(ObjectMapper objectMapper, ChannelDao channelDao) {
        this.objectMapper = objectMapper;
        this.channelDao = channelDao;
    }

    @Override
    @LoginPermission
    public CPResponse process(CPSession session, JsonNode data) {
        // 解析数据
        CPChannelGetProfileVO cpChannelGetProfileVO;
        try {
            cpChannelGetProfileVO = objectMapper.treeToValue(data, CPChannelGetProfileVO.class);
        } catch (Exception e) {
            return CPResponse.ERROR_RESPONSE.copy().setTextData("error parsing request data");
        }
        // 获取数据
        CPChannel channel = channelDao.getById(cpChannelGetProfileVO.getCid());
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

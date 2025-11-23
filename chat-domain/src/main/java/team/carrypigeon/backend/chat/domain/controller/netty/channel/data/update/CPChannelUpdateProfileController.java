package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.update;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 更新通道数据的接口<br/>
 * 请求url:/core/channel/profile/update<br/>
 * 请求参数:{@link CPChannelUpdateProfileVO}<br/>
 * 成功返回参数:无
 * @author midreamsheep
 * */
@CPControllerTag(
        path = "/core/channel/profile/update", voClazz = CPChannelUpdateProfileVO.class, resultClazz = CPControllerDefaultResult.class
)
public class CPChannelUpdateProfileController {}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.data.get;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 删除通道的接口<br/>
 * 请求url:/core/channel/profile/get<br/>
 * 请求参数:{@link CPChannelGetProfileVO}<br/>
 * 成功返回参数:{@link CPChannelGetProfileResult}<br/>
 * @author midreamsheep
 * */
@CPControllerTag(
        path = "/core/channel/profile/get", voClazz = CPChannelGetProfileVO.class,resultClazz = CPChannelGetProfileResult.class
)
public class CPChannelGetProfileController {
}

package team.carrypigeon.backend.chat.domain.controller.netty.channel.delete;

import team.carrypigeon.backend.api.chat.domain.controller.CPControllerDefaultResult;
import team.carrypigeon.backend.api.chat.domain.controller.CPControllerTag;

/**
 * 删除通道的接口<br/>
 * 请求url:/core/channel/delete<br/>
 * 请求参数:{@link CPChannelDeleteVO}<br/>
 * 成功返回参数:无<br/>
 * @author midreamsheep
 * */
@CPControllerTag(
        path = "/core/channel/delete", voClazz = CPChannelDeleteVO.class, resultClazz = CPControllerDefaultResult.class
)
public class CPChannelDeleteController {
}
